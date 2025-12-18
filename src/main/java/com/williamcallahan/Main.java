package com.williamcallahan;

import com.williamcallahan.domain.Conversation;
import com.williamcallahan.lattetui.ChatConversationScreen;
import com.williamcallahan.lattetui.WelcomeScreen;
import org.flatscrew.latte.Model;
import org.flatscrew.latte.Program;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/** Entry point for the brief TUI. */
public class Main {
    private static final String DISABLE_AUTOWRAP = "\u001b[?7l";
    private static final String ENABLE_AUTOWRAP = "\u001b[?7h";
    private static final String ENABLE_MOUSE_NORMAL_TRACKING = "\u001b[?1000h";
    private static final String DISABLE_MOUSE_NORMAL_TRACKING = "\u001b[?1000l";
    private static final String ENABLE_MOUSE_BUTTON_EVENT_TRACKING = "\u001b[?1002h";
    private static final String DISABLE_MOUSE_BUTTON_EVENT_TRACKING = "\u001b[?1002l";
    private static final String ENABLE_MOUSE_SGR = "\u001b[?1006h";
    private static final String DISABLE_MOUSE_SGR = "\u001b[?1006l";

    public static void main(String[] args) {
        // Default OFF so terminals keep normal scrollback.
        // Set BRIEF_ALT_SCREEN=1 to enable the alternate screen.
        boolean useAlt = "1".equals(System.getenv("BRIEF_ALT_SCREEN"));
        // Mouse behavior:
        // - unset: in-app drag-to-copy selection + wheel scroll (viewport-locked)
        // - 0: native terminal selection/copy (no in-app wheel scrolling)
        // - 1: Latte mouse all-motion tracking (best wheel support; most terminals disable native selection)
        // - wheel: wheel/click tracking only (often more compatible with native selection depending on terminal)
        // - select: wheel + drag tracking; app copies selected lines (native selection disabled)
        String mouseMode = System.getenv("BRIEF_MOUSE");
        if (mouseMode == null || mouseMode.isBlank()) {
            mouseMode = "select";
        }
        // Default ON: disable terminal autowrap so full-width borders don't soft-wrap at the last column.
        // Set BRIEF_AUTOWRAP=1 to keep terminal default wrapping behavior.
        boolean disableAutoWrap = !"1".equals(System.getenv("BRIEF_AUTOWRAP"));

        // Check config early to skip welcome screen if user name is already saved
        Config config = new Config();
        Model startScreen = selectStartScreen(config);

        Program program = new Program(startScreen);
        if (useAlt) {
            program = program.withAltScreen(); // fresh screen, restores original on exit
        }
        if ("1".equals(mouseMode)) {
            program = program.withMouseAllMotion();
        }

        if (disableAutoWrap) {
            System.out.print(DISABLE_AUTOWRAP);
            System.out.flush();
        }
        boolean enableWheelOnlyMouse = "wheel".equalsIgnoreCase(mouseMode);
        boolean enableSelectMouse = "select".equalsIgnoreCase(mouseMode);
        if (enableWheelOnlyMouse) {
            // Enable SGR mouse reporting (1006) + normal tracking (1000) for wheel events without all-motion tracking.
            System.out.print(ENABLE_MOUSE_SGR);
            System.out.print(ENABLE_MOUSE_NORMAL_TRACKING);
            System.out.flush();
        }
        if (enableSelectMouse) {
            // Enable SGR reporting + button-event tracking (motion while a button is pressed) for drag-to-copy in-app.
            System.out.print(ENABLE_MOUSE_SGR);
            System.out.print(ENABLE_MOUSE_NORMAL_TRACKING);
            System.out.print(ENABLE_MOUSE_BUTTON_EVENT_TRACKING);
            System.out.flush();
        }
        ConfigException configError = null;
        try {
            program.run();
        } catch (ConfigException e) {
            configError = e;
        } finally {
            if (enableSelectMouse) {
                System.out.print(DISABLE_MOUSE_BUTTON_EVENT_TRACKING);
                System.out.print(DISABLE_MOUSE_SGR);
                System.out.print(DISABLE_MOUSE_NORMAL_TRACKING);
                System.out.flush();
            }
            if (enableWheelOnlyMouse) {
                System.out.print(DISABLE_MOUSE_SGR);
                System.out.print(DISABLE_MOUSE_NORMAL_TRACKING);
                System.out.flush();
            }
            if (disableAutoWrap) {
                System.out.print(ENABLE_AUTOWRAP);
                System.out.flush();
            }
        }
        if (configError != null) {
            System.err.println(configError.getMessage());
            System.exit(1);
        }
    }

    /**
     * Decides which screen to start with based on saved config.
     * If user name exists, skips WelcomeScreen and goes directly to chat.
     */
    private static Model selectStartScreen(Config config) {
        if (!config.hasUserName()) {
            return new WelcomeScreen(config);
        }

        // User name exists - go directly to chat
        String userName = config.userName();
        Conversation.ConversationBuilder convoBuilder = Conversation.builder()
            .id("c_" + UUID.randomUUID())
            .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
            .updatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        // Priority: env var > saved config
        String envModel = System.getenv("LLM_MODEL");
        String model = null;
        boolean needsModelSelection = false;

        if (envModel != null && !envModel.isBlank()) {
            model = envModel.trim();
        } else if (config.hasModel()) {
            model = config.model();
        } else {
            needsModelSelection = true;
        }

        if (model != null) {
            convoBuilder.defaultModel(model);
        }

        return new ChatConversationScreen(userName, convoBuilder.build(), config, 80, 24, needsModelSelection);
    }
}
