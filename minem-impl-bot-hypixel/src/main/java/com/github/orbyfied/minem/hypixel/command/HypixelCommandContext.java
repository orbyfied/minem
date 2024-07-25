package com.github.orbyfied.minem.hypixel.command;

import com.github.orbyfied.minem.hypixel.HypixelBot;
import com.github.orbyfied.minem.hypixel.storage.MapAccess;
import com.github.orbyfied.minem.profile.MinecraftProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@RequiredArgsConstructor
@Getter
public class HypixelCommandContext {

    final HypixelBot hypixelBot;
    final HypixelBot.Channel channel;
    final MinecraftProfile senderProfile;
    final MapAccess<String, Object> senderProperties;
    String[] args;

    public boolean hasArgs(int count) {
        return args.length >= count;
    }

    public void assertArgs(int count) {
        if (!hasArgs(count)) {
            throw new IllegalArgumentException("missing args");
        }
    }

    public boolean validArgIndex(int idx) {
        return args.length >= idx - 1;
    }

    public String getArg(int i) {
        return args[i];
    }

    public <T> T getArg(int i, Function<String, T> function) {
        return function.apply(args[i]);
    }

    public String getArg(int i, String def) {
        return validArgIndex(i) ? args[i] : def;
    }

    public <T> T getArg(int i, Function<String, T> function, T def) {
        return validArgIndex(i) ? function.apply(args[i]) : def;
    }

    public HypixelCommandContext args(String[] args) {
        this.args = args;
        return this;
    }

    public <T> CompletableFuture<T> completed(T t) {
        return CompletableFuture.completedFuture(t);
    }

    public HypixelCommandResult success(String s) {
        return new HypixelCommandResult(true).text(s);
    }

    public HypixelCommandResult failed(String s) {
        return new HypixelCommandResult(false).text(s);
    }

}
