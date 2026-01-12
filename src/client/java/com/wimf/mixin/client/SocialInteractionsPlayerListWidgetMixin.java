package com.wimf.mixin.client;

import com.wimf.FriendManager;
import com.wimf.FriendProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListEntry;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListWidget;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.util.DefaultSkinHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mixin(SocialInteractionsPlayerListWidget.class)
public abstract class SocialInteractionsPlayerListWidgetMixin extends ElementListWidget<SocialInteractionsPlayerListEntry> {

    @Shadow @Final private SocialInteractionsScreen parent;

    // Теневая копия приватного списка, который использует игра
    @Shadow @Final @Mutable private List<SocialInteractionsPlayerListEntry> players;

    public SocialInteractionsPlayerListWidgetMixin(MinecraftClient client, int width, int height, int y, int itemHeight) {
        super(client, width, height, y, itemHeight);
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void addOfflineFriends(Collection<UUID> uuids, double scrollAmount, boolean includeBlocked, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) return;

        for (UUID uuid : uuids) {
            // Если игрока нет в табе (онлайн)
            if (client.getNetworkHandler().getPlayerListEntry(uuid) == null) {

                // Ищем профиль друга (сравниваем UUID как строки для надежности)
                Optional<FriendProfile> friendOpt = FriendManager.getInstance().getAllFriends().stream()
                        .filter(p -> p.getUuid() != null && p.getUuid().toString().equals(uuid.toString()))
                        .findFirst();

                if (friendOpt.isPresent()) {
                    FriendProfile profile = friendOpt.get();

                    SocialInteractionsPlayerListEntry entry = new SocialInteractionsPlayerListEntry(
                            client,
                            this.parent,
                            uuid,
                            profile.getNickname(),
                            () -> DefaultSkinHelper.getSkinTextures(uuid),
                            false
                    );

                    entry.setOffline(true);

                    // ВАЖНОЕ ИСПРАВЛЕНИЕ:
                    // 1. Добавляем в визуальный список (чтобы видеть прямо сейчас)
                    this.addEntry(entry);

                    // 2. Добавляем в скрытый список 'players' (чтобы поиск и сортировка не удаляли его)
                    this.players.add(entry);
                }
            }
        }
    }
}