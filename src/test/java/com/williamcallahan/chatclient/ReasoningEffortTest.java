package com.williamcallahan.chatclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReasoningEffortTest {

    @Test
    void parsesEveryCanonicalWireValue() {
        for (ReasoningEffort effort : ReasoningEffort.values()) {
            assertEquals(
                effort,
                ReasoningEffort.fromConfiguredValue(effort.wireValue()).orElseThrow()
            );
        }
    }

    @Test
    void leavesOmittedValueUnset() {
        assertTrue(ReasoningEffort.fromConfiguredValue(null).isEmpty());
        assertTrue(ReasoningEffort.fromConfiguredValue("   ").isEmpty());
    }

    @Test
    void rejectsUnknownValueWithSupportedValues() {
        ConfigException exception = assertThrows(
            ConfigException.class,
            () -> ReasoningEffort.fromConfiguredValue("adaptive")
        );

        assertTrue(exception.getMessage().contains("adaptive"));
        assertTrue(exception.getMessage().contains(ReasoningEffort.supportedValues()));
    }

    @Test
    void parsesReasoningEffortCliOption() {
        CliOptions options = CliOptions.parse(new String[] { "--reasoning-effort=xhigh" });

        assertEquals(ReasoningEffort.XHIGH, options.reasoningEffort().orElseThrow());
    }

    @Test
    void leavesOmittedCliOptionUnset() {
        assertTrue(CliOptions.parse(new String[0]).reasoningEffort().isEmpty());
    }
}
