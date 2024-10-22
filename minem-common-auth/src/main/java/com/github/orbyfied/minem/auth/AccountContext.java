package com.github.orbyfied.minem.auth;

import com.github.orbyfied.minem.http.HttpUtil;
import slatepowered.veru.misc.ANSI;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

/**
 * The context in which authentication and other {@link MinecraftAccount} operations happen.
 */
public interface AccountContext {

    static AccountContext create(boolean debug) {
        return new AccountContext() {
            final HttpClient client = MinecraftAccount.buildMSACompatibleHttpClient(null);

            @Override
            public HttpClient getHttpClient() {
                return client;
            }

            @Override
            public boolean isDebugMode() {
                return debug;
            }
        };
    }

    static AccountContext create() {
        return create(false);
    }

    /**
     * Get the HTTP client to be used for authentication requests.
     *
     * @return The HTTP client.
     */
    HttpClient getHttpClient();

    /**
     * Check if this account context is in debug mode.
     *
     * @return Whether it is in debug mode.
     */
    boolean isDebugMode();

    // verify and log the given response
    default  <T> HttpResponse<T> verifyResponse(HttpResponse<T> response) {
        if (isDebugMode()) {
            System.out.println(ANSI.CYAN + "[*] Received Response :" + response.statusCode() + " to " + response.uri() + ANSI.RESET);
            HttpUtil.trace(response);
            String body = response.body().toString();
            final int maxBodyLen = 750;
            if (body.length() > maxBodyLen) body = body.substring(0, maxBodyLen).replace("\n", "");
            System.out.println(ANSI.BLUE + "        Body: " + ANSI.GRAY + body + ANSI.RESET);
        }

        if (response.statusCode() < 200 || response.statusCode() > 300) {
            throw new IllegalStateException("Http request failed: to URL " + response.uri() + " got: code " + response.statusCode());
        }

        return response;
    }

}
