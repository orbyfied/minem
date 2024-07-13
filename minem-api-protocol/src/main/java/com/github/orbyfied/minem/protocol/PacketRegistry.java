package com.github.orbyfied.minem.protocol;

import java.util.Collection;
import java.util.List;

public interface PacketRegistry {

    /**
     * Get the packet mapping for the given numerical ID.
     *
     * @param id The ID.
     * @return The mapping.
     */
    PacketMapping getPacketMapping(int id);

    /**
     * Get the packet mapping by the given name.
     *
     * @param name The name.
     * @return The mapping.
     */
    PacketMapping getPacketMapping(String name);

    /**
     * Try and register the given mapping to this registry.
     *
     * @param mapping The mapping.
     * @throws UnsupportedOperationException If this registry is immutable.
     * @return This.
     */
    default PacketRegistry registerPacketMapping(PacketMapping mapping) {
        throw new UnsupportedOperationException("This registry does not support modification");
    }

    /**
     * Try and register the given mappings to this registry.
     *
     * @param mappings The mapping.
     * @throws UnsupportedOperationException If this registry is immutable.
     * @return This.
     */
    default PacketRegistry registerPacketMappings(Collection<PacketMapping> mappings) {
        throw new UnsupportedOperationException("This registry does not support modification");
    }

    /**
     * Try and register the given mappings to this registry.
     *
     * @param mappings The mapping.
     * @throws UnsupportedOperationException If this registry is immutable.
     * @return This.
     */
    default PacketRegistry registerPacketMappings(PacketMapping... mappings) {
        return registerPacketMappings(List.of(mappings));
    }

    /**
     * List all registered packet mappings.
     *
     * @return The mappings.
     */
    List<PacketMapping> allPacketMappings();

}
