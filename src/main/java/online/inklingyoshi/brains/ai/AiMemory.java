package online.inklingyoshi.brains.ai;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side memory for one AI player.
 */
public class AiMemory {
    private String summary = "";
    private final Map<UUID, BodyMemoryEntry> bodiesSeen = new ConcurrentHashMap<>();
    private final Map<UUID, SpokeToMeEntry> peopleWhoSpokeToMe = new ConcurrentHashMap<>();
    private final Map<String, Boolean> knownPoisonedLocations = new ConcurrentHashMap<>();

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Map<UUID, BodyMemoryEntry> getBodiesSeen() { return bodiesSeen; }
    public void addBody(UUID bodyId, String car, long tickSeen) { bodiesSeen.put(bodyId, new BodyMemoryEntry(bodyId, car, tickSeen)); }
    public Map<UUID, SpokeToMeEntry> getPeopleWhoSpokeToMe() { return peopleWhoSpokeToMe; }
    public void addSpokeToMe(UUID playerId, String message, long tick) { peopleWhoSpokeToMe.put(playerId, new SpokeToMeEntry(playerId, message, tick)); }
    public Map<String, Boolean> getKnownPoisonedLocations() { return knownPoisonedLocations; }
    public void markPoisoned(String locationKey) { knownPoisonedLocations.put(locationKey, true); }
    public boolean isLocationPoisoned(String locationKey) { return knownPoisonedLocations.getOrDefault(locationKey, false); }

    public void pruneOldMemories(long currentTick, long maxAgeTicks) {
        bodiesSeen.entrySet().removeIf(entry -> currentTick - entry.getValue().tickFirstSeen > maxAgeTicks);
        peopleWhoSpokeToMe.entrySet().removeIf(entry -> currentTick - entry.getValue().tickLastSpoke > maxAgeTicks);
    }

    public static class BodyMemoryEntry {
        public final UUID victimId;
        public final String car;
        public final long tickFirstSeen;
        public boolean stillPresent = true;

        public BodyMemoryEntry(UUID victimId, String car, long tickFirstSeen) {
            this.victimId = victimId;
            this.car = car;
            this.tickFirstSeen = tickFirstSeen;
        }
    }

    public static class SpokeToMeEntry {
        public final UUID playerId;
        public String lastMessage;
        public long tickLastSpoke;

        public SpokeToMeEntry(UUID playerId, String message, long tick) {
            this.playerId = playerId;
            this.lastMessage = message;
            this.tickLastSpoke = tick;
        }
    }
}
