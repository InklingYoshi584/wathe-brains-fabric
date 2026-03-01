package online.inklingyoshi.brains.ai;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all AI profiles and tracks AI fake players.
 */
public class AiManager {
    private static final Map<UUID, AiProfile> AI_PROFILES = new ConcurrentHashMap<>();
    public static final String AI_NAME_PREFIX = "AI_";
    public static final String KILLER_PREFIX = AI_NAME_PREFIX + "Killer_";
    public static final String VIGILANTE_PREFIX = AI_NAME_PREFIX + "Vigilante_";
    public static final String CIVILIAN_PREFIX = AI_NAME_PREFIX + "Civilian_";

    static {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onPlayerJoin(handler));
    }

    private static void onPlayerJoin(ServerPlayNetworkHandler handler) {
        String playerName = handler.getPlayer().getName().getString();
        
        if (playerName.startsWith(KILLER_PREFIX)) {
            AiProfile profile = new AiProfile(handler.getPlayer().getUuid(), "killer");
            profile.setPlayer(handler.getPlayer());
            AI_PROFILES.put(handler.getPlayer().getUuid(), profile);
        } else if (playerName.startsWith(VIGILANTE_PREFIX)) {
            AiProfile profile = new AiProfile(handler.getPlayer().getUuid(), "vigilante");
            profile.setPlayer(handler.getPlayer());
            AI_PROFILES.put(handler.getPlayer().getUuid(), profile);
        } else if (playerName.startsWith(CIVILIAN_PREFIX)) {
            AiProfile profile = new AiProfile(handler.getPlayer().getUuid(), "civilian");
            profile.setPlayer(handler.getPlayer());
            AI_PROFILES.put(handler.getPlayer().getUuid(), profile);
        }
    }

    public static Collection<AiProfile> getAiProfiles() { return AI_PROFILES.values(); }
    public static Optional<AiProfile> getProfile(UUID uuid) { return Optional.ofNullable(AI_PROFILES.get(uuid)); }
    public static Optional<AiProfile> getProfileByName(String name) {
        for (AiProfile profile : AI_PROFILES.values()) {
            if (profile.getPlayer() != null && profile.getPlayer().getName().getString().equals(name)) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    public static AiProfile registerAi(UUID uuid, String role, ServerPlayerEntity player) {
        AiProfile profile = new AiProfile(uuid, role);
        profile.setPlayer(player);
        AI_PROFILES.put(uuid, profile);
        return profile;
    }

    public static void removeProfile(UUID uuid) { AI_PROFILES.remove(uuid); }

    public static void spawnAi(MinecraftServer server, String role, int index) {
        String name;
        switch (role.toLowerCase()) {
            case "killer" -> name = KILLER_PREFIX + index;
            case "vigilante" -> name = VIGILANTE_PREFIX + index;
            case "civilian" -> name = CIVILIAN_PREFIX + index;
            default -> name = null;
        }
        
        if (name == null) return;
        
        var commandSource = server.getCommandSource();
        server.getCommandManager().executeWithPrefix(commandSource, "player " + name + " spawn");
    }

    public static long countByRole(String role) {
        return AI_PROFILES.values().stream().filter(p -> p.getRole().equalsIgnoreCase(role)).count();
    }

    public static boolean isAiName(String name) { return name.startsWith(AI_NAME_PREFIX); }
}
