package com.wimf.mixin.client;

import com.wimf.FriendManager;
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
        // Пробегаем по всем записям в пакете
        for (PlayerListS2CPacket.Entry entry : packet.getEntries()) {

            // --- ИСПРАВЛЕНИЕ: ПРОВЕРКА НА NULL ---
            // Если пакет обновляет только пинг (Latency), профиль может быть null.
            // Нам такие пакеты не нужны для получения имени, поэтому пропускаем их.
            if (entry.profile() == null) {
                continue;
            }

            String name = entry.profile().getName();

            // Если игрок в друзьях - обновляем время последнего захода
            if (FriendManager.getInstance().isFriend(name)) {
                FriendManager.getInstance().updateFriendStatus(name, entry.profile().getId());
            }
        }
    }
}