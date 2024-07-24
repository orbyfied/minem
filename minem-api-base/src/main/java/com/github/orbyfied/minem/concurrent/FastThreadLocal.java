package com.github.orbyfied.minem.concurrent;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * An optimized implementation of a thread local value store, similar
 * to {@link ThreadLocal}.
 *
 * @param <T> The value type.
 */
@SuppressWarnings("unchecked")
public class FastThreadLocal<T> {

    /** The values indexed by thread ID. */
    volatile Object[] values;

    {
        ensureArraySize(Thread.activeCount());
    }

    private int currentId() {
        return (int) Thread.currentThread().getId();
    }

    private synchronized Object[] ensureArraySize(int i) {
        if (values == null || i >= values.length) {
            Object[] newArr = new Object[(i + 1) * 2];
            if (values != null) {
                System.arraycopy(values, 0, newArr, 0, values.length);
            }

            this.values = newArr;
        }

        return values;
    }

    public T get() {
        int id = currentId();
        return id >= values.length ? null : (T) values[id];
    }

    public T get(int id) {
        return id >= values.length ? null : (T) values[id];
    }

    public void set(T value) {
        int id = currentId();
        ensureArraySize(id)[id] = value;
    }

    public void set(int id, T value) {
        ensureArraySize(id)[id] = value;
    }

    public T getOrCompute(Supplier<T> supplier) {
        return getOrCompute(currentId(), supplier);
    }

    public T getOrCompute(int id, Supplier<T> supplier) {
        if (id >= values.length) {
            return (T) (ensureArraySize(id)[id] = supplier.get());
        }

        T val = (T) values[id];
        if (val == null) {
            ensureArraySize(id)[id] = (val = supplier.get());
        }

        return val;
    }

    public void forEach(BiConsumer<Integer, T> value) {
        for (int i = 0; i < values.length; i++) {
            T val = (T) values[i];
            if (val != null) {
                value.accept(i, val);
            }
        }
    }

}
