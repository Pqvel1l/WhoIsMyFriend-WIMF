package com.wimf;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    // Инициализируем сразу, чтобы GSON не оставил здесь null
    private List<FriendProfile> friends = new ArrayList<>();

    // Настройки цветов (значения по умолчанию)
    private String onlineColor = "#55FF55";
    private String offlineColor = "#AAAAAA";
    private String friendIcon = "";
    private String friendIconColor = "gold";

    public List<FriendProfile> getFriends() {
        if (friends == null) {
            friends = new ArrayList<>();
        }
        return friends;
    }

    public void setFriends(List<FriendProfile> friends) {
        this.friends = friends;
    }

    // Геттеры и сеттеры для цветов
    public String getOnlineColor() { return onlineColor; }
    public void setOnlineColor(String onlineColor) { this.onlineColor = onlineColor; }

    public String getOfflineColor() { return offlineColor; }
    public void setOfflineColor(String offlineColor) { this.offlineColor = offlineColor; }

    public String getFriendIcon() { return friendIcon; }
    public void setFriendIcon(String friendIcon) { this.friendIcon = friendIcon; }

    public String getFriendIconColor() { return friendIconColor; }
    public void setFriendIconColor(String friendIconColor) { this.friendIconColor = friendIconColor; }
}