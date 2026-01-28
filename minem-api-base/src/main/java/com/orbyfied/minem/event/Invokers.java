package com.orbyfied.minem.event;

import slatepowered.veru.reflect.ReflectUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

/**
 * Utilities for working with invoker functions.
 */
final class Invokers {

    @SuppressWarnings("unchecked")
    public static <F> F createDynamicInvoker(Supplier<List<F>> handlerListSupplier,
                                             Class<F> fClass,
                                             Supplier<Object> accumulatorSupplier,
                                             ReturnAccumulator<Object, Object> returnAccumulator,
                                             Supplier<Object> lockSupplier) {
        // calculate which methods are
        // handler methods
        final HashSet<Method> handlerMethodSet = new HashSet<>();
        for (Method method : fClass.getMethods()) {
            if (!method.getDeclaringClass().isInterface())
                continue;
            handlerMethodSet.add(method);
        }

        // create proxy
        return (F) Proxy.newProxyInstance(Chain.class.getClassLoader(), new Class[] { fClass }, (proxy, method, args) -> {
            Object current = null;
            try {
                if (!handlerMethodSet.contains(method)) {
                    return ReflectUtil.invokeDefault(proxy, method, args);
                }

                // notify lock
                Object lock = lockSupplier.get();
                if (lock != null) {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }

                // invoke and update handlers
                List<Integer> toRemove = new ArrayList<>();

                current = accumulatorSupplier.get();
                List<F> handlerList = handlerListSupplier.get();
                final int length = handlerList.size();
                for (int i = 0; i < length; i++) {
                    F func = handlerList.get(i);

                    Object ret;
                    try {
                        ret = method.invoke(func, args);
                    } catch (InvocationTargetException ex) {
                        throw new EventInvocationException("An error occurred while invoking event handler(index: " + i + ")\n" +
                                "   method: " + method + "\n" +
                                "   accumulator: " + current, ex.getCause());
                    }

                    current = returnAccumulator.register(current, ret);

                    if (ret instanceof Integer) {
                        int f = (int) ret;
                        if ((f & Chain.REMOVE) > 0) {
                            toRemove.add(i);
                        }

                        if ((f & Chain.STOP) > 0) {
                            break;
                        }
                    }
                }

                for (int idx : toRemove) {
                    handlerList.remove(idx);
                }

                return current;
            } catch (Exception e) {
                e.printStackTrace();
                return current;
            }
        });
    }

}
