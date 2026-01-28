package com.orbyfied.minem.security;

import lombok.Getter;

import java.util.Optional;
import java.util.Properties;

/**
 * Represents a special type of secret, a token, which is always a string
 * and has an expiry time.
 */
@Getter
public class Token extends Secret<String> {

    long timeObtained;
    long expiry;

    public Token(String name) {
        super(name);
    }

    @Override
    public boolean isValid() {
        if (expiry == -1) return false; // never valid
        if (expiry == -2) return true; // always valid
        return super.isValid() && expiry > System.currentTimeMillis();
    }

    public Token timeObtained(long timeObtained) {
        this.timeObtained = timeObtained;
        return this;
    }

    public Token expiry(long expiry) {
        this.expiry = expiry;
        return this;
    }

    public Token permanent() {
        this.expiry = -1;
        return this;
    }

    @Override
    public synchronized Token value(String value) {
        return (Token) super.value(value);
    }

    public enum ExpiryPresencePolicy {
        OR_INVALID,
        OR_PERMANENT;
    }

    public static Optional<Token> namedFromProperties(Properties properties, String name, ExpiryPresencePolicy expiryPresencePolicy) {
        final String valueKey = "tk." + name + ".value";
        final String expiryKey = "tk." + name + ".expiry";
        String value = properties.getProperty(valueKey);
        if (value == null) {
            return Optional.empty();
        }

        String expiryStr = properties.getProperty(expiryKey);
        if (expiryStr == null && expiryPresencePolicy == ExpiryPresencePolicy.OR_INVALID) {
            return Optional.empty();
        }

        long expiry = expiryStr != null ? Long.parseLong(expiryStr) : /* perm */ -2;
        if (expiry != -2 && System.currentTimeMillis() > expiry) {
            return Optional.empty();
        }

        return Optional.of(new Token(name).value(value).expiry(expiry));
    }

    public static void saveToProperties(Properties properties, Token token) {
        final String valueKey = "tk." + token.name + ".value";
        final String expiryKey = "tk." + token.name + ".expiry";
        properties.setProperty(valueKey, token.value);
        if (token.expiry != 0) {
            properties.setProperty(expiryKey, Long.toString(token.expiry));
        }
    }

}
