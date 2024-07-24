package com.github.orbyfied.minem.protocol.login;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class LoginProperty {
    final String name;      // The name of the property
    final String value;     // The value of the property
    final boolean signed;   // Whether this property has been signed
    final String signature; // The signature if signed
}
