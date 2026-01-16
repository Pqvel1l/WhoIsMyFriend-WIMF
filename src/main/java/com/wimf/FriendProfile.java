package com.wimf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendProfile {
    private String nickname;
    private UUID uuid;
    private List<String> notes = new ArrayList<>();
    private long lastSeenTimestamp = 0;

    // НОВОЕ ПОЛЕ
    private boolean isFavorite = false;

    public FriendProfile(String nickname, UUID uuid) {
        this.nickname = nickname;
        this.uuid = uuid;
        this.notes = new ArrayList<>();
        this.lastSeenTimestamp = System.currentTimeMillis();
    }

    // Геттеры и сеттеры
    public String getNickname() { return nickname; }
    public UUID getUuid() { return uuid; }

    // ЛОГИКА UUID (из прошлого шага)
    public UUID getUuidOrFake() {
        if (this.uuid == null) return UUID.nameUUIDFromBytes(("OfflinePlayer:" + this.nickname).getBytes());
        return this.uuid;
    }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public long getLastSeenTimestamp() { return lastSeenTimestamp; }
    public void updateLastSeen() { this.lastSeenTimestamp = System.currentTimeMillis(); }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    // ... Методы заметок (getNotes, addNote и т.д.) оставь как были ...
    public List<String> getNotes() {
        if (this.notes == null) this.notes = new ArrayList<>();
        return this.notes;
    }
    public void setNotes(List<String> notes) { this.notes = notes; }
    public void addNote(String note) { getNotes().add(note); }
    public boolean removeNote(int index) {
        if (index >= 0 && index < getNotes().size()) {
            getNotes().remove(index);
            return true;
        }
        return false;
    }
    public boolean setNote(int index, String newNote) {
        if (index >= 0 && index < getNotes().size()) {
            getNotes().set(index, newNote);
            return true;
        }
        return false;
    }
}