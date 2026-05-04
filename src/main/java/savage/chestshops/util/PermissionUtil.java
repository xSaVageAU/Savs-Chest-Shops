package savage.chestshops.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

public class PermissionUtil {
    public static boolean isAdmin(ServerPlayer player) {
        return player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_ADMIN);
    }
}
