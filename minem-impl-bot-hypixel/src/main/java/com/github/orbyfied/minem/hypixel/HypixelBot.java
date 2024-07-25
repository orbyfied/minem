package com.github.orbyfied.minem.hypixel;

import com.github.orbyfied.minem.ClientComponent;
import com.github.orbyfied.minem.ClientState;
import com.github.orbyfied.minem.MinecraftClient;
import com.github.orbyfied.minem.component.ClientAuthenticator;
import com.github.orbyfied.minem.component.ClientChatHandler;
import com.github.orbyfied.minem.hypixel.storage.HypixelBotStorage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Core services for the Hypixel bot components.
 */
@RequiredArgsConstructor
@Getter
public class HypixelBot extends ClientComponent {

    static final Random RANDOM = new Random();

    /**
     * The data storage to use.
     */
    final HypixelBotStorage storage;

    /**
     * The ranks by ID.
     */
    PermissionRank[] ranks = PermissionRank.DEFAULTS;

    // Random messages to send when the bot joins
    String[] randomJoinMessages = new String[] { "hi" };

    @Override
    protected boolean attach(MinecraftClient client) {
        final ClientChatHandler chatHandler = client.find(ClientChatHandler.class);

        // listen to client closing
        client.onStateSwitch().addLast((oldState, newState) -> {
            if (newState == ClientState.NOT_CONNECTED) {
                CompletableFuture.runAsync(storage::save)
                        .whenComplete((unused, throwable) -> {
                            if (throwable != null) {
                                System.err.println("Failed to save Hypixel bot data storage " + storage);
                                throwable.printStackTrace();
                            }
                        });
            }
        });

        // listen to login success
        client.find(ClientAuthenticator.class).onLoginComplete().addLast((client1, authenticator, acknowledgedPacket) -> {
            if (randomJoinMessages.length > 0) {
                client.getScheduler().scheduleRealDelayed(() -> {
                    String message = randomJoinMessages[RANDOM.nextInt(randomJoinMessages.length)];
                    chatHandler.sendChatSync("/gc " + message);
                }, Duration.ofSeconds(2));
            }
        });

        return true;
    }

    @Override
    protected void resetState() {

    }

    /** Set all configured ranks on this bot. */
    public HypixelBot ranks(PermissionRank... ranks) {
        this.ranks = ranks;
        return this;
    }

    /** Get the rank by the given numerical ID. */
    public PermissionRank getRank(int id) {
        return ranks[id];
    }

    /** Try get a rank by the given name. */
    public PermissionRank getRank(String name) {
        for (PermissionRank rank : ranks) {
            if (rank != null && rank.name().equalsIgnoreCase(name)) {
                return rank;
            }
        }

        return null;
    }

    public HypixelBot randomJoinMessages(String... randomJoinMessages) {
        this.randomJoinMessages = randomJoinMessages;
        return this;
    }

}
