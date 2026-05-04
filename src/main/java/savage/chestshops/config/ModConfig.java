package savage.chestshops.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import savage.chestshops.SavsChestShops;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE;

    // Config fields
    public String economyProvider = "savs_common_economy";
    public String currencyId = "dollar";

    public static ModConfig getConfig() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("savs-chest-shops-config.json");
        File configFile = configPath.toFile();

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                INSTANCE = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                SavsChestShops.LOGGER.error("Failed to load config, using defaults", e);
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
    }

    public static void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("savs-chest-shops-config.json");
        try (FileWriter writer = new FileWriter(configPath.toFile())) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            SavsChestShops.LOGGER.error("Failed to save config", e);
        }
    }
}
