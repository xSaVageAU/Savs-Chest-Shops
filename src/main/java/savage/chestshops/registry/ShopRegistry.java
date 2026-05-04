package savage.chestshops.registry;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import savage.chestshops.SavsChestShops;
import savage.chestshops.model.ChestShop;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the persistence and registry of all chest shops.
 */
public class ShopRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Map<GlobalPos, ChestShop> shops = new ConcurrentHashMap<>();
    private final java.util.Set<GlobalPos> dirtyShops = ConcurrentHashMap.newKeySet();
    private final File storageFile;

    private static class Holder {
        static final ShopRegistry INSTANCE = new ShopRegistry();
    }

    public static ShopRegistry getInstance() {
        return Holder.INSTANCE;
    }

    private ShopRegistry() {
        this.storageFile = FabricLoader.getInstance().getConfigDir().resolve("savs-chest-shops.json").toFile();
    }

    public void addShop(ChestShop shop) {
        shops.put(shop.location(), shop);
        save();
    }

    public void removeShop(GlobalPos pos) {
        shops.remove(pos);
        save();
    }

    public ChestShop getShop(GlobalPos pos) {
        return shops.get(pos);
    }

    public boolean isShop(GlobalPos pos) {
        return shops.containsKey(pos);
    }

    public void markDirty(GlobalPos pos) {
        dirtyShops.add(pos);
    }

    public java.util.Set<GlobalPos> consumeDirtyShops() {
        java.util.Set<GlobalPos> consumed = new java.util.HashSet<>(dirtyShops);
        dirtyShops.clear();
        return consumed;
    }

    public java.util.Collection<ChestShop> getAllShops() {
        return shops.values();
    }

    public void load() {
        if (!storageFile.exists()) return;

        try (FileReader reader = new FileReader(storageFile)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("shops")) return;

            JsonArray array = root.getAsJsonArray("shops");
            shops.clear();

            for (JsonElement element : array) {
                try {
                    JsonObject obj = element.getAsJsonObject();
                    JsonObject loc = obj.getAsJsonObject("chestLocation");
                    BlockPos pos = new BlockPos(loc.get("x").getAsInt(), loc.get("y").getAsInt(), loc.get("z").getAsInt());
                    
                    String worldId = obj.get("worldId").getAsString();
                    ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, Identifier.tryParse(worldId));
                    GlobalPos globalPos = GlobalPos.of(dimension, pos);
                    
                    UUID ownerId = UUID.fromString(obj.get("ownerId").getAsString());
                    String ownerName = obj.get("ownerName").getAsString();
                    
                    boolean isBuying = obj.get("buying").getAsBoolean();
                    boolean isAdmin = obj.get("type").getAsString().equals("ADMIN");
                    BigInteger price = new BigInteger(obj.get("price").getAsString());

                    ItemStack itemStack = ItemStack.EMPTY;
                    if (obj.has("itemStackSnbt")) {
                        byte[] bytes = java.util.Base64.getDecoder().decode(obj.get("itemStackSnbt").getAsString());
                        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(bytes);
                        net.minecraft.nbt.CompoundTag nbt = net.minecraft.nbt.NbtIo.readCompressed(bais, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                        
                        net.minecraft.resources.RegistryOps<net.minecraft.nbt.Tag> ops = savage.chestshops.SavsChestShops.getServer().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                        itemStack = ItemStack.CODEC.parse(ops, nbt).getOrThrow();
                    }

                    ChestShop shop = new ChestShop(globalPos, ownerId, ownerName, itemStack, price, isBuying, isAdmin);
                    shops.put(shop.location(), shop);
                } catch (Exception e) {
                    SavsChestShops.LOGGER.error("Failed to load a shop entry", e);
                }
            }
        } catch (IOException e) {
            SavsChestShops.LOGGER.error("Failed to load shops", e);
        }
    }

    private final java.util.concurrent.ExecutorService ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(Thread.ofVirtual().name("ChestShop-IO").factory());

    public void save() {
        JsonObject root = new JsonObject();
        JsonArray array = new JsonArray();

        for (ChestShop shop : shops.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("worldId", shop.location().dimension().identifier().toString());
            
            JsonObject loc = new JsonObject();
            loc.addProperty("x", shop.location().pos().getX());
            loc.addProperty("y", shop.location().pos().getY());
            loc.addProperty("z", shop.location().pos().getZ());
            obj.add("chestLocation", loc);
            
            obj.addProperty("ownerId", shop.ownerId().toString());
            obj.addProperty("ownerName", shop.ownerName());
            obj.addProperty("type", shop.isAdmin() ? "ADMIN" : "PLAYER");
            obj.addProperty("price", shop.price().toString());
            obj.addProperty("buying", shop.isBuying());
            obj.addProperty("stock", 0); // Logic handled at runtime

            if (!shop.item().isEmpty()) {
                try {
                    net.minecraft.resources.RegistryOps<net.minecraft.nbt.Tag> ops = savage.chestshops.SavsChestShops.getServer().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                    net.minecraft.nbt.CompoundTag nbt = (net.minecraft.nbt.CompoundTag) ItemStack.CODEC.encodeStart(ops, shop.item()).getOrThrow();
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    net.minecraft.nbt.NbtIo.writeCompressed(nbt, baos);
                    obj.addProperty("itemStackSnbt", java.util.Base64.getEncoder().encodeToString(baos.toByteArray()));
                } catch (Exception e) {
                    SavsChestShops.LOGGER.error("Failed to encode item stack", e);
                }
            }
            
            array.add(obj);
        }

        root.add("shops", array);

        ioExecutor.submit(() -> {
            try {
                File tempFile = new File(storageFile.getParentFile(), storageFile.getName() + ".tmp");
                try (FileWriter writer = new FileWriter(tempFile)) {
                    GSON.toJson(root, writer);
                }
                
                if (storageFile.exists() && !storageFile.delete()) {
                    SavsChestShops.LOGGER.warn("Failed to delete old shops file, rename might fail");
                }
                if (!tempFile.renameTo(storageFile)) {
                    SavsChestShops.LOGGER.error("Failed to rename temp shops file");
                }
            } catch (IOException e) {
                SavsChestShops.LOGGER.error("Failed to save shops asynchronously", e);
            }
        });
    }
}
