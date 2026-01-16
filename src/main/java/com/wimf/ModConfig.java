package com.wimf;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private List<FriendProfile> friends = new ArrayList<>();

    // Цвета
    private String onlineColor = "#55FF55";
    private String offlineColor = "#00AA00";

    // Тосты (Уведомления)
    private boolean showJoinToast = true;

    // Иконки (Вернули эти поля!)
    private String friendIcon = "";      // Например "★"
    private String friendIconColor = "gold";

    public List<FriendProfile> getFriends() {
        if (friends == null) friends = new ArrayList<>();
        return friends;
    }
    public void setFriends(List<FriendProfile> friends) { this.friends = friends; }

    // --- Геттеры и Сеттеры для настроек ---

    public String getOnlineColor() { return onlineColor != null ? onlineColor : "#55FF55"; }
    public void setOnlineColor(String color) { this.onlineColor = color; }

    public String getOfflineColor() { return offlineColor != null ? offlineColor : "#00AA00"; }
    public void setOfflineColor(String color) { this.offlineColor = color; }

    public boolean isShowJoinToast() { return showJoinToast; }
    public void setShowJoinToast(boolean show) { this.showJoinToast = show; }

    // --- Методы для иконок (теперь ошибка пропадет) ---

    public String getFriendIcon() { return friendIcon != null ? friendIcon : ""; }
    public void setFriendIcon(String icon) { this.friendIcon = icon; }

    public String getFriendIconColor() { return friendIconColor != null ? friendIconColor : "gold"; }
    public void setFriendIconColor(String color) { this.friendIconColor = color; }
}