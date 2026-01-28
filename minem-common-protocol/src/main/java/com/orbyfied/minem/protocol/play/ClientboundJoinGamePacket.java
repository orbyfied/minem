package com.orbyfied.minem.protocol.play;

import lombok.*;

/**
 * Data class, requires static mapping.
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class ClientboundJoinGamePacket {

    int entityID;             // The entity ID of the local player
    Gamemode gamemode;        // The gamemode the player is in
    Dimension dimension;      // The dimension the player is in
    byte difficulty;          // The difficulty set on the world
    byte maxPlayers;          // The maximum amount of players
    String levelType;         // The level type
    boolean reducedDebugInfo; // Reduced debug info on F3

}
