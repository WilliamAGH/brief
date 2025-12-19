package com.williamcallahan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Reads and writes user preferences to ~/.config/brief/config.
 *
 * Uses XDG_CONFIG_HOME if set, otherwise defaults to ~/.config.
 * IO failures are soft errors — stored transiently for UI display, not thrown.
 */
public final class Config {

    private static final String APP_DIR = "brief";
    private static final String CONFIG_FILE = "config";
    private static final long ERROR_DISPLAY_MS = 10_000;

    private final Path configPath;
    private final Properties props = new Properties();

    private String lastError;
    private long lastErrorAt;

    public Config() {
        this.configPath = resolveConfigPath();
        load();
    }

    public String userName() {
        return props.getProperty("user.name", "").trim();
    }

    public void setUserName(String name) {
        props.setProperty("user.name", name == null ? "" : name.trim());
        save();
    }

    public boolean hasUserName() {
        String name = userName();
        return name != null && !name.isBlank();
    }

    public String model() {
        return props.getProperty("model", "").trim();
    }

    public void setModel(String model) {
        props.setProperty("model", model == null ? "" : model.trim());
        save();
    }

    public boolean hasModel() {
        String m = model();
        return m != null && !m.isBlank();
    }

    public String apiKey() {
        return props.getProperty("openai.api_key", "");
    }

    public void setApiKey(String key) {
        props.setProperty("openai.api_key", key == null ? "" : key);
        save();
    }

    /** Returns API key from env var (priority) or config file. Null if neither set. */
    public String resolveApiKey() {
        String envKey = System.getenv("OPENAI_API_KEY");
        if (envKey != null) return envKey;
        String configKey = apiKey();
        return configKey.isEmpty() ? null : configKey;
    }

    /** True if API key is set via env var or config (empty string counts as set). */
    public boolean hasResolvedApiKey() {
        return System.getenv("OPENAI_API_KEY") != null || !apiKey().isEmpty();
    }

    /** Returns transient error message if within display window, null otherwise. */
    public String transientError(long nowMs) {
        if (lastError == null) return null;
        if (nowMs - lastErrorAt > ERROR_DISPLAY_MS) {
            lastError = null;
            return null;
        }
        return lastError;
    }

    private void setError(String msg) {
        this.lastError = msg;
        this.lastErrorAt = System.currentTimeMillis();
    }

    private void load() {
        if (!Files.exists(configPath)) return;
        try (var reader = Files.newBufferedReader(configPath)) {
            props.load(reader);
        } catch (IOException e) {
            setError("Config unreadable: " + configPath.getFileName());
        }
    }

    private void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (var writer = Files.newBufferedWriter(configPath)) {
                props.store(writer, "brief configuration");
            }
        } catch (IOException e) {
            setError("Settings not saved: " + configPath.getFileName());
        }
    }

    private static Path resolveConfigPath() {
        String xdgConfig = System.getenv("XDG_CONFIG_HOME");
        Path base = (xdgConfig != null && !xdgConfig.isBlank())
            ? Path.of(xdgConfig)
            : Path.of(System.getProperty("user.home"), ".config");
        return base.resolve(APP_DIR).resolve(CONFIG_FILE);
    }
}
