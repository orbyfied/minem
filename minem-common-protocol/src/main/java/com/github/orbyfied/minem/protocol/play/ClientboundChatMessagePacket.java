package com.github.orbyfied.minem.protocol.play;

import lombok.*;
import net.kyori.adventure.text.Component;

/**
 * Data class, requires static mapping.
 */
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class ClientboundChatMessagePacket {

    /**
     * The chat message.
     */
    Component message;

    /**
     * The position of the chat message.
     */
    byte position;

    public ClientboundChatMessagePacket message(Component message) {
        this.message = message;
        return this;
    }

    public ClientboundChatMessagePacket position(byte position) {
        this.position = position;
        return this;
    }

}
