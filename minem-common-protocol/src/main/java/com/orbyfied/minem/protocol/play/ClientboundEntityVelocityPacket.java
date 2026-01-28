package com.orbyfied.minem.protocol.play;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class ClientboundEntityVelocityPacket {

    int entityID;
    double velocityX;
    double velocityY;
    double velocityZ;

}
