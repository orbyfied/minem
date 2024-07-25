package com.github.orbyfied.minem.listener;

import com.github.orbyfied.minem.MinecraftClient;
import com.github.orbyfied.minem.protocol.Packet;
import slatepowered.veru.misc.Throwables;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Represents an incoming packet listener.
 */
public interface IncomingPacketListener {

    default void subscribeAllIncomingPackets(MinecraftClient client) {
        try {
            for (Method method : getClass().getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers())) continue;
                method.setAccessible(true);

                SubscribePacket ann = method.getAnnotation(SubscribePacket.class);
                if (ann == null) {
                    continue;
                }

                Class<?>[] params = method.getParameterTypes();
                if (params.length != 2 || !Packet.class.isAssignableFrom(params[0])) {
                    continue;
                }

                Class<?> key = params[1];
                client.onTypedReceived().by(key).addLast(packet -> {
                    try {
                        method.invoke(this, packet, packet.data());
                        return 0;
                    } catch (Exception ex) {
                        Throwables.sneakyThrow(ex);
                        return 0;
                    }
                });
            }
        } catch (Exception ex) {
            Throwables.sneakyThrow(ex);
        }
    }

}
