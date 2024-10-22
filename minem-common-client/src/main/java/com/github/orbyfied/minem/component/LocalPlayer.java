package com.github.orbyfied.minem.component;

import com.github.orbyfied.minem.ClientComponent;
import com.github.orbyfied.minem.MinecraftClient;
import com.github.orbyfied.minem.entity.Entity;
import com.github.orbyfied.minem.event.Chain;
import com.github.orbyfied.minem.listener.IncomingPacketListener;
import com.github.orbyfied.minem.listener.SubscribePacket;
import com.github.orbyfied.minem.math.MinecraftRotation;
import com.github.orbyfied.minem.math.Vec2f;
import com.github.orbyfied.minem.math.Vec3d;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.play.*;
import lombok.ToString;

import java.util.List;

import static com.github.orbyfied.minem.protocol.play.ClientboundPositionAndLookPacket.*;

/**
 * Tracks entity information about the client (local) player.
 */
@ToString
public class LocalPlayer extends ClientComponent implements IncomingPacketListener, Entity {

    int entityID;        // The entity ID of the player
    Vec3d position;      // The current position of the player
    float pitch;         // The current horizontal rotation (pitch) of the player
    float yaw;           // The current vertical rotation (yaw) of the player
    Gamemode gamemode;   // The current gamemode of the player
    Dimension dimension; // The current world/dimension of the player

    volatile boolean canFly;        // Whether we have the capability to fly
    volatile boolean flying;        // Whether we are currently in fly mode
    volatile boolean grounded;      // Whether we are on the ground
    volatile Boolean forceGrounded; // The value to force grounded to
    volatile float flySpeed;        // The fly speed set by the server

    volatile long lastTransformUpdate = Long.MAX_VALUE; // The epochMS time of the last manual or server update to position or look sent to the server

    final Chain<FlyUpdateHandler> onFlyUpdate = new Chain<>(FlyUpdateHandler.class);
    final Chain<PositionInitializedHandler> onPositionInitialized = new Chain<>(PositionInitializedHandler.class);

    // Timings, in ticks, 0 to disable
    // Period = how many ticks per run,
    // Hit = the tick in that period on which the run should hit
    int periodSendPosition = 0;
    int hitSendPosition = 10;
    int periodSendGrounded = 10;
    int hitSendGrounded = 0;

    @Override
    protected boolean attach(MinecraftClient client) {
        subscribeAllIncomingPackets(client);

        client.onTick().addLast(this::onTick);

        return true;
    }

    /* Shared packets used in the tick loop */
    Packet sharedPacketPlayerGrounded;
    Packet sharedPacketPlayerPosition;

    // called every 50ms
    private void onTick(MinecraftClient client) {
        int tick = client.tickCount();
        if (periodSendPosition != 0 && tick % periodSendPosition == hitSendPosition && isPositionInitialized() && System.currentTimeMillis() - lastTransformUpdate >= 1000) {
            // send absolute data packet to server
            if (sharedPacketPlayerPosition == null) sharedPacketPlayerPosition = client.createPacket(new ServerboundPlayerPositionPacket());
            ServerboundPlayerPositionPacket data = sharedPacketPlayerPosition.data();
            data.setX(position.x);
            data.setY(position.y);
            data.setZ(position.z);
            data.setGrounded(effectivelyGrounded());
            client.sendSync(sharedPacketPlayerPosition);
        }

        if (periodSendGrounded != 0 && tick % periodSendGrounded == hitSendGrounded && isPositionInitialized() && System.currentTimeMillis() - lastTransformUpdate >= 250) {
            // send flying/stationary packet to server
            if (sharedPacketPlayerGrounded == null) sharedPacketPlayerGrounded = client.createPacket(new ServerboundPlayerGroundedPacket());
            ServerboundPlayerGroundedPacket data = sharedPacketPlayerGrounded.data();
            data.setGrounded(effectivelyGrounded());
            client.sendSync(sharedPacketPlayerGrounded);
        }
    }

    @SubscribePacket
    void onJoin(Packet c, ClientboundJoinGamePacket packet) {
        this.entityID = packet.getEntityID();
        this.gamemode = packet.getGamemode();
        this.dimension = packet.getDimension();
        this.position = null; // yet to be initialized with another packet
    }

    @SubscribePacket
    void onRespawn(Packet c, ClientboundRespawnPacket packet) {
        this.dimension = packet.getDimension();
        this.gamemode = packet.getGamemode();
        this.position = null; // yet to be initialized with another packet
    }

    @SubscribePacket
    void onPositionAndLook(Packet c, ClientboundPositionAndLookPacket packet) {
        int flags = packet.getFlags();
        Vec3d pos = position != null ? position : new Vec3d();

        if ((flags & FLAG_REL_X) > 0) pos.x += packet.getX(); else pos.x = packet.getX();
        if ((flags & FLAG_REL_Y) > 0) pos.y += packet.getY(); else pos.y = packet.getY();
        if ((flags & FLAG_REL_Z) > 0) pos.z += packet.getZ(); else pos.z = packet.getZ();

        if ((flags & FLAG_REL_YAW) > 0)   yaw += packet.getYaw();     else yaw =   packet.getYaw();
        if ((flags & FLAG_REL_PITCH) > 0) pitch += packet.getPitch(); else pitch = packet.getPitch();

        boolean initialized = this.position == null;
        this.position = pos;
        this.lastTransformUpdate = System.currentTimeMillis();
        if (initialized) {
            onPositionInitialized.invoker().onPositionInitialized(this, pos);
        }
    }

    @Override
    protected void resetState() {
        position = null;
        pitch = 0;
        yaw = 0;
        canFly = false;
        flying = false;
        grounded = false;
        lastTransformUpdate = Long.MAX_VALUE;
    }

    // get or create the position object
    private Vec3d posOrCreate() {
        return position != null ? position : (position = new Vec3d());
    }

    public boolean hasJoined() {
        return dimension != null;
    }

    public boolean isPositionInitialized() {
        return position != null;
    }

    @SubscribePacket
    void onAbilitiesPacket(Packet container, ClientboundPlayerAbilitiesPacket packet) {
        Boolean mCanFly = canFly == packet.isCanFly() ? null : packet.isCanFly();
        Boolean mFlying = flying == packet.isFlying() ? null : packet.isFlying();
        Float mFlySpeed = flySpeed == packet.getFlySpeed() ? null : packet.getFlySpeed();
        this.canFly = packet.isCanFly();
        this.flying = packet.isFlying();
        this.flySpeed = packet.getFlySpeed();
        onFlyUpdate.invoker().onFlyUpdate(this, mFlying, mCanFly, mFlySpeed);
    }

    /**
     * Enable or disable fly mode.
     *
     * @return Whether it updated successfully.
     */
    public boolean fly(boolean b) {
        if (flying == b) {
            return false;
        }

        if (!canFly && !flying && b) {
            return false;
        }

        flying = b;
        client.sendSync(client.createPacket(new ServerboundPlayerAbilitiesPacket()
                .flying(b)
                .flySpeed(flySpeed)
                .walkSpeed(1)));
        onFlyUpdate.invoker().onFlyUpdate(this, b, null, null);
        return true;
    }

    /**
     * Try and get the ground level our bounding box will hit at the given X and Z coordinates.
     */
    public int getGroundLevel(double x, double z) {
        return 0; // todo
    }

    public boolean canFly() {
        return canFly;
    }

    public boolean isFlying() {
        return flying;
    }

    public boolean isGrounded() {
        return grounded;
    }

    public boolean effectivelyGrounded() {
        return forceGrounded != null ? (!flying && forceGrounded) : grounded;
    }

    public Chain<FlyUpdateHandler> onFlyUpdate() {
        return onFlyUpdate;
    }

    public LocalPlayer forceGrounded(Boolean forceGrounded) {
        this.forceGrounded = forceGrounded;
        return this;
    }

    public Boolean forceGrounded() {
        return forceGrounded;
    }

    /**
     * Move to the given destination synchronously in one packet.
     */
    public void moveInstant(Vec3d destination) {
        this.position = destination;
        updatePosition();
    }

    /**
     * Move to the given destination and look in the given direction synchronously in one packet.
     */
    public void moveAndLookInstant(Vec3d destination, float yaw, float pitch) {
        this.position = destination;
        this.yaw = yaw;
        this.pitch = pitch;
        updatePositionAndLook();
    }

    /**
     * Look in the given direction synchronously in one packet.
     */
    public void lookInstant(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        updateLook();
    }

    public void updatePositionAndLook() {
        client.sendSync(client.createPacket(new ServerboundPlayerPositionAndLookPacket(position.x, position.y, position.z, yaw, pitch, effectivelyGrounded())));
        this.lastTransformUpdate = System.currentTimeMillis();
    }

    public void updatePosition() {
        client.sendSync(client.createPacket(new ServerboundPlayerPositionPacket(position.x, position.y, position.z, effectivelyGrounded())));
        this.lastTransformUpdate = System.currentTimeMillis();
    }

    public void updateLook() {
        client.sendSync(client.createPacket(new ServerboundPlayerLookPacket(yaw, pitch, effectivelyGrounded())));
        this.lastTransformUpdate = System.currentTimeMillis();
    }

    public Vec2f getYawAndPitch() {
        return new Vec2f(yaw, pitch);
    }

    public void setPosition(Vec3d pos) {
        this.position = pos;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setYawAndPitch(Vec2f vec) {
        this.yaw = vec.x();
        this.pitch = vec.y();
    }

    public void lookAt(Vec3d pos) {
        Vec3d lookVec = pos.sub(this.position).normalized();
        setYawAndPitch(MinecraftRotation.yawAndPitchFromLookVector(lookVec));
    }

    public Chain<PositionInitializedHandler> onPositionInitialized() {
        return onPositionInitialized;
    }

    @Override
    public int getEntityID() {
        return entityID;
    }

    @Override
    public Vec3d getPosition() {
        return position;
    }

    @Override
    public float getPitch() {
        return pitch;
    }

    @Override
    public float getYaw() {
        return yaw;
    }

    public Gamemode getGamemode() {
        return gamemode;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public float getFlySpeed() {
        return flySpeed;
    }

    // Event Handler
    public interface FlyUpdateHandler {
        void onFlyUpdate(LocalPlayer localPlayer, Boolean canFlyChange, Boolean flyingChange, Float flySpeedChange);
    }

    // Event Handler
    public interface PositionInitializedHandler {
        void onPositionInitialized(LocalPlayer localPlayer, Vec3d position);
    }

    public int periodSendPosition() {
        return periodSendPosition;
    }

    public LocalPlayer periodSendPosition(int periodSendPosition) {
        this.periodSendPosition = periodSendPosition;
        return this;
    }

    public int hitTickSendPosition() {
        return hitSendPosition;
    }

    public LocalPlayer hitTickSendPosition(int hitTickSendPosition) {
        this.hitSendPosition = hitTickSendPosition;
        return this;
    }

    public int periodSendGrounded() {
        return periodSendGrounded;
    }

    public LocalPlayer periodSendGrounded(int periodSendGrounded) {
        this.periodSendGrounded = periodSendGrounded;
        return this;
    }

    public int hitTickSendGrounded() {
        return hitSendGrounded;
    }

    public LocalPlayer hitTickSendGrounded(int hitTickSendGrounded) {
        this.hitSendGrounded = hitTickSendGrounded;
        return this;
    }

}
