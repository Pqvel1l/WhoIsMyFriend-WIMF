package com.wimf.mixin.client;

import com.wimf.FriendManager;
import com.wimf.IFriendScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListEntry;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Mixin(SocialInteractionsPlayerListEntry.class)
public class SocialInteractionsPlayerListEntryMixin {

    @Shadow @Final private String name;
    @Shadow @Final public static int WHITE_COLOR;

    // ДОБАВИЛИ @Mutable, чтобы мы могли перезаписать этот список
    @Shadow @Final @Mutable private List<ClickableWidget> buttons;

    @Unique
    private ButtonWidget wimf$friendButton;
    @Shadow private boolean offline;
    // --- ЛОГИКА ЦВЕТА ---
    @Redirect(
            method = "render",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/gui/screen/multiplayer/SocialInteractionsPlayerListEntry;WHITE_COLOR:I",
                    opcode = Opcodes.GETSTATIC
            )
    )
    private int changeColorOnGet() {
        if (FriendManager.getInstance().isFriend(this.name)) {
            // Если игрок ОФФЛАЙН
            if (this.offline) {
                // ВАРИАНТ 1: Темно-зеленый (0xFF00AA00)
                // return 0xFF00AA00;

                // ВАРИАНТ 2: Серый (0xFFAAAAAA) - как у обычных оффлайн игроков
                 return 0xFFAAAAAA;
            }

            // Если ОНЛАЙН — Ярко-зеленый
            return 0xFF55FF55;
        }
        return WHITE_COLOR;
    }

    @Unique
    private ButtonWidget wimf$noteButton;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addCustomButtons(MinecraftClient client, SocialInteractionsScreen parent, UUID uuid, String name, Supplier<SkinTextures> skinTexture, boolean reportable, CallbackInfo ci) {
        // 1. Кнопка Друг (+/-)
        boolean isFriend = FriendManager.getInstance().isFriend(name);
        Text buttonText = isFriend ? Text.literal("-") : Text.literal("+");

        this.wimf$friendButton = ButtonWidget.builder(buttonText, button -> {
                    boolean currentlyFriend = FriendManager.getInstance().isFriend(name);
                    if (currentlyFriend) {
                        FriendManager.getInstance().removeFriend(name);
                        button.setMessage(Text.literal("+"));
                        if (this.wimf$noteButton != null) this.wimf$noteButton.visible = false;
                    } else {
                        // ВАЖНО: Передаем uuid, который мы получили в аргументах метода
                        FriendManager.getInstance().addFriend(name, uuid);

                        button.setMessage(Text.literal("-"));
                        if (this.wimf$noteButton != null) this.wimf$noteButton.visible = true;
                    }

                    if (parent instanceof IFriendScreen friendScreen) {
                        if (friendScreen.wimf$isFriendTab()) {
                            friendScreen.wimf$refreshList();
                        }
                    }
                })
                .dimensions(0, 0, 20, 20)
                .tooltip(Tooltip.of(Text.translatable("wimf.gui.tooltip.toggle_friend")))
                .build();

        // 2. Кнопка Заметки (✎)
        this.wimf$noteButton = ButtonWidget.builder(Text.literal("✎"), button -> {
                    client.setScreen(new com.wimf.gui.FriendNoteScreen(parent, name));
                })
                .dimensions(0, 0, 20, 20)
                .tooltip(Tooltip.of(Text.translatable("wimf.gui.tooltip.open_notes")))
                .build();

        this.wimf$noteButton.visible = isFriend;

        List<ClickableWidget> mutableButtons = new ArrayList<>(this.buttons);
        mutableButtons.add(this.wimf$friendButton);
        mutableButtons.add(this.wimf$noteButton);
        this.buttons = mutableButtons;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderCustomButtons(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        // Отступ справа.
        // Если есть ванильные кнопки, сдвигаемся от них.
        int rightOffset = 10 + (this.buttons.size() - 2) * 24; // -2, потому что у нас 2 свои кнопки

        // Позиция кнопки Друг (+/-) — самая левая
        if (this.wimf$friendButton != null) {
            this.wimf$friendButton.setX(x + (entryWidth - rightOffset - 20 - 24)); // Еще левее
            this.wimf$friendButton.setY(y + (entryHeight - 20) / 2);
            this.wimf$friendButton.render(context, mouseX, mouseY, tickDelta);
        }

        // Позиция кнопки Заметка (✎) — между +/- и ванильными кнопками
        if (this.wimf$noteButton != null) {
            this.wimf$noteButton.setX(x + (entryWidth - rightOffset - 20));
            this.wimf$noteButton.setY(y + (entryHeight - 20) / 2);
            // Рисуем только если она видима (это обрабатывается внутри render, но на всякий случай)
            this.wimf$noteButton.render(context, mouseX, mouseY, tickDelta);
        }
    }
}