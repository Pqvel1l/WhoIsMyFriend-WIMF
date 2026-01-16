package com.wimf.mixin.client;

import com.wimf.FriendManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onPlayerList", at = @At("TAIL"))
    private void onPlayerListUpdate(PlayerListS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Если это не добавление игрока - выходим
        if (!packet.getActions().contains(PlayerListS2CPacket.Action.ADD_PLAYER)) return;
        if (client.player == null) return;

        for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {
            if (entry.profile() == null) continue;

            String name = entry.profile().getName();

            // Если это друг - обновляем его статус (время входа и UUID)
            // Но больше НЕ показываем уведомлений
            if (FriendManager.getInstance().isFriend(name)) {
                FriendManager.getInstance().updateFriendStatus(name, entry.profile().getId());
            }
        }
    }
}