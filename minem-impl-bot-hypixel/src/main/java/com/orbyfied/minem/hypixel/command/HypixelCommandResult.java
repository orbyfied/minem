package com.orbyfied.minem.hypixel.command;

import com.orbyfied.minem.hypixel.HypixelBot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class HypixelCommandResult {

    final boolean success;
    String text;
    HypixelBot.Channel replyChannel;
    boolean unique = true;

    public HypixelCommandResult text(String text) {
        this.text = text;
        return this;
    }

    public HypixelCommandResult replyChannel(HypixelBot.Channel replyChannel) {
        this.replyChannel = replyChannel;
        return this;
    }

    public HypixelCommandResult unique(boolean unique) {
        this.unique = unique;
        return this;
    }

}
