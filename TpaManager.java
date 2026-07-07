package com.yourserver.rankmod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks pending /tpa requests. This is intentionally in-memory only
 * (not saved to disk) - a request shouldn't survive a server restart.
 */
public class TpaManager {

    // requester UUID -> target UUID
    private static final Map<UUID, UUID> PENDING = new HashMap<>();

    public static void request(UUID requester, UUID target) {
        PENDING.put(requester, target);
    }

    /** Returns the requester who is waiting on `target` to accept, or null. */
    public static UUID findRequesterFor(UUID target) {
        for (Map.Entry<UUID, UUID> entry : PENDING.entrySet()) {
            if (entry.getValue().equals(target)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static void clear(UUID requester) {
        PENDING.remove(requester);
    }
}
