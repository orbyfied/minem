package com.orbyfied.minem.player;

import com.orbyfied.minem.ClientComponent;
import com.orbyfied.minem.MinecraftClient;
import com.orbyfied.minem.client.ClientTickHandler;
import com.orbyfied.minem.model.entity.Entity;
import com.orbyfied.minem.event.Chain;
import com.orbyfied.minem.listener.IncomingPacketListener;
import com.orbyfied.minem.listener.SubscribePacket;
import com.orbyfied.minem.math.MinecraftRotation;
import com.orbyfied.minem.math.Vec2f;
import com.orbyfied.minem.math.Vec3d;
import com.orbyfied.minem.model.entity.LivingEntity;
import com.orbyfied.minem.model.transform.AxisAlignedBB;
import com.orbyfied.minem.model.transform.Transform;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.play.*;
import com.orbyfied.minem.protocol47.adhoc.ClientboundUpdateHealthPacket47;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import static com.orbyfied.minem.protocol.play.ClientboundPositionAndLookPacket.*;

/**
 * Tracks entity information about the client (local) player.
 */
@ToString
@RequiredArgsConstructor
public class LocalPlayer extends ClientComponent implements IncomingPacketListener, LivingEntity {

    static final double BB_WIDTH = 0.6;
    static final double BB_HEIGHT = 1.8;

    public static LocalPlayer create() {
    return new LocalPlayer();
    }

    /* ----------- Player Entity State ----------- */

    int entityID;                 // The entity ID of the player

    Gamemode gamemode;            // The current gamemode of the player
    Dimension dimension;          // The current world/dimension of the player

    Transform transform;          // The current transform of the player
    Vec3d velocity = new Vec3d(); // Current motion or velocity of the player entity

    @Getter float health;
    @Getter int food;
    @Getter float foodSaturation;

    volatile boolean canFly;                // Whether we have the capability to fly
    volatile @Getter boolean flying;        // Whether we are currently in fly mode
    volatile @Getter boolean grounded;      // Whether we are on the ground
    volatile @Getter Boolean forceGrounded; // The value to force grounded to
    volatile @Getter float flySpeed;        // The fly speed set by the server



    Transform lastTickTransform;
    volatile boolean movementCommunicationPaused = false;
    volatile long lastPositionTick = 0;

    /* ------------------------------------------- */

    final Chain<ClientTickHandler> onMovementTick = new Chain<>(ClientTickHandler.class);
    final Chain<TransformInitializedHandler> onPositionInitialized = new Chain<>(TransformInitializedHandler.class);

    @Override
    protected boolean attach(MinecraftClient client) {
        subscribeAllIncomingPackets(client);

        client.onTick().addLast(this::onTick);

        return true;
    }

    /* Shared packets used in the tick loop */
    PacketContainer sharedPacketPlayerGrounded;
    PacketContainer sharedPacketPlayerPosition;

    @Override
    public Transform transform() {
        return transform != null ? transform : (transform = new Transform());
    }

    @Override
    public AxisAlignedBB boundingBox() {
        return AxisAlignedBB.ofCenterBase(transform.position, BB_WIDTH, BB_HEIGHT);
    }

    public Transform lastTransform() {
        return lastTickTransform;
    }

    // called every 50ms
    private void onTick(MinecraftClient client) {
        if (this.transform != null) {
            // update movement (physics, natural player movement)
            onMovementTick.invoker().onTick(client);

            // communicate updated movement if un-paused
            communicateMovement();

            // finally update last transform to record this tick
            this.lastTickTransform = this.transform.copy();
        }
    }

    private void communicateMovement() {
        if (movementCommunicationPaused) {
            this.movementCommunicationPaused = false;
            return;
        }

        // diff last transform and current, send necessary packet
        Transform current = this.transform;
        Transform last = this.lastTickTransform != null ? this.lastTickTransform : this.transform;

        boolean moved = Math.abs(current.x() - last.x()) > 1e-6 ||
                Math.abs(current.y() - last.y()) > 1e-6 ||
                Math.abs(current.z() - last.z()) > 1e-6;

        boolean rotated = current.yaw != last.yaw || current.pitch != last.pitch;

        if (moved && rotated) sendPositionAndLook();
        else if (moved) sendPosition();
        else if (rotated) sendLook();
        else sendGrounded();

        // every 20 ticks
        if (client.tickCount() - lastPositionTick >= 20) {
            if (rotated) sendPositionAndLook();
            else sendPosition();
        }
    }

    @SubscribePacket
    void onJoin(PacketContainer c, ClientboundJoinGamePacket packet) {
        this.entityID = packet.getEntityID();
        this.gamemode = packet.getGamemode();
        this.dimension = packet.getDimension();
        this.transform = null; // yet to be initialized with another packet
    }

    @SubscribePacket
    void onRespawn(PacketContainer c, ClientboundRespawnPacket packet) {
        this.dimension = packet.getDimension();
        this.gamemode = packet.getGamemode();
        this.transform = null; // yet to be initialized with another packet
    }

    @SubscribePacket
    void onPositionAndLook(PacketContainer c, ClientboundPositionAndLookPacket packet) {
        int flags = packet.getFlags();
        Transform transform = this.transform != null ? this.transform : new Transform();

        // atomically update position (must never mutate Transform.position instance, only replace)
        Vec3d position = transform.position().copy();
        if ((flags & FLAG_REL_X) > 0) position.x += packet.getX(); else position.x = packet.getX();
        if ((flags & FLAG_REL_Y) > 0) position.y += packet.getY(); else position.y = packet.getY();
        if ((flags & FLAG_REL_Z) > 0) position.z += packet.getZ(); else position.z = packet.getZ();
        transform.positioned(position);

        // apply rotations
        if ((flags & FLAG_REL_YAW) > 0)   transform.yaw += packet.getYaw();     else transform.yaw = packet.getYaw();
        if ((flags & FLAG_REL_PITCH) > 0) transform.pitch += packet.getPitch(); else transform.pitch = packet.getPitch();

        boolean initializedNow = this.transform == null;
        this.transform = transform;
        if (initializedNow) {
            onPositionInitialized.invoker().onTransformInitialized(this, transform);
        }

        // server-issued teleport, don't do movement this tick
        delayMovement();
    }

    @Override
    protected void resetState() {
        transform = null;
        canFly = false;
        flying = false;
        grounded = false;
    }

    public boolean hasJoined() {
        return dimension != null;
    }

    public boolean isTransformInitialized() {
        return transform != null;
    }

    public LocalPlayer forceGrounded(Boolean forceGrounded) {
        this.forceGrounded = forceGrounded;
        return this;
    }

    public Boolean forceGrounded() {
        return forceGrounded;
    }

    /**
     * Delays movement processing and packets for one tick, used when the
     * server issues a teleport/initialization of position.
     */
    public void delayMovement() {
        // don't send movement packets this tick
        this.movementCommunicationPaused = true;
        this.lastTickTransform = transform.copy();
    }

    /**
     * Move to the given destination synchronously in one packet.
     */
    public void moveInstant(Vec3d destination) {
        this.transform.positioned(destination);
    }

    /**
     * Move to the given destination and look in the given direction synchronously in one packet.
     */
    public void moveAndLookInstant(Vec3d destination, float yaw, float pitch) {
        this.transform.update(destination, yaw, pitch);
    }

    /**
     * Look in the given direction synchronously in one packet.
     */
    public void lookInstant(float yaw, float pitch) {
        this.transform.orient(yaw, pitch);
    }

    public Vec2f getYawAndPitch() {
        return new Vec2f(this.transform.yaw, this.transform.pitch);
    }

    public void setPosition(Vec3d pos) {
        this.transform.positioned(pos);
    }

    public void setYaw(float yaw) {
        this.transform.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.transform.pitch = pitch;
    }

    public void setYawAndPitch(Vec2f vec) {
        this.transform.orient(vec.x, vec.y);
    }

    public void lookAt(Vec3d pos) {
        Vec3d lookVec = pos.sub(this.transform.position()).normalized();
        setYawAndPitch(MinecraftRotation.yawAndPitchFromLookVector(lookVec));
    }

    public void sendPositionAndLook() {
        Vec3d position = this.transform.position;
        client.getConnection().sendSync(client.createPacket(new ServerboundPlayerPositionAndLookPacket(position.x, position.y, position.z, transform.yaw, transform.pitch, effectivelyGrounded())));
        this.lastPositionTick = client.tickCount();
    }

    public void sendPosition() {
        Vec3d position = this.transform.position;
        client.getConnection().sendSync(client.createPacket(new ServerboundPlayerPositionPacket(position.x, position.y, position.z, effectivelyGrounded())));
        this.lastPositionTick = client.tickCount();
    }

    public void sendLook() {
        client.getConnection().sendSync(client.createPacket(new ServerboundPlayerLookPacket(transform.yaw, transform.pitch, effectivelyGrounded())));
    }

    public void sendGrounded() {
        client.getConnection().sendSync(client.createPacket(new ServerboundPlayerGroundedPacket(effectivelyGrounded())));
    }

    public Chain<TransformInitializedHandler> onPositionInitialized() {
        return onPositionInitialized;
    }

    public Chain<ClientTickHandler> onMovementTick() {
        return onMovementTick;
    }

    @Override
    public int getEntityID() {
        return entityID;
    }

    @Override
    public Vec3d velocity() {
        return velocity;
    }

    // Event Handler
    public interface FlyUpdateHandler {
        void onServerFlyUpdate(LocalPlayer localPlayer, Boolean canFlyUpdate, Boolean flyingUpdate, Float flySpeedChange);
    }

    final Chain<FlyUpdateHandler> onFlyUpdate = new Chain<>(FlyUpdateHandler.class);

    @SubscribePacket
    void onAbilitiesPacket(PacketContainer container, ClientboundPlayerAbilitiesPacket packet) {
        Boolean mCanFly = canFly == packet.isCanFly() ? null : packet.isCanFly();
        Boolean mFlying = flying == packet.isFlying() ? null : packet.isFlying();
        Float mFlySpeed = flySpeed == packet.getFlySpeed() ? null : packet.getFlySpeed();
        this.canFly = packet.isCanFly();
        this.flying = packet.isFlying();
        this.flySpeed = packet.getFlySpeed();
        onFlyUpdate.invoker().onServerFlyUpdate(this, mCanFly, mFlying, mFlySpeed);

        if (flying && !canFly) {
            tryFly(false);
        } else if (mFlying != null) {
            tryFly(mFlying);
        }
    }

    /**
     * Enable or disable fly mode.
     *
     * @return Whether it updated successfully.
     */
    public boolean tryFly(boolean b) {
        if (flying == b) {
            return false;
        }

        if (!canFly && b) {
            return false;
        }

        flying = b;
        client.getConnection().sendSync(client.createPacket(new ServerboundPlayerAbilitiesPacket()
                .flying(b)
                .flySpeed(flySpeed)
                .walkSpeed(1)));
        return true;
    }

    public boolean canFly() {
        return canFly;
    }

    public boolean effectivelyGrounded() {
        return forceGrounded != null ? (!flying && forceGrounded) : grounded;
    }

    public Chain<FlyUpdateHandler> onFlyUpdate() {
        return onFlyUpdate;
    }

    // Event Handler
    public interface TransformInitializedHandler {
        void onTransformInitialized(LocalPlayer localPlayer, Transform transform);
    }

    @SubscribePacket
    void onHealthUpdate(PacketContainer c, ClientboundUpdateHealthPacket47 packet) {
        this.health = packet.getHealth();
        this.food = packet.getFood();
        this.foodSaturation = packet.getSaturation();
    }

}
