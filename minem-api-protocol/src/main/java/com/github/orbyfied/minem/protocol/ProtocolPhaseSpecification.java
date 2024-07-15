package com.github.orbyfied.minem.protocol;

import com.github.orbyfied.minem.event.Chain;
import com.github.orbyfied.minem.event.Placement;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

/**
 * Represents the registry/specification for one game phase.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ProtocolPhaseSpecification extends MutablePacketRegistry {

    public static ProtocolPhaseSpecification create(ProtocolPhase phase) {
        return new ProtocolPhaseSpecification(phase);
    }

    final ProtocolPhase phase;

    /**
     * The default packet handlers for this protocol phase spec.
     */
    final Chain<PacketHandler> packetHandlers = new Chain<>(PacketHandler.class);

    /**
     * Append all registrations from the given mapping to this one.
     */
    @SuppressWarnings("unchecked")
    public void merge(ProtocolPhaseSpecification other) {
        for (PacketMapping mapping : other.allPacketMappings()) {
            this.registerPacketMapping(mapping);
        }

        packetHandlers.add(other.getPacketHandlers(), Placement.LAST);
    }

    @Override
    public ProtocolPhaseSpecification registerPacketMapping(PacketMapping mapping) {
        return (ProtocolPhaseSpecification) super.registerPacketMapping(mapping);
    }

    @Override
    public ProtocolPhaseSpecification registerPacketMappings(Collection<PacketMapping> mappings) {
        return (ProtocolPhaseSpecification) super.registerPacketMappings(mappings);
    }

    @Override
    public ProtocolPhaseSpecification registerPacketMappings(PacketMapping... mappings) {
        return (ProtocolPhaseSpecification) super.registerPacketMappings(mappings);
    }

}
