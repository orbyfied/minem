package com.github.orbyfied.minem.protocol.play;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class ClientboundRespawnPacket {

    Gamemode gamemode;   // The gamemode the player is in
    Dimension dimension; // The dimension the player is in
    byte difficulty;     // The difficulty set on the world
    String levelType;    // The level type

}
