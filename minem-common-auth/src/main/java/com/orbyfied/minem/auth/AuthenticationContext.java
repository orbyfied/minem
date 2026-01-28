package com.orbyfied.minem.auth;

import com.orbyfied.minem.http.HttpUtil;
import com.microsoft.aad.msal4j.PublicClientApplication;
import slatepowered.veru.misc.ANSI;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

/**
 * The context in which authentication and other {@link MinecraftAccount} operations happen.
 */
public interface AuthenticationContext {

    static AuthenticationContext azure(final String clientId) {
        return new AuthenticationContext() {
            static final String authority = "https://login.microsoftonline.com/consumers";
            final PublicClientApplication app;
            final HttpClient httpClient = MinecraftAccount.buildCompatibleHttpClient(null);

            {
                try {
                    app = PublicClientApplication.builder(clientId)
                            .authority(authority)
                            .build();
                } catch (Exception ex) {
                    throw new ExceptionInInitializerError(ex);
                }
            }

            @Override
            public PublicClientApplication getAzureClient() {
                return app;
            }

            @Override
            public HttpClient getHttpClient() {
                return httpClient;
            }

            @Override
            public boolean isDebugMode() {
                return true;
            }
        };
    }

    /**
     * Get the Azure client application.
     *
     * @return The client app.
     */
    PublicClientApplication getAzureClient();

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
    default <T> HttpResponse<T> verifyResponse(HttpResponse<T> response) {
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
