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
public class ServerboundChatPacket {

    /**
     * The chat message.
     */
    String message;

    public ServerboundChatPacket message(String message) {
        this.message = message;
        return this;
    }

}
