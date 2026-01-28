package com.orbyfied.minem.protocol;

public class PacketUtil {

    /**
     * Compile all given packet mapping classes into packet mapping objects
     * which may be registered.
     *
     * @param classes The classes to compile.
     * @return The mappings.
     */
    public static PacketMapping[] compileAll(Class<?>[] classes) {
        PacketMapping[] mappings = new PacketMapping[classes.length];
        for (int i = 0; i < classes.length; i++) {
            mappings[i] = PacketMapping.compileMapping(classes[i]);
            System.out.println("Compiled: " + mappings[i]);
        }

        return mappings;
    }

}
