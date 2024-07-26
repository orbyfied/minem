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
import lombok.Getter;
import lombok.ToString;

import static com.github.orbyfied.minem.protocol.play.ClientboundPositionAndLookPacket.*;

/**
 * Tracks entity information about the client (local) player.
 */
@Getter
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

    final Chain<FlyUpdateHandler> onFlyUpdate = new Chain<>(FlyUpdateHandler.class);

    @Override
    protected boolean attach(MinecraftClient client) {
        subscribeAllIncomingPackets(client);

        client.onTick().addLast(this::onTick);

        return true;
    }

    // called every 50ms
    private void onTick(MinecraftClient client) {
        int tick = client.tickCount();
        if (tick % 20 == 0 && isPositionInitialized()) {
            // send absolute data packet to server
            client.sendSync(client.createPacket(new ServerboundPlayerPositionPacket(position.x, position.y, position.z, effectivelyGrounded())));
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

        this.position = pos;
//        client.sendSync(client.createPacket(new ServerboundPlayerPositionAndLookPacket(position.x, position.y, position.z, yaw, pitch, effectivelyGrounded())));
    }

    @Override
    protected void resetState() {
        position = null;
        pitch = 0;
        yaw = 0;
        canFly = false;
        flying = false;
        grounded = false;
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
        client.sendSync(client.createPacket(new ServerboundPlayerPositionPacket(destination.x, destination.y, destination.z, effectivelyGrounded())));
    }

    /**
     * Move to the given destination and look in the given direction synchronously in one packet.
     */
    public void moveAndLookInstant(Vec3d destination, float yaw, float pitch) {
        this.position = destination;
        this.yaw = yaw;
        this.pitch = pitch;
        client.sendSync(client.createPacket(new ServerboundPlayerPositionAndLookPacket(destination.x, destination.y, destination.z, yaw, pitch, effectivelyGrounded())));
    }

    /**
     * Look in the given direction synchronously in one packet.
     */
    public void lookInstant(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        client.sendSync(client.createPacket(new ServerboundPlayerLookPacket(yaw, pitch, effectivelyGrounded())));
    }

    public void updatePositionAndLook() {
        client.sendSync(client.createPacket(new ServerboundPlayerPositionAndLookPacket(position.x, position.y, position.z, yaw, pitch, effectivelyGrounded())));
    }

    public void updatePosition() {
        client.sendSync(client.createPacket(new ServerboundPlayerPositionPacket(position.x, position.y, position.z, effectivelyGrounded())));
    }

    public void updateLook() {
        client.sendSync(client.createPacket(new ServerboundPlayerLookPacket(yaw, pitch, effectivelyGrounded())));
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

    // Event Handler
    public interface FlyUpdateHandler {
        void onFlyUpdate(LocalPlayer localPlayer, Boolean canFlyChange, Boolean flyingChange, Float flySpeedChange);
    }

}
