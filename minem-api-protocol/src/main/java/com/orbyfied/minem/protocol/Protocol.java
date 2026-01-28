package com.orbyfied.minem.protocol;

import com.orbyfied.minem.event.Chain;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.Consumer;

/**
 * Represents the current version protocol implementation and registry/factory.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Protocol implements PacketRegistry {

    public static Protocol create(int protocolNumber) {
        return new Protocol(protocolNumber);
    }

    /**
     * The Minecraft protocol number this implementation facilitates.
     */
    @Getter
    final int protocolNumber;

    /**
     * All registered phase specs.
     */
    List<ProtocolPhaseSpecification> allSpecs = new ArrayList<>();

    /**
     * All phase spec objects by the phase ordinal.
     */
    ProtocolPhaseSpecification[] specsByPhaseOrdinal = new ProtocolPhaseSpecification[0];

    /**
     * The protocol resources added to this protocol.
     */
    final Map<ProtocolResource<?>, Object> resources = new HashMap<>();

    /**
     * The default packet handlers for this protocol.
     */
    @Getter
    final Chain<PacketHandler> packetHandlers = new Chain<>(PacketHandler.class);

    // The cached key mappings
    final Map<Object, Set<PacketMapping>> matchCache = new HashMap<>();

    public Protocol modifyHandlers(Consumer<Chain<PacketHandler>> consumer) {
        consumer.accept(packetHandlers);
        return this;
    }

    /**
     * List all phase mappings implemented by this protocol.
     */
    public List<ProtocolPhaseSpecification> allPhaseSpecs() {
        return Collections.unmodifiableList(allSpecs);
    }

    /**
     * Create and register a new specification for the given phase.
     */
    public ProtocolPhaseSpecification createPhaseSpec(ProtocolPhase phase) {
        ProtocolPhaseSpecification spec = new ProtocolPhaseSpecification(phase);
        registerPhaseSpec(spec);
        return spec;
    }

    /**
     * Register the given phase mapping to this protocol.
     *
     * @param spec The mapping.
     * @return This.
     */
    public Protocol registerPhaseSpec(ProtocolPhaseSpecification spec) {
        int ord = spec.getPhase().ordinal();
        if (ord >= specsByPhaseOrdinal.length) {
            // expand array
            ProtocolPhaseSpecification[] old = specsByPhaseOrdinal;
            specsByPhaseOrdinal = new ProtocolPhaseSpecification[ord + 1];
            System.arraycopy(old, 0, specsByPhaseOrdinal, 0, old.length);
        }

        ProtocolPhaseSpecification current = specsByPhaseOrdinal[ord];
        if (current == null) {
            specsByPhaseOrdinal[ord] = spec;
        } else {
            current.merge(spec);
        }

        allSpecs.add(spec);
        return this;
    }

    /**
     * Get the phase spec for the given phase.
     *
     * @param phase The phase.
     * @return The mapping.
     * @throws IllegalArgumentException If the phase is not mapped for this protocol.
     */
    public ProtocolPhaseSpecification forPhase(ProtocolPhase phase) {
        int ord = phase.ordinal();
        ProtocolPhaseSpecification spec = ord < specsByPhaseOrdinal.length ? specsByPhaseOrdinal[phase.ordinal()] : null;
        if (spec == null) {
            throw new IllegalArgumentException("Phase " + phase.name() + " [" + phase.ordinal() + "] is not supported by this protocol");
        }

        return spec;
    }

    /**
     * Get the phase spec for the given phase or null if absent.
     */
    public ProtocolPhaseSpecification forPhaseOrNull(ProtocolPhase phase) {
        int ord = phase.ordinal();
        return ord < specsByPhaseOrdinal.length ? specsByPhaseOrdinal[phase.ordinal()] : null;
    }

    /**
     * Get the phase spec for the given phase or null if absent.
     */
    public ProtocolPhaseSpecification forPhaseOrNull(int ord) {
        return ord < specsByPhaseOrdinal.length ? specsByPhaseOrdinal[ord] : null;
    }

    public ProtocolPhaseSpecification getOrCreatePhaseSpec(ProtocolPhase phase) {
        ProtocolPhaseSpecification spec = forPhaseOrNull(phase);
        if (spec == null) {
            spec = createPhaseSpec(phase);
        }

        return spec;
    }

    /**
     * Register the given protocol resource with the given key.
     */
    public <T> Protocol with(ProtocolResource<T> key, T value) {
        resources.put(key, value);
        return this;
    }

    /**
     * Get a protocol resource by the given key or null if absent.
     */
    @SuppressWarnings("unchecked")
    public <T> T getResource(ProtocolResource<T> key) {
        return (T) resources.get(key);
    }

    /**
     * Optionally get a protocol resource by the given key.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> optionalResource(ProtocolResource<T> key) {
        return Optional.ofNullable((T) resources.get(key));
    }

    /**
     * Require a protocol resource by the given key.
     */
    @SuppressWarnings("unchecked")
    public <T> T requireResource(ProtocolResource<T> key) {
        T res = (T) resources.get(key);
        if (res == null) {
            throw new IllegalStateException("A protocol resource by key " + key.name() + " of type " + key.klass().getSimpleName() + " is required");
        }

        return res;
    }

    @Override
    public PacketMapping getPacketMapping(int id) {
        throw new UnsupportedOperationException("Protocol can not get mapping by registry ID as it may be shared across phases");
    }

    @Override
    public PacketMapping getPacketMapping(String name) {
        for (PacketRegistry registry : allSpecs) {
            PacketMapping mapping = registry.getPacketMapping(name);
            if (mapping != null) {
                return mapping;
            }
        }

        return null;
    }

    @Override
    public PacketMapping getPacketMapping(Class<?> klass) {
        for (PacketRegistry registry : allSpecs) {
            PacketMapping mapping = registry.getPacketMapping(klass);
            if (mapping != null) {
                return mapping;
            }
        }

        return null;
    }

    public Protocol registerPacketMapping(PacketMapping mapping) {
        getOrCreatePhaseSpec(mapping.getPhase())
                .registerPacketMapping(mapping);
        return this;
    }

    public Protocol registerPacketMappings(Collection<PacketMapping> mappings) {
        for (PacketMapping mapping : mappings) {
            registerPacketMapping(mapping);
        }

        return this;
    }

    public Protocol registerPacketMappings(PacketMapping... mappings) {
        return registerPacketMappings(List.of(mappings));
    }

    @Override
    public List<PacketMapping> allPacketMappings() {
        List<PacketMapping> list = new ArrayList<>();
        allPacketMappings(list);
        return list;
    }

    @Override
    public void allPacketMappings(List<PacketMapping> list) {
        for (PacketRegistry registry : allSpecs) {
            registry.allPacketMappings(list);
        }
    }

    @Override
    public void match(Collection<PacketMapping> list, Object key) {
        Set<PacketMapping> list1 = matchCache.get(key);
        if (list1 == null) {
            list1 = new HashSet<>();
            for (PacketRegistry registry : allSpecs) {
                registry.match(list1, key);
            }

            matchCache.put(key, list1);
        }

        list.addAll(list1);
    }

}
