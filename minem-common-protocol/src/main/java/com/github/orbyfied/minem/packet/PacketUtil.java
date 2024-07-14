package com.github.orbyfied.minem.packet;

import com.github.orbyfied.minem.protocol.PacketMapping;

import java.util.List;

public class PacketUtil {

    public static PacketMapping[] compileAll(Class<?>[] classes) {
        PacketMapping[] mappings = new PacketMapping[classes.length];
        for (int i = 0; i < classes.length; i++) {
            mappings[i] = PacketMapping.compile(classes[i]);
        }

        return mappings;
    }

}
