package com.github.orbyfied.minem.hypixel.command;

import com.github.orbyfied.minem.hypixel.HypixelBot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class HypixelCommandResult {

    final boolean success;
    String text;
    HypixelBot.Channel replyChannel;

    public HypixelCommandResult text(String text) {
        this.text = text;
        return this;
    }

    public HypixelCommandResult replyChannel(HypixelBot.Channel replyChannel) {
        this.replyChannel = replyChannel;
        return this;
    }

}
