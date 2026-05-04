package savage.chestshops.util;

import net.minecraft.server.level.ServerPlayer;
import me.lucko.fabric.api.permissions.v0.Permissions;

public class PermissionUtil {
    public static boolean isAdmin(ServerPlayer player) {
        return Permissions.check(player, "savschestshops.admin", 2);
    }
}
