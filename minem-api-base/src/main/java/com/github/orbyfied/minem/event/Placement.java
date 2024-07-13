package com.github.orbyfied.minem.event;

import java.util.List;
import java.util.function.Predicate;

/**
 * Finds an index to place an element
 * of type T at inside an ordered collection
 * of type T.
 *
 * @param <T> The element type.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
public interface Placement<T> {

    static <T> Placement<T> at(int offset) {
        return (list, value) -> {
            if (offset < 0) {
                return list.size() - 1 + offset;
            } else {
                return offset;
            }
        };
    }

    static <T> Placement<T> last() {
        return LAST;
    }

    static <T> Placement<T> last(int offset) {
        return (list, value) -> list.size() + offset;
    }

    // single instance first
    Placement FIRST = (list, value) -> 0;
    Placement LAST = (list, value) -> list.size();

    @SuppressWarnings("unchecked")
    static <T> Placement<T> first() {
        return (Placement<T>) FIRST;
    }

    static <T> Placement<T> before(Predicate<T> predicate) {
        return (list, value) -> {
            final int l = list.size();
            for (int i = 0; i < l; i++) {
                T t = list.get(i);

                if (predicate.test(t)) {
                    return i;
                }
            }

            return -1;
        };
    }

    ////////////////////////////////

    /**
     * Finds the index to position the
     * given value in a given list.
     *
     * @param list The list to find it in.
     * @param value The value to position.
     * @return The index or -1.
     */
    int find(List<T> list, T value);

    /** Find a position and throw an exception if no position was found. */
    default int findChecked(List<T> list, T value) {
        int index = find(list, value);
        if (index == -1)
            throw new IllegalArgumentException("Could not find an index for the given element");
        return index;
    }

    default void insert(List<T> list, T value) {
        list.add(findChecked(list, value), value);
    }

}
