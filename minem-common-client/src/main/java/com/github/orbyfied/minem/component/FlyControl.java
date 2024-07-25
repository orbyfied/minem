package com.github.orbyfied.minem.component;

import com.github.orbyfied.minem.ClientComponent;
import com.github.orbyfied.minem.MinecraftClient;
import com.github.orbyfied.minem.listener.IncomingPacketListener;
import com.github.orbyfied.minem.listener.SubscribePacket;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.play.ClientboundPlayerAbilitiesPacket;
import com.github.orbyfied.minem.protocol.play.ServerboundPlayerAbilitiesPacket;

/**
 * Can be used to avoid flying in lobbies.
 */
public class FlyControl extends ClientComponent implements IncomingPacketListener {

    LocalPlayer localPlayer;
    volatile boolean canFly;       // Whether we have the capability to fly
    volatile boolean flying;       // Whether we are currently in fly mode
    volatile boolean grounded;     // Whether we are on the ground
    volatile boolean avoid = true; // Whether to enable anti-fly movement simulation
    volatile float flySpeed;       // The fly speed set by the server

    @Override
    protected boolean attach(MinecraftClient client) {
        subscribeAllIncomingPackets(client);
        this.localPlayer = client.find(LocalPlayer.class);
        return true;
    }

    @SubscribePacket
    void onAbilitiesPacket(Packet container, ClientboundPlayerAbilitiesPacket packet) {
        this.canFly = packet.isCanFly();
        this.flying = packet.isFlying();
        this.flySpeed = packet.getFlySpeed();
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

    public FlyControl avoid(boolean avoid) {
        this.avoid = avoid;
        return this;
    }

    public boolean avoid() {
        return avoid;
    }

}

