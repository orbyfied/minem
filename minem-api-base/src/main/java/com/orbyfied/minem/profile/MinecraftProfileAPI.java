package com.orbyfied.minem.profile;

import com.orbyfied.minem.http.HttpUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import slatepowered.veru.misc.Throwables;

import java.nio.ByteBuffer;
import java.util.UUID;

public class MinecraftProfileAPI {

    // the gson instance
    private static final Gson GSON = new Gson();

    public static UUID fetchUUIDFromName(String username) {
        try {
            // query mojang api
            String responseString = HttpUtil.readStringBody("https://api.mojang.com/users/profiles/minecraft/" + username);
            JsonObject response = GSON.fromJson(responseString, JsonObject.class);

            // check success
            if (response.has("errorMessage")) {
                throw new IllegalArgumentException("Error message response from Mojang API: " + response.get("errorMessage").getAsString());
            }

            // parse json and get id
            String uuidStr = response.get("id").getAsString();
            if (uuidStr == null || uuidStr.length() != 32)
                throw new IllegalArgumentException("Invalid UUID response from Mojang API: " + uuidStr);

            // parse uuid
            ByteBuffer buf = ByteBuffer.wrap(hexStringToByteArray(uuidStr));
            return new UUID(buf.getLong(), buf.getLong());
        } catch (Exception e) {
            Throwables.sneakyThrow(e);
            throw new AssertionError();
        }
    }

    public static String fetchNameFromUUID(UUID uuid) {
        try {
            // query mojang api
            String responseString = HttpUtil.readStringBody("https://api.mojang.com/user/profile/" + uuid);
            JsonObject response = GSON.fromJson(responseString, JsonObject.class);

            // check success
            if (response.has("errorMessage")) {
                throw new IllegalArgumentException("Error message response from Mojang API: " + response.get("errorMessage").getAsString());
            }

            // parse json and get name
            String name = response.get("name").getAsString();
            if (name == null)
                throw new IllegalArgumentException("Invalid username response from Mojang API");
            return name;
        } catch (Exception e) {
            Throwables.sneakyThrow(e);
            throw new AssertionError();
        }
    }

    /* s must be an even-length string.
     *  https://stackoverflow.com/a/140861/14837740 */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }

        return data;
    }

}
