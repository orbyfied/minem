package com.github.orbyfied.minem.hypixel.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor
@Getter
public class HypixelCommand {

    // The name of the command
    String name;
    // The aliases for this command
    String[] aliases = new String[0];
    // The minimum rank required
    int rank = 0;
    // The executor
    Function<HypixelCommandContext, HypixelCommandResult> executor;

    public HypixelCommand name(String name) {
        this.name = name;
        return this;
    }

    public HypixelCommand aliases(String... aliases) {
        this.aliases = aliases;
        return this;
    }

    public HypixelCommand rank(int rank) {
        this.rank = rank;
        return this;
    }

    public HypixelCommand executor(Function<HypixelCommandContext, HypixelCommandResult> executor) {
        this.executor = executor;
        return this;
    }

}
