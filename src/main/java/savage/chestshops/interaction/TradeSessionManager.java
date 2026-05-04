package savage.chestshops.interaction;

import savage.chestshops.model.ChestShop;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active trade sessions where players are inputting amounts.
 */
public class TradeSessionManager {
    private final Map<UUID, PendingTrade> activeSessions = new ConcurrentHashMap<>();

    private static class Holder {
        static final TradeSessionManager INSTANCE = new TradeSessionManager();
    }

    public static TradeSessionManager getInstance() {
        return Holder.INSTANCE;
    }

    public void startSession(UUID playerId, ChestShop shop) {
        activeSessions.put(playerId, new PendingTrade(shop));
    }

    public PendingTrade getSession(UUID playerId) {
        PendingTrade session = activeSessions.get(playerId);
        if (session != null && session.isExpired()) {
            activeSessions.remove(playerId);
            return null;
        }
        return session;
    }

    public void endSession(UUID playerId) {
        activeSessions.remove(playerId);
    }

    public void cleanupExpiredSessions() {
        activeSessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public static class PendingTrade {
        private final ChestShop shop;
        private final long startTime;

        public PendingTrade(ChestShop shop) {
            this.shop = shop;
            this.startTime = System.currentTimeMillis();
        }

        public ChestShop getShop() {
            return shop;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > 30000; // 30 seconds expiry
        }
    }
}
