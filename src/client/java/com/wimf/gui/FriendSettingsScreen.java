package com.wimf.gui;

import com.wimf.FriendManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class FriendSettingsScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget onlineColorInput;
    private TextFieldWidget offlineColorInput;
    private ButtonWidget toastButton;

    public FriendSettingsScreen(Screen parent) {
        super(Text.translatable("wimf.gui.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        // 1. Поле Online Color
        this.onlineColorInput = new TextFieldWidget(this.textRenderer, centerX - 100, 60, 210, 20, Text.literal(""));
        this.onlineColorInput.setMaxLength(7);
        this.onlineColorInput.setText(com.wimf.ConfigManager.getInstance().getConfig().getOnlineColor());
        this.addDrawableChild(this.onlineColorInput);

        // 2. Поле Offline Color
        this.offlineColorInput = new TextFieldWidget(this.textRenderer, centerX - 100, 100, 210, 20, Text.literal(""));
        this.offlineColorInput.setMaxLength(7);
        this.offlineColorInput.setText(com.wimf.ConfigManager.getInstance().getConfig().getOfflineColor());
        this.addDrawableChild(this.offlineColorInput);

        // 3. Кнопка Тостов (Toggle)
        boolean currentToast = FriendManager.getInstance().isShowToasts();
        this.toastButton = ButtonWidget.builder(getToastText(currentToast), button -> {
                    boolean newState = !FriendManager.getInstance().isShowToasts();
                    FriendManager.getInstance().setShowToasts(newState);
                    button.setMessage(getToastText(newState));
                })
                .dimensions(centerX - 100, 140, 210, 20)
                .build();
        this.addDrawableChild(this.toastButton);

        // 4. Кнопка Сохранить и Выйти
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> {
                    // Сохраняем цвета
                    FriendManager.getInstance().setOnlineColor(this.onlineColorInput.getText());
                    FriendManager.getInstance().setOfflineColor(this.offlineColorInput.getText());
                    this.client.setScreen(this.parent);
                })
                .dimensions(centerX - 100, this.height - 40, 210, 20)
                .build());
    }

    private Text getToastText(boolean enabled) {
        return Text.translatable("wimf.gui.settings.toasts", enabled ? "ON" : "OFF");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0000000);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFFFF);

        // --- ИЗМЕНЕНИЕ: Используем ключи ---
        context.drawTextWithShadow(this.textRenderer, Text.translatable("wimf.gui.settings.online_label"), this.width / 2 - 100, 48, 0xFFAAAAAA);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("wimf.gui.settings.offline_label"), this.width / 2 - 100, 88, 0xFFAAAAAA);
    }
}