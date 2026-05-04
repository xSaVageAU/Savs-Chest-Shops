package savage.chestshops.registry;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    private final Map<String, ChestShop> shops = new ConcurrentHashMap<>();
    private final java.util.Set<String> dirtyShops = ConcurrentHashMap.newKeySet();
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
        shops.put(shop.getShopId(), shop);
        save();
    }

    public void removeShop(BlockPos pos, String worldId) {
        shops.remove(worldId + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
        save();
    }

    public ChestShop getShop(BlockPos pos, String worldId) {
        return shops.get(worldId + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
    }

    public boolean isShop(BlockPos pos, String worldId) {
        return shops.containsKey(worldId + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
    }

    public void markDirty(BlockPos pos, String worldId) {
        dirtyShops.add(worldId + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
    }

    public java.util.Set<String> consumeDirtyShops() {
        java.util.Set<String> consumed = new java.util.HashSet<>(dirtyShops);
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

                    ChestShop shop = new ChestShop(pos, worldId, ownerId, ownerName, itemStack, price, isBuying, isAdmin);
                    shops.put(shop.getShopId(), shop);
                } catch (Exception e) {
                    SavsChestShops.LOGGER.error("Failed to load a shop entry", e);
                }
            }
        } catch (IOException e) {
            SavsChestShops.LOGGER.error("Failed to load shops", e);
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(storageFile)) {
            JsonObject root = new JsonObject();
            JsonArray array = new JsonArray();

            for (ChestShop shop : shops.values()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("shopId", shop.getShopId());
                obj.addProperty("worldId", shop.getWorldId());
                
                JsonObject loc = new JsonObject();
                loc.addProperty("x", shop.getPos().getX());
                loc.addProperty("y", shop.getPos().getY());
                loc.addProperty("z", shop.getPos().getZ());
                obj.add("chestLocation", loc);
                
                obj.addProperty("ownerId", shop.getOwnerId().toString());
                obj.addProperty("ownerName", shop.getOwnerName());
                obj.addProperty("type", shop.isAdmin() ? "ADMIN" : "PLAYER");
                obj.addProperty("price", shop.getPrice().toString());
                obj.addProperty("buying", shop.isBuying());
                obj.addProperty("stock", 0); // Logic handled at runtime

                if (!shop.getItem().isEmpty()) {
                    try {
                        net.minecraft.resources.RegistryOps<net.minecraft.nbt.Tag> ops = savage.chestshops.SavsChestShops.getServer().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
                        net.minecraft.nbt.CompoundTag nbt = (net.minecraft.nbt.CompoundTag) ItemStack.CODEC.encodeStart(ops, shop.getItem()).getOrThrow();
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
            GSON.toJson(root, writer);
        } catch (IOException e) {
            SavsChestShops.LOGGER.error("Failed to save shops", e);
        }
    }
}
