package com.github.orbyfied.minem.auth;

import slatepowered.veru.misc.ANSI;
import slatepowered.veru.misc.Throwables;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

final class HttpUtil {

    public static void trace(HttpResponse<?> response) {
        int h = 0;
        for (; response != null; response = response.previousResponse().orElse(null)) {
            System.out.println(ANSI.PURPLE + "   TRACE (" + (h++) + ") -- Response :" + response.statusCode() + " to request [" + response.request() + "]" + ANSI.RESET);
        }
    }

    public static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<>();

        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }

        return map;
    }
    /**
     * Read the body from the given URL, giving a very bare-bones response.
     *
     * @param urlToRead The URL.
     * @return The response string.
     */
    public static String readStringBody(String urlToRead) {
        try {
            StringBuilder result = new StringBuilder();
            URL url = new URL(urlToRead);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    result.append(line);
                }
            }

            return result.toString();
        } catch (Throwable t) {
            Throwables.sneakyThrow(t);
            throw new AssertionError();
        }
    }

}