package com.github.orbyfied.minem.auth;

import com.github.orbyfied.minem.http.HttpUtil;
import com.github.orbyfied.minem.profile.MinecraftProfile;
import com.github.orbyfied.minem.security.SecretStore;
import com.google.gson.*;
import lombok.Getter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.math.BigInteger;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an authenticated/authenticate-able Minecraft account.
 */
@Getter
public class MinecraftAccount extends SecretStore {

    static final String CLIENT_ID = "000000004C12AE6F";

    static final Gson GSON = new GsonBuilder().setLenient().create();
    static final String USER_AGENT = "Java-net-http";
    static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    static final Pattern SFT_PATTERN = Pattern.compile("value=\"(.+?)\"");
    static final Pattern URP_PATTERN = Pattern.compile("urlPost:'(.+?)'");

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
    public synchronized CompletableFuture<MinecraftAccount> loginMojang(AccountContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient httpClient = context.getHttpClient();
                JsonObject payload;
                String payloadStr;

                String msAccessToken = getSecretValue("MSAccessToken");
                if (msAccessToken == null) {
                    throw new IllegalStateException("No MSAccessToken found, authenticate with Microsoft before calling this procedure");
                }

                // step 3: sign in to XBOX Live
                payload = new JsonObject();
                payload.addProperty("RelyingParty", "http://auth.xboxlive.com");
                payload.addProperty("TokenType", "JWT");
                var props = new JsonObject();
                props.addProperty("AuthMethod", "RPS");
                props.addProperty("SiteName", "user.auth.xboxlive.com");
                props.addProperty("RpsTicket", /*"d=" +*/ msAccessToken); // const preamble = azure ? 'd=' : 't='
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
                storeToken("XBLToken", xblToken, xblExpiryInstant.until(Instant.now(), ChronoUnit.MILLIS));

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
                storeToken("XSTSToken", xstsToken, xstsExpiryInstant.until(Instant.now(), ChronoUnit.MILLIS));

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
                storeToken("MojangBearer", accessToken, tokenDuration);
                return this;
            } catch (Throwable t) {
                throw handleException(new RuntimeException("An error occurred while signing into Xbox Live", t));
            }
        }, EXECUTOR);
    }

    /**
     * Try to log to Microsoft and Xbox with email and password.
     *
     * @return The account.
     */
    public synchronized CompletableFuture<MinecraftAccount> loginMSA(AccountContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient httpClient = context.getHttpClient();
                CookieManager cookieManager = (CookieManager) httpClient.cookieHandler().get();
                String body;

                String email = getSecretValue("Email");
                String password = getSecretValue("Password");

                // step 1: extract SFT
                String sftStr = context.verifyResponse(httpClient.send(HttpRequest.newBuilder()
                        .uri(URI.create("https://login.live.com/oauth20_authorize.srf?client_id=" + CLIENT_ID + "&redirect_uri=https://login.live.com/oauth20_desktop.srf&scope=service::user.auth.xboxlive.com::MBI_SSL&response_type=token&locale=en"))
                        .header("User-Agent", USER_AGENT)
                        .GET()
                        .build(), HttpResponse.BodyHandlers.ofString())).body();
                Matcher matcher;
                matcher = SFT_PATTERN.matcher(sftStr);
                matcher.find();
                String sftValue = matcher.group(1);
                matcher = URP_PATTERN.matcher(sftStr);
                matcher.find();
                String urlPost = matcher.group(1);
                URI uriPost = new URI(urlPost);

                // step 2: sign in to Microsoft and extract values from redirect
                String emailEncoded = URLEncoder.encode(email, StandardCharsets.UTF_8);
                String passwordEncoded = URLEncoder.encode(password, StandardCharsets.UTF_8);
                String sftValueEncoded = URLEncoder.encode(sftValue, StandardCharsets.UTF_8);

                Map<String, String> urlPostParams = HttpUtil.getQueryMap(uriPost.getQuery());
                String contextId = urlPostParams.get("contextid");
                String clientId = urlPostParams.get("client_id");

                CookieStore store = cookieManager.getCookieStore();
                store.add(uriPost, new HttpCookie("ContextID", contextId));
                store.add(uriPost, new HttpCookie("contextID", contextId));
                store.add(uriPost, new HttpCookie("contextId", contextId));
                store.add(uriPost, new HttpCookie("ContextId", contextId));
                store.add(uriPost, new HttpCookie("contextid", contextId));
                store.add(uriPost, new HttpCookie("context_id", contextId));

                body = "login=" + emailEncoded + "&loginfmt=" + emailEncoded + "&passwd=" + passwordEncoded + "&PPFT=" + sftValueEncoded;
                HttpResponse<String> response1 = context.verifyResponse(httpClient.send(HttpRequest.newBuilder()
                        .uri(uriPost)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .header("User-Agent", USER_AGENT)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .build(), HttpResponse.BodyHandlers.ofString()));

                String redirectUri = response1.request().uri().toString();
                if (redirectUri.equalsIgnoreCase(urlPost) && !redirectUri.contains("access_token")) {
                    throw new IllegalStateException("MSA LOGIN FAILED: Redirect failed, no access_token");
                }

                body = response1.body();
                if (body.contains("Sign in to")) {
                    throw new IllegalStateException("MSA LOGIN FAILED: Invalid credentials, Sign in to");
                }

                if (body.contains("Help us protect your account")) {
                    throw new IllegalStateException("MSA LOGIN FAILED: 2fa is enabled");
                }

                if (!redirectUri.contains("access_token")) {
                    throw new IllegalStateException("MSA LOGIN FAILED: no access_token in redirect");
                }

                String params = redirectUri.split("#")[1];
                Map<String, String> loginData = new HashMap<>();
                for (String str : params.split("&")) {
                    String[] split = str.split("=");
                    loginData.put(split[0], URLDecoder.decode(split[1], StandardCharsets.UTF_8));
                }

                storeToken("MSAccessToken", loginData.get("access_token"), -1);
                return this;
            } catch (Throwable t) {
                throw handleException(new RuntimeException("An error occurred while authenticating account using MSA email-passwd", t));
            }
        });
    }

    /**
     * Retrieve the profile information for this account. This expects the account
     * to already be authenticated.
     */
    public synchronized CompletableFuture<MinecraftAccount> fetchProfile(AccountContext context) {
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

    // handle the given exception
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
    public static HttpClient buildMSACompatibleHttpClient(InetSocketAddress proxyAddress) {
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
