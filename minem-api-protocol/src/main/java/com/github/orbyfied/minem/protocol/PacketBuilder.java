package com.github.orbyfied.minem.protocol;

import com.github.orbyfied.minem.reflect.UnsafeFieldDesc;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Allows building of packets using a builder pattern.
 */
public class PacketBuilder {

    final PacketMapping mapping;
    final Packet packet;

    Map<String, UnsafeFieldDesc> fields;
    Object data;

    public PacketBuilder(PacketMapping mapping, Packet packet) {
        this.mapping = mapping;
        this.packet = packet;

        fields = mapping.getFields();
        data = packet.getData();
    }

    public PacketBuilder set(String name, Object val) {
        fields.get(name).setFromObject(data, val);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <I> PacketBuilder modify(Consumer<I> itfConsumer) {
        itfConsumer.accept((I) data);
        return this;
    }

    public Packet build() {
        return packet;
    }

}
