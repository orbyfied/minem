package com.github.orbyfied.minem.security;

/**
 * Represents a special type of secret, a token, which is always a string
 * and has an expiry time.
 */
public class Token extends Secret<String> {

    long timeObtained;
    long duration;

    public Token(String name) {
        super(name);
    }

    @Override
    public boolean isValid() {
        return super.isValid() && timeObtained + duration > System.currentTimeMillis();
    }

    public Token timeObtained(long timeObtained) {
        this.timeObtained = timeObtained;
        return this;
    }

    public Token duration(long duration) {
        this.duration = duration;
        return this;
    }

    @Override
    public synchronized Token value(String value) {
        return (Token) super.value(value);
    }

}
