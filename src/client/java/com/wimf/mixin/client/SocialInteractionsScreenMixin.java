package com.wimf.mixin.client;

import com.wimf.FriendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListWidget;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.wimf.IFriendScreen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Mixin(SocialInteractionsScreen.class)
public abstract class SocialInteractionsScreenMixin extends Screen implements IFriendScreen {

    @Shadow private SocialInteractionsPlayerListWidget playerList;
    @Shadow protected abstract void updateServerLabel(MinecraftClient client);

    @Unique
    private static final Text FRIENDS_TAB_TITLE = Text.translatable("wimf.gui.tab.friends");

    @Unique
    private boolean wimf$isFriendTab = false;

    @Unique
    private ButtonWidget wimf$friendButton;

    protected SocialInteractionsScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addFriendTabButton(CallbackInfo ci) {
        // РАСЧЕТ КООРДИНАТ СРАЗУ ПРИ СОЗДАНИИ
        // (this.width - 238) / 2 — это формула центрирования меню в ванильном коде
        int windowLeft = (this.width - 238) / 2;
        int rightEdgeOfBackground = windowLeft + 238;

        // Ставим кнопку справа от фона + маленький отступ (2 пикселя)
        int buttonX = rightEdgeOfBackground + 2;
        int buttonY = 45; // Стандартная высота вкладок

        this.wimf$friendButton = ButtonWidget.builder(FRIENDS_TAB_TITLE, button -> {
                    this.wimf$isFriendTab = true;

                    // --- ЯДЕРНОЕ РЕШЕНИЕ ---
                    // Принудительно перезагружаем данные перед показом
                    FriendManager.getInstance().reload();

                    if (this.playerList != null) {
                        this.playerList.setScrollY(0);
                    }

                    this.updateFriendList();
                    this.updateButtonStyles();
                })
                // ВАЖНО: Передаем buttonX и buttonY, а не 0, 0
                .dimensions(buttonX, buttonY, 50, 20)
                .build();

        this.addDrawableChild(this.wimf$friendButton);
        if (this.wimf$isFriendTab) {
            this.updateFriendList();
        }
    }

    /**
     * Этот метод нужен, чтобы кнопка не уезжала, если игрок изменит размер окна, не закрывая меню
     */
    @Inject(method = "refreshWidgetPositions", at = @At("TAIL"))
    private void updateMyButtonPosition(CallbackInfo ci) {
        if (this.wimf$friendButton != null) {
            int windowLeft = (this.width - 238) / 2;
            int rightEdgeOfBackground = windowLeft + 238;

            this.wimf$friendButton.setX(rightEdgeOfBackground + 2);
            this.wimf$friendButton.setY(45);
        }
    }

    @Inject(method = "setCurrentTab", at = @At("HEAD"))
    private void onSetCurrentTab(SocialInteractionsScreen.Tab tab, CallbackInfo ci) {
        // Сбрасываем нашу вкладку при переключении на ванильные
        this.wimf$isFriendTab = false;
        this.updateButtonStyles();
    }

    @Unique
    private void updateFriendList() {
        if (this.playerList != null && this.client != null) {

            // --- ОТЛАДКА ---
            List<com.wimf.FriendProfile> friends = FriendManager.getInstance().getAllFriends();
            // ----------------

            List<UUID> uuidsToShow = new ArrayList<>();
            List<String> addedNicknames = new ArrayList<>();

            // 1. Онлайн
            if (this.client.getNetworkHandler() != null) {
                Collection<UUID> onlineUUIDs = this.client.getNetworkHandler().getPlayerUuids();
                for (UUID uuid : onlineUUIDs) {
                    PlayerListEntry entry = this.client.getNetworkHandler().getPlayerListEntry(uuid);
                    if (entry != null) {
                        String name = entry.getProfile().getName();
                        if (FriendManager.getInstance().isFriend(name)) {
                            uuidsToShow.add(uuid);
                            addedNicknames.add(name);
                            FriendManager.getInstance().updateFriendStatus(name, uuid);
                        }
                    }
                }
            }

            // 2. Оффлайн
            for (com.wimf.FriendProfile profile : friends) {
                if (addedNicknames.contains(profile.getNickname())) continue;

                UUID fUuid = profile.getUuid();
                if (!uuidsToShow.contains(fUuid)) {
                    uuidsToShow.add(fUuid);
                }
            }


            this.playerList.setScrollY(0);
            this.playerList.update(uuidsToShow, 0, false);
        }
    }
    @Unique
    private void updateButtonStyles() {
        if (this.wimf$friendButton != null) {
            if (this.wimf$isFriendTab) {
                this.wimf$friendButton.setMessage(FRIENDS_TAB_TITLE.copy().formatted(Formatting.UNDERLINE));
            } else {
                this.wimf$friendButton.setMessage(FRIENDS_TAB_TITLE);
            }
        }
    }
    @Override
    public void wimf$refreshList() {
        // Просто вызываем наш существующий метод обновления
        this.updateFriendList();
    }

    @Override
    public boolean wimf$isFriendTab() {
        return this.wimf$isFriendTab;
    }
}