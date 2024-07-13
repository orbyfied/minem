package com.github.orbyfied.minem.protocol;

import java.util.*;

public class MutablePacketRegistry implements PacketRegistry {

    static final int HIGH_ID_THRESHOLD = 256 * 8;

    /** The mappings in a list for easy iteration. */
    protected List<PacketMapping> mappingList = new ArrayList<>();
    /** The mappings mapped by ID using an array. */
    protected PacketMapping[] mappingsById = new PacketMapping[255];
    /** All mappings with an ID above {@link #HIGH_ID_THRESHOLD}. */
    protected Map<Integer, PacketMapping> mappingsByIdHigh = new HashMap<>();
    /** All mappings by string alias. */
    protected Map<String, PacketMapping> mappingsByAlias = new HashMap<>();

    @Override
    public PacketMapping getPacketMapping(int id) {
        return id < mappingsById.length ? mappingsById[id] :
                id >= HIGH_ID_THRESHOLD ? mappingsByIdHigh.get(id) : null;
    }

    @Override
    public PacketMapping getPacketMapping(String name) {
        return mappingsByAlias.get(name);
    }

    @Override
    public PacketRegistry registerPacketMapping(PacketMapping mapping) {
        mappingList.add(mapping);

        // append to index
        int id = mapping.getId();
        if (id < HIGH_ID_THRESHOLD) {
            if (id >= mappingsById.length) {
                PacketMapping[] old = mappingsById;
                mappingsById = new PacketMapping[id + /* extra room */ 20];
                System.arraycopy(old, 0, mappingsById, 0, old.length);
            }

            mappingsById[id] = mapping;
        } else {
            mappingsByIdHigh.put(id, mapping);
        }

        // append to aliases
        mappingsByAlias.put(mapping.getPrimaryName(), mapping);
        for (String alias : mapping.getAliases()) {
            mappingsByAlias.put(alias, mapping);
        }

        return this;
    }

    @Override
    public PacketRegistry registerPacketMappings(Collection<PacketMapping> mappings) {
        for (PacketMapping mapping : mappings) {
            registerPacketMapping(mapping);
        }

        return this;
    }

    @Override
    public List<PacketMapping> allPacketMappings() {
        return Collections.unmodifiableList(this.mappingList);
    }

}
