package com.wimf.mixin.client;

import com.wimf.FriendManager;
import com.wimf.FriendProfile;
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

import java.util.*;

@Mixin(SocialInteractionsScreen.class)
public abstract class SocialInteractionsScreenMixin extends Screen implements com.wimf.IFriendScreen {

    @Shadow private SocialInteractionsPlayerListWidget playerList;

    @Unique private static final Text FRIENDS_TAB_TITLE = Text.translatable("wimf.gui.tab.friends");
    @Unique private boolean wimf$isFriendTab = false;
    @Unique private ButtonWidget wimf$friendButton;
    @Unique private ButtonWidget wimf$settingsButton; // Кнопка Шестеренки

    protected SocialInteractionsScreenMixin(Text title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void addFriendTabButton(CallbackInfo ci) {
        int windowLeft = (this.width - 238) / 2;
        int rightEdgeOfBackground = windowLeft + 238;

        // 1. Кнопка "Друзья"
        this.wimf$friendButton = ButtonWidget.builder(FRIENDS_TAB_TITLE, button -> {
                    this.wimf$isFriendTab = true;
                    FriendManager.getInstance().reload();
                    if (this.playerList != null) this.playerList.setScrollY(0);
                    this.updateFriendList();
                    this.updateButtonStyles();
                })
                .dimensions(rightEdgeOfBackground + 2, 45, 50, 20)
                .build();
        this.addDrawableChild(this.wimf$friendButton);

        // 2. Кнопка "Шестеренка" (Настройки)
        this.wimf$settingsButton = ButtonWidget.builder(Text.literal("⚙"), button -> {
                    // Открываем настройки
                    this.client.setScreen(new com.wimf.gui.FriendSettingsScreen(this));
                })
                .dimensions(rightEdgeOfBackground + 2 + 52, 45, 20, 20) // Справа от кнопки Друзья
                .build();
        this.wimf$settingsButton.visible = false; // Скрыта, пока не выберем вкладку друзей
        this.addDrawableChild(this.wimf$settingsButton);

        if (this.wimf$isFriendTab) {
            this.updateFriendList();
            this.updateButtonStyles();
        }
    }

    @Inject(method = "refreshWidgetPositions", at = @At("TAIL"))
    private void updateMyButtonPosition(CallbackInfo ci) {
        if (this.wimf$friendButton != null) {
            int windowLeft = (this.width - 238) / 2;
            int rightEdge = windowLeft + 238;
            this.wimf$friendButton.setX(rightEdge + 2);
            this.wimf$friendButton.setY(45);

            if (this.wimf$settingsButton != null) {
                this.wimf$settingsButton.setX(rightEdge + 2 + 52);
                this.wimf$settingsButton.setY(45);
            }
        }
    }

    @Inject(method = "setCurrentTab", at = @At("HEAD"))
    private void onSetCurrentTab(SocialInteractionsScreen.Tab tab, CallbackInfo ci) {
        this.wimf$isFriendTab = false;
        this.updateButtonStyles();
    }

    @Unique
    private void updateFriendList() {
        if (this.playerList == null || this.client == null) return;

        // --- ЛОГИКА СОРТИРОВКИ ---
        List<FriendProfile> allFriends = new ArrayList<>(FriendManager.getInstance().getAllFriends());

        // Сортируем: Избранные -> Онлайн -> Оффлайн
        allFriends.sort((p1, p2) -> {
            if (p1.isFavorite() && !p2.isFavorite()) return -1; // p1 выше
            if (!p1.isFavorite() && p2.isFavorite()) return 1;  // p2 выше

            // Если статус "Избранное" одинаковый, можно сортировать по нику или онлайну
            return p1.getNickname().compareToIgnoreCase(p2.getNickname());
        });

        // Теперь собираем UUID в нужном порядке
        List<UUID> uuidsToShow = new ArrayList<>();
        List<String> addedNicknames = new ArrayList<>();

        // ВНИМАНИЕ: Чтобы сортировка работала, нам нужно добавлять UUID в том порядке,
        // в котором они отсортированы в allFriends.
        // Но SocialPlayerListWidget сам пытается сортировать онлайн игроков.
        // Мы сделаем просто: добавим всех по списку.

        for (FriendProfile profile : allFriends) {
            UUID uuid = profile.getUuid();
            if (uuid == null) {
                uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + profile.getNickname()).getBytes());
            }

            // Если этот друг есть онлайн (в табе) - добавляем его реальный UUID
            if (client.getNetworkHandler() != null && client.getNetworkHandler().getPlayerListEntry(uuid) != null) {
                uuidsToShow.add(uuid);
                addedNicknames.add(profile.getNickname());
            }
            // Если оффлайн - добавляем всё равно
            else if (!uuidsToShow.contains(uuid)) {
                uuidsToShow.add(uuid);
            }
        }

        this.playerList.setScrollY(0);
        this.playerList.update(uuidsToShow, 0, false);
    }

    @Unique
    private void updateButtonStyles() {
        if (this.wimf$friendButton != null) {
            if (this.wimf$isFriendTab) {
                this.wimf$friendButton.setMessage(FRIENDS_TAB_TITLE.copy().formatted(Formatting.UNDERLINE));
                this.wimf$settingsButton.visible = true; // Показываем шестеренку
            } else {
                this.wimf$friendButton.setMessage(FRIENDS_TAB_TITLE);
                this.wimf$settingsButton.visible = false; // Скрываем
            }
        }
    }

    @Override public void wimf$refreshList() { this.updateFriendList(); }
    @Override public boolean wimf$isFriendTab() { return this.wimf$isFriendTab; }
}