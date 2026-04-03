package com.williamcallahan.chatclient.diagnostics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/** Runtime trace for diagnosing terminal event paths (opt-in via BRIEF_RUNTIME_TRACE=1). */
public final class RuntimeTrace {

    private static final String TRACE_ENV = "BRIEF_RUNTIME_TRACE";
    private static final Path TRACE_PATH = Path.of(
        "/tmp/brief-runtime-trace.log"
    );
    private static final boolean ENABLED =
        "1".equals(System.getenv(TRACE_ENV));

    private RuntimeTrace() {}

    public static boolean enabled() {
        return ENABLED;
    }

    public static void reset(String sessionName) {
        if (!enabled()) {
            return;
        }
        String line =
            Instant.now() +
            " [session] start " +
            sessionName +
            " pid=" +
            ProcessHandle.current().pid() +
            System.lineSeparator();
        try {
            Files.writeString(
                TRACE_PATH,
                line,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException ignored) {
            // Diagnostics must never break the app path.
        }
    }

    public static void log(String category, String message) {
        if (!enabled()) {
            return;
        }
        write(category, message);
    }

    private static synchronized void write(String category, String message) {
        String line =
            Instant.now() + " [" + category + "] " + message + System.lineSeparator();
        try {
            Files.writeString(
                TRACE_PATH,
                line,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // Diagnostics must never break the app path.
        }
    }
}
