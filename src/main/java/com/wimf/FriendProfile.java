package com.wimf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendProfile {

    private String nickname;
    private UUID uuid; // ID игрока
    private List<String> notes = new ArrayList<>();
    private long lastSeenTimestamp = 0; // Время последнего входа

    // Конструктор
    public FriendProfile(String nickname, UUID uuid) {
        this.nickname = nickname;
        this.uuid = uuid;
        this.notes = new ArrayList<>();
        this.lastSeenTimestamp = System.currentTimeMillis();
    }

    // --- Геттеры и Сеттеры ---
    public String getNickname() { return nickname; }

    public UUID getUuid() {
        if (this.uuid == null) {
            // Если настоящего UUID нет, генерируем постоянный ID на основе ника.
            // Приставка "OfflinePlayer:" нужна, чтобы не пересечься с реальными ID.
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + this.nickname).getBytes());
        }
        return this.uuid;
    }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public long getLastSeenTimestamp() { return lastSeenTimestamp; }
    public void updateLastSeen() { this.lastSeenTimestamp = System.currentTimeMillis(); }
    public void setLastSeenTimestamp(long ts) { this.lastSeenTimestamp = ts; }

    // --- Заметки (Безопасный метод) ---
    public List<String> getNotes() {
        if (this.notes == null) {
            this.notes = new ArrayList<>();
        }
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