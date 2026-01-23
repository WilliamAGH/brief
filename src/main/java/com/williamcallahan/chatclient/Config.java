package com.williamcallahan.chatclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * User preferences stored in ~/.config/brief/config.
 * Resolution priority controlled by BRIEF_CONFIG_PRIORITY (env var or config.priority property).
 */
public final class Config {

    public enum Priority { ENV, CONFIG }

    private static final long ERROR_DISPLAY_MS = 10_000;

    private final Path configPath;
    private final Properties props = new Properties();
    private final Priority priority;
    private String lastError;
    private long lastErrorAt;

    public Config() {
        String xdg = System.getenv("XDG_CONFIG_HOME");
        Path base = (xdg != null && !xdg.isBlank()) ? Path.of(xdg)
            : Path.of(System.getProperty("user.home"), ".config");
        this.configPath = base.resolve("brief").resolve("config");
        load();
        this.priority = resolvePriority();
    }

    /** Returns the active priority mode. */
    public Priority priority() { return priority; }

    // ── Resolve (respects priority setting) ─────────────────────────────────────

    public String resolveApiKey()  { return resolve("OPENAI_API_KEY", "openai.api_key"); }
    public String resolveBaseUrl() { return resolve("OPENAI_BASE_URL", "openai.base_url"); }
    public String resolveModel()   { return resolve("LLM_MODEL", "model"); }
    public String resolveAppleMapsToken() { return resolve("APPLE_MAPS_TOKEN", "apple_maps.token"); }

    public boolean hasResolvedApiKey() { return resolveApiKey() != null; }
    public boolean hasAppleMapsToken() { return resolveAppleMapsToken() != null; }

    private String resolve(String envVar, String propKey) {
        String env = System.getenv(envVar);
        String cfg = props.getProperty(propKey, "").trim();
        boolean hasEnv = env != null && !env.isBlank();
        boolean hasCfg = !cfg.isEmpty();

        if (priority == Priority.CONFIG) {
            return hasCfg ? cfg : (hasEnv ? env.trim() : null);
        } else {
            return hasEnv ? env.trim() : (hasCfg ? cfg : null);
        }
    }

    /** Bootstrap priority (this meta-setting always uses env > config). */
    private Priority resolvePriority() {
        String env = System.getenv("BRIEF_CONFIG_PRIORITY");
        if (env != null && !env.isBlank()) {
            return "config".equalsIgnoreCase(env.trim()) ? Priority.CONFIG : Priority.ENV;
        }
        String cfg = props.getProperty("config.priority", "").trim();
        return "config".equalsIgnoreCase(cfg) ? Priority.CONFIG : Priority.ENV;
    }

    // ── Setters (persist to config file) ────────────────────────────────────────

    public void setApiKey(String v)  { set("openai.api_key", v); }
    public void setBaseUrl(String v) { set("openai.base_url", v); }
    public void setModel(String v)   { set("model", v); }
    public void setUserName(String v){ set("user.name", v); }
    public void setAppleMapsToken(String v) { set("apple_maps.token", v); }

    private void set(String key, String value) {
        props.setProperty(key, value == null ? "" : value.trim());
        save();
    }

    // ── User name (no env var, config only) ─────────────────────────────────────

    public String userName() { return props.getProperty("user.name", "").trim(); }
    public boolean hasUserName() { return !userName().isBlank(); }

    // ── Summary settings ────────────────────────────────────────────────────────

    private static final int DEFAULT_SUMMARY_TARGET_TOKENS = 8000;

    /** Returns whether summarization is enabled (default: true). */
    public boolean isSummaryEnabled() {
        String cfg = props.getProperty("summary.disabled", "").trim();
        return !"true".equalsIgnoreCase(cfg);
    }

    /** Sets whether summarization is disabled. */
    public void setSummaryDisabled(boolean disabled) {
        set("summary.disabled", disabled ? "true" : "false");
    }

    /** Returns the target token count for summaries (default: 8000). */
    public int getSummaryTargetTokens() {
        String cfg = props.getProperty("summary.target_tokens", "").trim();
        if (!cfg.isEmpty()) {
            int parsed = parseIntOrDefault(cfg);
            if (parsed > 0) {
                return parsed;
            }
        }
        return DEFAULT_SUMMARY_TARGET_TOKENS;
    }

    private int parseIntOrDefault(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Sets the target token count for summaries. */
    public void setSummaryTargetTokens(int tokens) {
        set("summary.target_tokens", String.valueOf(tokens));
    }

    // ── Transient error display ─────────────────────────────────────────────────

    public String transientError(long nowMs) {
        if (lastError == null || nowMs - lastErrorAt > ERROR_DISPLAY_MS) {
            lastError = null;
            return null;
        }
        return lastError;
    }

    // ── IO ──────────────────────────────────────────────────────────────────────

    private void load() {
        if (!Files.exists(configPath)) return;
        try (var reader = Files.newBufferedReader(configPath)) {
            props.load(reader);
        } catch (IOException e) {
            lastError = "Config unreadable: " + configPath.getFileName();
            lastErrorAt = System.currentTimeMillis();
        }
    }

    private void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (var writer = Files.newBufferedWriter(configPath)) {
                props.store(writer, "brief configuration");
            }
        } catch (IOException e) {
            lastError = "Settings not saved: " + configPath.getFileName();
            lastErrorAt = System.currentTimeMillis();
        }
    }
}
