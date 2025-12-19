package com.williamcallahan.lattetui;

import com.williamcallahan.Config;
import org.flatscrew.latte.Model;
import org.flatscrew.latte.Program;
import org.flatscrew.latte.UpdateResult;

/** Welcome screen prompting for user name. */
public class WelcomeScreen extends ConfigPromptScreen {

    public WelcomeScreen(Config config) {
        super(config, "your name", 50);
    }

    @Override
    protected String promptTitle() {
        return "Welcome!";
    }

    @Override
    protected String promptBody() {
        return "What should I call you?";
    }

    @Override
    protected UpdateResult<? extends Model> onSubmit(String name) {
        if (name != null && !name.isBlank()) {
            config.setUserName(name);
        }
        // After name, check if API key is needed
        if (!config.hasResolvedApiKey()) {
            return UpdateResult.from(new ApiKeyPromptScreen(config, name, width, height));
        }
        return ApiKeyPromptScreen.transitionToChat(config, name, width, height);
    }

    public static void main(String[] args) {
        new Program(new WelcomeScreen(new Config())).run();
    }
}
