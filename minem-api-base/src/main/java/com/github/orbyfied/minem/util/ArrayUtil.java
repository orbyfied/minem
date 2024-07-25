package com.github.orbyfied.minem.util;

public class ArrayUtil {

    /**
     * Join the given elements in the array using the
     * given separator string.
     */
    public static String join(Object[] arr, String sep) {
        StringBuilder b = new StringBuilder();
        for (Object o : arr) {
            b.append(sep).append(o);
        }

        return b.delete(0, sep.length()).toString();
    }

}
