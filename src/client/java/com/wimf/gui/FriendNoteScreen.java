package com.wimf.gui;

import com.wimf.FriendManager;
import com.wimf.FriendProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

public class FriendNoteScreen extends Screen {
    private final Screen parent;
    private final String nickname;
    private TextFieldWidget noteInput;

    private static final int FIELD_WIDTH = 200;

    public FriendNoteScreen(Screen parent, String nickname) {
        // Заголовок окна
        super(Text.translatable("wimf.gui.notes.title", nickname));
        this.parent = parent;
        this.nickname = nickname;
    }

    @Override
    protected void init() {
        // Поле ввода
        this.noteInput = new TextFieldWidget(this.textRenderer,
                (this.width - FIELD_WIDTH) / 2,
                this.height - 80,
                FIELD_WIDTH, 20,
                Text.translatable("wimf.gui.notes.input_placeholder"));
        this.noteInput.setMaxLength(128);
        this.addDrawableChild(this.noteInput);

        // Кнопка "Добавить"
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("wimf.gui.notes.button.add"), button -> {
                    String text = this.noteInput.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        FriendManager.getInstance().addNote(this.nickname, text);
                        this.noteInput.setText("");
                    }
                })
                .dimensions((this.width - FIELD_WIDTH) / 2, this.height - 55, 98, 20)
                .tooltip(Tooltip.of(Text.translatable("wimf.gui.notes.tooltip.add"))) // Подсказка
                .build());

        // Кнопка "Удалить последнюю"
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("wimf.gui.notes.button.delete_last"), button -> {
                    Optional<FriendProfile> profile = FriendManager.getInstance().getFriend(this.nickname);
                    profile.ifPresent(p -> {
                        List<String> notes = p.getNotes();
                        if (!notes.isEmpty()) {
                            FriendManager.getInstance().removeNote(this.nickname, notes.size() - 1);
                        }
                    });
                })
                .dimensions((this.width - FIELD_WIDTH) / 2 + 102, this.height - 55, 98, 20)
                .tooltip(Tooltip.of(Text.translatable("wimf.gui.notes.tooltip.delete"))) // Подсказка
                .build());

        // Кнопка "Готово"
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("wimf.gui.notes.button.done"), button -> {
                    this.client.setScreen(this.parent);
                })
                .dimensions((this.width - 200) / 2, this.height - 30, 200, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Фон
        context.fill(0, 0, this.width, this.height, 0xC0000000);

        // Заголовок
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);

        Optional<FriendProfile> profileOpt = FriendManager.getInstance().getFriend(this.nickname);
        if (profileOpt.isPresent()) {
            List<String> notes = profileOpt.get().getNotes();
            int y = 50;

            if (notes.isEmpty()) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("wimf.gui.notes.empty"), this.width / 2, y, 0xAAAAAA);
            } else {
                int startX = (this.width - FIELD_WIDTH) / 2;

                for (int i = 0; i < notes.size(); i++) {
                    String rawText = (i + 1) + ". " + notes.get(i);

                    // --- ИСПРАВЛЕНИЕ: Превращаем строку в Text объект ---
                    Text renderText = Text.literal(rawText);

                    context.drawTextWithShadow(this.textRenderer, renderText, startX, y, 0xFFFFFF);
                    y += 12;

                    // Ограничитель, чтобы текст не наезжал на кнопки
                    if (y > this.height - 95) {
                        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("..."), this.width / 2, y, 0xAAAAAA);
                        break;
                    }
                }
            }
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Error: Friend not found"), this.width / 2, 50, 0xFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }
}