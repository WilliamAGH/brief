package com.williamcallahan.chatclient.ui;

import com.williamcallahan.chatclient.Config;
import com.williamcallahan.chatclient.domain.ChatMessage;
import com.williamcallahan.chatclient.domain.Conversation;
import com.williamcallahan.chatclient.domain.Role;
import com.williamcallahan.chatclient.ui.slash.ModelSlashCommand;
import com.williamcallahan.chatclient.ui.slash.SlashCommand;
import com.williamcallahan.chatclient.ui.slash.SlashCommands;
import com.williamcallahan.chatclient.ui.slash.WeatherSlashCommand;
import com.williamcallahan.chatclient.service.ChatCompletionService;
import com.williamcallahan.chatclient.service.OpenAiService;
import com.williamcallahan.chatclient.service.SummaryService;
import com.williamcallahan.chatclient.service.ToolExecutor;
import com.williamcallahan.chatclient.service.tools.WeatherForecastTool;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.ansi.TextWrapper;
import com.williamcallahan.tui4j.compat.lipgloss.border.StandardBorder;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyAliases.KeyAlias;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.bubbletea.PasteMessage;
import com.williamcallahan.tui4j.compat.bubbletea.QuitMessage;
import com.williamcallahan.tui4j.compat.bubbletea.WindowSizeMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseButton;
import com.williamcallahan.tui4j.input.MouseClickMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage;
import com.williamcallahan.tui4j.input.MouseTarget;
import com.williamcallahan.tui4j.input.MouseTargetProvider;
import com.williamcallahan.tui4j.compat.bubbles.spinner.Spinner;
import com.williamcallahan.tui4j.compat.bubbles.spinner.TickMessage;
import com.williamcallahan.tui4j.compat.bubbles.spinner.SpinnerType;
import com.williamcallahan.tui4j.compat.bubbles.textarea.Textarea;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.williamcallahan.tui4j.compat.bubbletea.Command.batch;
import static com.williamcallahan.tui4j.compat.bubbletea.Command.setWindowTitle;

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
    private final SummaryService summaryService;
    private final List<SlashCommand> slashCommands = SlashCommands.defaults();
    private final Textarea composer = new Textarea();
    private final SlashCommandPalette slashPalette = new SlashCommandPalette();
    private final ModelPalette modelPalette = new ModelPalette();
    private final boolean printToScrollback;
    private final boolean mouseSelectionEnabled;
    private final boolean showToolMessages;
    private boolean needsModelSelection;

    private int width = 80;
    private int height = 24;
    private static final int MAX_COMPOSER_LINES = 6;

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

    // Paste handling state
    private record PastedContent(int index, String displayText, String actualText) {}
    private final List<PastedContent> pastedContents = new ArrayList<>();
    private int pasteCounter = 0;

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
        this.summaryService = new SummaryService(chatCompletionService, config);

        // Configure Textarea for multi-line input
        composer.setPrompt("> ");
        composer.setPlaceholder("Ask me anything...");
        composer.setShowLineNumbers(false);
        composer.setEndOfBufferCharacter(' ');
        composer.setMaxHeight(MAX_COMPOSER_LINES);
        composer.setWidth(Math.max(20, this.width - 8));
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
            setWindowTitle("brief - " + conversation.getDefaultModel()),
            Command.checkWindowSize(),
            Command.setMouseCursorText()
        );
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (msg instanceof WindowSizeMessage w) {
            width = Math.max(40, w.width());
            height = Math.max(12, w.height());
            composer.setWidth(Math.max(20, width - 8));

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

        // Handle pasted content - NEVER triggers submit, preserves line breaks
        if (msg instanceof PasteMessage paste) {
            return handlePaste(paste.content());
        }

        if (ComposerNewlineDecider.shouldInsertNewline(msg)) {
            return insertComposerNewline();
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
                // Plain Enter submits
                String text = resolveSubmitText();
                if (text.isEmpty() || waiting) return UpdateResult.from(this);

                long now = System.currentTimeMillis();
                if (text.equals(lastEnterText) && (now - lastEnterAtMs) < 1000) {
                    composer.reset();
                    clearPasteState();
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

    /**
     * Insert a newline by going through the textarea's normal key binding path.
     *
     * This is important because the textarea's "insert rune/string" path sanitizes user input,
     * and in tui4j's current port the sanitizer collapses newlines to spaces. Upstream Bubbles
     * inserts newlines via the InsertNewline key binding, which splits the current line.
     */
    private UpdateResult<? extends Model> insertComposerNewline() {
        KeyType enterType = KeyAliases.getKeyType(KeyAlias.KeyEnter);
        UpdateResult<Textarea> r = composer.update(new KeyPressMessage(new Key(enterType)));
        return UpdateResult.from(this, r.command());
    }

    /**
     * Handles pasted content by creating a placeholder and storing the actual text.
     * Line breaks are preserved; paste NEVER triggers submit.
     */
    private UpdateResult<? extends Model> handlePaste(String content) {
        if (content == null || content.isEmpty()) {
            return UpdateResult.from(this);
        }

        pasteCounter++;
        SummaryService.PasteSummary summary = summaryService.processPaste(content, pasteCounter);

        // Store the mapping from placeholder to actual content
        pastedContents.add(new PastedContent(pasteCounter, summary.displayText(), summary.actualText()));

        // Insert the placeholder (or content if short) into the composer
        composer.insertString(summary.displayText() + " ");

        return UpdateResult.from(this);
    }

    /**
     * Resolves the submit text by replacing paste placeholders with actual content.
     */
    private String resolveSubmitText() {
        String display = composer.value();

        // Replace all placeholders with actual content
        for (PastedContent pc : pastedContents) {
            display = display.replace(pc.displayText(), pc.actualText());
        }

        clearPasteState();
        return display.trim();
    }

    /**
     * Clears paste state after submission or reset.
     */
    private void clearPasteState() {
        pastedContents.clear();
        pasteCounter = 0;
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

        // Use only left, right, and bottom borders - we'll render top border manually with title
        Style frame = Style.newStyle()
            .border(StandardBorder.NormalBorder, false, true, true, true)
            .borderForeground(TuiTheme.BORDER)
            .padding(0, 1, 0, 1);

        int frameWidth = Math.max(1, viewportWidth - frame.getHorizontalBorderSize() - frame.getHorizontalMargins());
        // Account for the custom top border we'll add (1 line)
        int frameHeight = Math.max(1, viewportHeight - frame.getVerticalBorderSize() - frame.getVerticalMargins() - 1);
        frame.width(frameWidth).height(frameHeight);

        int innerWidth = Math.max(20, viewportWidth - frame.getHorizontalFrameSize());
        // Subtract 1 for our custom top border line
        int innerHeight = Math.max(4, viewportHeight - frame.getVerticalFrameSize() - 1);

        innerLeft = frame.getBorderLeftSize() + frame.leftPadding();
        innerTop = 1; // Top border is now line 0, content starts at line 1

        List<String> composerLines = renderComposer(innerWidth);
        if (composerLines.isEmpty()) composerLines = List.of(renderComposerLine("", 0, innerWidth, 0));

        String statusLeft = "";
        if (waiting) {
            Style spinnerStyle = TuiTheme.spinner();
            Style textStyle = TuiTheme.hint();
            statusLeft = spinnerStyle.render(spinner.view()) + " " + textStyle.render("thinking...");
        }
        String rightHints = TuiTheme.shortcutRow(
            TuiTheme.shortcutHint("newline", "Ctrl+J"),
            TuiTheme.shortcutHint("/", "commands"),
            TuiTheme.shortcutHint("esc", "quit")
        );
        String statusRow = joinLeftRight(statusLeft, rightHints, innerWidth);

        int dividerHeight = 1;
        int composerHeight = composerLines.size();
        int statusHeight = 1;
        int fixedHeight = dividerHeight + composerHeight + statusHeight;
        int historyHeight = Math.max(1, innerHeight - fixedHeight);

        List<String> lines = new ArrayList<>();

        HistoryRender historyRender = renderHistory(innerWidth, historyHeight);
        List<String> visibleHistory = new ArrayList<>(historyRender.visibleStyled());
        int historyStartRow = 1; // After custom top border
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
        String framedContent = frame.render(content);

        // Render custom top border with title embedded
        String topBorder = renderTitleBorder(innerWidth + 2); // +2 for left/right padding
        return topBorder + "\n" + framedContent;
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
                        setWindowTitle("brief - " + next.getDefaultModel()),
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

    private String renderTitleBorder(int width) {
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

        Style borderStyle = Style.newStyle().foreground(TuiTheme.BORDER);
        Style titleStyle = Style.newStyle().foreground(TuiTheme.PRIMARY).bold(true);
        Style infoStyle = Style.newStyle().foreground(TuiTheme.MUTED);

        String configError = config.transientError(nowMs);

        // Border characters
        String topLeft = borderStyle.render("┌");
        String topRight = borderStyle.render("┐");
        String horizontal = "─";

        // Calculate available space for content (width minus corners)
        int contentWidth = width - 2;

        // Render title and info
        String titleRendered = titleStyle.render(title);
        String infoRendered = infoStyle.render(info);

        int titleVisualWidth = TuiTheme.visualWidth(title);
        int infoVisualWidth = TuiTheme.visualWidth(info);

        if (configError == null) {
            // No error: ┌─ brief ──────────────────── model-name ─┐
            // Need space for: " brief " on left, " model-name " on right, line chars between
            int usedWidth = titleVisualWidth + infoVisualWidth + 4; // 4 for spaces around title and info
            int lineWidth = Math.max(0, contentWidth - usedWidth);

            String line = borderStyle.render(horizontal.repeat(lineWidth));
            return topLeft
                + borderStyle.render(horizontal + " ")
                + titleRendered
                + borderStyle.render(" ")
                + line
                + borderStyle.render(" ")
                + infoRendered
                + borderStyle.render(" " + horizontal)
                + topRight;
        }

        // With error: ┌─ brief ─ error message ─ model-name ─┐
        int usedWidth = titleVisualWidth + infoVisualWidth + 6; // spaces around all three elements
        int middleWidth = Math.max(1, contentWidth - usedWidth);

        String clipped = TuiTheme.truncate(configError, middleWidth);
        int clippedWidth = TuiTheme.visualWidth(clipped);
        String errorRendered = TuiTheme.warning().render(clipped);

        // Distribute remaining line chars between sections
        int remainingLine = contentWidth - titleVisualWidth - infoVisualWidth - clippedWidth - 6;
        int leftLine = Math.max(1, remainingLine / 2);
        int rightLine = Math.max(1, remainingLine - leftLine);

        return topLeft
            + borderStyle.render(horizontal + " ")
            + titleRendered
            + borderStyle.render(" " + horizontal.repeat(leftLine) + " ")
            + errorRendered
            + borderStyle.render(" " + horizontal.repeat(rightLine) + " ")
            + infoRendered
            + borderStyle.render(" " + horizontal)
            + topRight;
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

        String content = sanitizeForDisplay((m.content() == null) ? "" : m.content());
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

    /**
     * Sanitizes text for terminal display.
     *
     * Pasted content frequently includes ANSI escape sequences (colors/cursor moves) that will
     * corrupt the TUI layout if rendered directly. We strip common ANSI sequences and normalize
     * newlines for predictable wrapping.
     */
    private static String sanitizeForDisplay(String s) {
        if (s == null || s.isEmpty()) return "";
        String out = s.replace("\r\n", "\n").replace('\r', '\n');
        // Tabs render with terminal-dependent width; normalize to spaces so wrapping stays correct.
        out = out.replace("\t", "    ");
        // ANSI CSI sequences (most common).
        out = out.replaceAll("\u001B\\[[0-?]*[ -/]*[@-~]", "");
        // ANSI OSC sequences (window title, hyperlinks, etc.).
        out = out.replaceAll("\u001B\\][^\u0007]*(\u0007|\u001B\\\\)", "");
        // Strip other control characters that can corrupt terminal layout (keep newlines).
        out = out.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        return out;
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

    /**
     * Renders a clean input panel without repeated prompts.
     * Shows "› " on the first line only, continuation lines are indented.
     * Cursor is shown as reverse-video block on the current position.
     */
    private List<String> renderComposer(int width) {
        String value = composer.value();
        if (value == null) value = "";

        // Show placeholder if empty
        if (value.isEmpty()) {
            return List.of(renderPlaceholderLine(width));
        }

        // Split into lines (user creates lines with Shift+Enter)
        String[] lines = value.split("\n", -1);

        // Get cursor position
        int cursorRow = composer.line();
        int cursorCol = composer.lineInfo().columnOffset();

        List<String> result = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            boolean hasCursor = (i == cursorRow);
            int cursorPos = hasCursor ? cursorCol : -1;
            result.add(renderComposerLine(lines[i], i, width, cursorPos));
        }

        return result;
    }

    /**
     * Renders a single composer line with prompt on first line only.
     * If cursorPos >= 0, renders cursor at that position.
     */
    private String renderComposerLine(String text, int lineIndex, int width, int cursorPos) {
        Style promptStyle = TuiTheme.inputPrompt();
        Style textStyle = Style.newStyle().foreground(TuiTheme.PRIMARY);

        String prefix;
        if (lineIndex == 0) {
            prefix = promptStyle.render("› ");
        } else {
            prefix = "  "; // Indent continuation lines to align with first line
        }

        // Calculate available width for text (subtract prefix width)
        int prefixWidth = 2;
        int textWidth = width - prefixWidth;

        // Build the text with cursor
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);

        if (cursorPos < 0) {
            // No cursor on this line
            String displayText = text;
            if (TuiTheme.visualWidth(displayText) > textWidth) {
                displayText = TuiTheme.truncate(displayText, textWidth);
            }
            sb.append(textStyle.render(displayText));
        } else {
            // Render with cursor
            int pos = Math.min(cursorPos, text.length());

            // Text before cursor
            if (pos > 0) {
                sb.append(textStyle.render(text.substring(0, pos)));
            }

            // Cursor character (reverse video)
            String cursorChar = (pos < text.length()) ? String.valueOf(text.charAt(pos)) : " ";
            sb.append("\u001b[7m").append(cursorChar).append("\u001b[0m");

            // Text after cursor
            if (pos + 1 < text.length()) {
                sb.append(textStyle.render(text.substring(pos + 1)));
            }
        }

        return TuiTheme.padRight(sb.toString(), width);
    }

    /**
     * Renders the placeholder line when composer is empty.
     */
    private String renderPlaceholderLine(int width) {
        Style promptStyle = TuiTheme.inputPrompt();
        Style placeholderStyle = TuiTheme.hint();

        String prefix = promptStyle.render("› ");
        // Show cursor at start, then placeholder
        String cursor = "\u001b[7m \u001b[0m";
        String placeholder = placeholderStyle.render("Ask me anything...");

        return TuiTheme.padRight(prefix + cursor + placeholder, width);
    }
}
