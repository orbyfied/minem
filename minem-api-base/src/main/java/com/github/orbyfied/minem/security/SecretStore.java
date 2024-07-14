package com.github.orbyfied.minem.security;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Simple storage for {@link Secret}s.
 */
public class SecretStore {

    final Map<String, Secret<?>> secretMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> Secret<T> getSecret(String name) {
        return (Secret<T>) secretMap.computeIfAbsent(name, __ -> new Secret<>(name));
    }

    @SuppressWarnings("unchecked")
    public <T, S extends Secret<T>> S getSecret(String name, Function<String, S> computer) {
        return (S) secretMap.computeIfAbsent(name, computer);
    }

    public <T> T getValidSecretValue(String name) {
        Secret<T> secret = getSecret(name);
        return secret.isValid() ? secret.getValue() : null;
    }

    public SecretStore storeSecret(Secret<?> secret) {
        secretMap.put(secret.getName(), secret);
        return this;
    }

    public SecretStore storeStringSecret(String name, String value) {
        return storeSecret(new Secret<>(name).value(value));
    }

    public SecretStore storeToken(String name, String value, long duration) {
        return storeSecret(new Token(name).value(value).timeObtained(System.currentTimeMillis()).duration(duration));
    }

}
