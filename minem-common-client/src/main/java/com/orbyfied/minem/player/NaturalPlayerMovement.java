package com.orbyfied.minem.player;

import com.orbyfied.minem.ClientComponent;
import com.orbyfied.minem.MinecraftClient;
import com.orbyfied.minem.listener.IncomingPacketListener;
import com.orbyfied.minem.listener.SubscribePacket;
import com.orbyfied.minem.math.Vec3d;
import com.orbyfied.minem.model.transform.AxisAlignedBB;
import com.orbyfied.minem.model.transform.Transform;
import com.orbyfied.minem.model.world.WorldPhysicsProvider;
import com.orbyfied.minem.protocol.PacketContainer;
import com.orbyfied.minem.protocol.play.ClientboundEntityVelocityPacket;
import com.orbyfied.minem.protocol47.adhoc.ServerboundClientStatusPacket47;

public class NaturalPlayerMovement extends ClientComponent implements IncomingPacketListener {

    public static NaturalPlayerMovement create() {
        return new NaturalPlayerMovement();
    }

    transient LocalPlayer player;
    transient WorldPhysicsProvider physics = WorldPhysicsProvider.stub();

    private float strafe;   // -1..1 (A/D)
    private float forward;  // -1..1 (S/W)
    private boolean jumping;
    private boolean sneaking;
    private boolean sprinting;

    @Override
    protected boolean attach(MinecraftClient client) {
        subscribeAllIncomingPackets(client);

        player = client.require(LocalPlayer.class);
        player.onMovementTick().addLast(this::tickMovement);

        return super.attach(client);
    }

    @Override
    protected void resetState() {

    }

    private AxisAlignedBB currentBoundingBox;

    // called before movement packets by LocalPlayer to update
    // physics and basic movement
    private void tickMovement(MinecraftClient client) {
        if (player.isTransformInitialized()) {
            // setup movement calculations
            this.currentBoundingBox = player.boundingBox();

            // resolve illegal states (?)
            if (player.velocity == null) {
                player.velocity = new Vec3d(0, 0, 0);
            }

            // apply forces and physics
            applyInputs();
            applyGravity();
            applyFrictions();

            // perform generic position and collision calculations
            performMotion();
        }

        // todo: ad-hoc
        if (player.isDead()) {
            client.getConnection().sendSync(client.createPacket(new ServerboundClientStatusPacket47(/* respawn */ 0)));
        }
    }

    private void applyInputs() {
        float speed = 0.1f;

        if (sprinting) speed *= 1.3f;
        if (sneaking) speed *= 0.3f;

        float yawRad = (float) Math.toRadians(player.transform().yaw);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double accelX = (strafe * cos - forward * sin) * speed;
        double accelZ = (forward * cos + strafe * sin) * speed;

        player.velocity.x += accelX;
        player.velocity.z += accelZ;

        if (jumping && player.effectivelyGrounded()) {
            player.velocity = new Vec3d(player.velocity.x, 0.42, player.velocity.z);
        }
    }

    private void applyGravity() {
        if (!player.isFlying()) {
            if (!player.isGrounded()) {
                player.velocity.y += -0.08;
                player.velocity.y *= 0.98;
                if (player.velocity.y < -3.92) {
                    player.velocity.y = -3.92;
                }
            } else if (player.velocity.y < 0) {
                player.velocity.y = 0;
            }
        } else {
            player.velocity = new Vec3d(player.velocity.x, 0, player.velocity.z);
        }
    }

    private void applyFrictions() {
        // apply friction
        float slip = player.grounded ? physics.getBlockSlipperinessBelow(currentBoundingBox) : 0.9f;
        player.velocity = new Vec3d(
                player.velocity.x * slip,
                player.velocity.y,
                player.velocity.z * slip
        );
    }

    private void performMotion() {
        Transform transform = player.transform();
        AxisAlignedBB bb = currentBoundingBox;

        // the AABB when moved by the current velocity
        AxisAlignedBB moved = bb.offset(player.velocity.x, player.velocity.y, player.velocity.z);

        // todo: resolve collisions robustly n shit
        //  for now, simple on only y-axis
        Vec3d pos = transform.position.add(player.velocity);
        double groundHeight = physics.getGroundHeight(moved);
        if (pos.y < groundHeight) {
            pos.y = groundHeight;
        }

        transform.positioned(pos);

        // update grounding state
        player.grounded = physics.isOnGround(moved);
//        System.out.println(bb + " -> " + moved + " grounded: " + player.grounded + ", velocity: " + player.velocity);
    }

    @SubscribePacket
    void onVelocity(PacketContainer c, ClientboundEntityVelocityPacket packet) {
        if (packet.getEntityID() == player.entityID) {
            player.velocity.x = packet.getVelocityX();
            player.velocity.y = packet.getVelocityY();
            player.velocity.z = packet.getVelocityZ();
        }
    }

}
