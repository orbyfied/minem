package com.github.orbyfied.minem.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A chain of interfaces which can be invoked in-order,
 * with the return values accumulated.
 *
 * @param <F> The invoker/handler function.
 */
public class Chain<F> {

    /**
     * Integer result flag.
     *
     * Stop the execution of the current chain.
     */
    public static int STOP = 1 << 1;

    /**
     * Integer result flag.
     *
     * Remove this handler from the chain.
     */
    public static int REMOVE = 1 << 2;

    /**
     * The function class.
     */
    private final Class<F> fClass;

    /**
     * The handler list.
     */
    private final List<F> handlers = new ArrayList<>();

    /**
     * Supplies the accumulator values.
     */
    private Supplier<?> accumulatorSupplier;

    /**
     * The return accumulator for the standard invoker.
     */
    private ReturnAccumulator<?, ?> returnAccumulator;

    /**
     * The standard invoker.
     */
    private F invoker;

    /**
     * The awaitable lock.
     */
    private Object lock;

    public Chain(Class<F> fClass) {
        this.fClass = fClass;
    }

    public Class<F> getFunctionClass() {
        return fClass;
    }

    // update/create the standard invoker
    @SuppressWarnings("unchecked")
    private void updateStandardInvoker() {
        invoker = Invokers.createDynamicInvoker(
                () -> handlers,
                fClass,
                accumulatorSupplier != null ? (Supplier<Object>) accumulatorSupplier : () -> null,
                returnAccumulator != null ? (ReturnAccumulator<Object, Object>) returnAccumulator : (__, obj) -> obj,
                () -> this.lock
        );
    }

    public synchronized Object lock() {
        if (lock == null) {
            lock = new Object();
        }

        return lock;
    }

    public void await() throws InterruptedException {
        synchronized (lock()) {
            lock.wait();
        }
    }

    public void await(long timeoutMillis) throws InterruptedException {
        synchronized (lock()) {
            lock.wait(timeoutMillis);
        }
    }

    /**
     * Set the return accumulator for the standard invoker.
     *
     * @return This.
     */
    public <A, R> Chain<F> withReturnAccumulator(ReturnAccumulator<A, R> returnAccumulator) {
        this.returnAccumulator = returnAccumulator;
        updateStandardInvoker();
        return this;
    }

    public Chain<F> accumulatorSupplier(Supplier<?> accumulatorSupplier) {
        this.accumulatorSupplier = accumulatorSupplier;
        return this;
    }

    /**
     * Get or create the standard invoker for this chain.
     *
     * @return The invoker.
     */
    public F invoker() {
        if (invoker == null) {
            updateStandardInvoker();
        }

        return invoker;
    }

    /**
     * Add the given handler function to the
     * start of the handler list.
     *
     * @param function The function.
     * @return This.
     */
    public Chain<F> addFirst(F function) {
        handlers.add(0, function);
        return this;
    }

    /**
     * Add the given chain as a handler to the
     * start of the handler list.
     *
     * @param chain The chain.
     * @return This.
     */
    public Chain<F> addFirst(Chain<? extends F> chain) {
        return addFirst(chain.invoker());
    }

    /**
     * Add the given handler function to the
     * end of the handler list.
     *
     * @param function The function.
     * @return This.
     */
    public Chain<F> addLast(F function) {
        handlers.add(function);
        return this;
    }

    /**
     * Add the given chain as a handler to the
     * end of the handler list.
     *
     * @param chain The chain.
     * @return This.
     */
    public Chain<F> addLast(Chain<? extends F> chain) {
        return addLast(chain.invoker());
    }

    /**
     * Add the given handler function to the handler list
     * at the given placement.
     *
     * @param function The handler function.
     * @param placement The placement.
     * @return This.
     */
    public Chain<F> add(F function, Placement<F> placement) {
        placement.insert(handlers, function);
        return this;
    }

    /**
     * Add the given chain as a handler to the handler list
     * at the given placement.
     *
     * @param chain The chain.
     * @param placement The placement.
     * @return This.
     */
    public Chain<F> add(Chain<? extends F> chain, Placement<F> placement) {
        return add(chain.invoker(), placement);
    }

    public Chain<F> integerFlagHandling() {
        accumulatorSupplier(() -> 0);
        return this.<Integer, Integer>withReturnAccumulator((current, result) -> {
            return result != null ? current | result : current;
        });
    }

}
