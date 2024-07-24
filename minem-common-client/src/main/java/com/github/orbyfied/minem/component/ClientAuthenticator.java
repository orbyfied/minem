package com.github.orbyfied.minem.component;

import com.github.orbyfied.minem.ClientComponent;
import com.github.orbyfied.minem.ClientState;
import com.github.orbyfied.minem.MinecraftClient;
import com.github.orbyfied.minem.auth.AccountContext;
import com.github.orbyfied.minem.auth.MinecraftAccount;
import com.github.orbyfied.minem.event.Chain;
import com.github.orbyfied.minem.protocol.login.*;
import com.github.orbyfied.minem.protocol.Packet;
import com.google.gson.JsonObject;
import lombok.Getter;
import slatepowered.veru.misc.Throwables;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * Performs encryption and authentication with a server and Minecraft services
 * needed for connecting to online mode servers.
 */
public class ClientAuthenticator extends ClientComponent {

    static final MessageDigest SHA1;
    static final KeyFactory RSA_KEY_FACTORY;

    static {
        try {
            SHA1 = MessageDigest.getInstance("SHA-1");
            RSA_KEY_FACTORY = KeyFactory.getInstance("RSA");
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    @Getter
    AccountContext accountContext;

    @Getter
    MinecraftAccount account;

    /** The properties received from the server after login. */
    Map<String, LoginProperty> profileProperties = new HashMap<>();

    PublicKey publicKey;     // The public key
    byte[] verifyTokenBytes; // The bytes of the verification token
    byte[] secretKeyBytes;   // The secret key bytes

    SecureRandom random = new SecureRandom();

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
        this.publicKey = null;
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
        // encryption request
        if (pc.data() instanceof ClientboundEncryptionRequestPacket packet) {
            try {
                byte[] publicKeyBytes = packet.getPublicKeyBytes();
                this.verifyTokenBytes = packet.getVerifyTokenBytes();
                boolean shouldAuth = packet.isShouldAuth();
                String stringID = packet.getServerID();

                this.publicKey = RSA_KEY_FACTORY.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
                Cipher rsa = Cipher.getInstance("RSA");
                rsa.init(Cipher.ENCRYPT_MODE, publicKey);

                // generate private key
                secretKeyBytes = new byte[16];
                random.nextBytes(secretKeyBytes);

                byte[] encryptedSecret = rsa.doFinal(secretKeyBytes);
                byte[] encryptedVerifyToken = rsa.doFinal(verifyTokenBytes);

                // authenticate
                if (shouldAuth) {
                    String accessToken = account.assertAuthenticated();
                    String uuidDashless = account.getProfileUUID().toString().replace("-", "");

                    SHA1.update(stringID.getBytes(StandardCharsets.US_ASCII));
                    SHA1.update(secretKeyBytes);
                    SHA1.update(publicKey.getEncoded());
                    String hexDigest = new BigInteger(SHA1.digest()).toString(16);

                    JsonObject body = new JsonObject();
                    body.addProperty("accessToken", accessToken);
                    body.addProperty("selectedProfile", uuidDashless);
                    body.addProperty("serverId", hexDigest);

                    var response = accountContext.verifyResponse(accountContext.getHttpClient().send(HttpRequest.newBuilder()
                            .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/join"))
                            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                            .build(), HttpResponse.BodyHandlers.ofString()));
                }

                // send encryption response
                client.sendSync(client.createPacket("ServerboundEncryptionResponse",
                        new ServerboundEncryptionResponsePacket(encryptedSecret, encryptedVerifyToken)));

                // register the encryption to the client
                SecretKey secretKey = new SecretKeySpec(secretKeyBytes, "AES");
                IvParameterSpec parameterSpec = new IvParameterSpec(secretKeyBytes);
                Cipher encryptionCipher = Cipher.getInstance("AES/CFB8/NoPadding");
                encryptionCipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
                Cipher decryptionCipher = Cipher.getInstance("AES/CFB8/NoPadding");
                decryptionCipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
                client.withCiphers(encryptionCipher, decryptionCipher);
            } catch (Exception ex) {
                Throwables.sneakyThrow(ex);
            }
        }

        // success packet
        if (pc.data() instanceof ClientboundLoginSuccessPacket packet) {
            this.profileProperties.putAll(packet.getProperties());
            onLoginComplete.invoker().onLoginComplete(client, this, pc);
            client.switchState(ClientState.PLAY);
        }

        return 0;
    }

    /**
     * Event Handler: called when the client successfully completed the login process, as marked
     * by the receiving of the {@link ClientboundLoginSuccessPacket} packet.
     */
    public interface ClientLoginCompletedHandler {
        void onLoginComplete(MinecraftClient client,
                             ClientAuthenticator authenticator,
                             Packet acknowledgedPacket);
    }

}
