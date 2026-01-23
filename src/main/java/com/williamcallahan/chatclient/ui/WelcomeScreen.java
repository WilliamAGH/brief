package com.williamcallahan.chatclient.ui;

import com.williamcallahan.chatclient.Config;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.Program;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

/**
 * Welcome screen prompting for user name.
 * Simple, friendly onboarding - press Enter to continue or Esc to skip.
 */
public class WelcomeScreen extends ConfigPromptScreen {

    public WelcomeScreen(Config config) {
        super(config, "your name (optional)", 50);
    }

    @Override
    protected String promptTitle() {
        return "Welcome to Brief";
    }

    @Override
    protected String promptBody() {
        return "What's your name?";
    }

    @Override
    protected String skipHint() {
        return "skip";  // Override to show "esc to skip" instead of "esc to quit"
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        // Allow Esc to skip (continue without name) instead of quit
        if (msg instanceof KeyPressMessage key && KeyType.keyESC == key.type()) {
            return proceedToNextScreen(null);
        }
        return super.update(msg);
    }

    @Override
    protected UpdateResult<? extends Model> onSubmit(String name) {
        return proceedToNextScreen(name);
    }

    private UpdateResult<? extends Model> proceedToNextScreen(String name) {
        if (name != null && !name.isBlank()) {
            config.setUserName(name);
        }
        // After name, check if API key is needed
        if (!config.hasResolvedApiKey()) {
            String effectiveName = (name == null || name.isBlank()) ? config.userName() : name;
            return UpdateResult.from(new ApiKeyPromptScreen(config, effectiveName, width, height));
        }
        String effectiveName = (name == null || name.isBlank()) ? config.userName() : name;
        return ApiKeyPromptScreen.transitionToChat(config, effectiveName, width, height);
    }

    public static void main(String[] args) {
        new Program(new WelcomeScreen(new Config())).run();
    }
}
