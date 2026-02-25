package com.williamcallahan.chatclient.ui;

import com.williamcallahan.chatclient.ui.maps.PlacesOverlay;
import com.williamcallahan.tui4j.compat.bubbles.textarea.Textarea;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.PasteMessage;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.Key;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.input.MouseTarget;
import com.williamcallahan.tui4j.input.MouseTargetProvider;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

/**
 * Wraps a model and reroutes paste into key runes for single-line overlay inputs
 * that do not currently handle {@link PasteMessage}.
 */
final class PasteRoutingModel implements Model, MouseTargetProvider {

    @FunctionalInterface
    interface PasteRoutingStrategy {
        boolean shouldRoutePasteAsRunes(Model model);
    }

    private static final Field CHAT_COMPOSER_FIELD =
        resolveField(ChatConversationScreen.class, "composer");
    private static final PasteRoutingStrategy CHAT_PLACES_INPUT_STRATEGY =
        new ChatPlacesInputStrategy();

    private Model delegate;
    private final PasteRoutingStrategy pasteRoutingStrategy;

    PasteRoutingModel(Model delegate) {
        this(delegate, CHAT_PLACES_INPUT_STRATEGY);
    }

    PasteRoutingModel(Model delegate, PasteRoutingStrategy pasteRoutingStrategy) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.pasteRoutingStrategy = Objects.requireNonNull(
            pasteRoutingStrategy,
            "pasteRoutingStrategy"
        );
        applyDelegateStyleOverrides(this.delegate);
    }

    @Override
    public Command init() {
        return delegate.init();
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        if (
            msg instanceof PasteMessage paste &&
            shouldRoutePasteAsRunes(paste.content())
        ) {
            UpdateResult<? extends Model> routed = routePasteAsRunes(
                paste.content()
            );
            delegate = routed.model();
            applyDelegateStyleOverrides(delegate);
            return UpdateResult.from(this, routed.command());
        }

        UpdateResult<? extends Model> result = delegate.update(msg);
        delegate = result.model();
        applyDelegateStyleOverrides(delegate);
        return UpdateResult.from(this, result.command());
    }

    @Override
    public String view() {
        return delegate.view();
    }

    @Override
    public List<MouseTarget> mouseTargets() {
        if (delegate instanceof MouseTargetProvider provider) {
            return provider.mouseTargets();
        }
        return List.of();
    }

    private boolean shouldRoutePasteAsRunes(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        return pasteRoutingStrategy.shouldRoutePasteAsRunes(delegate);
    }

    private UpdateResult<? extends Model> routePasteAsRunes(String content) {
        String normalized = normalizeForSingleLineInput(content);
        if (normalized.isEmpty()) {
            return UpdateResult.from(delegate);
        }

        KeyPressMessage pasteAsRunes = new KeyPressMessage(
            new Key(KeyType.KeyRunes, normalized.toCharArray())
        );
        return delegate.update(pasteAsRunes);
    }

    static String normalizeForSingleLineInput(String content) {
        StringBuilder normalized = new StringBuilder(content.length());
        boolean lastInsertedSpace = false;
        for (char ch : content.toCharArray()) {
            if (ch == '\r' || ch == '\n' || ch == '\t' || ch < 32) {
                if (!lastInsertedSpace) {
                    normalized.append(' ');
                    lastInsertedSpace = true;
                }
                continue;
            }
            normalized.append(ch);
            lastInsertedSpace = (ch == ' ');
        }
        return normalized.toString().trim();
    }

    static void applyComposerStyles(Textarea composer) {
        if (composer == null) {
            return;
        }
        applyTextareaStyle(composer.focusedStyle());
        applyTextareaStyle(composer.blurredStyle());
        applyTextareaStyle(composer.style());
    }

    private void applyDelegateStyleOverrides(Model model) {
        if (
            !(model instanceof ChatConversationScreen) ||
            CHAT_COMPOSER_FIELD == null
        ) {
            return;
        }
        try {
            Textarea composer = (Textarea) CHAT_COMPOSER_FIELD.get(model);
            applyComposerStyles(composer);
        } catch (IllegalAccessException | ClassCastException ignored) {}
    }

    private static void applyTextareaStyle(Textarea.Style style) {
        style
            .base(Style.newStyle())
            .prompt(TuiTheme.inputPrompt())
            .text(Style.newStyle().foreground(TuiTheme.PRIMARY))
            .cursorLine(Style.newStyle().foreground(TuiTheme.PRIMARY))
            .placeholder(TuiTheme.hint());
    }

    private static Field resolveField(Class<?> type, String fieldName) {
        try {
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    private static final class ChatPlacesInputStrategy
        implements PasteRoutingStrategy
    {

        private static final Field PLACES_OVERLAY_FIELD =
            resolveField(ChatConversationScreen.class, "placesOverlay");

        @Override
        public boolean shouldRoutePasteAsRunes(Model model) {
            if (!(model instanceof ChatConversationScreen)) {
                return false;
            }
            if (PLACES_OVERLAY_FIELD == null) {
                return false;
            }
            try {
                PlacesOverlay overlay = (PlacesOverlay) PLACES_OVERLAY_FIELD.get(
                    model
                );
                return (
                    overlay != null && overlay.isOpen() && overlay.isInputMode()
                );
            } catch (IllegalAccessException | ClassCastException e) {
                return false;
            }
        }

    }
}
