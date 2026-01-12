package com.wimf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    // ВАЖНО: Сначала объявляем файл и GSON, чтобы они были готовы к использованию
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("whoismyfriendwimf.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ВАЖНО: И только ПОТОМ создаем INSTANCE, который вызывает конструктор
    private static final ConfigManager INSTANCE = new ConfigManager();

    private ModConfig config;

    // Конструктор
    private ConfigManager() {
        load();
    }

    public static ConfigManager getInstance() {
        return INSTANCE;
    }

    public ModConfig getConfig() {
        if (config == null) {
            // Если вдруг конфига нет в памяти, грузим принудительно
            load();
        }
        return config;
    }

    public boolean load() {
        // Теперь CONFIG_FILE точно не null
        if (!CONFIG_FILE.exists()) {
            config = new ModConfig();
            save();
            return true;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            config = GSON.fromJson(reader, ModConfig.class);

            if (config == null) {
                config = new ModConfig();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            config = new ModConfig();
            return false;
        }
    }

    public void save() {
        if (config == null) return;
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}