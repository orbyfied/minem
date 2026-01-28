package com.orbyfied.minem.event;

import java.util.Collection;

public interface ChainAccess<F> {

    /**
     * Get an awaitable object for invocations of this chain if supported.
     */
    Object lock();

    default void await() throws InterruptedException {
        await(0L);
    }

    default void await(long timeoutMillis) throws InterruptedException {
        Object lock;
        synchronized (lock = lock()) {
            lock.wait(timeoutMillis);
        }
    }

    /**
     * Add the given handler function to the
     * start of the handler list.
     *
     * @param function The function.
     * @return This.
     */
    ChainAccess<F> addFirst(F function);

    /**
     * Add the given chain as a handler to the
     * start of the handler list.
     *
     * @param chain The chain.
     * @return This.
     */
    ChainAccess<F> addFirst(Chain<? extends F> chain);

    /**
     * Add the given handler function to the
     * end of the handler list.
     *
     * @param function The function.
     * @return This.
     */
    ChainAccess<F> addLast(F function);

    /**
     * Add the given chain as a handler to the
     * end of the handler list.
     *
     * @param chain The chain.
     * @return This.
     */
    ChainAccess<? extends F> addLast(Chain<? extends F> chain);

    /**
     * Add the given handler function to the handler list
     * at the given placement.
     *
     * @param function The handler function.
     * @param placement The placement.
     * @return This.
     */
    ChainAccess<F> add(F function, Placement<F> placement);

    /**
     * Add the given chain as a handler to the handler list
     * at the given placement.
     *
     * @param chain The chain.
     * @param placement The placement.
     * @return This.
     */
    ChainAccess<F> add(Chain<? extends F> chain, Placement<F> placement);

    static <F> ChainAccess<F> of(Collection<Chain<F>> list) {
        return new ChainAccess<F>() {
            @Override
            public Object lock() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ChainAccess<F> addFirst(F function) {
                for (var c : list) {
                    c.addFirst(function);
                }

                return this;
            }

            @Override
            public ChainAccess<F> addFirst(Chain<? extends F> chain) {
                for (var c : list) {
                    c.addFirst(chain);
                }

                return this;
            }

            @Override
            public ChainAccess<F> addLast(F function) {
                for (var c : list) {
                    c.addLast(function);
                }

                return this;
            }

            @Override
            public ChainAccess<F> addLast(Chain<? extends F> chain) {
                for (var c : list) {
                    c.addLast(chain);
                }

                return this;
            }

            @Override
            public ChainAccess<F> add(F function, Placement<F> placement) {
                for (var c : list) {
                    c.add(function, placement);
                }

                return this;
            }

            @Override
            public ChainAccess<F> add(Chain<? extends F> chain, Placement<F> placement) {
                for (var c : list) {
                    c.add(chain, placement);
                }

                return this;
            }
        };
    }

}
