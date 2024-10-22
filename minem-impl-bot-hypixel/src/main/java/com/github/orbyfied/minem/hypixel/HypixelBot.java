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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

        // listen to chat
        client.find(ClientChatHandler.class).onChatReceived().addLast((handler, message, rawMessage, type) -> {
            analyzeAndProcessChat(rawMessage);
        });

        init();
        return true;
    }

    /**
     * Try to avoid spam.
     */
    public static String makeUnique(String str) {
        int l = Math.min(10, Math.max(2, str.length() / 7)) + (int) (Math.random() * 2);
        return str + " #" + Long.toHexString((long) (Math.random() * Long.MAX_VALUE)).toUpperCase().substring(0, l);
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

    public void runCommandAfter(String msg, long delayMillis) {
        client.getScheduler().getRealTimeExecutor().schedule(() -> chatHandler.sendChatSync(msg), delayMillis, TimeUnit.MILLISECONDS);
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

        HypixelChat chat = new HypixelChat(channel, content, ignWithRank, ignWithoutRankOrRole, type, message);
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
                              String content,
                              String ignRanked, String ignStripped,
                              Type type, String message) {

    }

    public interface HypixelChatHandler {
        void onChat(HypixelChat chat);
    }

    public void send(Channel channel, String str) {
        send(channel, str, true);
    }

    /**
     * Send the given message in the given channel.
     */
    public void send(Channel channel, String str, boolean unique) {
        String cmd = switch (channel) {
            case ALL -> "/ac";
            case PRIVATE -> "/r";
            case GUILD -> "/gc";
            case PARTY -> "/pc";
        };

        // sanitize and make unique
        if (unique && (channel == Channel.GUILD || channel == Channel.PRIVATE)) {
            str = makeUnique(str);
        }

        str = str.replaceAll("(www|http:|https:)+[^\\s]+[\\w]", "(some url)");
        str = str.replace(".", " ");

        chatHandler.sendChatSync(cmd + " " + str);
    }

    /**
     * Send the given message in the given channel.
     */
    public void send(SendableResult result) {
        if (result.message() != null) {
            send(result.channel(), result.message(), result.unique());
        }
    }

    public record SendableResult(boolean success, Channel channel, String message, boolean unique) {

    }

    /**
     * The console profile.
     */
    @Getter
    MinecraftProfile consoleProfile = new MinecraftProfile().username("*").uuid(new UUID(0, 0));

    /**
     * The console profile properties.
     */
    @Getter
    MapAccess<String, Object> consoleProperties = new MapAccess<>(new HashMap<>(), null);

    public CompletableFuture<SendableResult> exec(HypixelChat chat, String str) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String[] split = str.split(" ");
                String alias = split[0];
                HypixelCommand command = commandMap.get(alias);
                if (command == null) {
                    return null;
                }

                String[] args = Arrays.copyOfRange(split, 1, split.length);
                MapAccess<String, Object> props;
                MinecraftProfile profile;
                if (chat.ignStripped.equalsIgnoreCase("*")) {
                    profile = consoleProfile;
                    props = consoleProperties;
                    consoleProperties.set("rank", ranks.length - 1);
                } else {
                    profile = MinecraftProfile.CACHE.fetchByName(chat.ignStripped());
                    props = storage.forPlayer(profile.getUUID());
                }

                HypixelCommandContext ctx = new HypixelCommandContext(this, chat.channel(), profile, props)
                        .args(args);

                // check permissions
                int rank = props.getAsOr("rank", Number.class, 0).intValue();
                if (rank < command.getRank()) {
                    return new SendableResult(false, ctx.getChannel(), "u need " + getRank(command.getRank()).name().toLowerCase() + " or higher to exec", true);
                }

                try {
                    var res = command.getExecutor().apply(ctx);
                    if (res != null) {
                        return new SendableResult(res.isSuccess(), ctx.getChannel(), (res.isSuccess() ? "" : "err: ") + res.getText(), true);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return new SendableResult(false, ctx.getChannel(), "err: " + ex, true);
                }

                return new SendableResult(true, ctx.getChannel(), null, false);
            } catch (Exception ex) {
                ex.printStackTrace();
                return new SendableResult(false, chat.channel(), "internal error", true);
            }
        });
    }

    private void init() {
        // Register Command Handler
        onChat.addLast(chat -> {
            if (chat.type == Type.CHAT) {
                // check for command execution
                String message = chat.message().strip();
                if (message.startsWith("!")) {
                    exec(chat, message.substring(1)).thenAccept(this::send);
                }

                // check for my ign
                if (message.toLowerCase().contains(selfAccount.getProfileName().toLowerCase())) {
                    send(chat.channel(), "hi " + chat.ignStripped(), true);
                }

                return;
            }
        });

        // Register Core Commands
        register(new HypixelCommand().name("rank").rank(0).executor(ctx -> {
            ctx.assertArgs(1);

            MinecraftProfile target = MinecraftProfile.CACHE.fetchByName(ctx.getArg(0));
            var targetData = storage.forPlayer(target.getUUID());

            boolean set = false;
            if (ctx.hasArgs(2)) {
                // --- set rank
                int rank = ctx.getSenderProperties().getAsOr("rank", (Number) 0).intValue();
                if (rank < /* admin */ 4) {
                    return ctx.failed("u need " + getRank(4).name().toLowerCase() + " or higher to set");
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
