package test.minem.hypixelbot;

import com.ezylang.evalex.EvaluationException;
import com.ezylang.evalex.Expression;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.parser.ParseException;
import com.github.orbyfied.minem.MinecraftClient;
import com.github.orbyfied.minem.auth.AccountContext;
import com.github.orbyfied.minem.auth.MinecraftAccount;
import com.github.orbyfied.minem.component.ClientAuthenticator;
import com.github.orbyfied.minem.component.ClientChatHandler;
import com.github.orbyfied.minem.component.LocalPlayer;
import com.github.orbyfied.minem.hypixel.HypixelBot;
import com.github.orbyfied.minem.hypixel.command.HypixelCommand;
import com.github.orbyfied.minem.hypixel.command.HypixelCommandResult;
import com.github.orbyfied.minem.hypixel.storage.YAMLHypixelBotStorage;
import com.github.orbyfied.minem.profile.MinecraftProfile;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol.UnknownPacket;
import com.github.orbyfied.minem.protocol47.Protocol47;
import com.github.orbyfied.minem.security.Token;
import com.github.orbyfied.minem.util.ArrayUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.hypixel.api.HypixelAPI;
import net.hypixel.api.apache.ApacheHttpClient;
import net.hypixel.api.reply.PlayerReply;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.ansi.ColorLevel;
import org.junit.jupiter.api.Test;
import slatepowered.veru.misc.ANSI;
import slatepowered.veru.string.StringReader;

import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SimpleHypixelChatBotTest {

    HypixelAPI hypixelAPI;

    @Test
    void test() throws Exception {
        Properties properties = new Properties();
        final String secrets = "../secrets.properties";
        properties.load(new FileReader(secrets));

        AccountContext accountContext = AccountContext.create();
        MinecraftAccount account = new MinecraftAccount();
        if (!account.isAuthenticated()) {
            long expiry = 0;
            if (properties.containsKey("mcbearertoken")
//                    && (expiry = Long.parseLong(properties.getProperty("mcbearerexpiry"))) < System.currentTimeMillis()
            ) {
                account.storeToken("MojangBearer", URLDecoder.decode(properties.getProperty("mcbearertoken"),
                        StandardCharsets.UTF_8), 100000000);
            } else {
                System.out.println("Need to reauth");
                account.storeStringSecret("Email", properties.getProperty("email"));
                account.storeStringSecret("Password", properties.getProperty("password"));
                account.loginMSA(accountContext).join();
                account.loginMojang(accountContext).join();

                Token token = account.getSecret("MojangBearer");
                properties.setProperty("mcbearertoken", URLEncoder.encode(token.getValue(), StandardCharsets.UTF_8));
                properties.setProperty("mcbearerexpiry", String.valueOf(token.getDuration() + token.getTimeObtained()));
                properties.store(new FileWriter(secrets), null);
            }
        }

        account.fetchProfile(accountContext).join();
        System.out.println("[*] Profile: [name = " + account.getProfileName() + ", uuid = " + account.getProfileUUID() + "]");

        // log in to hypixel API
        String hypixelAPIKey = properties.getProperty("hypixelapikey");
        if (hypixelAPIKey != null) {
            hypixelAPI = new HypixelAPI(new ApacheHttpClient(UUID.fromString(hypixelAPIKey)));
        }

        MinecraftClient client = new MinecraftClient()
                .executor(Executors.newFixedThreadPool(2))
                .protocol(Protocol47.PROTOCOL)
                .with(new ClientAuthenticator().account(account).accountContext(accountContext))
                .with(new ClientChatHandler())
                .with(new LocalPlayer());

        AtomicInteger totalUnknownReceived = new AtomicInteger(0);
        Map<Integer, Integer> receivedIdCount = new HashMap<>();
        Set<Integer> unknownReceivedIds = new HashSet<>();

        client.onStateSwitch().addFirst((oldState, newState) -> {
            System.out.println("[!!] Client state switched " + oldState + " -> " + newState);
        });

        client.find(ClientAuthenticator.class).onLoginComplete().addFirst((client1, authenticator, acknowledgedPacket) -> {
            System.out.println("[OK] Login completed");
        });

        final ANSIComponentSerializer serializer = ANSIComponentSerializer.builder()
                .colorLevel(ColorLevel.TRUE_COLOR)
                .build();
        client.find(ClientChatHandler.class).onChatReceived().addLast((handler, message, rawMessage, type) -> {
            System.out.println("    " + serializer.serializeOrNull(message));
        });

        client.onPacketReceived().addFirst(packet -> {
            if (packet.getPhase() == ProtocolPhases.PLAY) receivedIdCount.put(packet.getNetworkId(), receivedIdCount.getOrDefault(packet.getNetworkId(), 0) + 1);

            if (packet.getNetworkId() == 0x15) {
                LocalPlayer localPlayer = client.find(LocalPlayer.class);
                UnknownPacket packet1 = packet.data(UnknownPacket.class);
                if (packet1.buffer().readVarInt() == localPlayer.getEntityID()) {
                    System.out.println("received 0x15 for local player");
                }
            }

            if (packet.isUnknown()) {
                if (packet.getPhase() == ProtocolPhases.PLAY) unknownReceivedIds.add(packet.getNetworkId());
                totalUnknownReceived.incrementAndGet();
                return 0;
            }

//            System.out.println("[<-] Received Packet NID 0x" + Integer.toHexString(packet.getNetworkId()) + ", DataType: " + packet.data().getClass().getSimpleName() + ", Data: " + packet.data());
            return 0;
        });

        client.onPacketSink().addFirst(packet -> {
//            System.out.println("[->] Sent Packet NID 0x" + Integer.toHexString(packet.getNetworkId()) + ", DataType: " + packet.data().getClass().getSimpleName() + ", Data: " + packet.data());
            return 0;
        });

        client.onDisconnect().addLast((client1, reason, details) -> {
            System.out.println("[DC] Disconnected for reason " + reason);
            if (details instanceof Throwable throwable) {
                throwable.printStackTrace();
            } else if (details instanceof Component component) {
                System.out.println("   Details: " + ANSI.YELLOW + PlainTextComponentSerializer.plainText().serialize(component) + ANSI.RESET);
            } else {
                System.out.println("   Details: " + ANSI.CYAN + details + ANSI.RESET);
            }
        });

        LocalPlayer localPlayer = client.find(LocalPlayer.class);
        localPlayer.forceGrounded(true);
        localPlayer.onFlyUpdate().addLast((localPlayer1, canFlyChange, flyingChange, flySpeedChange) -> {
            if (canFlyChange != null) {
                localPlayer1.fly(canFlyChange);
            }
        });

        /* Hypixel Bot Setup */
        client.with(new HypixelBot(new YAMLHypixelBotStorage(Path.of("../run/hypixel-bot-data.yml"))
                .owner("a6e74213-a823-4131-baf9-9201906dd156"))
                .randomJoinMessages("cybean", "hi"));
        client.find(HypixelBot.class).getStorage().load();
        register(client, client.find(HypixelBot.class));

        System.out.print("\n\n");
        long t1 = System.currentTimeMillis();
        client.connect(new InetSocketAddress("mc.hypixel.net", 25565)).join();
        long t2 = System.currentTimeMillis();

        long waitUntilDisconnect = (long) (/* min 8 seconds */ 120 * 1000 + /* 6s deviation */ Math.random() * 6 * 1000);
        client.onDisconnect().await(waitUntilDisconnect); // let it run for 15 seconds
        client.disconnect(MinecraftClient.DisconnectReason.FORCE, null);
        long t3 = System.currentTimeMillis();

        // send diagnostics
        try { Thread.sleep(100); } catch (InterruptedException ignored) { }
        System.out.println("");
        System.out.println(ANSI.YELLOW + "-------- DIAGNOSTICS -----------------------------------------");
        System.out.print("Most Received [PLAY] (red = unknown): ");
        receivedIdCount.entrySet()
                .stream()
                .sorted(Comparator.<Map.Entry<Integer, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .forEachOrdered(integerIntegerEntry -> {
                    System.out.print((unknownReceivedIds.contains(integerIntegerEntry.getKey()) ? ANSI.RED : ANSI.GREEN) +
                            "0x" + Integer.toHexString(integerIntegerEntry.getKey()) + ": " + integerIntegerEntry.getValue() + ANSI.RESET + ", ");
                });
        System.out.println();
        System.out.println("Total Unknown Received: " + totalUnknownReceived.get());
        System.out.println("Connected for: " + (t3 - t2) + "ms");
        System.out.println("Time to connect: " + (t2 - t1) + "ms");
        System.out.println();
        System.out.println();
    }

    void register(MinecraftClient client, HypixelBot bot) {
        final ClientChatHandler chatHandler = client.find(ClientChatHandler.class);
        Random random = new Random();

        bot.register(new HypixelCommand().name("disconnect").rank(4).aliases("dc").executor(ctx -> {
            client.find(ClientChatHandler.class).sendChatSync("/gc ok bye");
            client.disconnect(MinecraftClient.DisconnectReason.ERROR, "ING command issued by " + ctx.getSenderProfile().getName());
            return null;
        }));

        bot.register(new HypixelCommand().name("echo").rank(2).executor(ctx -> {
            ctx.assertArgs(1);
            return ctx.success(ArrayUtil.join(ctx.getArgs(), " "));
        }));

        bot.register(new HypixelCommand().name("pleave").aliases("pl").rank(3).executor(ctx -> {
            chatHandler.sendChatSync("/p leave");
            return ctx.success("ok");
        }));

        bot.register(new HypixelCommand().name("squidgames").executor(ctx -> {
            chatHandler.sendChatSync("/msg " + ctx.getSenderProfile().getName() + " squidgames");
            return null;
        }));

        bot.register(new HypixelCommand().name("coinflip").aliases("cf").executor(ctx -> {
            boolean heads = Math.random() >= 0.5f;
            return ctx.success(heads ? "heads" : "tails");
        }));

        bot.register(new HypixelCommand().name("random").aliases("r").executor(ctx -> {
            int low = 1;
            int high = 11;
            if (ctx.hasArgs(2)) {
                low = Integer.parseInt(ctx.getArg(0));
                high = Integer.parseInt(ctx.getArg(1)) + 1;
            }

            return ctx.success("=" + random.nextInt(low, high));
        }));

        final DecimalFormat format = new DecimalFormat("#.###");
        final DecimalFormat format1 = new DecimalFormat("#.##");
        bot.register(new HypixelCommand().name("calc").aliases("c").executor(ctx -> {
            ctx.assertArgs(1);
            String[] args = ctx.getArgs();

            // parse flags
            boolean raw = false;
            boolean hex = false;
            if (args[0].startsWith("/")) {
                String flags = args[0].substring(1);
                args = Arrays.copyOfRange(args, 1, args.length);

                if (flags.contains("r")) raw = true;
                if (flags.contains("x")) hex = true;
            }

            String expr = ArrayUtil.join(args, " ").split("#")[0];
            Expression expression = new Expression(expr);

            try {
                EvaluationValue value = expression.evaluate();

                // stringify according to flags
                String resultStr;
                if (raw) {
                    resultStr = value.getValue() + " with type " + value.getDataType();
                } else if (hex && value.getDataType() == EvaluationValue.DataType.NUMBER) {
                    BigDecimal numberValue = value.getNumberValue();
                    resultStr = "0x" + numberValue.toBigInteger().toString(16);
                } else {
                    resultStr = switch (value.getDataType()) {
                        case NULL -> "null";
                        case NUMBER -> format.format(value.getNumberValue().doubleValue());
                        case STRING -> "\"" + value.getStringValue() + "\"";
                        default -> value.getValue().toString();
                    };
                }

                return ctx.success("= " + resultStr);
            } catch (EvaluationException e) {
                return ctx.failed("eval: " + e.getMessage());
            } catch (ParseException e) {
                return ctx.failed("syntax: " + e.getMessage());
            }
        }));

        bot.register(new HypixelCommand().name("bedwars").aliases("bw").executor(ctx -> {
            if (hypixelAPI == null) {
                return ctx.failed("no hypixel api support");
            }

            ctx.assertArgs(1);
            MinecraftProfile profile = MinecraftProfile.GLOBAL_CACHE.fetchByName(ctx.getArg(0));

            return (HypixelCommandResult) hypixelAPI.getPlayerByUuid(profile.getUUID()).thenApply(playerReply -> {
                PlayerReply.Player player = playerReply.getPlayer();
                JsonObject bedwarsStats = player.getObjectProperty("stats").getAsJsonObject("Bedwars");

                int wins = bedwarsStats.get("wins_bedwars").getAsInt();
                int loss = bedwarsStats.get("losses_bedwars").getAsInt();
                int fks = bedwarsStats.get("final_kills_bedwars").getAsInt();
                int fds = bedwarsStats.get("final_deaths_bedwars").getAsInt();
                JsonPrimitive ws = bedwarsStats.getAsJsonPrimitive("winstreak");
                float fkdr = fds != 0 ? (fks * 1f / fds) : fks;
                float wlr = loss != 0 ? (wins * 1f / loss) : loss;

                bot.send(ctx.getChannel(), "[BW] " + profile.getName() + ": " +
                                "fkdr(" + format1.format(fkdr).replace('.', ',') + ") " +
                                "ws(" + (ws == null ? "idk" : ws.getAsInt()) + ") " +
                                "wlr(" + format1.format(wlr).replace('.', ',') + ") " +
                                ""
                        , false);

                return null;
            }).exceptionally(t -> ctx.failed("api fail: " + t.getMessage())).join();
        }));

        AtomicLong lastLeave = new AtomicLong(0);
        bot.onChat().addLast(chat -> {
            String message = chat.content().toLowerCase();
            String[] splitByNewline = message.split("\n");

            // check for join game
            if (message.contains("has joined") || message.contains("the game starts")) {
                if (System.currentTimeMillis() - lastLeave.get() < 1000) {
                    return;
                }

                chatHandler.sendChatSync("/l bedwars");
                bot.runCommandAfter("/pc i told u dont q fuck u", 100);
                bot.runCommandAfter("/pc i told u dont q fuck u", 320);
                bot.runCommandAfter("/pc i told u dont q fuck u", 420);
                bot.runCommandAfter("/p leave", 800);
                lastLeave.set(System.currentTimeMillis());
            }

            // check for party invite
            if (splitByNewline.length > 1 && splitByNewline[1].contains("invited you to join")) {
                StringReader reader = new StringReader(splitByNewline[1]);
                reader.consumeWhitespace();
                if (reader.curr() == '[') {
                    reader.collect(c -> c != ']');
                    reader.next(2); // skip [ and space
                }

                reader.consumeWhitespace();
                String ign = reader.collect(c -> c != ' ').trim();

                chatHandler.sendChatSync("/p join " + ign);
                bot.runCommandAfter("/pc .", 310);
                bot.runCommandAfter("/pc hi am bot", 505);
                bot.runCommandAfter("/pc [!] dont q or i leave", 920);
                bot.runCommandAfter("/pc .", 1270);
            }
        });
    }

}
