package com.williamcallahan.ui;

import com.williamcallahan.Config;
import com.williamcallahan.domain.ChatMessage;
import com.williamcallahan.domain.Conversation;
import com.williamcallahan.domain.Role;
import com.williamcallahan.ui.slash.ModelSlashCommand;
import com.williamcallahan.ui.slash.SlashCommand;
import com.williamcallahan.ui.slash.SlashCommands;
import com.williamcallahan.ui.slash.WeatherSlashCommand;
import com.williamcallahan.service.ChatCompletionService;
import com.williamcallahan.service.OpenAiService;
import com.williamcallahan.service.ToolExecutor;
import com.williamcallahan.service.tools.WeatherForecastTool;
import com.williamcallahan.tui4j.Command;
import com.williamcallahan.tui4j.Message;
import com.williamcallahan.tui4j.Model;
import com.williamcallahan.tui4j.UpdateResult;
import com.williamcallahan.tui4j.ansi.TextWrapper;
import com.williamcallahan.tui4j.compat.bubbletea.lipgloss.Style;
import com.williamcallahan.tui4j.compat.bubbletea.lipgloss.border.StandardBorder;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases.KeyAlias;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.bubbletea.message.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.message.QuitMessage;
import com.williamcallahan.tui4j.compat.bubbletea.message.WindowSizeMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseButton;
import com.williamcallahan.tui4j.input.MouseClickMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage;
import com.williamcallahan.tui4j.input.MouseTarget;
import com.williamcallahan.tui4j.input.MouseTargetProvider;
import com.williamcallahan.tui4j.compat.bubbletea.bubbles.spinner.Spinner;
import com.williamcallahan.tui4j.compat.bubbletea.bubbles.spinner.TickMessage;
import com.williamcallahan.tui4j.compat.bubbletea.bubbles.spinner.SpinnerType;
import com.williamcallahan.tui4j.compat.bubbletea.bubbles.textinput.TextInput;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.williamcallahan.tui4j.Command.batch;
import static com.williamcallahan.tui4j.Command.setWidowTitle;

/**
 * Main chat interface providing a conversation view, input composer, and slash commands.
 * Implements a CRT-style green terminal aesthetic.
 */
public final class ChatConversationScreen implements Model, MouseTargetProvider {

    private record SlashLlmOverride(
        Function<String, String> userText,
        Function<String, String> systemPrompt
    ) {
        String userTextFor(String input) {
            return userText == null ? input : userText.apply(input);
        }

        String systemPromptFor(String input) {
            return systemPrompt == null ? null : systemPrompt.apply(input);
        }
    }

    private static final Map<String, SlashLlmOverride> SLASH_LLM_OVERRIDES = Map.of(
        "/weather",
        new SlashLlmOverride(WeatherSlashCommand::toUserRequest, WeatherSlashCommand::toLlmPrompt)
    );

    private record AssistantReplyMessage(String text) implements Message {}
    private record LocalDisplayMessage(String text) implements Message {}
    private record SystemContextMessage(String text) implements Message {}
    private record HistoryRender(List<String> visibleStyled, List<String> visiblePlain,
                                 List<String> allPlain, int windowStartIndex) {}

    private final Conversation conversation;
    private final String userName;
    private final Config config;

    private final OpenAiService openAiService;
    private final ChatCompletionService chatCompletionService;
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
    private List<MouseTarget> mouseTargets = List.of();
    private PaletteOverlay.Layout slashOverlayLayout;
    private PaletteOverlay.Layout modelOverlayLayout;
    private int innerLeft = 0;
    private int innerTop = 0;

    private long lastEnterAtMs = 0L;
    private String lastEnterText = null;

    private Spinner spinner = new Spinner(SpinnerType.DOT);

    public ChatConversationScreen(String userName, Conversation conversation, Config config,
                                  int width, int height, boolean needsModelSelection) {
        this.userName = (userName == null || userName.isBlank()) ? "You" : userName.trim();
        this.conversation = conversation;
        this.config = config;
        this.width = Math.max(40, width);
        this.height = Math.max(12, height);
        this.needsModelSelection = needsModelSelection;
        this.printToScrollback = "1".equals(System.getenv("BRIEF_SCROLLBACK"));
        this.mouseSelectionEnabled = "select".equalsIgnoreCase(resolveMouseMode());
        this.showToolMessages = "1".equals(System.getenv("BRIEF_SHOW_TOOLS"));

        this.openAiService = new OpenAiService(config);
        this.chatCompletionService = new ChatCompletionService(openAiService);
        this.toolExecutor = new ToolExecutor(chatCompletionService, List.of(new WeatherForecastTool()));

        composer.setPrompt("> ");
        composer.setPlaceholder("Ask me anything...");
        composer.setCharLimit(4000);
        composer.setWidth(this.width - 6);
        composer.focus();
    }

    @Override
    public List<MouseTarget> mouseTargets() {
        return mouseTargets;
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
            Command.checkWindowSize(),
            Command.setMouseCursorText()
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
            Command cmd = mouseSelection.handle(mouse);
            if (cmd != null) return UpdateResult.from(this, cmd);
            if (mouseSelection.isSelecting()) return UpdateResult.from(this);
        }

        if (msg instanceof MouseClickMessage click) {
            UpdateResult<? extends Model> clickResult = handleMouseClick(click);
            if (clickResult != null) {
                return clickResult;
            }
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

        if (msg instanceof LocalDisplayMessage reply) {
            waiting = false;
            historyViewport.follow();
            return UpdateResult.from(this, maybePrintToScrollback("", reply.text()));
        }

        if (msg instanceof SystemContextMessage reply) {
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
                    UpdateResult<? extends Model> result = handleSlashPaletteUpdate(pu);
                    if (result != null) return result;
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

            if (key.type() == KeyType.KeyShiftLeft) {
                historyViewport.top();
                return UpdateResult.from(this);
            }
            if (key.type() == KeyType.KeyShiftRight) {
                historyViewport.follow();
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

    private UpdateResult<? extends Model> handleMouseClick(MouseClickMessage click) {
        if (click.button() != MouseButton.MouseButtonLeft) {
            return null;
        }
        if (mouseSelectionEnabled && mouseSelection.isSelecting()) {
            return UpdateResult.from(this);
        }

        int column = click.column() - innerLeft;
        int row = click.row() - innerTop;

        if (modelPalette.isOpen() && modelOverlayLayout != null) {
            if (!modelOverlayLayout.contains(column, row)) {
                modelPalette.close();
                return UpdateResult.from(this);
            }
            int index = modelOverlayLayout.itemIndexAt(column, row);
            if (index >= 0) {
                ModelPalette.PaletteResult result = modelPalette.click(index);
                if (result.handled() && result.selectedModel() != null) {
                    conversation.setDefaultModel(result.selectedModel());
                    config.setModel(result.selectedModel());
                }
                return UpdateResult.from(this);
            }
            return UpdateResult.from(this);
        }

        if (slashPalette.isOpen() && slashOverlayLayout != null) {
            if (!slashOverlayLayout.contains(column, row)) {
                slashPalette.close();
                return UpdateResult.from(this);
            }
            int index = slashOverlayLayout.itemIndexAt(column, row);
            if (index >= 0) {
                SlashCommandPalette.PaletteUpdate pu = slashPalette.click(index, composer, slashCommands);
                return handleSlashPaletteUpdate(pu);
            }
            return UpdateResult.from(this);
        }

        MouseTarget target = click.target();
        if (target == null) {
            return null;
        }

        if ("toolbar.commands".equals(target.id())) {
            if (!waiting) {
                slashPalette.openFromMouse(composer, slashCommands);
                return UpdateResult.from(this);
            }
            return UpdateResult.from(this);
        }

        if (target.hyperlink() != null) {
            return UpdateResult.from(this, Command.openUrl(target.hyperlink()));
        }

        return null;
    }

    private UpdateResult<? extends Model> handleSlashPaletteUpdate(SlashCommandPalette.PaletteUpdate pu) {
        if (pu == null || !pu.handled()) return null;
        if (pu.submitText() != null) return submitUserText(pu.submitText());
        return UpdateResult.from(this, pu.command());
    }

    @Override
    public String view() {
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

        innerLeft = frame.getBorderLeftSize() + frame.leftPadding();
        innerTop = frame.getBorderTopSize();

        String header = renderHeader(innerWidth);

        String composerView = composer.view();
        List<String> composerLines = splitLines(composerView);
        if (composerLines.isEmpty()) composerLines = List.of("");

        String statusLeft = "";
        if (waiting) {
            Style spinnerStyle = TuiTheme.spinner();
            Style textStyle = TuiTheme.hint();
            statusLeft = spinnerStyle.render(spinner.view()) + " " + textStyle.render("thinking...");
        }
        String rightHints = TuiTheme.shortcutRow(
            TuiTheme.shortcutHint("scroll", "Shift+↑/↓"),
            TuiTheme.shortcutHint("/", "commands"),
            TuiTheme.shortcutHint("esc", "quit")
        );
        String statusRow = joinLeftRight(statusLeft, rightHints, innerWidth);

        int headerHeight = 1;
        int dividerHeight = 1;
        int composerHeight = composerLines.size();
        int statusHeight = 1;
        int fixedHeight = headerHeight + dividerHeight + composerHeight + statusHeight;
        int historyHeight = Math.max(1, innerHeight - fixedHeight);

        List<String> lines = new ArrayList<>();
        lines.add(TuiTheme.padRight(header, innerWidth));

        HistoryRender historyRender = renderHistory(innerWidth, historyHeight);
        List<String> visibleHistory = new ArrayList<>(historyRender.visibleStyled());
        int historyStartRow = frame.getBorderTopSize() + headerHeight;
        int historyStartCol = frame.getBorderLeftSize() + frame.leftPadding();
        mouseSelection.updateHistoryMapping(
            historyStartRow,
            historyStartCol,
            historyRender.windowStartIndex(),
            historyRender.visiblePlain(),
            historyRender.allPlain()
        );

        if (mouseSelectionEnabled && mouseSelection.hasSelection()) {
            int selectionStartLineIndex = mouseSelection.selectionStartLineIndex();
            int selectionStartCol = mouseSelection.selectionStartCol();
            int selectionEndLineIndex = mouseSelection.selectionEndLineIndex();
            int selectionEndCol = mouseSelection.selectionEndCol();

            if (selectionStartLineIndex > selectionEndLineIndex
                || (selectionStartLineIndex == selectionEndLineIndex && selectionStartCol > selectionEndCol)) {
                int tempLine = selectionStartLineIndex; selectionStartLineIndex = selectionEndLineIndex; selectionEndLineIndex = tempLine;
                int tempCol = selectionStartCol; selectionStartCol = selectionEndCol; selectionEndCol = tempCol;
            }

            int windowStartIndex = historyRender.windowStartIndex();
            int windowEndIndex = windowStartIndex + historyRender.visiblePlain().size() - 1;
            int visibleStartIndex = Math.max(selectionStartLineIndex, windowStartIndex);
            int visibleEndIndex = Math.min(selectionEndLineIndex, windowEndIndex);

            for (int lineIndex = visibleStartIndex; lineIndex <= visibleEndIndex; lineIndex++) {
                int visibleRowIndex = lineIndex - windowStartIndex;
                int startCol = (lineIndex == selectionStartLineIndex) ? selectionStartCol : 0;
                int endCol = (lineIndex == selectionEndLineIndex) ? selectionEndCol : Integer.MAX_VALUE;
                visibleHistory.set(
                    visibleRowIndex,
                    highlightSelection(historyRender.visiblePlain().get(visibleRowIndex), innerWidth, startCol, endCol)
                );
            }
        }
        lines.addAll(visibleHistory);

        int dividerRow = lines.size();
        lines.add(TuiTheme.divider(innerWidth));

        for (String line : composerLines) {
            lines.add(TuiTheme.padRight(line, innerWidth));
        }

        lines.add(TuiTheme.padRight(statusRow, innerWidth));
        int statusRowIndex = lines.size() - 1;

        PaletteOverlay.Overlay slashOverlay = slashPalette.applyOverlay(
            lines, innerWidth, innerHeight, dividerRow, slashCommands, composer.value()
        );
        PaletteOverlay.Overlay modelOverlay = modelPalette.applyOverlay(lines, innerWidth, innerHeight, dividerRow);
        slashOverlayLayout = (slashOverlay == null) ? null : slashOverlay.layout();
        modelOverlayLayout = (modelOverlay == null) ? null : modelOverlay.layout();

        mouseTargets = buildMouseTargets(statusRow, statusRowIndex, slashOverlay, modelOverlay);

        while (lines.size() < innerHeight) {
            lines.add(" ".repeat(innerWidth));
        }
        if (lines.size() > innerHeight) {
            lines = lines.subList(0, innerHeight);
        }

        String content = String.join("\n", lines);
        return frame.render(content);
    }

    private List<MouseTarget> buildMouseTargets(
            String statusRow,
            int statusRowIndex,
            PaletteOverlay.Overlay slashOverlay,
            PaletteOverlay.Overlay modelOverlay
    ) {
        List<MouseTarget> targets = new ArrayList<>();
        addToolbarTargets(statusRow, statusRowIndex, targets);
        addOverlayTargets("slash", slashOverlay, targets);
        addOverlayTargets("model", modelOverlay, targets);
        return targets;
    }

    private void addToolbarTargets(String statusRow, int statusRowIndex, List<MouseTarget> targets) {
        String plain = TuiTheme.stripAnsi(statusRow);
        int commandsIndex = plain.indexOf("commands");
        if (commandsIndex < 0) return;
        int row = innerTop + statusRowIndex;
        int col = innerLeft + commandsIndex;
        targets.add(MouseTarget.click("toolbar.commands", col, row, "commands".length(), 1));
    }

    private void addOverlayTargets(String prefix, PaletteOverlay.Overlay overlay, List<MouseTarget> targets) {
        if (overlay == null || overlay.layout() == null) return;
        PaletteOverlay.Layout layout = overlay.layout();
        int visibleCount = layout.visibleItemCount();
        int itemStartRow = innerTop + layout.itemRowStart();
        int itemStartCol = innerLeft + layout.leftCol() + 1;
        int itemWidth = layout.innerBoxWidth();
        for (int i = 0; i < visibleCount; i++) {
            int index = layout.scrollTop() + i;
            int row = itemStartRow + i;
            targets.add(MouseTarget.click(prefix + ".item." + index, itemStartCol, row, itemWidth, 1));
        }
    }

    private UpdateResult<? extends Model> submitUserText(String text) {
        if (text == null) text = "";
        text = text.trim();
        if (text.isEmpty() || waiting) return UpdateResult.from(this);

        if (text.startsWith("/")) {
            SlashCommand sc = SlashCommands.matchInvocation(slashCommands, text);
            if (sc != null && sc.quits()) return UpdateResult.from(this, QuitMessage::new);

            if (sc instanceof ModelSlashCommand) {
                composer.reset();
                return openModelPalette();
            }

            SlashLlmOverride override = (sc == null) ? null : SLASH_LLM_OVERRIDES.get(sc.name());
            if (override != null) {
                return submitToLlmDetached(text, override.userTextFor(text), override.systemPromptFor(text));
            }

            if (sc != null && ("/clear".equals(sc.name()) || "/new".equals(sc.name()))) {
                Conversation next = newConversationLikeCurrent();
                ChatConversationScreen nextScreen = new ChatConversationScreen(userName, next, config, width, height, false);
                return UpdateResult.from(
                    nextScreen,
                    batch(
                        setWidowTitle("brief - " + next.getDefaultModel()),
                        Command.checkWindowSize(),
                        Command.setMouseCursorText()
                    )
                );
            }

            composer.reset();
            waiting = true;
            historyViewport.follow();
            spinner = new Spinner(SpinnerType.DOT);

            Command call = slashOrChatCall(text);
            Command printUser = maybePrintToScrollback(userName, text);
            return UpdateResult.from(this, batch(printUser, call, spinner.init()));
        }

        return submitToLlm(text, null);
    }

    private UpdateResult<? extends Model> openModelPalette() {
        try {
            List<String> models = openAiService.modelChoices();
            if (models.isEmpty()) {
                return UpdateResult.from(this, () -> new LocalDisplayMessage("No models available from API"));
            }
            modelPalette.open(models);
            return UpdateResult.from(this);
        } catch (Exception e) {
            String error = e.getMessage();
            return UpdateResult.from(this, () -> new LocalDisplayMessage(
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

        return llmCall();
    }

    private UpdateResult<? extends Model> submitToLlm(String text, String internalSystemPrompt) {
        append(Role.USER, ChatMessage.Source.USER_INPUT, text);
        appendInternalSystemPrompt(internalSystemPrompt);
        composer.reset();
        waiting = true;
        historyViewport.follow();
        spinner = new Spinner(SpinnerType.DOT);

        Command call = llmCall();
        Command printUser = maybePrintToScrollback(userName, text);
        return UpdateResult.from(this, batch(printUser, call, spinner.init()));
    }

    private UpdateResult<? extends Model> submitToLlmDetached(String displayText, String llmUserText, String internalSystemPrompt) {
        append(Role.USER, ChatMessage.Source.LOCAL, displayText);
        appendInternalSystemPrompt(internalSystemPrompt);
        String userText = (llmUserText == null || llmUserText.isBlank()) ? displayText : llmUserText;
        append(Role.USER, ChatMessage.Source.INTERNAL, userText);
        composer.reset();
        waiting = true;
        historyViewport.follow();
        spinner = new Spinner(SpinnerType.DOT);

        Command call = llmCall();
        Command printUser = maybePrintToScrollback(userName, displayText);
        return UpdateResult.from(this, batch(printUser, call, spinner.init()));
    }

    private void appendInternalSystemPrompt(String internalSystemPrompt) {
        if (internalSystemPrompt == null || internalSystemPrompt.isBlank()) return;
        append(Role.SYSTEM, ChatMessage.Source.INTERNAL, internalSystemPrompt);
    }

    private Command llmCall() {
        return () -> {
            try {
                String replyText = toolExecutor.respond(conversation, conversation.getDefaultModel());
                return new AssistantReplyMessage(replyText);
            } catch (Throwable t) {
                String err = t.getMessage();
                String baseUrl = openAiService.baseUrl();
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
        long nowMs = System.currentTimeMillis();
        String title = "brief";
        String info = conversation.getDefaultModel();
        if (historyViewport.scrollOffsetLines() > 0) {
            info = info + "  ↑" + historyViewport.scrollOffsetLines() + "L";
        }
        String status = mouseSelection.transientStatus(nowMs);
        if (status != null) {
            info = info + "  " + status;
        }

        String configError = config.transientError(nowMs);
        if (configError == null) {
            return TuiTheme.headerWithInfo(title, info, width);
        }

        Style titleStyle = Style.newStyle().foreground(TuiTheme.PRIMARY).bold(true);
        Style infoStyle = Style.newStyle().foreground(TuiTheme.MUTED);
        Style spacerStyle = Style.newStyle().foreground(TuiTheme.MUTED).faint(true);

        String titleRendered = titleStyle.render(title);
        String infoRendered = infoStyle.render(info);

        int usedWidth = TuiTheme.visualWidth(title) + TuiTheme.visualWidth(info) + 2;
        int middleWidth = Math.max(1, width - usedWidth);

        String clipped = TuiTheme.truncate(configError, middleWidth);
        String centered = TuiTheme.padRight(TuiTheme.center(clipped, middleWidth), middleWidth);
        String middleRendered = TuiTheme.warning().render(centered);

        return titleRendered + spacerStyle.render(" ") + middleRendered + spacerStyle.render(" ") + infoRendered;
    }

    private static String joinLeftRight(String left, String right, int width) {
        if (width <= 0) return "";
        if (left == null) left = "";
        if (right == null) right = "";

        int lw = TuiTheme.visualWidth(left);
        int rw = TuiTheme.visualWidth(right);
        int gap = width - lw - rw;

        if (gap >= 0) {
            return left + " ".repeat(gap) + right;
        }

        if (lw >= width) {
            return TuiTheme.truncate(left, width);
        }

        int remaining = Math.max(0, width - lw - 1);
        if (remaining == 0) return TuiTheme.truncate(left, width);
        return left + " " + TuiTheme.truncate(right, remaining);
    }

    private HistoryRender renderHistory(int wrapWidth, int maxLines) {
        String allHistory = renderAllHistory(wrapWidth);
        String[] allLines = allHistory.isEmpty() ? new String[0] : allHistory.split("\n", -1);

        if (allLines.length == 0) {
            List<String> empty = new ArrayList<>();
            Style emptyStyle = TuiTheme.hint();
            String emptyMsg = emptyStyle.render("Start a conversation...");
            int pad = Math.max(0, maxLines / 2);
            for (int i = 0; i < pad; i++) empty.add("");
            empty.add(TuiTheme.center(emptyMsg, wrapWidth));
            while (empty.size() < maxLines) empty.add("");
            List<String> visiblePlain = empty.stream()
                .map(TuiTheme::stripAnsi)
                .map(ChatConversationScreen::rstrip)
                .toList();
            return new HistoryRender(empty, visiblePlain, List.of(), 0);
        }

        HistoryViewport.Window window = historyViewport.window(allLines.length, maxLines);

        List<String> visibleStyled = new ArrayList<>();
        for (int i = window.startInclusive(); i < window.endExclusive(); i++) {
            visibleStyled.add(TuiTheme.padRight(allLines[i], wrapWidth));
        }
        while (visibleStyled.size() < maxLines) {
            visibleStyled.add(" ".repeat(wrapWidth));
        }

        List<String> visiblePlain = visibleStyled.stream()
            .map(TuiTheme::stripAnsi)
            .map(ChatConversationScreen::rstrip)
            .toList();
        List<String> allPlain = Arrays.stream(allLines)
            .map(TuiTheme::stripAnsi)
            .map(ChatConversationScreen::rstrip)
            .toList();

        return new HistoryRender(visibleStyled, visiblePlain, allPlain, window.startInclusive());
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
        if (m.role() == Role.ASSISTANT && content.isBlank() && m.toolCalls() != null && !m.toolCalls().isEmpty()) {
            String toolName = m.toolCalls().getFirst().name();
            String banner = ToolCallBanner.render(toolName, wrapWidth);
            for (String line : banner.split("\n", -1)) {
                out.append(line).append("\n");
            }
        } else {
            String wrapped = new TextWrapper().wrap(content, Math.max(10, wrapWidth));
            for (String line : wrapped.split("\n", -1)) {
                out.append(line).append("\n");
            }
        }
        out.append("\n");

        return out.toString();
    }

    private void append(Role role, ChatMessage.Source source, String content) {
        int index = conversation.getMessages().size();
        conversation.addMessage(new ChatMessage(
            "m_%04d_%s".formatted(index + 1, UUID.randomUUID().toString().substring(0, 8)),
            conversation.getId(),
            index,
            role,
            source,
            content == null ? "" : content,
            OffsetDateTime.now(ZoneOffset.UTC),
            conversation.getDefaultModel(),
            conversation.getProvider().name().toLowerCase(),
            null,
            null,
            null,
            null,
            null
        ));
    }

    private Command maybePrintToScrollback(String label, String content) {
        if (!printToScrollback) return null;
        String safeLabel = (label == null || label.isBlank()) ? "" : label.trim();
        String safeContent = content == null ? "" : content;
        String line = safeLabel.isEmpty() ? safeContent : (safeLabel + ": " + safeContent);
        return Command.println(line);
    }

    private static String highlightSelection(String plain, int width, int startCol, int endCol) {
        if (plain == null || plain.isEmpty()) return TuiTheme.padRight("", width);

        int n = plain.length();
        int s = Math.max(0, Math.min(startCol, n));
        int e = Math.max(0, Math.min(endCol, n));

        int first = Math.min(s, e);
        int last = Math.max(s, e);

        String prefix = plain.substring(0, first);
        String selected = plain.substring(first, last);
        String suffix = plain.substring(last);

        return TuiTheme.padRight(prefix + "\u001b[7m" + selected + "\u001b[0m" + suffix, width);
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
