package com.orbyfied.minem.auth;

import com.orbyfied.minem.profile.MinecraftProfile;
import com.orbyfied.minem.security.SecretStore;
import com.google.gson.*;
import com.microsoft.aad.msal4j.*;
import lombok.Getter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.math.BigInteger;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Represents an authenticated/authenticate-able Minecraft account.
 */
@Getter
public class MinecraftAccount extends SecretStore {

    static final Gson GSON = new GsonBuilder().setLenient().create();
    static final String USER_AGENT = "Java-net-http";
    static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    /* Profile Information */
    private String profileName;
    private UUID profileUUID;
    private MinecraftProfile profile;
    private List<JsonObject> skins;
    private List<JsonObject> capes;
    private int activeSkin;
    private int activeCape;

    /**
     * Check whether this account is fully authenticated.
     * (Has a valid Mojang bearer token)
     */
    public synchronized boolean isAuthenticated() {
        return getValidSecretValue("MojangBearer") != null;
    }

    /**
     * Assert that this account is authenticated and get the Mojang bearer.
     */
    public synchronized String assertAuthenticated() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("This account has not been authenticated yet");
        }

        return getValidSecretValue("MojangBearer");
    }

    /**
     * Signs this account in to Xbox Live and then Mojang/Minecraft using the stored
     * {@code MSAccessToken}.
     *
     * @return This.
     */
    public synchronized CompletableFuture<MinecraftAccount> loginMojang(AuthenticationContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient httpClient = context.getHttpClient();
                JsonObject payload;
                String payloadStr;

                String msAccessToken = getValidSecretValue("MSAccessToken");
                if (msAccessToken == null) {
                    throw new IllegalStateException("No valid MSAccessToken found, authenticate with Microsoft before calling this procedure");
                }

                // step 3: sign in to XBOX Live
                payload = new JsonObject();
                payload.addProperty("RelyingParty", "http://auth.xboxlive.com");
                payload.addProperty("TokenType", "JWT");
                var props = new JsonObject();
                props.addProperty("AuthMethod", "RPS");
                props.addProperty("SiteName", "user.auth.xboxlive.com");
                props.addProperty("RpsTicket", "d=" + /*"d=" +*/ msAccessToken); // const preamble = azure ? 'd=' : 't='
                payload.add("Properties", props);
                payloadStr = GSON.toJson(payload);

                HttpResponse<String> response2 = context.verifyResponse(httpClient.send(HttpRequest.newBuilder()
                        .uri(new URI("https://user.auth.xboxlive.com/user/authenticate"))
                        .POST(HttpRequest.BodyPublishers.ofString(payloadStr))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build(), HttpResponse.BodyHandlers.ofString()));
                payload = GSON.fromJson(response2.body(), JsonObject.class);

                String uhs = payload.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0)
                        .getAsJsonObject().get("uhs").getAsString();
                String xblToken = payload.get("Token").getAsString();
                Instant xblExpiryInstant = Instant.parse(payload.get("NotAfter").getAsString());
                storeToken("XBLToken", xblToken, xblExpiryInstant.toEpochMilli());

                // step 4: sign in to xSTS
                payload = new JsonObject();
                props = new JsonObject();
                JsonArray tokens = new JsonArray();
                tokens.add(xblToken);
                props.addProperty("SandboxId", "RETAIL");
                props.add("UserTokens", tokens);
                payload.add("Properties", props);
                payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                payload.addProperty("TokenType", "JWT");
                payloadStr = GSON.toJson(payload);

                HttpResponse<String> response3 = context.verifyResponse(httpClient.send(HttpRequest.newBuilder()
                        .uri(new URI("https://xsts.auth.xboxlive.com/xsts/authorize"))
                        .POST(HttpRequest.BodyPublishers.ofString(payloadStr))
                        .build(), HttpResponse.BodyHandlers.ofString()));
                payload = GSON.fromJson(response3.body(), JsonObject.class);

                Instant xstsExpiryInstant = Instant.parse(payload.get("NotAfter").getAsString());
                String xstsToken = payload.get("Token").getAsString();
                storeToken("XSTSToken", xstsToken, xstsExpiryInstant.toEpochMilli());

                // step 5: Get Mojang bearer token
                payload = new JsonObject();
                payload.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
                payloadStr = GSON.toJson(payload);
                HttpResponse<String> response4 = context.verifyResponse(httpClient.send(HttpRequest.newBuilder()
                        .uri(new URI("https://api.minecraftservices.com/authentication/login_with_xbox"))
                        .POST(HttpRequest.BodyPublishers.ofString(payloadStr))
                        .header("User-Agent", USER_AGENT)
                        .build(), HttpResponse.BodyHandlers.ofString()));
                payload = GSON.fromJson(response4.body(), JsonObject.class);

                String accessToken = payload.get("access_token").getAsString();
                long tokenDuration = payload.get("expires_in").getAsInt() * 1000L;
                storeToken("MojangBearer", accessToken, System.currentTimeMillis() + tokenDuration);
                return this;
            } catch (Throwable t) {
                throw handleException(new RuntimeException("An error occurred while signing into Xbox Live/Minecraft Services", t));
            }
        }, EXECUTOR);
    }

    /**
     * Try to log to Microsoft and Xbox using MSAL Oauth.
     *
     * @return The account.
     */
    public synchronized CompletableFuture<MinecraftAccount> loginMSA(AuthenticationContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Set<String> scope = Set.of("XboxLive.signin", "XboxLive.offline_access");

                PublicClientApplication pca = context.getAzureClient();
                var result = pca.acquireToken(InteractiveRequestParameters.builder(new URI("http://localhost:5623"))
                        .scopes(scope)
                        .prompt(Prompt.SELECT_ACCOUNT)
                        .build())
                        .join();
                long expiry = result.expiresOnDate().getTime();
                storeToken("MSAccessToken", result.accessToken(), expiry);
                return this;
            } catch (Throwable t) {
                throw handleException(new RuntimeException("An error occurred while authenticating account using MSAL", t));
            }
        });
    }

    /**
     * Retrieve the profile information for this account. This expects the account
     * to already be authenticated.
     */
    public synchronized CompletableFuture<MinecraftAccount> fetchProfile(AuthenticationContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = context.verifyResponse(context.getHttpClient().send(authorize(HttpRequest.newBuilder()
                        .uri(new URI("https://api.minecraftservices.com/minecraft/profile"))
                        .GET()).build(), HttpResponse.BodyHandlers.ofString()));
                JsonObject infoObject = GSON.fromJson(response.body(), JsonObject.class);

                String uuidWithoutDashses = infoObject.get("id").getAsString();
                String name = infoObject.get("name").getAsString();

                BigInteger bi1 = new BigInteger(uuidWithoutDashses.substring(0, 16), 16);
                BigInteger bi2 = new BigInteger(uuidWithoutDashses.substring(16, 32), 16);
                UUID uuid = new UUID(bi1.longValue(), bi2.longValue());

                this.profileName = name;
                this.profileUUID = uuid;
                var profileCache = MinecraftProfile.CACHE;
                this.profile = profileCache.setName(profileCache.referenceUUID(uuid), profileName);

                // parse skins and capes
                skins = new ArrayList<>();
                int i = 0;
                for (JsonElement elem : infoObject.getAsJsonArray("skins")) {
                    JsonObject object = elem.getAsJsonObject();
                    i++;

                    skins.add(object);
                    if (object.has("state") && object.get("state").getAsString().equalsIgnoreCase("active")) {
                        activeSkin = i;
                    }
                }

                capes = new ArrayList<>();
                i = 0;
                for (JsonElement elem : infoObject.getAsJsonArray("capes")) {
                    JsonObject object = elem.getAsJsonObject();
                    i++;

                    capes.add(object);
                }

                return this;
            } catch (Throwable t) {
                throw handleException(new RuntimeException("An error occurred while retrieving the profile for this account", t));
            }
        }, EXECUTOR);
    }

    public synchronized HttpRequest.Builder authorize(HttpRequest.Builder request) {
        assertAuthenticated();
        return request.header("Authorization", "Bearer " + this.getValidSecretValue("MojangBearer"));
    }

    // utility for shit exception handling lol
    private RuntimeException handleException(RuntimeException ex) {
//        ex.printStackTrace();
        return ex;
    }

    /**
     * Build an MSA compatible TLS/HTTP client, optionally routing through
     * the given proxy address.
     *
     * @param proxyAddress The proxy address.
     * @return The client.
     */
    public static HttpClient buildCompatibleHttpClient(InetSocketAddress proxyAddress) {
        try {
            CookieManager cookieManager = new CookieManager();
            HttpClient.Builder builder = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .proxy(ProxySelector.of(proxyAddress))
                    .cookieHandler(cookieManager);

            if (proxyAddress != null) {
                builder.proxy(ProxySelector.of(proxyAddress));
            }

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, new SecureRandom());
            builder.sslContext(sslContext);
            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setNeedClientAuth(false);
//            sslParameters.setWantClientAuth(false);
            sslParameters.setEnableRetransmissions(true);
            builder.sslParameters(sslParameters);

            return builder.build();
        } catch (Throwable t) {
            throw new IllegalArgumentException("Failed to create HttpClient", t);
        }
    }

}
