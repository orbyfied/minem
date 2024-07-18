package com.github.orbyfied.minem.security;

import lombok.Getter;

/**
 * Represents a special type of secret, a token, which is always a string
 * and has an expiry time.
 */
@Getter
public class Token extends Secret<String> {

    long timeObtained;
    long duration;

    public Token(String name) {
        super(name);
    }

    @Override
    public boolean isValid() {
        if (duration == -1) return false; // never valid
        if (duration == -2) return true; // always valid
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
