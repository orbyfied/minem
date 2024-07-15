package com.github.orbyfied.minem.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Simple storage for {@link Secret}s.
 */
public class SecretStore {

    final Map<String, Secret<?>> secretMap = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> Secret<T> getSecret(String name) {
        return (Secret<T>) secretMap.computeIfAbsent(name.toLowerCase(), __ -> new Secret<>(name));
    }

    @SuppressWarnings("unchecked")
    public <T, S extends Secret<T>> S getSecret(String name, Function<String, S> computer) {
        return (S) secretMap.computeIfAbsent(name.toLowerCase(), __ -> computer.apply(name));
    }

    public <T> T getValidSecretValue(String name) {
        Secret<T> secret = getSecret(name);
        return secret.isValid() ? secret.getValue() : null;
    }

    @SuppressWarnings("unchecked")
    public <T> T getSecretValue(String name) {
        return (T) getSecret(name).getValue();
    }

    public SecretStore storeSecret(Secret<?> secret) {
        secretMap.put(secret.getName().toLowerCase(), secret);
        return this;
    }

    public SecretStore storeStringSecret(String name, String value) {
        return storeSecret(new Secret<>(name).value(value));
    }

    public SecretStore storeToken(String name, String value, long duration) {
        return storeSecret(new Token(name).value(value).timeObtained(System.currentTimeMillis()).duration(duration));
    }

}
