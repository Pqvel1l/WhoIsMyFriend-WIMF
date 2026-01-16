package com.wimf.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class FriendJoinToast implements Toast {
    private static final long DURATION = 5000L;

    private final Text title;
    private final Text message;

    // Новое поле для хранения статуса
    private Toast.Visibility visibility = Toast.Visibility.SHOW;

    public FriendJoinToast(Text title, Text message) {
        this.title = title;
        this.message = message;
    }

    // --- НОВЫЙ ОБЯЗАТЕЛЬНЫЙ МЕТОД ---
    @Override
    public Toast.Visibility getVisibility() {
        return this.visibility;
    }

    // --- ОБНОВЛЕННЫЙ МЕТОД DRAW (Void, другие аргументы) ---
    @Override
    public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
        // Рисуем фон

        // Рисуем текст (аргумент textRenderer теперь приходит в метод сам)
        context.drawText(textRenderer, this.title, 18, 7, 0xFFFFFF00, false);
        context.drawText(textRenderer, this.message, 18, 18, 0xFFFFFFFF, false);

        // Логика исчезновения: просто меняем переменную visibility
        if (startTime >= DURATION) {
            this.visibility = Toast.Visibility.HIDE;
        }
    }

    // Метод update нужен интерфейсу, но нам он не обязателен для простой логики
    @Override
    public void update(ToastManager manager, long time) {
        // Оставляем пустым
    }
}