package com.github.orbyfied.minem.protocol;

import com.github.orbyfied.minem.event.Chain;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents the current version protocol implementation and registry/factory.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Protocol {

    public static Protocol create(int protocolNumber) {
        return new Protocol(protocolNumber);
    }

    /**
     * The Minecraft protocol number this implementation facilitates.
     */
    @Getter
    final int protocolNumber;

    /**
     * All phase spec objects by the phase ordinal.
     */
    ProtocolPhaseSpecification[] specsByPhaseOrdinal = new ProtocolPhaseSpecification[0];

    /**
     * The default packet handlers for this protocol.
     */
    @Getter
    final Chain<PacketHandler> packetHandlers = new Chain<>(PacketHandler.class);

    public Protocol modifyHandlers(Consumer<Chain<PacketHandler>> consumer) {
        consumer.accept(packetHandlers);
        return this;
    }

    /**
     * List all phase mappings implemented by this protocol.
     */
    public List<ProtocolPhaseSpecification> allPhaseSpecs() {
        List<ProtocolPhaseSpecification> list = new ArrayList<>();
        for (int i = 0; i < specsByPhaseOrdinal.length; i++) {
            ProtocolPhaseSpecification mapping = specsByPhaseOrdinal[i];
            if (mapping != null) {
                list.add(mapping);
            }
        }

        return list;
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

}
