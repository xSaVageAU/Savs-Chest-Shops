package savage.chestshops;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import savage.chestshops.command.ShopCommands;
import savage.chestshops.event.ChatEventHandler;
import savage.chestshops.event.InteractionHandler;
import savage.chestshops.event.ProtectionHandler;
import savage.chestshops.event.CreationHandler;
import savage.chestshops.registry.ShopRegistry;

public class SavsChestShops implements ModInitializer {
	public static final String MOD_ID = "savs-chest-shops";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static net.minecraft.server.MinecraftServer server;

	public static net.minecraft.server.MinecraftServer getServer() {
		return server;
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Sav's Chest Shops...");

		// Capture Server Instance
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(s -> server = s);

		// Load Configuration
		savage.chestshops.config.ModConfig.load();

		// Load shops from disk
		ShopRegistry.getInstance().load();

		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ShopCommands.register(dispatcher);
		});

		// Register event handlers
		ProtectionHandler.register();
		InteractionHandler.register();
		ChatEventHandler.register();
		CreationHandler.register();

		// Automatic Sign Updates and Cleanup
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			// Dirty Shop Sign Updates (every 1 second)
			if (server.getTickCount() % 20 == 0) {
				java.util.Set<String> dirty = ShopRegistry.getInstance().consumeDirtyShops();
				if (!dirty.isEmpty()) {
					for (net.minecraft.server.level.ServerLevel world : server.getAllLevels()) {
						String worldId = world.dimension().identifier().toString();
						for (String shopId : dirty) {
							// Check if this shop belongs to this world
							if (shopId.startsWith(worldId + ":")) {
								String[] parts = shopId.substring(worldId.length() + 1).split(",");
								net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(
									Integer.parseInt(parts[0]),
									Integer.parseInt(parts[1]),
									Integer.parseInt(parts[2])
								);
								savage.chestshops.model.ChestShop shop = ShopRegistry.getInstance().getShop(pos, worldId);
								if (shop != null) {
									net.minecraft.core.BlockPos signPos = savage.chestshops.util.SignUtil.findSignForChest(world, pos);
									if (signPos != null) {
										savage.chestshops.util.SignUtil.updateSign(world, signPos, shop);
									}
								}
							}
						}
					}
					// Persist any runtime changes
					ShopRegistry.getInstance().save();
				}
			}

			// Orphan Shop Cleanup (every 5 seconds)
			if (server.getTickCount() % 100 == 0) {
				java.util.List<savage.chestshops.model.ChestShop> toRemove = new java.util.ArrayList<>();
				for (net.minecraft.server.level.ServerLevel world : server.getAllLevels()) {
					String worldId = world.dimension().identifier().toString();
					for (savage.chestshops.model.ChestShop shop : ShopRegistry.getInstance().getAllShops()) {
						if (worldId.equals(shop.getWorldId())) {
							if (!(world.getBlockState(shop.getPos()).getBlock() instanceof net.minecraft.world.level.block.ChestBlock)) {
								toRemove.add(shop);
							}
						}
					}
				}
				for (savage.chestshops.model.ChestShop shop : toRemove) {
					ShopRegistry.getInstance().removeShop(shop.getPos(), shop.getWorldId());
				}
			}
		});

		LOGGER.info("Sav's Chest Shops initialized!");
	}
}