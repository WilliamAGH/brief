package com.williamcallahan.ui;

import com.williamcallahan.Config;
import com.williamcallahan.domain.Conversation;
import com.williamcallahan.tui4j.Command;
import com.williamcallahan.tui4j.Model;
import com.williamcallahan.tui4j.UpdateResult;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.williamcallahan.tui4j.Command.batch;
import static com.williamcallahan.tui4j.Command.setWidowTitle;

/** Prompts for OpenAI API key when missing from env and config. */
public class ApiKeyPromptScreen extends ConfigPromptScreen {

    private final String userName;

    public ApiKeyPromptScreen(Config config, String userName, int width, int height) {
        super(config, "your API key", 256);
        this.userName = userName;
        this.width = width;
        this.height = height;
    }

    @Override
    protected String promptTitle() {
        return "API Key Required";
    }

    @Override
    protected String promptBody() {
        return "Enter your OpenAI API key:";
    }

    @Override
    protected UpdateResult<? extends Model> onSubmit(String apiKey) {
        config.setApiKey(apiKey);
        return transitionToChat(config, userName, width, height);
    }

    /** Shared transition to ChatConversationScreen. */
    public static UpdateResult<? extends Model> transitionToChat(Config config, String name, int width, int height) {
        Conversation.ConversationBuilder convoBuilder = Conversation.builder()
            .id("c_" + UUID.randomUUID())
            .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
            .updatedAt(OffsetDateTime.now(ZoneOffset.UTC));

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

        Conversation convo = convoBuilder.build();
        ChatConversationScreen next = new ChatConversationScreen(name, convo, config, width, height, needsModelSelection);
        return UpdateResult.from(
            next,
            batch(
                setWidowTitle("brief - " + convo.getDefaultModel()),
                Command.checkWindowSize()
            )
        );
    }
}

