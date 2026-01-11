package com.wimf;

import java.util.List;
import java.util.Optional;

public class FriendManager {
    private static final FriendManager INSTANCE = new FriendManager();
    public static FriendManager getInstance() { return INSTANCE; }
    private FriendManager() {}

    private List<FriendProfile> getFriendsList() {
        return ConfigManager.getInstance().getConfig().getFriends();
    }

    private void save() {
        ConfigManager.getInstance().save();
    }

    // --- Основные операции с друзьями ---

    public boolean addFriend(String nickname) {
        if (isFriend(nickname)) {
            return false;
        }
        getFriendsList().add(new FriendProfile(nickname));
        save();
        return true;
    }

    public boolean removeFriend(String nickname) {
        boolean removed = getFriendsList().removeIf(profile -> profile.getNickname().equalsIgnoreCase(nickname));
        if (removed) {
            save();
        }
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

    public List<FriendProfile> getAllFriends() {
        return getFriendsList();
    }

    // --- Новые методы для управления заметками (Logic moved from Commands) ---

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
            // NoteIndex ожидаем 0-based (начинается с 0, а не с 1)
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
}