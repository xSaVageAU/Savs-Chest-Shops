package savage.chestshops.util;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Utility functions for inventory manipulation.
 */
public class InventoryHelper {

    /**
     * Counts how many items of the given template are in the container.
     */
    public static int countItems(Container container, ItemStack template) {
        int count = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, template)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Removes a specific amount of items from the container.
     * Returns true if the full amount was removed.
     */
    public static boolean removeItems(Container container, ItemStack template, int amount) {
        if (countItems(container, template) < amount) return false;

        for (int i = 0; i < container.getContainerSize() && amount > 0; i++) {
            ItemStack stack = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, template)) {
                int take = Math.min(amount, stack.getCount());
                stack.shrink(take);
                amount -= take;
            }
        }
        container.setChanged();
        return amount == 0;
    }

    /**
     * Adds items to the container. Returns true if all items were successfully added.
     */
    public static boolean addItems(Container container, ItemStack stack) {
        ItemStack toAdd = stack.copy();
        
        // 1. Try to stack with existing items
        for (int i = 0; i < container.getContainerSize() && !toAdd.isEmpty(); i++) {
            ItemStack existing = container.getItem(i);
            if (ItemStack.isSameItemSameComponents(existing, toAdd)) {
                int canFit = existing.getMaxStackSize() - existing.getCount();
                int transfer = Math.min(canFit, toAdd.getCount());
                if (transfer > 0) {
                    existing.grow(transfer);
                    toAdd.shrink(transfer);
                }
            }
        }

        // 2. Try to fill empty slots
        for (int i = 0; i < container.getContainerSize() && !toAdd.isEmpty(); i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, toAdd.copy());
                toAdd.setCount(0);
            }
        }

        if (toAdd.isEmpty()) {
            container.setChanged();
            return true;
        }
        return false;
    }

    /**
     * Specialized version for players to handle inventory vs hand or other specific player logic.
     */
    public static boolean addItemsToPlayer(Player player, ItemStack stack) {
        if (player.getInventory().add(stack.copy())) {
            return true;
        }
        return false;
    }

    /**
     * Calculates how many more items of the given template can fit into the container.
     */
    public static int calculateSpace(Container container, ItemStack template) {
        int space = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                space += template.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(stack, template)) {
                space += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            }
        }
        return space;
    }
}
