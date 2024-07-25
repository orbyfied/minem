package com.github.orbyfied.minem.hypixel;

import com.github.orbyfied.minem.ClientComponent;
import com.github.orbyfied.minem.ClientState;
import com.github.orbyfied.minem.MinecraftClient;
import com.github.orbyfied.minem.auth.MinecraftAccount;
import com.github.orbyfied.minem.component.ClientAuthenticator;
import com.github.orbyfied.minem.component.ClientChatHandler;
import com.github.orbyfied.minem.component.LocalPlayer;
import com.github.orbyfied.minem.event.Chain;
import com.github.orbyfied.minem.hypixel.command.HypixelCommand;
import com.github.orbyfied.minem.hypixel.command.HypixelCommandContext;
import com.github.orbyfied.minem.hypixel.storage.HypixelBotStorage;
import com.github.orbyfied.minem.hypixel.storage.MapAccess;
import com.github.orbyfied.minem.hypixel.util.HypixelChatUtils;
import com.github.orbyfied.minem.profile.MinecraftProfile;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import slatepowered.veru.string.StringReader;

import java.time.Duration;
import java.util.*;
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

    // All registered commands
    private final Map<String, HypixelCommand> commandMap = new HashMap<>();

    // Random messages to send when the bot joins
    String[] randomJoinMessages = new String[] { "hi" };

    /* Events */
    final Chain<HypixelChatHandler> onChat = new Chain<>(HypixelChatHandler.class);

    /* Components */
    ClientChatHandler chatHandler;
    LocalPlayer localPlayer;
    MinecraftAccount selfAccount;

    @Override
    protected boolean attach(MinecraftClient client) {
        chatHandler = client.find(ClientChatHandler.class);
        selfAccount = client.find(ClientAuthenticator.class).getAccount();

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

        // listen to chat
        client.find(ClientChatHandler.class).onChatReceived().addLast((handler, message, rawMessage, type) -> {
            analyzeAndProcessChat(rawMessage);
        });

        init();
        return true;
    }

    @Override
    protected void resetState() {

    }

    public HypixelBot register(HypixelCommand command) {
        this.commandMap.put(command.getName(), command);
        for (String alias : command.getAliases()) {
            this.commandMap.put(alias, command);
        }

        return this;
    }

    public Chain<HypixelChatHandler> onChat() {
        return onChat;
    }

    // analyze and process the given chat message
    private void analyzeAndProcessChat(String rawMessage) {
        Channel channel = Channel.ALL;
        if (rawMessage.startsWith("Guild >")) channel = Channel.GUILD;
        else if (rawMessage.startsWith("Party >")) channel = Channel.PARTY;
        else if (rawMessage.startsWith("From ")) channel = Channel.PRIVATE;

        String content = rawMessage.substring(channel.getChatPrefix().length());

        // check for username
        StringReader reader = new StringReader(content);
//        String ign = reader.collect(c -> (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || (c >= '0' && c <= '9'));
        String ignWithRank = reader.collect(c -> c != ':');
        String ignWithoutRankOrRole = HypixelChatUtils.stripIGN(ignWithRank);
        String message = null;
        if (reader.curr() == ':') {
            reader.next();
            message = reader.collect();
        }

        Type type = message == null ? Type.OTHER : Type.CHAT;
        String ownIgn = selfAccount.getProfileName().toLowerCase();

        if (type == Type.CHAT ?
                ignWithRank.toLowerCase().contains(ownIgn) :
                content.toLowerCase().contains(ownIgn)
        ) {
            return;
        }

        if (type == Type.OTHER) {
            ignWithRank = null;
            ignWithoutRankOrRole = null;
        }

        HypixelChat chat = new HypixelChat(channel, ignWithRank, ignWithoutRankOrRole, type, message);
        onChat.invoker().onChat(chat);
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

    /**
     * The type of the message.
     */
    public enum Type {
        CHAT, // A chat message sent by a player
        OTHER // An event, such as a kill, or system message
    }

    /**
     * The channel the message was sent in.
     */
    @RequiredArgsConstructor
    @Getter
    public enum Channel {
        GUILD("Guild > "),
        PARTY("Party > "),
        PRIVATE("From "),
        ALL("")
        ;

        final String chatPrefix;
    }

    public record HypixelChat(Channel channel,
                              String ignRanked, String ignStripped,
                              Type type, String message) {

    }

    public interface HypixelChatHandler {
        void onChat(HypixelChat chat);
    }

    /**
     * Send the given message in the given channel.
     */
    public void send(Channel channel, String str) {
        String cmd = switch (channel) {
            case ALL -> "/ac";
            case PRIVATE -> "/r";
            case GUILD -> "/gc";
            case PARTY -> "/pc";
        };

        chatHandler.sendChatSync(cmd + " " + str);
    }

    private void init() {
        // Register Command Handler
        onChat.addLast(chat -> {
            if (chat.type == Type.CHAT) {
                // check for command execution
                String message = chat.message().strip();
                if (message.startsWith("!")) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            String[] split = message.substring(1).split(" ");
                            String alias = split[0];
                            HypixelCommand command = commandMap.get(alias);
                            if (command == null) {
                                return;
                            }

                            String[] args = Arrays.copyOfRange(split, 1, split.length);
                            MinecraftProfile profile = MinecraftProfile.GLOBAL_CACHE.fetchByName(chat.ignStripped());
                            MapAccess<String, Object> props = storage.forPlayer(profile.getUUID());
                            HypixelCommandContext ctx = new HypixelCommandContext(this, chat.channel(), profile, props)
                                    .args(args);

                            // check permissions
                            int rank = props.getAsOr("rank", Number.class, 1).intValue();
                            if (rank < command.getRank()) {
                                send(ctx.getChannel(), "u need " + getRank(command.getRank()).name() + "+ to exec");
                                return;
                            }

                            try {
                                var res = command.getExecutor().apply(ctx);
                                if (res != null) {
                                    send(ctx.getChannel(), (res.isSuccess() ? "" : "err: ") + res.getText());
                                }
                            } catch (Exception ex) {
                                send(ctx.getChannel(), "err: " + ex.getMessage());
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }

                // check for my ign
                if (message.toLowerCase().contains(selfAccount.getProfileName().toLowerCase())) {
                    send(chat.channel(), "hi " + chat.ignStripped());
                }

                return;
            }
        });

        // Register Core Commands
        register(new HypixelCommand().name("rank").rank(0).executor(ctx -> {
            ctx.assertArgs(1);

            MinecraftProfile target = MinecraftProfile.GLOBAL_CACHE.fetchByName(ctx.getArg(0));
            var targetData = storage.forPlayer(target.getUUID());

            boolean set = false;
            if (ctx.hasArgs(2)) {
                // --- set rank
                int rank = ctx.getSenderProperties().getAsOr("rank", (Number) 0).intValue();
                if (rank < /* admin */ 4) {
                    return ctx.failed("u need " + getRank(rank).name() + "+ to set");
                }

                PermissionRank toSet = getRank(ctx.getArg(1));
                if (toSet == null) {
                    return ctx.failed("invalid rank");
                }

                if (toSet.id() > rank) {
                    return ctx.failed("no authority");
                }

                targetData.set("rank", toSet.id());
                set = true;
            }

            return ctx.success((set ? "set" : "has") + " rank " + getRank(targetData.getAsOr("rank", (Number) 0).intValue()).name());
        }));
    }

}
