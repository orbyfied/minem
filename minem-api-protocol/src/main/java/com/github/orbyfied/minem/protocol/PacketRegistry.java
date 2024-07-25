package com.github.orbyfied.minem.protocol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface PacketRegistry {

    static int getNetworkID(int registryId, Destination dest) {
        return (registryId - dest.getIdX2Offset()) / 2;
    }

    static int getRegistryID(int networkId, Destination dest) {
        return networkId * 2 + dest.getIdX2Offset();
    }

    default PacketMapping getClientboundPacketMapping(int networkID) {
        return getPacketMapping(networkID * 2 + Destination.CLIENTBOUND.getIdX2Offset());
    }

    default PacketMapping getServerboundPacketMapping(int networkID) {
        return getPacketMapping(networkID * 2 + Destination.SERVERBOUND.getIdX2Offset());
    }

    /**
     * Get the packet mapping for the given registry ID.
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
     * Get the packet mapping by the given data class.
     *
     * @param klass The class.
     * @return The mapping.
     */
    PacketMapping getPacketMapping(Class<?> klass);

    /**
     * Require the packet mapping to be present, then return it.
     *
     * @param id The ID.
     * @return The mapping.
     * @throws IllegalArgumentException If the mapping is absent.
     */
    default PacketMapping requirePacketMapping(int id) {
        PacketMapping mapping = getPacketMapping(id);
        if (mapping == null) {
            throw new IllegalArgumentException("No packet mapping for registry ID 0x" + Integer.toHexString(id));
        }

        return mapping;
    }

    /**
     * Require the packet mapping to be present, then return it.
     *
     * @param name The name.
     * @return The mapping.
     * @throws IllegalArgumentException If the mapping is absent.
     */
    default PacketMapping requirePacketMapping(String name) {
        PacketMapping mapping = getPacketMapping(name);
        if (mapping == null) {
            throw new IllegalArgumentException("No packet mapping for name '" + name + "'");
        }

        return mapping;
    }

    /**
     * Require the packet mapping to be present, then return it.
     *
     * @param klass The class.
     * @return The mapping.
     * @throws IllegalArgumentException If the mapping is absent.
     */
    default PacketMapping requirePacketMapping(Class<?> klass) {
        PacketMapping mapping = getPacketMapping(klass);
        if (mapping == null) {
            throw new IllegalArgumentException("No packet mapping for data class " + klass.getName());
        }

        return mapping;
    }

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

    default PacketRegistry compileAndRegister(Class<?> klass) {
        return registerPacketMapping(PacketMapping.compileMapping(klass));
    }

    /**
     * List all registered packet mappings.
     *
     * @return The mappings.
     */
    List<PacketMapping> allPacketMappings();

    /**
     * List all registered packet mappings into the given list.
     *
     * @param list The destination list.
     */
    void allPacketMappings(List<PacketMapping> list);

    /**
     * Try and list all mappings which are described by the given key.
     *
     * @param list The destination collection.
     * @param key The key, could be a name, ID, data class, etc.
     */
    void match(Collection<PacketMapping> list, Object key);

    /**
     * Try and list all mappings which are described by the given key.
     *
     * @param key The key, could be a name, ID, data class, etc.
     * @return The mappings.
     */
    default Collection<PacketMapping> match(Object key) {
        List<PacketMapping> list = new ArrayList<>();
        match(list, key);
        return list;
    }

}
