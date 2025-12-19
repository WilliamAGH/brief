package com.williamcallahan.lattetui;

import com.williamcallahan.Config;
import com.williamcallahan.domain.ChatMessage;
import com.williamcallahan.domain.Conversation;
import com.williamcallahan.domain.Role;
import com.williamcallahan.lattetui.slash.ModelSlashCommand;
import com.williamcallahan.lattetui.slash.SlashCommand;
import com.williamcallahan.lattetui.slash.SlashCommands;
import com.williamcallahan.lattetui.slash.WeatherSlashCommand;
import com.williamcallahan.service.ChatCompletionService;
import com.williamcallahan.service.OpenAiService;
import com.williamcallahan.service.ToolExecutor;
import com.williamcallahan.service.tools.WeatherForecastTool;
import org.flatscrew.latte.Command;
import org.flatscrew.latte.Message;
import org.flatscrew.latte.Model;
import org.flatscrew.latte.UpdateResult;
import org.flatscrew.latte.ansi.TextWrapper;
import org.flatscrew.latte.cream.Style;
import org.flatscrew.latte.cream.border.StandardBorder;
import org.flatscrew.latte.input.key.KeyAliases;
import org.flatscrew.latte.input.key.KeyAliases.KeyAlias;
import org.flatscrew.latte.input.key.KeyType;
import org.flatscrew.latte.message.KeyPressMessage;
import org.flatscrew.latte.message.QuitMessage;
import org.flatscrew.latte.message.WindowSizeMessage;
import org.flatscrew.latte.input.MouseButton;
import org.flatscrew.latte.input.MouseMessage;
import org.flatscrew.latte.spice.spinner.Spinner;
import org.flatscrew.latte.spice.spinner.TickMessage;
import org.flatscrew.latte.spice.spinner.SpinnerType;
import org.flatscrew.latte.spice.textinput.TextInput;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.flatscrew.latte.Command.batch;
import static org.flatscrew.latte.Command.setWidowTitle;

/** Chat conversation screen with a CRT-style theme. */
public final class ChatConversationScreen implements Model {

    private record AssistantReplyMessage(String text) implements Message {}
    private record ToolReplyMessage(String text) implements Message {}
    /** Display-only output (not added to conversation or sent to LLM). */
    private record LocalDisplayMessage(String text) implements Message {}
    /** System context message (added to conversation as SYSTEM role for LLM context). */
    private record SystemContextMessage(String text) implements Message {}

    private final Conversation conversation;
    private final String userName;
    private final Config config;

    private final OpenAiService openAi;
    private final ToolExecutor toolExecutor;
    private final List<SlashCommand> slashCommands = SlashCommands.defaults();
    private final TextInput composer = new TextInput();
    private final SlashCommandPalette slashPalette = new SlashCommandPalette();
    private final ModelPalette modelPalette = new ModelPalette();
    private final boolean printToScrollback;
    private final boolean mouseSelectionEnabled;
    private final boolean showToolMessages;
    private boolean needsModelSelection;

    private int width = 80;
    private int height = 24;

    private boolean waiting = false;
    private final HistoryViewport historyViewport = new HistoryViewport();

    private final MouseSelectionController mouseSelection = new MouseSelectionController();

    private long lastEnterAtMs = 0L;
    private String lastEnterText = null;

    private Spinner spinner = new Spinner(SpinnerType.DOT);

    /**
     * Creates a chat screen with an explicit starting viewport size.
     *
     * @param userName User-visible label for the local user
     * @param conversation Backing conversation state
     * @param config User configuration for persisting preferences
     * @param width Current terminal columns
     * @param height Current terminal rows
     * @param needsModelSelection If true, opens model palette on first render
     */
    public ChatConversationScreen(String userName, Conversation conversation, Config config, int width, int height, boolean needsModelSelection) {
        this.userName = (userName == null || userName.isBlank()) ? "You" : userName.trim();
        this.conversation = conversation;
        this.config = config;
        this.width = Math.max(40, width);
        this.height = Math.max(12, height);
        this.needsModelSelection = needsModelSelection;
        this.printToScrollback = "1".equals(System.getenv("BRIEF_SCROLLBACK"));
        this.mouseSelectionEnabled = "select".equalsIgnoreCase(resolveMouseMode());
        // Default OFF: tool JSON is usually noisy. Set BRIEF_SHOW_TOOLS=1 for debugging.
        this.showToolMessages = "1".equals(System.getenv("BRIEF_SHOW_TOOLS"));

        this.openAi = new OpenAiService(config);
        this.toolExecutor = new ToolExecutor(new ChatCompletionService(openAi), List.of(new WeatherForecastTool()));

        composer.setPrompt("> ");
        composer.setPlaceholder("Ask me anything...");
        composer.setCharLimit(4000);
        composer.setWidth(this.width - 6);
        composer.focus();
    }

    private static String resolveMouseMode() {
        String mode = System.getenv("BRIEF_MOUSE");
        if (mode == null || mode.isBlank()) return "select";
        return mode;
    }

    @Override
    public Command init() {
        return batch(
            setWidowTitle("brief - " + conversation.getDefaultModel()),
            Command.checkWindowSize()
        );
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage w) {
            width = Math.max(40, w.width());
            height = Math.max(12, w.height());
            composer.setWidth(Math.max(20, width - 6));

            if (needsModelSelection) {
                needsModelSelection = false;
                return openModelPalette();
            }
            return UpdateResult.from(this);
        }

        if (msg instanceof MouseMessage mouse && mouse.isWheel()) {
            if (mouse.getButton() == MouseButton.MouseButtonWheelUp) {
                historyViewport.scrollUp(3);
                return UpdateResult.from(this);
            }
            if (mouse.getButton() == MouseButton.MouseButtonWheelDown) {
                historyViewport.scrollDown(3);
                return UpdateResult.from(this);
            }
        }

        if (mouseSelectionEnabled && msg instanceof MouseMessage mouse) {
            if (mouseSelection.handle(mouse)) return UpdateResult.from(this);
        }

        if (msg instanceof TickMessage) {
            if (!waiting) return UpdateResult.from(this);
            UpdateResult<? extends Model> r = spinner.update(msg);
            return UpdateResult.from(this, r.command());
        }

        if (msg instanceof AssistantReplyMessage reply) {
            append(Role.ASSISTANT, ChatMessage.Source.LLM_OUTPUT, reply.text());
            waiting = false;
            historyViewport.follow();
            return UpdateResult.from(this, maybePrintToScrollback("Assistant", reply.text()));
        }

        if (msg instanceof ToolReplyMessage reply) {
            // ToolReplyMessage is only for actual tool call results - displayed but not added to conversation
            // since there's no preceding tool_call. Use LocalDisplayMessage or SystemContextMessage instead.
            waiting = false;
            historyViewport.follow();
            return UpdateResult.from(this, maybePrintToScrollback("Tool", reply.text()));
        }

        if (msg instanceof LocalDisplayMessage reply) {
            // Display-only: shown in UI but NOT added to conversation (no LLM context)
            waiting = false;
            historyViewport.follow();
            return UpdateResult.from(this, maybePrintToScrollback("", reply.text()));
        }

        if (msg instanceof SystemContextMessage reply) {
            // Added to conversation as SYSTEM role so LLM can use it for follow-up questions
            append(Role.SYSTEM, ChatMessage.Source.SYSTEM, reply.text());
            waiting = false;
            historyViewport.follow();
            return UpdateResult.from(this, maybePrintToScrollback("", reply.text()));
        }

        if (msg instanceof KeyPressMessage key) {
            if (KeyAliases.getKeyType(KeyAlias.KeyCtrlC) == key.type()) {
                return UpdateResult.from(this, QuitMessage::new);
            }

            if (KeyType.keyESC == key.type()) {
                if (modelPalette.isOpen()) {
                    modelPalette.close();
                    return UpdateResult.from(this);
                }
                if (slashPalette.isOpen()) {
                    slashPalette.close();
                    return UpdateResult.from(this);
                }
                return UpdateResult.from(this, QuitMessage::new);
            }

            if (modelPalette.isOpen()) {
                ModelPalette.PaletteResult result = modelPalette.update(key);
                if (result.handled()) {
                    if (result.selectedModel() != null) {
                        conversation.setDefaultModel(result.selectedModel());
                        config.setModel(result.selectedModel());
                    }
                    return UpdateResult.from(this);
                }
            }

            if (slashPalette.isOpen() || (!waiting && key.type() == KeyType.KeyRunes)) {
                SlashCommandPalette.PaletteUpdate pu = slashPalette.update(key, msg, composer, waiting, slashCommands);
                if (pu.handled()) {
                    if (pu.submitText() != null) return submitUserText(pu.submitText());
                    return UpdateResult.from(this, pu.command());
                }
            }

            if (key.type() == KeyType.KeyShiftUp) {
                historyViewport.scrollUp(1);
                return UpdateResult.from(this);
            }
            if (key.type() == KeyType.KeyShiftDown) {
                historyViewport.scrollDown(1);
                return UpdateResult.from(this);
            }

            if (key.type() == KeyType.KeyHome || key.type() == KeyType.KeyCtrlHome) {
                historyViewport.top();
                return UpdateResult.from(this);
            }

            if (key.type() == KeyType.KeyPgUp) {
                historyViewport.scrollUp(5);
                return UpdateResult.from(this);
            }
            if (key.type() == KeyType.KeyPgDown) {
                historyViewport.scrollDown(5);
                return UpdateResult.from(this);
            }
            if (key.type() == KeyType.KeyEnd) {
                historyViewport.follow();
                return UpdateResult.from(this);
            }

            if (KeyAliases.getKeyType(KeyAlias.KeyEnter) == key.type()) {
                String text = composer.value().trim();
                if (text.isEmpty() || waiting) return UpdateResult.from(this);

                long now = System.currentTimeMillis();
                if (text.equals(lastEnterText) && (now - lastEnterAtMs) < 1000) {
                    composer.reset();
                    return UpdateResult.from(this);
                }
                lastEnterText = text;
                lastEnterAtMs = now;
                return submitUserText(text);
            }
        }

        UpdateResult<? extends Model> inputUpdate = composer.update(msg);
        return UpdateResult.from(this, inputUpdate.command());
    }

    @Override
    public String view() {
        // Latte `Style.width/height` apply *before* borders/margins; borders add additional rows/cols.
        // If I set width/height to the terminal dimensions directly, the rendered frame becomes larger
        // than the terminal and Latte's renderer truncates it (making the right/bottom borders look wrong).
        int viewportWidth = Math.max(40, width);
        int viewportHeight = Math.max(12, height);

        Style frame = Style.newStyle()
            .border(StandardBorder.NormalBorder, true, true, true, true)
            .borderForeground(TuiTheme.BORDER)
            .padding(0, 1, 0, 1);

        int frameWidth = Math.max(1, viewportWidth - frame.getHorizontalBorderSize() - frame.getHorizontalMargins());
        int frameHeight = Math.max(1, viewportHeight - frame.getVerticalBorderSize() - frame.getVerticalMargins());
        frame.width(frameWidth).height(frameHeight);

        int innerWidth = Math.max(20, viewportWidth - frame.getHorizontalFrameSize());
        int innerHeight = Math.max(4, viewportHeight - frame.getVerticalFrameSize());

        String header = renderHeader(innerWidth);
        String footer = renderFooter(innerWidth);

        String composerView = composer.view();
        List<String> composerLines = splitLines(composerView);
        if (composerLines.isEmpty()) composerLines = List.of("");

        String status = "";
        if (waiting) {
            Style spinnerStyle = TuiTheme.spinner();
            Style textStyle = TuiTheme.hint();
            status = spinnerStyle.render(spinner.view()) + " " + textStyle.render("thinking...");
        }

        int headerHeight = 1;
        int dividerHeight = 1;
        int composerHeight = composerLines.size();
        int statusHeight = status.isEmpty() ? 0 : 1;
        int footerHeight = 1;
        int fixedHeight = headerHeight + dividerHeight + composerHeight + statusHeight + footerHeight;
        int historyHeight = Math.max(1, innerHeight - fixedHeight);

        List<String> lines = new ArrayList<>();

        lines.add(TuiTheme.padRight(header, innerWidth));

        List<String> visibleHistory = new ArrayList<>(renderHistoryLines(innerWidth, historyHeight));
        int historyStartRow = frame.getBorderTopSize() + headerHeight;
        int historyStartCol = frame.getBorderLeftSize() + frame.leftPadding();
        List<String> lastVisibleHistoryPlain = visibleHistory.stream()
            .map(TuiTheme::stripAnsi)
            .map(ChatConversationScreen::rstrip)
            .toList();
        mouseSelection.updateHistoryMapping(historyStartRow, historyStartCol, lastVisibleHistoryPlain);

        if (mouseSelectionEnabled && mouseSelection.isSelecting()
            && mouseSelection.selectionStart() >= 0 && mouseSelection.selectionEnd() >= 0) {
            int start = Math.min(mouseSelection.selectionStart(), mouseSelection.selectionEnd());
            int end = Math.max(mouseSelection.selectionStart(), mouseSelection.selectionEnd());
            start = Math.max(0, Math.min(start, visibleHistory.size() - 1));
            end = Math.max(0, Math.min(end, visibleHistory.size() - 1));
            for (int i = start; i <= end; i++) {
                visibleHistory.set(i, highlightSelection(lastVisibleHistoryPlain.get(i), innerWidth));
            }
        }
        lines.addAll(visibleHistory);

        int dividerRow = lines.size();
        lines.add(TuiTheme.divider(innerWidth));

        for (String line : composerLines) {
            lines.add(TuiTheme.padRight(line, innerWidth));
        }

        if (!status.isEmpty()) {
            lines.add(TuiTheme.padRight(status, innerWidth));
        }

        lines.add(TuiTheme.padRight(footer, innerWidth));

        slashPalette.applyOverlay(lines, innerWidth, innerHeight, dividerRow, slashCommands, composer.value());
        modelPalette.applyOverlay(lines, innerWidth, innerHeight, dividerRow);

        while (lines.size() < innerHeight) {
            lines.add(" ".repeat(innerWidth));
        }
        if (lines.size() > innerHeight) {
            lines = lines.subList(0, innerHeight);
        }

        String content = String.join("\n", lines);
        return frame.render(content);
    }

    private UpdateResult<? extends Model> submitUserText(String text) {
        if (text == null) text = "";
        text = text.trim();
        if (text.isEmpty() || waiting) return UpdateResult.from(this);

        // /weather is LLM-routed: convert to a user prompt and let the model choose the tool call.
        if (text.equals("/weather") || text.startsWith("/weather ")) {
            String userRequest = WeatherSlashCommand.toUserRequest(text);
            String routing = WeatherSlashCommand.toLlmPrompt(text);

            append(Role.USER, ChatMessage.Source.USER_INPUT, userRequest);
            append(Role.SYSTEM, ChatMessage.Source.INTERNAL, routing);

            composer.reset();
            waiting = true;
            historyViewport.follow();
            spinner = new Spinner(SpinnerType.DOT);

            Command call = slashOrChatCall(userRequest);
            Command printUser = maybePrintToScrollback(userName, text);
            return UpdateResult.from(this, batch(printUser, call, spinner.init()));
        }

        if (text.startsWith("/")) {
            SlashCommand sc = SlashCommands.matchInvocation(slashCommands, text);
            if (sc != null && sc.quits()) return UpdateResult.from(this, QuitMessage::new);

            // /model opens the model selection palette
            if (sc instanceof ModelSlashCommand) {
                composer.reset();
                return openModelPalette();
            }

            // Session lifecycle commands: rotate conversation id by transitioning to a fresh screen.
            if (sc != null && ("/clear".equals(sc.name()) || "/new".equals(sc.name()))) {
                Conversation next = newConversationLikeCurrent();
                ChatConversationScreen nextScreen = new ChatConversationScreen(userName, next, config, width, height, false);
                return UpdateResult.from(
                    nextScreen,
                    batch(
                        setWidowTitle("brief - " + next.getDefaultModel()),
                        Command.checkWindowSize()
                    )
                );
            }

            // Other slash commands are local-only: don't append to the LLM conversation.
            composer.reset();
            waiting = true;
            historyViewport.follow();
            spinner = new Spinner(SpinnerType.DOT);

            Command call = slashOrChatCall(text);
            Command printUser = maybePrintToScrollback(userName, text);
            return UpdateResult.from(this, batch(printUser, call, spinner.init()));
        }

        append(Role.USER, ChatMessage.Source.USER_INPUT, text);
        composer.reset();
        waiting = true;
        historyViewport.follow();
        spinner = new Spinner(SpinnerType.DOT);

        Command call = slashOrChatCall(text);

        Command printUser = maybePrintToScrollback(userName, text);
        return UpdateResult.from(this, batch(printUser, call, spinner.init()));
    }

    private UpdateResult<? extends Model> openModelPalette() {
        try {
            List<String> models = openAi.modelChoices();
            if (models.isEmpty()) {
                return UpdateResult.from(this, () -> new ToolReplyMessage("No models available from API"));
            }
            modelPalette.open(models);
            return UpdateResult.from(this);
        } catch (Exception e) {
            String error = e.getMessage();
            return UpdateResult.from(this, () -> new ToolReplyMessage(
                "Failed to fetch models: " + (error == null ? e.getClass().getSimpleName() : error)
            ));
        }
    }

    private Conversation newConversationLikeCurrent() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return Conversation.builder()
            .id("c_" + UUID.randomUUID())
            .createdAt(now)
            .updatedAt(now)
            .provider(conversation.getProvider())
            .apiFamily(conversation.getApiFamily())
            .defaultModel(conversation.getDefaultModel())
            .build();
    }

    private Command slashOrChatCall(String text) {
        SlashCommand sc = text != null && text.startsWith("/") ? SlashCommands.matchInvocation(slashCommands, text) : null;
        if (text != null && text.startsWith("/") && sc == null) {
            return () -> new LocalDisplayMessage("Unknown slash command: " + text.split("\\s+", 2)[0]);
        }
        if (sc != null) {
            return () -> {
                try {
                    String out = sc.run(text);
                    return switch (sc.contextType()) {
                        case ASSISTANT -> new AssistantReplyMessage(out);
                        case SYSTEM -> new SystemContextMessage(out);
                        case NONE -> new LocalDisplayMessage(out);
                    };
                } catch (Throwable t) {
                    String err = t.getMessage();
                    return new LocalDisplayMessage(
                        "ERROR " + t.getClass().getSimpleName()
                            + (err == null || err.isBlank() ? "" : (": " + err))
                    );
                }
            };
        }

        return () -> {
            try {
                String replyText = toolExecutor.respond(conversation, conversation.getDefaultModel());
                return new AssistantReplyMessage(replyText);
            } catch (Throwable t) {
                String err = t.getMessage();
                String baseUrl = openAi.baseUrl();
                return new AssistantReplyMessage(
                    "ERROR " + t.getClass().getSimpleName()
                        + (err == null || err.isBlank() ? "" : (": " + err))
                        + "\nBASE_URL=" + (baseUrl == null ? "(sdk default)" : baseUrl)
                        + "\nMODEL=" + conversation.getDefaultModel()
                );
            }
        };
    }

    private String renderHeader(int width) {
        String title = "brief";
        String info = conversation.getDefaultModel();
        if (historyViewport.scrollOffsetLines() > 0) {
            info = info + "  ↑" + historyViewport.scrollOffsetLines() + "L";
        }
        String status = mouseSelection.transientStatus(System.currentTimeMillis());
        if (status != null) {
            info = info + "  " + status;
        }
        return TuiTheme.headerWithInfo(title, info, width);
    }

    private String renderFooter(int width) {
        String shortcuts = TuiTheme.shortcutRow(
            TuiTheme.shortcutHint("scroll", "Shift+↑/↓ PgUp/PgDn Home"),
            TuiTheme.shortcutHint("end", "follow"),
            TuiTheme.shortcutHint("/", "commands"),
            TuiTheme.shortcutHint("ctrl+c", "quit")
        );

        String configError = config.transientError(System.currentTimeMillis());
        if (configError != null) {
            String errorText = TuiTheme.warning().render(configError);
            int shortcutsLen = TuiTheme.stripAnsi(shortcuts).length();
            int errorLen = configError.length();
            int gap = width - shortcutsLen - errorLen - 2;
            if (gap > 0) {
                return shortcuts + " ".repeat(gap) + errorText;
            }
            // Not enough room — show error only
            return TuiTheme.truncate(errorText, width);
        }

        return TuiTheme.truncate(shortcuts, width);
    }

    private List<String> renderHistoryLines(int wrapWidth, int maxLines) {
        String allHistory = renderAllHistory(wrapWidth);
        String[] allLines = allHistory.isEmpty() ? new String[0] : allHistory.split("\n", -1);

        if (allLines.length == 0) {
            // Show empty state
            List<String> empty = new ArrayList<>();
            Style emptyStyle = TuiTheme.hint();
            String emptyMsg = emptyStyle.render("Start a conversation...");
            int pad = Math.max(0, maxLines / 2);
            for (int i = 0; i < pad; i++) empty.add("");
            empty.add(TuiTheme.center(emptyMsg, wrapWidth));
            while (empty.size() < maxLines) empty.add("");
            return empty;
        }

        HistoryViewport.Window w = historyViewport.window(allLines.length, maxLines);

        List<String> result = new ArrayList<>();
        for (int i = w.startInclusive(); i < w.endExclusive(); i++) {
            result.add(TuiTheme.padRight(allLines[i], wrapWidth));
        }
        while (result.size() < maxLines) {
            result.add(" ".repeat(wrapWidth));
        }
        return result;
    }

    private String renderAllHistory(int wrapWidth) {
        StringBuilder out = new StringBuilder();
        for (ChatMessage m : conversation.getMessages()) {
            if (m != null && m.source() == ChatMessage.Source.INTERNAL) continue;
            if (!showToolMessages && m != null && m.role() == Role.TOOL
                && m.toolCallId() != null && !m.toolCallId().isBlank()) continue;
            out.append(renderMessageBlock(m, wrapWidth));
        }
        return TuiTheme.stripTrailingNewlines(out.toString());
    }

    private String renderMessageBlock(ChatMessage m, int wrapWidth) {
        if (m == null) return "";

        String label = switch (m.role()) {
            case USER -> TuiTheme.userLabel().render(userName);
            case ASSISTANT -> TuiTheme.assistantLabel().render("Assistant");
            case SYSTEM -> TuiTheme.warning().render("System");
            case TOOL -> TuiTheme.hint().render("Tool");
        };

        StringBuilder out = new StringBuilder();
        out.append(label).append("\n");

        String content = (m.content() == null) ? "" : m.content();
        String wrapped = new TextWrapper().wrap(content, Math.max(10, wrapWidth));
        for (String line : wrapped.split("\n", -1)) {
            out.append(line).append("\n");
        }
        out.append("\n");

        return out.toString();
    }

    private void append(Role role, ChatMessage.Source source, String content) {
        int index = conversation.getMessages().size();
        conversation.addMessage(new ChatMessage(
            "m_%04d_%s".formatted(index + 1, UUID.randomUUID().toString().substring(0, 8)),
            conversation.getId(), index, role, source, content == null ? "" : content,
            OffsetDateTime.now(ZoneOffset.UTC), conversation.getDefaultModel(),
            conversation.getProvider().name().toLowerCase(), null, null, null, null, null));
    }

    private Command maybePrintToScrollback(String label, String content) {
        if (!printToScrollback) return null;
        String safeLabel = (label == null || label.isBlank()) ? "" : label.trim();
        String safeContent = content == null ? "" : content;
        String line = safeLabel.isEmpty() ? safeContent : (safeLabel + ": " + safeContent);
        return Command.println(line);
    }

    private static String highlightSelection(String plain, int width) {
        // Use reverse-video so the selection is visible regardless of theme.
        // Keep content plain (no ANSI) to avoid embedded resets breaking the highlight.
        String padded = TuiTheme.padRight(plain, width);
        return "\u001b[7m" + padded + "\u001b[0m";
    }

    private static String rstrip(String s) {
        if (s == null || s.isEmpty()) return "";
        int i = s.length() - 1;
        while (i >= 0 && Character.isWhitespace(s.charAt(i))) i--;
        return s.substring(0, i + 1);
    }

    private static List<String> splitLines(String s) {
        if (s == null || s.isEmpty()) return List.of();
        List<String> result = new ArrayList<>();
        for (String line : s.split("\n", -1)) {
            result.add(line);
        }
        return result;
    }
}