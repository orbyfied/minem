package test.minem.hypixelbot;

import com.github.orbyfied.minem.component.ClientAuthenticator;
import com.github.orbyfied.minem.MinecraftClient;
import com.github.orbyfied.minem.auth.AccountContext;
import com.github.orbyfied.minem.auth.MinecraftAccount;
import com.github.orbyfied.minem.component.ClientChatHandler;
import com.github.orbyfied.minem.component.LocalPlayer;
import com.github.orbyfied.minem.hypixel.HypixelBot;
import com.github.orbyfied.minem.component.FlyControl;
import com.github.orbyfied.minem.hypixel.storage.YAMLHypixelBotStorage;
import com.github.orbyfied.minem.io.ProtocolIO;
import com.github.orbyfied.minem.protocol.ProtocolPhases;
import com.github.orbyfied.minem.protocol47.Protocol47;
import com.github.orbyfied.minem.security.Token;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.ansi.ColorLevel;
import org.junit.jupiter.api.Test;
import slatepowered.veru.misc.ANSI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleHypixelChatBotTest {

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

        MinecraftClient client = new MinecraftClient()
                .executor(Executors.newFixedThreadPool(2))
                .protocol(Protocol47.PROTOCOL)
                .with(new ClientAuthenticator().account(account).accountContext(accountContext))
                .with(new ClientChatHandler())
                .with(new LocalPlayer())
                .with(new FlyControl());

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
        client.find(ClientChatHandler.class).onChatReceived().addLast((handler, message, type) -> {
            System.out.println("[CH] " + serializer.serializeOrNull(message));
        });

        client.onPacketReceived().addFirst(packet -> {
            if (packet.getPhase() == ProtocolPhases.PLAY) receivedIdCount.put(packet.getNetworkId(), receivedIdCount.getOrDefault(packet.getNetworkId(), 0) + 1);

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

        /* Hypixel Bot Setup */
        client.with(new HypixelBot(new YAMLHypixelBotStorage(Path.of("../run/hypixel-bot-data.yml")))
                .randomJoinMessages("hi", "cybean"));

        System.out.print("\n\n");
        long t1 = System.currentTimeMillis();
        client.connect(new InetSocketAddress("mc.hypixel.net", 25565)).join();
        long t2 = System.currentTimeMillis();

        long waitUntilDisconnect = (long) (/* min 8 seconds */ 8 * 1000 + /* 6s deviation */ Math.random() * 6 * 1000);
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

    @Test
    void test2() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ProtocolIO.writeVarIntToStream(outputStream, 0xFFFFFF);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        System.out.println(ProtocolIO.readVarIntFromStream(inputStream));
    }

}
