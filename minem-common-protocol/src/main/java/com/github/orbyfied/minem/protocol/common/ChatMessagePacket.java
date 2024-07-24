package com.github.orbyfied.minem.protocol.common;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

@Getter
@Setter
public abstract class ChatMessagePacket {

    /**
     * The chat message.
     */
    Component message;

    /**
     * The position of the chat message.
     */
    byte position;

}
