package com.github.orbyfied.minem.auth;

import slatepowered.veru.misc.ANSI;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

/**
 * The context in which authentication and other {@link MinecraftAccount} operations happen.
 */
public interface AccountContext {

    static AccountContext create() {
        return new AccountContext() {
            final HttpClient client = MinecraftAccount.buildMSACompatibleHttpClient(null);

            @Override
            public HttpClient getHttpClient() {
                return client;
            }
        };
    }

    boolean DEBUG_MODE = true;

    /**
     * Get the HTTP client to be used for authentication requests.
     *
     * @return The HTTP client.
     */
    HttpClient getHttpClient();

    // verify and log the given response
    default  <T> HttpResponse<T> verifyResponse(HttpResponse<T> response) {
        if (DEBUG_MODE) {
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
