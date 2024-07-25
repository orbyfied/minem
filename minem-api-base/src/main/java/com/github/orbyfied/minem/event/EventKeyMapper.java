package com.github.orbyfied.minem.event;

import java.util.Collection;

public interface EventKeyMapper<K> {

    Collection<K> mapKeys(Object obj);

}
