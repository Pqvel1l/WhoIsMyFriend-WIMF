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

import java.text.SimpleDateFormat;
import java.util.*;

public class FriendNoteScreen extends Screen {
    private final Screen parent;
    private final String nickname;

    private NoteListWidget listWidget;
    private TextFieldWidget newNoteInput;
    private ButtonWidget layoutButton;

    private static int columns = 3; // По умолчанию 3, но карточки всегда фиксированного размера
    private static final int MAX_NOTE_LENGTH = 100;

    public FriendNoteScreen(Screen parent, String nickname) {
        super(Text.translatable("wimf.gui.notes.title", nickname));
        this.parent = parent;
        this.nickname = nickname;
    }

    @Override
    protected void init() {
        this.listWidget = new NoteListWidget(this.client, this.nickname);
        this.addDrawableChild(this.listWidget);

        // --- УВЕЛИЧЕННОЕ ПОЛЕ ВВОДА ---
        int inputWidth = 260; // Шире
        int inputHeight = 24; // Выше
        this.newNoteInput = new TextFieldWidget(this.textRenderer,
                (this.width - inputWidth) / 2, this.height - 35,
                inputWidth, inputHeight, Text.literal(""));
        this.newNoteInput.setMaxLength(MAX_NOTE_LENGTH);
        this.newNoteInput.setPlaceholder(Text.translatable("wimf.gui.notes.input_placeholder"));
        this.addDrawableChild(this.newNoteInput);

        // Кнопка Add (Тоже больше)
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("wimf.gui.notes.button.add"), button -> {
                    String text = this.newNoteInput.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        FriendManager.getInstance().addNote(this.nickname, text);
                        this.newNoteInput.setText("");
                        this.refreshList();
                    }
                })
                .dimensions((this.width - inputWidth) / 2 + inputWidth + 5, this.height - 35, 60, 24)
                .build());

        // Кнопка Back (Большая)
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("wimf.gui.notes.button.back_to_list"), button -> {
                    if (this.client != null) this.client.setScreen(this.parent);
                })
                .dimensions(10, 10, 160, 24)
                .build());

        // Кнопка Layout
        this.layoutButton = ButtonWidget.builder(Text.translatable("wimf.gui.notes.layout", columns), button -> {
                    columns++;
                    if (columns > 3) columns = 1;
                    // Обновляем текст кнопки при нажатии
                    button.setMessage(Text.translatable("wimf.gui.notes.layout", columns));
                    this.refreshList();
                })
                .dimensions(this.width - 80, 10, 70, 20)
                .tooltip(Tooltip.of(Text.translatable("wimf.gui.notes.tooltip.layout"))) // Тултип
                .build();
        this.addDrawableChild(this.layoutButton);
        this.refreshList();
    }

    private void refreshList() {
        this.listWidget.refreshNotes();
    }

    private String getFormattedDate(long timestamp) {
        if (timestamp == 0) return "Never";
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }

    private Text getStatusText(FriendProfile profile) {
        // 1. ПРОВЕРКА РЕАЛЬНОГО ОНЛАЙНА
        // Если игрок прямо сейчас есть на сервере — пишем ONLINE, игнорируя таймер
        if (isPlayerOnline(profile.getUuid())) {
            return Text.translatable("wimf.status.online_now").formatted(Formatting.GREEN);
        }

        // 2. Если не онлайн — считаем время
        long timestamp = profile.getLastSeenTimestamp();

        if (timestamp == 0) {
            return Text.translatable("wimf.status.never_seen").formatted(Formatting.GRAY);
        }

        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / 60000;
        long hours = minutes / 60;
        long days = hours / 24;

        // Если вышел меньше минуты назад
        if (minutes < 1) return Text.translatable("wimf.status.seen_minutes", "< 1").formatted(Formatting.GRAY);

        if (minutes < 60) return Text.translatable("wimf.status.seen_minutes", minutes).formatted(Formatting.GRAY);
        if (hours < 24) return Text.translatable("wimf.status.seen_hours", hours).formatted(Formatting.GRAY);
        return Text.translatable("wimf.status.seen_days", days).formatted(Formatting.GRAY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0000000);
        super.render(context, mouseX, mouseY, delta);

        // Заголовок (1.5x)
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float)(this.width / 2.0), 10.0f);
        context.getMatrices().scale(1.5f, 1.5f);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, 0, 0, 0xFFFFFFFF);
        context.getMatrices().popMatrix();

        // Last Seen
        FriendManager.getInstance().getFriend(this.nickname).ifPresent(profile -> {
            // Передаем весь профиль целиком
            Text status = getStatusText(profile);
            int centerX = this.width / 2;
            int statusY = 30;

            context.drawCenteredTextWithShadow(this.textRenderer, status, centerX, statusY, 0xFFFFFFFF);

            // Тултип
            int textWidth = this.textRenderer.getWidth(status);
            if (mouseY >= statusY && mouseY <= statusY + 10 && mouseX >= centerX - textWidth/2 && mouseX <= centerX + textWidth/2) {
                context.drawTooltip(this.textRenderer, Text.literal(getFormattedDate(profile.getLastSeenTimestamp())), mouseX, mouseY);
            }




        });
    }



    // ====================================================================================
    // СПИСОК
    // ====================================================================================

    class NoteListWidget extends ElementListWidget<NoteListWidget.NoteRowEntry> {
        private final String nickname;




        public NoteListWidget(MinecraftClient client, String nickname) {
            super(client, FriendNoteScreen.this.width, FriendNoteScreen.this.height - 80, 45, 60);
            this.nickname = nickname;
        }


        public void refreshNotes() {
            this.clearEntries();
            Optional<FriendProfile> profile = FriendManager.getInstance().getFriend(this.nickname);
            if (profile.isPresent()) {
                List<String> notes = profile.get().getNotes();
                for (int i = 0; i < notes.size(); i += columns) {
                    List<Integer> indices = new ArrayList<>();
                    List<String> texts = new ArrayList<>();
                    for (int j = 0; j < columns; j++) {
                        if (i + j < notes.size()) {
                            indices.add(i + j);
                            texts.add(notes.get(i + j));
                        }
                    }
                    this.addEntry(new NoteRowEntry(indices, texts));
                }
            }
        }

        public class NoteRowEntry extends ElementListWidget.Entry<NoteRowEntry> {
            private final List<SubNoteWidget> subWidgets = new ArrayList<>();

            public NoteRowEntry(List<Integer> indices, List<String> texts) {
                // --- ФИКСИРОВАННЫЙ РАЗМЕР КАРТОЧКИ ---
                // Мы берем ширину экрана, делим на 3 (максимально колонок) и отнимаем отступы.
                // Это будет эталонный размер.
                int screenW = FriendNoteScreen.this.width - 50;
                int cardWidth = (screenW - (5 * 2)) / 3; // Всегда размер как для 3 колонок
                int gap = 5;

                for (int k = 0; k < indices.size(); k++) {
                    // xOffset считаем относительно начала группы
                    this.subWidgets.add(new SubNoteWidget(indices.get(k), texts.get(k), k * (cardWidth + gap), cardWidth));
                }
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                // Центрируем группу карточек в строке
                int totalRowWidth = subWidgets.size() * subWidgets.get(0).width + (subWidgets.size() - 1) * 5;
                int startX = FriendNoteScreen.this.width / 2 - totalRowWidth / 2;

                for (SubNoteWidget widget : subWidgets) {
                    // ОБНОВЛЯЕМ РЕАЛЬНЫЕ КООРДИНАТЫ ДЛЯ ХИТБОКСА
                    widget.realX = startX + widget.xOffset;
                    widget.realY = y;
                    widget.render(context, widget.realX, widget.realY, mouseX, mouseY, tickDelta);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                for (SubNoteWidget widget : subWidgets) {
                    if (widget.mouseClicked(mouseX, mouseY, button)) return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                for (SubNoteWidget widget : subWidgets) if (widget.keyPressed(keyCode, scanCode, modifiers)) return true;
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            @Override
            public boolean charTyped(char chr, int modifiers) {
                for (SubNoteWidget widget : subWidgets) if (widget.charTyped(chr, modifiers)) return true;
                return super.charTyped(chr, modifiers);
            }
            @Override public List<? extends Element> children() { return subWidgets; }
            @Override public List<? extends Selectable> selectableChildren() { return subWidgets; }
        }

        class SubNoteWidget implements Element, Selectable {
            int noteIndex;
            String text;
            int xOffset;
            int width;
            int height = 55;

            // Храним реальные координаты на экране
            int realX, realY;

            ButtonWidget editBtn;
            ButtonWidget deleteBtn;
            TextFieldWidget inlineEditor;
            ButtonWidget saveBtn;
            boolean isEditing = false;
            private boolean focused = false;

            public SubNoteWidget(int index, String text, int xOffset, int width) {
                this.noteIndex = index;
                this.text = text;
                this.xOffset = xOffset;
                this.width = width;

                // УВЕЛИЧЕННЫЕ КНОПКИ (20x20)
                this.editBtn = ButtonWidget.builder(Text.literal("✎"), b -> startEdit())
                        .dimensions(0, 0, 20, 20).build(); // Было 15

                this.deleteBtn = ButtonWidget.builder(Text.literal("x").formatted(Formatting.RED), b -> {
                    FriendManager.getInstance().removeNote(nickname, noteIndex);
                    FriendNoteScreen.this.refreshList();
                }).dimensions(0, 0, 20, 20).build(); // Было 15

                this.inlineEditor = new TextFieldWidget(client.textRenderer, 0, 0, width - 10, 15, Text.empty());
                this.inlineEditor.setMaxLength(MAX_NOTE_LENGTH);
                this.inlineEditor.setText(text);
                this.inlineEditor.visible = false;

                this.saveBtn = ButtonWidget.builder(Text.literal("✔").formatted(Formatting.GREEN), b -> saveEdit())
                        .dimensions(0, 0, 20, 20).build();
                this.saveBtn.visible = false;
            }

            void startEdit() {
                isEditing = true;
                inlineEditor.visible = true;
                inlineEditor.setFocused(true);
                saveBtn.visible = true;
                editBtn.visible = false;
            }

            void saveEdit() {
                FriendManager.getInstance().editNote(nickname, noteIndex, inlineEditor.getText());
                isEditing = false;
                inlineEditor.visible = false;
                saveBtn.visible = false;
                editBtn.visible = true;
                this.text = inlineEditor.getText();
            }

            public void render(DrawContext context, int x, int y, int mouseX, int mouseY, float delta) {
                int borderColor = 0xFFFFFFFF;
                int bgColor = 0xFF202020;

                context.fill(x, y, x + width, y + height, borderColor);
                context.fill(x + 1, y + 1, x + width - 1, y + height - 1, bgColor);

                if (!isEditing) {
                    context.drawText(client.textRenderer, "#" + (noteIndex + 1), x + 4, y + 4, 0xFFAAAAAA, false);
                    List<OrderedText> lines = client.textRenderer.wrapLines(Text.literal(text), width - 8);
                    int limit = Math.min(lines.size(), 3);
                    for (int i = 0; i < limit; i++) {
                        context.drawText(client.textRenderer, lines.get(i), x + 4, y + 16 + (i * 10), 0xFFFFFFFF, false);
                    }
                } else {
                    inlineEditor.setX(x + 4);
                    inlineEditor.setY(y + 16);
                    inlineEditor.setWidth(width - 8);
                    inlineEditor.render(context, mouseX, mouseY, delta);

                    saveBtn.setX(x + width - 22);
                    saveBtn.setY(y + height - 22);
                    saveBtn.render(context, mouseX, mouseY, delta);
                }

                // Кнопки рисуем всегда, если навели на карточку (или всегда, чтобы видеть)
                if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                    if (!isEditing) {
                        editBtn.setX(x + width - 44); // Сдвинули левее
                        editBtn.setY(y + 2);
                        editBtn.render(context, mouseX, mouseY, delta);

                        deleteBtn.setX(x + width - 22);
                        deleteBtn.setY(y + 2);
                        deleteBtn.render(context, mouseX, mouseY, delta);
                    }
                }
            }

            @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
                // ИСПРАВЛЕНИЕ КЛИКА: Используем realX/realY для проверки
                // Но кнопки сами проверяют свои координаты (которые мы задали в render).
                // Главное - вызвать mouseClicked у кнопок.

                if (isEditing) {
                    if (saveBtn.mouseClicked(mouseX, mouseY, button)) return true;
                    if (inlineEditor.mouseClicked(mouseX, mouseY, button)) return true;
                } else {
                    if (editBtn.mouseClicked(mouseX, mouseY, button)) return true;
                    if (deleteBtn.mouseClicked(mouseX, mouseY, button)) return true;
                }
                return false;
            }

            // ... остальной код (keyPressed и т.д.) ...
            @Override public void setFocused(boolean focused) { this.focused = focused; }
            @Override public boolean isFocused() { return this.focused; }
            @Override public SelectionType getType() { return SelectionType.NONE; }
            @Override public void appendNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {}
            @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (isEditing) return inlineEditor.keyPressed(keyCode, scanCode, modifiers);
                return false;
            }
            @Override public boolean charTyped(char chr, int modifiers) {
                if (isEditing) return inlineEditor.charTyped(chr, modifiers);
                return false;
            }
        }
        @Override
        protected int getScrollbarX() {
            // Сдвигаем скроллбар к правому краю экрана (ширина экрана - 6 пикселей)
            return this.width - 6;
        }

        @Override
        public int getRowWidth() {
            // Делаем строку широкой, почти во весь экран.
            // Это ОБЯЗАТЕЛЬНО, чтобы клики мышкой засчитывались в правой части экрана.
            return this.width - 10;
        }
    }
    private boolean isPlayerOnline(UUID uuid) {
        if (this.client.getNetworkHandler() == null || uuid == null) return false;
        // Проверяем, есть ли запись об игроке в текущем соединении
        return this.client.getNetworkHandler().getPlayerListEntry(uuid) != null;
    }
}