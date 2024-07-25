package com.github.orbyfied.minem.component;

import com.github.orbyfied.minem.ClientComponent;
import com.github.orbyfied.minem.MinecraftClient;
import com.github.orbyfied.minem.listener.IncomingPacketListener;
import com.github.orbyfied.minem.listener.SubscribePacket;
import com.github.orbyfied.minem.math.Vec3d;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.protocol.play.*;
import lombok.Getter;

import static com.github.orbyfied.minem.protocol.play.ClientboundPositionAndLookPacket.*;

/**
 * Tracks entity information about the client (local) player.
 */
@Getter
public class LocalPlayer extends ClientComponent implements IncomingPacketListener {

    int entityID;        // The entity ID of the player
    Vec3d position;      // The current position of the player
    float pitch;         // The current horizontal rotation (pitch) of the player
    float yaw;           // The current vertical rotation (yaw) of the player
    Gamemode gamemode;   // The current gamemode of the player
    Dimension dimension; // The current world/dimension of the player

    @Override
    protected boolean attach(MinecraftClient client) {
        subscribeAllIncomingPackets(client);
        return true;
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
        Vec3d pos = posOrCreate();

        if ((flags & FLAG_REL_X) > 0) pos.x += packet.getX(); else pos.x = packet.getX();
        if ((flags & FLAG_REL_Y) > 0) pos.y += packet.getY(); else pos.y = packet.getY();
        if ((flags & FLAG_REL_Z) > 0) pos.z += packet.getZ(); else pos.z = packet.getZ();

        if ((flags & FLAG_REL_YAW) > 0)   yaw += packet.getYaw();     else yaw =   packet.getYaw();
        if ((flags & FLAG_REL_PITCH) > 0) pitch += packet.getPitch(); else pitch = packet.getPitch();
        System.out.println("new position: " + pos);
    }

    @Override
    protected void resetState() {
        position = null;
        pitch = 0;
        yaw = 0;
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

}
