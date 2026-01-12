package com.wimf.gui;

import com.wimf.FriendManager;
import com.wimf.FriendProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class FriendNoteScreen extends Screen {
    private final Screen parent;
    private final String nickname;

    private NoteListWidget listWidget;
    private TextFieldWidget newNoteInput;
    private ButtonWidget deleteSelectedButton;

    private final Set<Integer> selectedNotes = new HashSet<>();
    private static final int MAX_NOTE_LENGTH = 100;

    public FriendNoteScreen(Screen parent, String nickname) {
        super(Text.translatable("wimf.gui.notes.title", nickname));
        this.parent = parent;
        this.nickname = nickname;
    }

    @Override
    protected void init() {
        // 1. Кнопка "Вернуться к списку" (Левый ВЕРХНИЙ угол)
        // Ширина побольше, чтобы влез текст
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("wimf.gui.notes.button.back_to_list"), button -> {
                    if (this.client != null) {
                        this.client.setScreen(this.parent);
                    }
                })
                .dimensions(10, 10, 150, 20)
                .build());

        // 2. Список заметок
        this.listWidget = new NoteListWidget(this.client, this.nickname);
        this.addDrawableChild(this.listWidget);

        // 3. Поле ввода новой заметки
        int inputWidth = 200;
        this.newNoteInput = new TextFieldWidget(this.textRenderer,
                (this.width - inputWidth) / 2, this.height - 30,
                inputWidth, 20, Text.literal(""));
        this.newNoteInput.setMaxLength(MAX_NOTE_LENGTH);
        this.newNoteInput.setPlaceholder(Text.translatable("wimf.gui.notes.input_placeholder"));
        this.addDrawableChild(this.newNoteInput);

        // 4. Кнопка "Добавить"
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("wimf.gui.notes.button.add"), button -> {
                    String text = this.newNoteInput.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        FriendManager.getInstance().addNote(this.nickname, text);
                        this.newNoteInput.setText("");
                        this.refreshList();
                    }
                })
                .dimensions((this.width - inputWidth) / 2 + inputWidth + 5, this.height - 30, 60, 20)
                .build());

        // 5. Кнопка "Удалить выбранные" (Левый НИЖНИЙ угол)
        this.deleteSelectedButton = ButtonWidget.builder(Text.translatable("wimf.gui.notes.button.delete_selected", 0), button -> {
                    deleteSelectedNotes();
                })
                .dimensions(10, this.height - 30, 100, 20)
                .build();

        // Скрываем кнопку при инициализации (так как ничего не выбрано)
        this.deleteSelectedButton.visible = false;
        this.addDrawableChild(this.deleteSelectedButton);

        this.refreshList();
    }

    private void refreshList() {
        this.selectedNotes.clear();
        this.updateDeleteButton();
        this.listWidget.refreshNotes();
    }

    private void deleteSelectedNotes() {
        if (selectedNotes.isEmpty()) return;
        List<Integer> sortedIndices = new ArrayList<>(selectedNotes);
        sortedIndices.sort(Collections.reverseOrder());
        for (int index : sortedIndices) {
            FriendManager.getInstance().removeNote(this.nickname, index);
        }
        refreshList();
    }

    public void toggleSelection(int index, boolean isSelected) {
        if (isSelected) selectedNotes.add(index);
        else selectedNotes.remove(index);
        updateDeleteButton();
    }

    private void updateDeleteButton() {
        if (selectedNotes.isEmpty()) {
            // Если пусто - скрываем кнопку
            this.deleteSelectedButton.visible = false;
        } else {
            // Если есть выбор - показываем и обновляем текст
            this.deleteSelectedButton.visible = true;
            this.deleteSelectedButton.setMessage(Text.translatable("wimf.gui.notes.button.delete_selected", selectedNotes.size()));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0000000);
        super.render(context, mouseX, mouseY, delta);
        // Заголовок чуть ниже, так как сверху кнопка "Назад"
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFFFF);
        Optional<FriendProfile> profile = FriendManager.getInstance().getFriend(this.nickname);
        if (profile.isPresent()) {
            Text lastSeen = getLastSeenText(profile.get().getLastSeenTimestamp());

            context.drawCenteredTextWithShadow(this.textRenderer, lastSeen, this.width / 2, 22, 0xFFFFFFFF);
        }
    }

    // ====================================================================================
    // СПИСОК
    // ====================================================================================

    class NoteListWidget extends ElementListWidget<NoteListWidget.NoteEntry> {
        private final String nickname;

        public NoteListWidget(MinecraftClient client, String nickname) {
            // Увеличили отступ сверху (40), чтобы не наезжать на кнопку "Назад"
            super(client, FriendNoteScreen.this.width, FriendNoteScreen.this.height - 70, 40, 36);
            this.nickname = nickname;
        }

        public void refreshNotes() {
            this.clearEntries();
            Optional<FriendProfile> profile = FriendManager.getInstance().getFriend(this.nickname);
            if (profile.isPresent()) {
                List<String> notes = profile.get().getNotes();
                for (int i = 0; i < notes.size(); i++) {
                    this.addEntry(new NoteEntry(i, notes.get(i)));
                }
            }
        }

        public class NoteEntry extends ElementListWidget.Entry<NoteEntry> {
            private final int index;
            private String text;

            private final CheckboxWidget checkbox;
            private final ButtonWidget editBtn;
            private final ButtonWidget saveBtn;
            private final ButtonWidget deleteBtn;

            private final TextFieldWidget inlineEditor;
            private boolean isEditing = false;

            public NoteEntry(int index, String text) {
                this.index = index;
                this.text = text;

                // Чекбокс
                this.checkbox = CheckboxWidget.builder(Text.empty(), client.textRenderer)
                        .pos(0, 0)
                        .callback((cb, checked) -> FriendNoteScreen.this.toggleSelection(index, checked))
                        .build();
                // По умолчанию скрыт, пока не наведешь мышь (логика в render)
                this.checkbox.visible = false;

                this.editBtn = ButtonWidget.builder(Text.literal("✎"), btn -> startInlineEditing())
                        .dimensions(0, 0, 20, 20)
                        .tooltip(Tooltip.of(Text.translatable("wimf.gui.notes.tooltip.edit")))
                        .build();

                this.saveBtn = ButtonWidget.builder(Text.literal("✔").formatted(Formatting.GREEN), btn -> saveInlineEditing())
                        .dimensions(0, 0, 20, 20)
                        .tooltip(Tooltip.of(Text.translatable("wimf.gui.notes.button.save")))
                        .build();
                this.saveBtn.visible = false;

                this.deleteBtn = ButtonWidget.builder(Text.literal("x").formatted(Formatting.RED), btn -> {
                            FriendManager.getInstance().removeNote(nickname, index);
                            FriendNoteScreen.this.refreshList();
                        })
                        .dimensions(0, 0, 20, 20)
                        .tooltip(Tooltip.of(Text.translatable("wimf.gui.notes.tooltip.delete")))
                        .build();

                this.inlineEditor = new TextFieldWidget(client.textRenderer, 0, 0, 100, 18, Text.empty());
                this.inlineEditor.setMaxLength(MAX_NOTE_LENGTH);
                this.inlineEditor.setText(text);
                this.inlineEditor.visible = false;
            }

            private void startInlineEditing() {
                this.isEditing = true;
                this.inlineEditor.visible = true;
                this.inlineEditor.setFocused(true);
                this.inlineEditor.setSelectionStart(0);
                this.editBtn.visible = false;
                this.saveBtn.visible = true;
                // При редактировании чекбокс лучше скрыть или оставить,
                // но пока оставим как есть (он будет виден при наведении)
            }

            private void saveInlineEditing() {
                String newText = this.inlineEditor.getText();
                if (newText != null && !newText.trim().isEmpty()) {
                    FriendManager.getInstance().editNote(nickname, index, newText);
                    this.text = newText;
                }
                this.isEditing = false;
                this.inlineEditor.visible = false;
                this.editBtn.visible = true;
                this.saveBtn.visible = false;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int checkboxX = x + 2;
                int checkboxY = y + (entryHeight - 20) / 2;

                // --- ЛОГИКА ВИДИМОСТИ ЧЕКБОКСА ---
                // Виден, если: навели мышь ИЛИ он уже нажат
                this.checkbox.visible = hovered || this.checkbox.isChecked();

                this.checkbox.setX(checkboxX);
                this.checkbox.setY(checkboxY);
                this.checkbox.render(context, mouseX, mouseY, tickDelta);

                int textStartX = checkboxX + 24;
                int buttonsWidth = 45;
                int maxTextWidth = entryWidth - 24 - buttonsWidth - 5;

                if (this.isEditing) {
                    this.inlineEditor.setX(textStartX);
                    this.inlineEditor.setY(y + (entryHeight - 18) / 2);
                    this.inlineEditor.setWidth(maxTextWidth);
                    this.inlineEditor.render(context, mouseX, mouseY, tickDelta);

                    this.saveBtn.setX(x + entryWidth - 25);
                    this.saveBtn.setY(y + (entryHeight - 20) / 2);
                    this.saveBtn.render(context, mouseX, mouseY, tickDelta);

                } else {
                    List<OrderedText> lines = client.textRenderer.wrapLines(Text.literal(this.text), maxTextWidth);
                    int limit = Math.min(lines.size(), 3);
                    for (int i = 0; i < limit; i++) {
                        context.drawText(client.textRenderer, lines.get(i), textStartX, y + 8 + (i * 9), 0xFFFFFFFF, true);
                    }

                    if (hovered) {
                        this.editBtn.setX(x + entryWidth - 48);
                        this.editBtn.setY(y + (entryHeight - 20) / 2);
                        this.editBtn.render(context, mouseX, mouseY, tickDelta);

                        this.deleteBtn.setX(x + entryWidth - 24);
                        this.deleteBtn.setY(y + (entryHeight - 20) / 2);
                        this.deleteBtn.render(context, mouseX, mouseY, tickDelta);
                    }
                }
            }

            @Override
            public List<? extends Element> children() {
                if (isEditing) return List.of(this.inlineEditor, this.saveBtn, this.checkbox);
                return List.of(this.editBtn, this.deleteBtn, this.checkbox);
            }

            @Override
            public List<? extends Selectable> selectableChildren() {
                if (isEditing) return List.of(this.inlineEditor, this.saveBtn, this.checkbox);
                return List.of(this.editBtn, this.deleteBtn, this.checkbox);
            }

        }
    }
    private Text getLastSeenText(long timestamp) {
        if (timestamp == 0) {
            return Text.translatable("wimf.status.never_seen").formatted(Formatting.GRAY);
        }

        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (diff < 60 * 1000) {
            return Text.translatable("wimf.status.online_now").formatted(Formatting.GREEN);
        }
        if (minutes < 60) {
            return Text.translatable("wimf.status.seen_minutes", minutes).formatted(Formatting.GRAY);
        }
        if (hours < 24) {
            return Text.translatable("wimf.status.seen_hours", hours).formatted(Formatting.GRAY);
        }
        return Text.translatable("wimf.status.seen_days", days).formatted(Formatting.GRAY);
    }
}