package com.williamcallahan.lattetui;

import com.williamcallahan.Config;
import org.flatscrew.latte.Command;
import org.flatscrew.latte.Message;
import org.flatscrew.latte.Model;
import org.flatscrew.latte.UpdateResult;
import org.flatscrew.latte.cream.Position;
import org.flatscrew.latte.cream.Style;
import org.flatscrew.latte.cream.border.StandardBorder;
import org.flatscrew.latte.input.key.KeyAliases;
import org.flatscrew.latte.input.key.KeyAliases.KeyAlias;
import org.flatscrew.latte.input.key.KeyType;
import org.flatscrew.latte.message.KeyPressMessage;
import org.flatscrew.latte.message.QuitMessage;
import org.flatscrew.latte.message.WindowSizeMessage;
import org.flatscrew.latte.spice.textinput.TextInput;

import static org.flatscrew.latte.Command.batch;
import static org.flatscrew.latte.Command.setWidowTitle;

/**
 * Reusable config prompt screen with consistent CRT terminal styling.
 * Subclasses provide title, placeholder, and transition logic.
 */
public abstract class ConfigPromptScreen implements Model {

    private static final String APP_NAME = "brief";
    private static final String VERSION = "v0.1";

    protected final Config config;
    protected final TextInput textInput;
    protected int width = 80;
    protected int height = 24;

    protected ConfigPromptScreen(Config config, String placeholder, int charLimit) {
        this.config = config;
        this.textInput = new TextInput();
        textInput.setPlaceholder(placeholder);
        textInput.setPromptStyle(TuiTheme.inputPrompt());
        textInput.focus();
        textInput.setCharLimit(charLimit);
        textInput.setWidth(30);
    }

    /** Title shown above the prompt (e.g., "Welcome!"). */
    protected abstract String promptTitle();

    /** Body text below the title (e.g., "What should I call you?"). */
    protected abstract String promptBody();

    /** Called when user presses Enter. Must return next screen or quit. */
    protected abstract UpdateResult<? extends Model> onSubmit(String value);

    @Override
    public Command init() {
        return batch(setWidowTitle(APP_NAME + " " + VERSION), Command.checkWindowSize());
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage w) {
            width = Math.max(40, w.width());
            height = Math.max(12, w.height());
            return UpdateResult.from(this);
        }

        if (msg instanceof KeyPressMessage key) {
            if (KeyAliases.getKeyType(KeyAlias.KeyEnter) == key.type()) {
                return onSubmit(textInput.value());
            }
            if (KeyAliases.getKeyType(KeyAlias.KeyCtrlC) == key.type()
                    || KeyType.keyESC == key.type()) {
                return UpdateResult.from(this, QuitMessage::new);
            }
        }

        UpdateResult<? extends Model> r = textInput.update(msg);
        return UpdateResult.from(this, r.command());
    }

    @Override
    public String view() {
        int boxWidth = Math.min(50, width - 4);

        Style titleStyle = TuiTheme.brandTitle();
        Style versionStyle = Style.newStyle().foreground(TuiTheme.MUTED);
        String headerLine = titleStyle.render(APP_NAME) + " " + versionStyle.render(VERSION);

        Style welcomeStyle = Style.newStyle().foreground(TuiTheme.LIGHT).bold(true);
        Style bodyStyle = Style.newStyle().foreground(TuiTheme.MUTED);

        String content = TuiTheme.joinVertical(Position.Left,
            "",
            welcomeStyle.render(promptTitle()),
            bodyStyle.render(promptBody()),
            "",
            textInput.view(),
            "",
            TuiTheme.divider(boxWidth - 4),
            "",
            TuiTheme.shortcutRow(
                TuiTheme.shortcutHint("enter", "continue"),
                TuiTheme.shortcutHint("esc", "quit")
            ),
            ""
        );

        Style boxStyle = Style.newStyle()
            .border(StandardBorder.NormalBorder, true, true, true, true)
            .borderForeground(TuiTheme.BORDER)
            .padding(0, 2, 0, 2)
            .width(boxWidth);

        return TuiTheme.joinVertical(Position.Left,
            "",
            indent(headerLine, 2),
            "",
            indent(boxStyle.render(content), 2)
        );
    }

    private static String indent(String s, int spaces) {
        if (s == null || s.isEmpty()) return "";
        return s.indent(spaces).stripTrailing();
    }
}

