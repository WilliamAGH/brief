package com.williamcallahan.chatclient;

import com.williamcallahan.chatclient.ui.ApiKeyPromptScreen;
import com.williamcallahan.chatclient.ui.WelcomeScreen;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.Program;
import org.jline.utils.Signals;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Entry point for the brief TUI. */
public class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private static final String DISABLE_AUTOWRAP = "\u001b[?7l";
    private static final String ENABLE_AUTOWRAP = "\u001b[?7h";
    // Reset sequences: disable mouse modes (1000,1002,1003,1006), show cursor (?25h), reset cursor shape
    private static final String RESET_TERMINAL =
        "\u001b[?1000l\u001b[?1002l\u001b[?1003l\u001b[?1006l\u001b[?25h\u001b]22;\u001b\\";

    public static void main(String[] args) {
        // Register shutdown hook to reset terminal on JVM exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.print(RESET_TERMINAL);
            System.out.print(ENABLE_AUTOWRAP);
            System.out.flush();
        }, "terminal-reset"));

        // Handle SIGTSTP (Ctrl+Z) - reset terminal before suspend, restore on resume
        registerSuspendHandlers();

        // Default OFF so terminals keep normal scrollback.
        // Set BRIEF_ALT_SCREEN=1 to enable the alternate screen.
        boolean useAlt = "1".equals(System.getenv("BRIEF_ALT_SCREEN"));
        // Mouse behavior:
        // - unset: in-app drag-to-copy selection + wheel scroll (viewport-locked)
        // - 0: native terminal selection/copy (no in-app wheel scrolling)
        // - 1: tui4j mouse all-motion tracking (best wheel support; most terminals disable native selection)
        // - wheel: wheel/click tracking only (often more compatible with native selection depending on terminal)
        // - select: wheel + drag tracking; app copies selected lines (native selection disabled)
        String mouseMode = System.getenv("BRIEF_MOUSE");
        if (mouseMode == null || mouseMode.isBlank()) {
            mouseMode = "select";
        }
        // Default ON: disable terminal autowrap so full-width borders don't soft-wrap at the last column.
        // Set BRIEF_AUTOWRAP=1 to keep terminal default wrapping behavior.
        boolean disableAutoWrap = !"1".equals(System.getenv("BRIEF_AUTOWRAP"));

        // Validate config before modifying terminal state - fail fast if config is broken
        Config config;
        Model startScreen;
        try {
            config = new Config();
            startScreen = selectStartScreen(config);
        } catch (ConfigException e) {
            System.err.println(e.getMessage());
            System.exit(1);
            return; // unreachable but satisfies compiler
        }

        // Terminal mode setup - only after config validation succeeds
        boolean enableSelectMouse = "select".equalsIgnoreCase(mouseMode);
        boolean enableAllMotionMouse = "1".equals(mouseMode);
        boolean enableCellMotionMouse = enableSelectMouse;

        if (disableAutoWrap) {
            System.out.print(DISABLE_AUTOWRAP);
            System.out.flush();
        }

        // Mouse tracking is now handled entirely by tui4j - no manual escape sequences here.
        // This prevents escape sequence leakage during startup/shutdown race conditions.

        try {
            Program program = new Program(startScreen);
            if (useAlt) {
                program = program.withAltScreen();
            }
            if (enableAllMotionMouse) {
                program = program.withMouseAllMotion();
            } else if (enableCellMotionMouse) {
                program = program.withMouseCellMotion();
            }
            if (!"0".equals(mouseMode)) {
                program = program.withMouseClicks();
            }
            program.run();
        } finally {
            // tui4j handles mouse cleanup in Program.run() finally block via disableMouse()
            if (disableAutoWrap) {
                System.out.print(ENABLE_AUTOWRAP);
                System.out.flush();
            }
        }
    }

    /**
     * Decides which screen to start with based on saved config.
     * Chain: name prompt (if missing) → API key prompt (if missing) → chat.
     */
    private static Model selectStartScreen(Config config) {
        if (!config.hasUserName()) {
            return new WelcomeScreen(config);
        }
        if (!config.hasResolvedApiKey()) {
            return new ApiKeyPromptScreen(config, config.userName(), 80, 24);
        }
        // Both name and API key present - go directly to chat
        return ApiKeyPromptScreen.transitionToChat(config, config.userName(), 80, 24).model();
    }

    /**
     * Register handler for SIGTSTP (Ctrl+Z suspend).
     * Resets terminal state before suspend to prevent escape sequence contamination,
     * then re-registers default handler so the process actually suspends.
     *
     * <p>Note: Uses {@code sun.misc.Signal} to raise SIGTSTP. This is an internal JDK API
     * with no standard alternative for raising signals to the current process. The call is
     * guarded by try/catch so platforms lacking this API gracefully degrade (terminal resets
     * but process doesn't suspend).
     */
    private static void registerSuspendHandlers() {
        try {
            Signals.register("TSTP", () -> {
                System.out.print(RESET_TERMINAL);
                System.out.print(ENABLE_AUTOWRAP);
                System.out.flush();
                Signals.registerDefault("TSTP");
                try {
                    sun.misc.Signal.raise(new sun.misc.Signal("TSTP"));
                } catch (IllegalArgumentException e) {
                    LOG.log(Level.FINE, "Failed to raise SIGTSTP; terminal reset but process not suspended", e);
                }
            });
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINE, "SIGTSTP not available on this platform", e);
        }

        try {
            Signals.register("CONT", () -> registerSuspendHandlers());
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINE, "SIGCONT not available on this platform", e);
        }
    }
}
