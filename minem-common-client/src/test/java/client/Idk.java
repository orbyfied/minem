package client;

import com.github.orbyfied.minem.ClientAuthenticator;
import com.github.orbyfied.minem.MinecraftClient;
import com.github.orbyfied.minem.auth.AccountContext;
import com.github.orbyfied.minem.auth.MinecraftAccount;
import com.github.orbyfied.minem.io.ProtocolIO;
import com.github.orbyfied.minem.packet.CommonPacketImplementations;
import com.github.orbyfied.minem.protocol.Protocol;
import com.github.orbyfied.minem.security.Token;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.Executors;

public class Idk {

    @Test
    void test() throws Exception {
        Properties properties = new Properties();
        final String secrets = "../secrets.properties";
        properties.load(new FileReader(secrets));
        System.out.println("Logging into account");

        AccountContext accountContext = AccountContext.create();
        MinecraftAccount account = new MinecraftAccount();
        if (!account.isAuthenticated()) {
            long expiry = 0;
            if (properties.containsKey("mcbearertoken")
//                    && (expiry = Long.parseLong(properties.getProperty("mcbearerexpiry"))) < System.currentTimeMillis()
            ) {
                account.storeToken("MojangBearer", URLDecoder.decode(properties.getProperty("mcbearertoken"),
                        StandardCharsets.UTF_8), 100000000);
                System.out.println("Found existing valid MojangBearer token");
            } else {
                account.storeStringSecret("Email", properties.getProperty("email"));
                account.storeStringSecret("Password", properties.getProperty("password"));
                account.loginMSA(accountContext).join();
                account.loginMojang(accountContext).join();

                Token token = account.getSecret("MojangBearer");
                properties.setProperty("mcbearertoken", URLEncoder.encode(token.getValue(), StandardCharsets.UTF_8));
                properties.setProperty("mcbearerexpiry", String.valueOf(token.getDuration() + token.getTimeObtained()));
                properties.store(new FileWriter(secrets), null);
                System.out.println("Logged into account");
            }
        }

        System.out.println("Fetching profile");
        account.fetchProfile(accountContext).join();
        System.out.println("Fetched profile: [name = " + account.getProfileName() + ", uuid = " + account.getProfileUUID() + "]");

        MinecraftClient client = new MinecraftClient()
                .executor(Executors.newFixedThreadPool(2))
                .protocol(Protocol.create(47)
                        .registerPacketMappings(CommonPacketImplementations.MAPPINGS)
                )
                .with(new ClientAuthenticator().account(account).accountContext(accountContext));

        client.onPacket().addLast(packet -> {
            System.out.println("Packet NID 0x" + Integer.toHexString(packet.getNetworkId()) + ", DataType: " + packet.data().getClass().getSimpleName() + ", Data: " + packet.data());
            return 0;
        });

        System.out.println("Created client, connecting");
        client.connect(new InetSocketAddress("mc.hypixel.net", 25565)).join();

        client.find(ClientAuthenticator.class).onLoginComplete().addLast((client1, authenticator, acknowledgedPacket) -> {
            System.out.println("Login completed (handler called)");
        });

        while (client.isOpen());
    }

    @Test
    void test2() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ProtocolIO.writeVarIntToStream(outputStream, 0xFFFFFF);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        System.out.println(ProtocolIO.readVarIntFromStream(inputStream));
    }

}
