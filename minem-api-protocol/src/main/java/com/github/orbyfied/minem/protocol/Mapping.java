package com.github.orbyfied.minem.protocol;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes a class as a data class for a packet and allows the class
 * to be compiled to a {@link PacketMapping}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mapping {

    int id();                                    // The numeric ID
    ProtocolPhases phase();                      // The phase (only default for now)
    String primaryName() default "";             // The primary name
    String[] aliases() default {};               // The aliases for the mapping
    Destination dest() default Destination.FIND; // Where the packet is bound
    int flags() default 0;                       // The flags on this mapping, see PacketMapping#flags

}
