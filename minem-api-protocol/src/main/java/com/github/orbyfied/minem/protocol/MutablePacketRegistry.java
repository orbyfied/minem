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
    /** All mappings by class. */
    protected Map<Class<?>, PacketMapping> mappingsByClass = new HashMap<>();

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
    public PacketMapping getPacketMapping(Class<?> klass) {
        return mappingsByClass.get(klass);
    }

    @Override
    public PacketRegistry registerPacketMapping(PacketMapping mapping) {
        mappingList.add(mapping);

        // append to index
        int id = mapping.getRegistryId();
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

        // append to by class
        mappingsByClass.put(mapping.getDataClass(), mapping);

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

    @Override
    public void allPacketMappings(List<PacketMapping> list) {
        list.addAll(this.mappingList);
    }

    @Override
    public void match(Collection<PacketMapping> list, Object key) {
        if (key instanceof String str) {
            PacketMapping byAlias = mappingsByAlias.get(str);
            if (byAlias != null) {
                list.add(byAlias);
            }
        } else if (key instanceof Class<?> klass) {
            PacketMapping byClass = mappingsByClass.get(klass);
            if (byClass != null) {
                list.add(byClass);
                return;
            }

            // check for data interfaces
            for (PacketMapping mapping : mappingList) {
                if (mapping.getDataInterfaces().contains(klass)) {
                    list.add(mapping);
                }
            }
        }
    }

}
