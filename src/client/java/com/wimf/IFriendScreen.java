package com.wimf;

public interface IFriendScreen {
    // Метод, чтобы заставить экран обновить список
    void wimf$refreshList();

    // Метод, чтобы узнать, открыта ли вкладка "Друзья"
    boolean wimf$isFriendTab();
}