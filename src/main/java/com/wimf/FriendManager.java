package com.wimf;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FriendManager {
    private static final FriendManager INSTANCE = new FriendManager();

    public static FriendManager getInstance() {
        return INSTANCE;
    }
    static {
        System.out.println("WIMF: FriendManager initializing...");
        ConfigManager.getInstance().getConfig();
    }
    private FriendManager() {}

    // НОВЫЙ МЕТОД: Принудительная перезагрузка
    public void reload() {
        ConfigManager.getInstance().load();
    }

    private List<FriendProfile> getFriendsList() {
        return ConfigManager.getInstance().getConfig().getFriends();
    }

    private void save() {
        ConfigManager.getInstance().save();
    }

    // Добавление друга (Ник + UUID)
    public boolean addFriend(String nickname, UUID uuid) {
        if (isFriend(nickname)) {
            return false;
        }
        getFriendsList().add(new FriendProfile(nickname, uuid));
        save();
        return true;
    }

    // Перегрузка для совместимости (если UUID неизвестен)
    public boolean addFriend(String nickname) {
        return addFriend(nickname, null);
    }

    // Обновление статуса (Last Seen)
    public void updateFriendStatus(String nickname, UUID uuid) {
        getFriend(nickname).ifPresent(profile -> {
            profile.updateLastSeen();
            if (profile.getUuid() == null && uuid != null) {
                profile.setUuid(uuid);
            }
        });
    }

    public boolean removeFriend(String nickname) {
        boolean removed = getFriendsList().removeIf(profile -> profile.getNickname().equalsIgnoreCase(nickname));
        if (removed) save();
        return removed;
    }

    public boolean isFriend(String nickname) {
        return getFriendsList().stream().anyMatch(profile -> profile.getNickname().equalsIgnoreCase(nickname));
    }

    public Optional<FriendProfile> getFriend(String nickname) {
        return getFriendsList().stream()
                .filter(profile -> profile.getNickname().equalsIgnoreCase(nickname))
                .findFirst();
    }

    // --- Заметки ---
    public boolean addNote(String nickname, String noteText) {
        Optional<FriendProfile> profileOpt = getFriend(nickname);
        if (profileOpt.isPresent()) {
            profileOpt.get().addNote(noteText);
            save();
            return true;
        }
        return false;
    }

    public boolean editNote(String nickname, int noteIndex, String newText) {
        Optional<FriendProfile> profileOpt = getFriend(nickname);
        if (profileOpt.isPresent()) {
            boolean success = profileOpt.get().setNote(noteIndex, newText);
            if (success) save();
            return success;
        }
        return false;
    }

    public boolean removeNote(String nickname, int noteIndex) {
        Optional<FriendProfile> profileOpt = getFriend(nickname);
        if (profileOpt.isPresent()) {
            boolean success = profileOpt.get().removeNote(noteIndex);
            if (success) save();
            return success;
        }
        return false;
    }

    // ЭТОТ МЕТОД НУЖЕН ДЛЯ MIXIN
    public List<FriendProfile> getAllFriends() {
        return getFriendsList();
    }

}