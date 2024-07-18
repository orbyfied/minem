package com.github.orbyfied.minem;

import com.github.orbyfied.minem.auth.AccountContext;
import com.github.orbyfied.minem.auth.MinecraftAccount;
import com.github.orbyfied.minem.event.Chain;
import com.github.orbyfied.minem.packet.*;
import com.github.orbyfied.minem.protocol.Packet;
import com.github.orbyfied.minem.security.AsymmetricEncryptionProfile;
import com.github.orbyfied.minem.security.SymmetricEncryptionProfile;
import com.google.gson.JsonObject;
import lombok.Getter;
import slatepowered.veru.collection.ArrayUtil;
import slatepowered.veru.misc.Throwables;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Performs encryption and authentication with a server and Minecraft services
 * needed for connecting to online mode servers.
 */
public class ClientAuthenticator extends ClientComponent {

    static final MessageDigest SHA1;

    static {
        try {
            SHA1 = MessageDigest.getInstance("SHA-1");
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Getter
    AccountContext accountContext;

    @Getter
    MinecraftAccount account;

    AsymmetricEncryptionProfile publicProfile; // The profile containing the server public key

    /** The properties received from the server after login. */
    Map<String, LoginProperty> profileProperties = new HashMap<>();

    byte[] verifyTokenBytes; // The bytes of the verification token
    byte[] secretKeyBytes;   // The secret key bytes

    final Chain<ClientLoginCompletedHandler> onLoginComplete = new Chain<>(ClientLoginCompletedHandler.class);

    {
        resetState();
    }

    public Chain<ClientLoginCompletedHandler> onLoginComplete() {
        return onLoginComplete;
    }

    public ClientAuthenticator account(MinecraftAccount account) {
        this.account = account;
        return this;
    }

    public ClientAuthenticator accountContext(AccountContext accountContext) {
        this.accountContext = accountContext;
        return this;
    }

    @Override
    protected void resetState() {
        this.publicProfile = AsymmetricEncryptionProfile.UTILITY_RSA_1024;
        this.verifyTokenBytes = null;
        this.secretKeyBytes = null;
    }

    @Override
    protected boolean attach(MinecraftClient client) {
        client.onPacketReceived().addLast(this::onPacketReceived);
        client.onStateSwitch().addLast((oldState, newState) -> {
            if (newState == ClientState.LOGIN) {
                // send login start packet
                client.sendSync(client.createPacket("ServerboundLoginStart",
                        new ServerboundLoginStartPacket(account.getProfileName(), account.getProfileUUID())));
            }
        });

        return super.attach(client);
    }

    // Handler for all incoming packets
    private int onPacketReceived(Packet pc) {
        SecureRandom random = new SecureRandom();
        if (pc.data() instanceof ClientboundEncryptionRequestPacket packet) {
            byte[] publicKeyBytes = packet.getPublicKeyBytes();
            byte[] verifyTokenBytes = packet.getVerifyTokenBytes();
            boolean shouldAuth = packet.isShouldAuth();
            String stringID = packet.getServerID();

            publicProfile.withPublicKey(publicProfile.decodeKey(PublicKey.class, publicKeyBytes));
            this.verifyTokenBytes = verifyTokenBytes;

            // generate private key
            secretKeyBytes = new byte[16];
            random.nextBytes(secretKeyBytes);

            // encrypt verify token
            byte[] verifyTokenEncrypted = publicProfile.encrypt(verifyTokenBytes);

            // authenticate
            if (shouldAuth) {
                try {
                    String accessToken = account.assertAuthenticated();
                    String uuidDashless = account.getProfileUUID().toString().replace("-", "");

                    SHA1.update(stringID.getBytes(StandardCharsets.US_ASCII));
                    SHA1.update(secretKeyBytes);
                    SHA1.update(publicKeyBytes);
                    String hexDigest = new BigInteger(SHA1.digest()).toString(16);

                    JsonObject body = new JsonObject();
                    body.addProperty("accessToken", accessToken);
                    body.addProperty("selectedProfile", uuidDashless);
                    body.addProperty("serverId", hexDigest);

                    var response = accountContext.getHttpClient().send(HttpRequest.newBuilder()
                            .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/join"))
                            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                            .build(), HttpResponse.BodyHandlers.ofString());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            // send encryption response
            client.sendSync(client.createPacket("ServerboundEncryptionResponse",
                    new ServerboundEncryptionResponsePacket(secretKeyBytes, verifyTokenEncrypted)));
        }

        if (pc.data() instanceof ClientboundLoginSuccessPacket packet) {
            this.profileProperties.putAll(packet.getProperties());
            onLoginComplete.invoker().onLoginComplete(client, this, pc);
        }

        try {
            // register the encryption to the client
            SecretKey secretKey = new SecretKeySpec(secretKeyBytes, "AES");
            Cipher encryptionCipher = Cipher.getInstance("AES/CFB8/NoPadding");
            encryptionCipher.init(Cipher.ENCRYPT_MODE, secretKey, random);
            Cipher decryptionCipher = Cipher.getInstance("AES/CFB8/NoPadding");
            decryptionCipher.init(Cipher.DECRYPT_MODE, secretKey, random);
            client.withCiphers(encryptionCipher, decryptionCipher);
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
        }

        return 0;
    }

    public interface ClientLoginCompletedHandler {
        void onLoginComplete(MinecraftClient client,
                             ClientAuthenticator authenticator,
                             Packet acknowledgedPacket);
    }

}
