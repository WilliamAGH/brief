package com.williamcallahan.chatclient;

import java.io.IOException;
import java.util.Properties;

/** Application metadata constants. */
public final class AppInfo {
    public static final String NAME = "brief";
    public static final String VERSION = loadVersion();

    private AppInfo() {}

    private static String loadVersion() {
        var props = new Properties();
        try (var in = AppInfo.class.getResourceAsStream("/version.properties")) {
            if (in != null) {
                props.load(in);
                return "v" + props.getProperty("app.version", "unknown");
            }
        } catch (IOException ignored) {}
        return "v-unknown";
    }
}
