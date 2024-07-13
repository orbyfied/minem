package com.github.orbyfied.minem.protocol;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents the mapping of a packet from a universal representation to
 * a version specific implementation.
 */
@RequiredArgsConstructor
@Getter
public class PacketMapping {

    final int protocolVersion;
    final int id;
    final String primaryName;
    final String[] aliases;

}
