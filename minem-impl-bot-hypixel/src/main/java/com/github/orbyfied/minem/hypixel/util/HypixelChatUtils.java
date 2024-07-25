package com.github.orbyfied.minem.hypixel.util;

import slatepowered.veru.string.StringReader;

public class HypixelChatUtils {

    public static String stripIGN(String ignWithRank) {
        StringReader reader = new StringReader(ignWithRank.trim());
        if (reader.curr() == '[') {
            reader.next();
            reader.collect(c -> c != ']');
            reader.next();
        }

        String ign = reader.collect(c -> c != '[');
        return ign.trim();
    }

}
