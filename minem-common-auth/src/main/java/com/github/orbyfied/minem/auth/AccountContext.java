package com.github.orbyfied.minem.auth;

import java.net.http.HttpClient;

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

    HttpClient getHttpClient();

}
