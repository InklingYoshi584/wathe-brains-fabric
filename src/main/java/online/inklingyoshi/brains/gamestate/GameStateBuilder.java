package online.inklingyoshi.brains.gamestate;

import online.inklingyoshi.brains.ai.AiMemory;
import online.inklingyoshi.brains.ai.AiProfile;
import online.inklingyoshi.brains.api.WatheApi;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds GameState JSON snapshots for the LLM using Wathe API.
 */
public class GameStateBuilder {

    public static GameState buildFor(AiProfile ai, MinecraftServer server, String triggerReason) {
        GameState state = new GameState();
        ServerPlayerEntity self = ai.getPlayer();
        
        if (self == null) return state;

        state.self = buildSelfInfo(ai, self);
        state.players = buildPlayerList(ai, self, server);
        state.bodiesSeen = buildBodiesSeen(ai, server);
        state.peopleWhoSpokeToMe = buildPeopleWhoSpokeToMe(ai, server);
        state.trigger = triggerReason;
        state.memorySummary = ai.getMemory().getSummary();
        
        return state;
    }

    private static GameState.SelfInfo buildSelfInfo(AiProfile ai, ServerPlayerEntity self) {
        GameState.SelfInfo info = new GameState.SelfInfo();
        
        info.name = self.getName().getString();
        info.role = ai.getRole();
        info.health = self.getHealth();
        info.coins = WatheApi.getCoins(self);
        info.car = WatheApi.getPlayerRegion(self);
        info.region = info.car;
        info.position = new GameState.Position();
        Vec3d pos = self.getPos();
        info.position.x = pos.x;
        info.position.y = pos.y;
        info.position.z = pos.z;
        
        if (!ai.getRole().equals("killer")) {
            info.mood = (double) WatheApi.getMood(self);
            info.moodPrompt = getMoodPrompt(self);
            info.roomId = null;
            info.inOwnRoom = false;
        }
        
        if (ai.getRole().equals("killer")) {
            info.psycho = new GameState.PsychoStatus();
            info.psycho.active = WatheApi.isInPsychoMode(self);
            info.psycho.remainingSec = WatheApi.getPsychoRemainingSec(self);
            info.psycho.cooldownSec = 0;
        }
        
        info.inventory = buildInventoryInfo(self);
        
        return info;
    }

    private static String getMoodPrompt(ServerPlayerEntity player) {
        PlayerMoodComponent mood = PlayerMoodComponent.KEY.get(player);
        
        if (mood.tasks.containsKey(PlayerMoodComponent.Task.EAT)) return "snack";
        if (mood.tasks.containsKey(PlayerMoodComponent.Task.DRINK)) return "drink";
        if (mood.tasks.containsKey(PlayerMoodComponent.Task.SLEEP)) return "sleep";
        if (mood.tasks.containsKey(PlayerMoodComponent.Task.OUTSIDE)) return "fresh_air";
        
        float moodValue = WatheApi.getMood(player);
        if (moodValue < 0.3f) return null;
        
        return null;
    }

    private static GameState.InventoryInfo buildInventoryInfo(ServerPlayerEntity player) {
        GameState.InventoryInfo info = new GameState.InventoryInfo();
        
        info.hasKnife = WatheApi.hasKnife(player);
        info.hasRevolver = WatheApi.hasRevolver(player);
        info.hasGrenade = WatheApi.hasGrenade(player);
        info.hasLockpick = WatheApi.hasLockpick(player);
        info.hasPoisonVial = WatheApi.hasPoisonVial(player);
        info.hasScorpion = WatheApi.hasScorpion(player);
        info.hasBodyBag = WatheApi.hasBodyBag(player);
        info.hasBlackout = WatheApi.hasBlackout(player);
        
        return info;
    }

    private static List<GameState.PlayerInfo> buildPlayerList(AiProfile ai, ServerPlayerEntity self, MinecraftServer server) {
        List<GameState.PlayerInfo> players = new ArrayList<>();
        var playersList = server.getPlayerManager().getPlayerList();
        Vec3d selfPos = self.getPos();
        
        boolean isKiller = ai.getRole().equals("killer");
        
        for (ServerPlayerEntity other : playersList) {
            if (other.getUuid().equals(self.getUuid())) continue;
            
            GameState.PlayerInfo info = new GameState.PlayerInfo();
            info.name = other.getName().getString();
            info.isSelf = false;
            info.alive = WatheApi.isAliveAndSurvival(other);
            
            double distance = selfPos.distanceTo(other.getPos());
            info.distance = distance;
            
            boolean inLos = hasLineOfSight(self, other);
            
            if (isKiller) {
                info.inLineOfSight = inLos;
                info.car = WatheApi.getPlayerRegion(other);
                info.region = info.car;
                info.armed = WatheApi.hasRevolver(other) || WatheApi.hasKnife(other);
                info.suspiciousScore = calculateSuspicion(other);
            } else {
                info.inLineOfSight = inLos;
                if (inLos) {
                    info.car = WatheApi.getPlayerRegion(other);
                    info.region = info.car;
                    info.armed = WatheApi.hasRevolver(other) || WatheApi.hasKnife(other);
                } else {
                    info.car = null;
                    info.region = null;
                    info.armed = false;
                }
                info.suspiciousScore = null;
            }
            
            players.add(info);
        }
        
        return players;
    }

    private static List<GameState.BodyInfo> buildBodiesSeen(AiProfile ai, MinecraftServer server) {
        List<GameState.BodyInfo> bodies = new ArrayList<>();
        long currentTick = server.getTicks();
        
        for (var entry : ai.getMemory().getBodiesSeen().entrySet()) {
            AiMemory.BodyMemoryEntry body = entry.getValue();
            GameState.BodyInfo info = new GameState.BodyInfo();
            info.victim = body.victimId.toString();
            info.car = body.car;
            info.timeFirstSeenSec = (currentTick - body.tickFirstSeen) / 20.0;
            info.stillPresent = body.stillPresent;
            bodies.add(info);
        }
        
        return bodies;
    }

    private static java.util.Map<String, String> buildPeopleWhoSpokeToMe(AiProfile ai, MinecraftServer server) {
        java.util.Map<String, String> speakers = new java.util.HashMap<>();
        long currentTick = server.getTicks();
        
        for (var entry : ai.getMemory().getPeopleWhoSpokeToMe().entrySet()) {
            AiMemory.SpokeToMeEntry spoke = entry.getValue();
            String name = "unknown";
            long ticksAgo = currentTick - spoke.tickLastSpoke;
            speakers.put(name, spoke.lastMessage + " (T-" + (ticksAgo / 20.0) + "s)");
        }
        
        return speakers;
    }

    private static boolean hasLineOfSight(ServerPlayerEntity self, ServerPlayerEntity other) {
        double distance = self.getPos().distanceTo(other.getPos());
        if (distance > 30) return false;
        
        String selfRegion = WatheApi.getPlayerRegion(self);
        String otherRegion = WatheApi.getPlayerRegion(other);
        
        return selfRegion.equals(otherRegion);
    }

    private static Double calculateSuspicion(ServerPlayerEntity player) {
        double suspicion = 0.0;
        
        if (WatheApi.hasRevolver(player)) suspicion += 0.3;
        if (WatheApi.hasKnife(player)) suspicion += 0.2;
        if (WatheApi.hasGrenade(player)) suspicion += 0.2;
        if (WatheApi.hasPoisonVial(player)) suspicion += 0.1;
        if (WatheApi.hasScorpion(player)) suspicion += 0.1;
        
        return suspicion;
    }
}
