package com.williamcallahan.chatclient;

import java.util.Optional;

/** Command-line settings that intentionally override persisted configuration. */
public final class CliOptions {

    private final Optional<ReasoningEffort> reasoningEffort;

    private CliOptions(Optional<ReasoningEffort> reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }

    public static CliOptions parse(String[] args) {
        Optional<ReasoningEffort> selectedEffort = Optional.empty();
        boolean effortProvided = false;

        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            String value = null;
            if ("--reasoning-effort".equals(argument)) {
                if (++index == args.length) {
                    throw missingReasoningEffortValue();
                }
                value = args[index];
            } else if (argument.startsWith("--reasoning-effort=")) {
                value = argument.substring("--reasoning-effort=".length());
            }

            if (value == null) {
                continue;
            }
            if (effortProvided) {
                throw new ConfigException("--reasoning-effort may be specified only once.");
            }
            if (value.isBlank()) {
                throw missingReasoningEffortValue();
            }
            selectedEffort = ReasoningEffort.fromConfiguredValue(value);
            effortProvided = true;
        }

        return new CliOptions(selectedEffort);
    }

    public static CliOptions empty() {
        return new CliOptions(Optional.empty());
    }

    public Optional<ReasoningEffort> reasoningEffort() {
        return reasoningEffort;
    }

    private static ConfigException missingReasoningEffortValue() {
        return new ConfigException(
            "--reasoning-effort requires one of: " + ReasoningEffort.supportedValues()
        );
    }
}
