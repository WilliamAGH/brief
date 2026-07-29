package com.williamcallahan.chatclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigReasoningEffortTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void leavesReasoningEffortOmittedWhenNoSourceSelectsIt() {
        Config config = new Config(
            temporaryDirectory.resolve("config"),
            Map.of(),
            CliOptions.empty()
        );

        assertTrue(config.resolveReasoningEffort().isEmpty());
    }

    @Test
    void keepsConfiguredNoneExplicit() throws IOException {
        Path configPath = writeConfig("reasoning.effort=none\n");
        Config config = new Config(configPath, Map.of(), CliOptions.empty());

        assertEquals(ReasoningEffort.NONE, config.resolveReasoningEffort().orElseThrow());
    }

    @Test
    void followsConfiguredPriorityBeforeCliOverride() throws IOException {
        Path configPath = writeConfig("config.priority=config\nreasoning.effort=medium\n");
        Config configured = new Config(
            configPath,
            Map.of("BRIEF_REASONING_EFFORT", "high"),
            CliOptions.empty()
        );
        Config commandLine = new Config(
            configPath,
            Map.of("BRIEF_REASONING_EFFORT", "high"),
            CliOptions.parse(new String[] { "--reasoning-effort", "max" })
        );

        assertEquals(ReasoningEffort.MEDIUM, configured.resolveReasoningEffort().orElseThrow());
        assertEquals(ReasoningEffort.MAX, commandLine.resolveReasoningEffort().orElseThrow());
    }

    private Path writeConfig(String contents) throws IOException {
        Path configPath = temporaryDirectory.resolve("brief").resolve("config");
        Files.createDirectories(configPath.getParent());
        Files.writeString(configPath, contents);
        return configPath;
    }
}
