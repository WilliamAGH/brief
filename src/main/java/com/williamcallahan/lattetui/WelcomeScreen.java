package com.williamcallahan.lattetui;

import com.williamcallahan.domain.Conversation;
import org.flatscrew.latte.Command;
import org.flatscrew.latte.Message;
import org.flatscrew.latte.Model;
import org.flatscrew.latte.Program;
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

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.flatscrew.latte.Command.batch;
import static org.flatscrew.latte.Command.setWidowTitle;

/** Welcome screen with a green CRT terminal design. */
public class WelcomeScreen implements Model {

    private static final String APP_NAME = "brief";
    private static final String VERSION = "v0.1";

    private TextInput textInput;
    private int width = 80;
    private int height = 24;

    public WelcomeScreen() {
        this.textInput = new TextInput();
        textInput.setPlaceholder("your name");
        textInput.setPromptStyle(TuiTheme.inputPrompt());
        textInput.focus();
        textInput.setCharLimit(50);
        textInput.setWidth(30);
    }

    @Override
    public Command init() {
        return batch(
            setWidowTitle(APP_NAME + " " + VERSION),
            Command.checkWindowSize()
        );
    }

    /**
     * Handles input and window-resize events.
     *
     * Note on screen transitions: Latte only calls {@link Model#init()} for the initial model.
     * When transitioning screens, it carries over viewport dimensions and enqueues init-like commands
     * (window-title + window-size check) via the {@link UpdateResult}.
     */
    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage w) {
            width = Math.max(40, w.width());
            height = Math.max(12, w.height());
            return UpdateResult.from(this);
        }

        if (msg instanceof KeyPressMessage keyPressMessage) {
            if (KeyAliases.getKeyType(KeyAlias.KeyEnter) == keyPressMessage.type()) {
                String name = textInput.value();

                Conversation.ConversationBuilder convoBuilder = Conversation.builder()
                    .id("c_" + UUID.randomUUID())
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .updatedAt(OffsetDateTime.now(ZoneOffset.UTC));

                String model = System.getenv("LLM_MODEL");
                if (model != null && !model.isBlank()) {
                    convoBuilder.defaultModel(model.trim());
                }
                Conversation convo = convoBuilder.build();
                ChatConversationScreen next = new ChatConversationScreen(name, convo, width, height);
                return UpdateResult.from(
                    next,
                    batch(
                        setWidowTitle("brief - " + convo.getDefaultModel()),
                        Command.checkWindowSize()
                    )
                );
            }

            if (KeyAliases.getKeyType(KeyAlias.KeyCtrlC) == keyPressMessage.type()
                    || KeyType.keyESC == keyPressMessage.type()) {
                return UpdateResult.from(this, QuitMessage::new);
            }
        }

        UpdateResult<? extends Model> updateResult = textInput.update(msg);
        return UpdateResult.from(this, updateResult.command());
    }

    @Override
    public String view() {
        int boxWidth = Math.min(50, width - 4);

        Style titleStyle = TuiTheme.brandTitle();
        Style versionStyle = Style.newStyle().foreground(TuiTheme.MUTED);
        String headerLine = titleStyle.render(APP_NAME) + " " + versionStyle.render(VERSION);

        Style welcomeStyle = Style.newStyle().foreground(TuiTheme.LIGHT).bold(true);
        Style bodyStyle = Style.newStyle().foreground(TuiTheme.MUTED);
        String welcomeTitle = welcomeStyle.render("Welcome!");
        String welcomeBody = bodyStyle.render("What should I call you?");

        String inputView = textInput.view();

        String hints = TuiTheme.shortcutRow(
            TuiTheme.shortcutHint("enter", "continue"),
            TuiTheme.shortcutHint("esc", "quit")
        );

        String content = TuiTheme.joinVertical(Position.Left,
            "",
            welcomeTitle,
            welcomeBody,
            "",
            inputView,
            "",
            TuiTheme.divider(boxWidth - 4),
            "",
            hints,
            ""
        );

        Style boxStyle = Style.newStyle()
            .border(StandardBorder.NormalBorder, true, true, true, true)
            .borderForeground(TuiTheme.BORDER)
            .padding(0, 2, 0, 2)
            .width(boxWidth);
        String box = boxStyle.render(content);

        return TuiTheme.joinVertical(Position.Left,
            "",
            indent(headerLine, 2),
            "",
            indent(box, 2)
        );
    }

    private static String indent(String s, int spaces) {
        if (s == null || s.isEmpty()) return "";
        return s.indent(spaces).stripTrailing();
    }

    public static void main(String[] args) {
        new Program(new WelcomeScreen()).run();
    }
}
