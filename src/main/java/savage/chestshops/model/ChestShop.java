package savage.chestshops.model;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.UUID;

/**
 * Represents a single Chest Shop in the world.
 */
public class ChestShop {
    private final BlockPos pos;
    private final UUID ownerId;
    private final ItemStack item;
    private final BigInteger price;
    private final boolean isBuying; // true if shop buys (player sells), false if shop sells (player buys)
    private final String worldId;
    private final boolean isAdmin;
    private final String ownerName;

    public ChestShop(BlockPos pos, String worldId, UUID ownerId, String ownerName, ItemStack item, BigInteger price, boolean isBuying, boolean isAdmin) {
        this.pos = pos;
        this.worldId = worldId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.item = item;
        this.price = price;
        this.isBuying = isBuying;
        this.isAdmin = isAdmin;
    }

    public BlockPos getPos() {
        return pos;
    }

    public String getWorldId() {
        return worldId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public ItemStack getItem() {
        return item;
    }

    public BigInteger getPrice() {
        return price;
    }

    public boolean isBuying() {
        return isBuying;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getShopId() {
        return worldId + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
