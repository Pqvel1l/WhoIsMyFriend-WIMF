package com.wimf; // Убедись, что пакет правильный!

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class WhoIsMyFriendsWIMFClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ConfigManager.getInstance().load();
		//ModCommands.register();
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			FriendTeam.initialize();
		});
	}
}
