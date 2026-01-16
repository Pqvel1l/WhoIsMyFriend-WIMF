package com.wimf.mixin.client;

import com.wimf.FriendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListEntry;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen;
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
    @Shadow @Final @Mutable private List<ClickableWidget> buttons;
    @Shadow private boolean offline;

    @Unique private ButtonWidget wimf$friendButton; // +/-
    @Unique private ButtonWidget wimf$noteButton;   // Карандаш
    @Unique private ButtonWidget wimf$favButton;    // Звезда

    // Цвет ника (изменения не требуются)
    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/multiplayer/SocialInteractionsPlayerListEntry;WHITE_COLOR:I", opcode = Opcodes.GETSTATIC))
    private int changeColorOnGet() {
        if (FriendManager.getInstance().isFriend(this.name)) {
            if (this.offline) return FriendManager.getInstance().getOfflineColor();
            return FriendManager.getInstance().getOnlineColor();
        }
        return WHITE_COLOR;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addCustomButtons(MinecraftClient client, SocialInteractionsScreen parent, UUID uuid, String name, Supplier<SkinTextures> skinTexture, boolean reportable, CallbackInfo ci) {
        boolean isFriend = FriendManager.getInstance().isFriend(name);

        // 1. Кнопка "Звезда" (Избранное)
        boolean isFav = FriendManager.getInstance().getFriend(name).map(p -> p.isFavorite()).orElse(false);
        this.wimf$favButton = ButtonWidget.builder(Text.literal(isFav ? "★" : "☆").styled(s -> s.withColor(0xFFD700)), button -> {
                    FriendManager.getInstance().toggleFavorite(name);
                    boolean newFav = FriendManager.getInstance().getFriend(name).map(p -> p.isFavorite()).orElse(false);
                    button.setMessage(Text.literal(newFav ? "★" : "☆").styled(s -> s.withColor(0xFFD700)));
                    if (parent instanceof com.wimf.IFriendScreen fs && fs.wimf$isFriendTab()) fs.wimf$refreshList();
                })
                .dimensions(0, 0, 20, 20)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("wimf.gui.toggle_favorite")))
                .build();
        this.wimf$favButton.visible = isFriend;

        // 2. Кнопка +/-
        Text buttonText = isFriend ? Text.literal("-") : Text.literal("+");
        this.wimf$friendButton = ButtonWidget.builder(buttonText, button -> {
                    boolean currentlyFriend = FriendManager.getInstance().isFriend(name);

                    if (currentlyFriend) {
                        // --- ЭКРАН ПОДТВЕРЖДЕНИЯ ---
                        client.setScreen(new net.minecraft.client.gui.screen.ConfirmScreen(
                                (confirmed) -> {
                                    // Возвращаемся в меню (обязательно)
                                    client.setScreen(parent);

                                    if (confirmed) {
                                        FriendManager.getInstance().removeFriend(name);
                                        // Обновляем список, если мы во вкладке Друзья
                                        if (parent instanceof com.wimf.IFriendScreen fs && fs.wimf$isFriendTab()) {
                                            fs.wimf$refreshList();
                                        }

                                        // Обновляем кнопку (визуально, хотя список уже может перерисоваться)
                                        button.setMessage(Text.literal("+"));
                                        if (this.wimf$noteButton != null) this.wimf$noteButton.visible = false;
                                        if (this.wimf$favButton != null) this.wimf$favButton.visible = false;
                                    }
                                },
                                Text.translatable("wimf.message.remove_confirm", name), // Заголовок
                                Text.literal("This cannot be undone!") // Описание
                        ));
                        // -----------------------------
                    } else {
                        FriendManager.getInstance().addFriend(name, uuid);
                        button.setMessage(Text.literal("-"));
                        if (this.wimf$noteButton != null) this.wimf$noteButton.visible = true;
                        if (this.wimf$favButton != null) this.wimf$favButton.visible = true;

                        if (parent instanceof com.wimf.IFriendScreen fs && fs.wimf$isFriendTab()) {
                            fs.wimf$refreshList();
                        }
                    }
                })
                // ... (dimensions и tooltip без изменений) ...
                .dimensions(0, 0, 20, 20)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("wimf.gui.tooltip.toggle_friend")))
                .build();

        // 3. Кнопка Заметки
        this.wimf$noteButton = ButtonWidget.builder(Text.literal("✎"), button -> {
                    client.setScreen(new com.wimf.gui.FriendNoteScreen(parent, name));
                })
                .dimensions(0, 0, 20, 20)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable("wimf.gui.tooltip.open_notes")))
                .build();
        this.wimf$noteButton.visible = isFriend;

        List<ClickableWidget> mutableButtons = new ArrayList<>(this.buttons);
        mutableButtons.add(this.wimf$friendButton);
        mutableButtons.add(this.wimf$noteButton);
        mutableButtons.add(this.wimf$favButton); // Добавляем звезду
        this.buttons = mutableButtons;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderCustomButtons(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        // Мы позиционируем кнопки ОТНОСИТЕЛЬНО ПРАВОГО КРАЯ (entryWidth),
        // но с учетом ванильных кнопок.
        // Если игрок оффлайн, ванильных кнопок (mute/report) может не быть.

        // Считаем отступ справа.
        // buttons.size() включает наши 3 кнопки + ванильные.
        // Чтобы встать левее всех, считаем (все кнопки - 3 наших) * ширину.
        int vanillaButtonsCount = this.buttons.size() - 3;
        int rightOffset = 10 + (vanillaButtonsCount * 24);

        // Порядок: [Fav] [Note] [+/-] [Vanilla...]

        // 1. Кнопка +/- (Самая правая из наших)
        if (this.wimf$friendButton != null) {
            this.wimf$friendButton.setX(x + (entryWidth - rightOffset - 20));
            this.wimf$friendButton.setY(y + (entryHeight - 20) / 2);
            this.wimf$friendButton.render(context, mouseX, mouseY, tickDelta);
        }

        // 2. Кнопка Заметки (Левее)
        if (this.wimf$noteButton != null) {
            this.wimf$noteButton.setX(x + (entryWidth - rightOffset - 20 - 24));
            this.wimf$noteButton.setY(y + (entryHeight - 20) / 2);
            this.wimf$noteButton.render(context, mouseX, mouseY, tickDelta);
        }

        // 3. Кнопка Звезда (Еще левее)
        if (this.wimf$favButton != null) {
            this.wimf$favButton.setX(x + (entryWidth - rightOffset - 20 - 48));
            this.wimf$favButton.setY(y + (entryHeight - 20) / 2);
            this.wimf$favButton.render(context, mouseX, mouseY, tickDelta);
        }
    }
}