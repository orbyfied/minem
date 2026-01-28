package com.orbyfied.minem.event;

/**
 * Accumulates return values of an invocation.
 */
public interface ReturnAccumulator<A, R> {

    /**
     * Register a new return value to the accumulator.
     *
     * @param current The current accumulator value.
     * @param value The returned value.
     * @return The new accumulator value.
     */
    A register(A current, R value);

    /**
     * Create a new return accumulator which returns
     * the last value in the chain.
     *
     * @param <R> The return value.
     * @return The return accumulator.
     */
    static <R> ReturnAccumulator<R, R> last() {
        return (current, value) -> value;
    }

}
