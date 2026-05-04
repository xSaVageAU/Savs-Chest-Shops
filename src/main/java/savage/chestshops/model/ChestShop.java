package savage.chestshops.model;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.item.ItemStack;

import java.math.BigInteger;
import java.util.UUID;

/**
 * Represents a single Chest Shop in the world.
 */
public record ChestShop(
    GlobalPos location,
    UUID ownerId,
    String ownerName,
    ItemStack item,
    BigInteger price,
    boolean isBuying,
    boolean isAdmin
) {
}
