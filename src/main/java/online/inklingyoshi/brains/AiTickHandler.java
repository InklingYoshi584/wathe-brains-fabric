package online.inklingyoshi.brains;

import online.inklingyoshi.brains.action.AiActionExecutor;
import online.inklingyoshi.brains.ai.AiManager;
import online.inklingyoshi.brains.ai.AiProfile;
import online.inklingyoshi.brains.gamestate.GameState;
import online.inklingyoshi.brains.gamestate.GameStateBuilder;
import online.inklingyoshi.brains.llm.LlmEngine;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.server.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Collection;

/**
 * Main AI tick loop - runs every server tick to make decisions and execute actions.
 */
public class AiTickHandler {

    // Decision interval in ticks (~5 seconds at 20 tps)
    private static final long DECISION_INTERVAL_TICKS = 100;
    
    // Max time in seconds before switching psycho target
    private static final double PSYCHO_TARGET_TIMEOUT_SEC = 5.0;

    /**
     * Register the tick handler
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AiTickHandler::onServerTick);
        
        // Register chat listener
        // TODO: Listen for messages mentioning AI names
        // ServerPlayNetworking.registerReceiver(...);
    }

    private static void onServerTick(MinecraftServer server) {
        long tick = server.getTicks();
        
        // Process each AI
        Collection<AiProfile> profiles = AiManager.getAiProfiles();
        
        for (AiProfile ai : profiles) {
            if (!ai.isInGame()) continue;
            
            // Check if decision is due
            boolean duePeriodic = ai.getLastDecisionTick() == 0 || 
                tick - ai.getLastDecisionTick() >= DECISION_INTERVAL_TICKS;
            boolean important = ai.consumeImportantTrigger();
            
            if (!duePeriodic && !important) {
                // Still handle psycho chase behavior for killers
                if (ai.getRole().equals("killer") && ai.isInPsychoState()) {
                    tickPsychoChase(ai, server, tick);
                }
                continue;
            }
            
            // Determine trigger reason
            String triggerReason = important ? ai.getLastTriggerReason() : "periodic";
            
            // Build game state
            GameState state = GameStateBuilder.buildFor(ai, server, triggerReason);
            
            // Get decision from LLM
            LlmEngine.AiDecision decision = LlmEngine.decideAction(state, ai);
            
            // Execute the decision
            AiActionExecutor.apply(decision, ai, server);
            
            // Update last decision tick
            ai.setLastDecisionTick(tick);
            
            // Tick psycho chase behavior for killers
            if (ai.getRole().equals("killer") && ai.isInPsychoState()) {
                tickPsychoChase(ai, server, tick);
            }
        }
    }

    /**
     * Handle psycho mode chase behavior for killers
     */
    private static void tickPsychoChase(AiProfile ai, MinecraftServer server, long tick) {
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null) return;
        
        // Check if we need a new target
        if (ai.getPsychoTarget() == null) {
            // Find nearest player
            ServerPlayerEntity nearest = findNearestPlayer(player, server);
            if (nearest != null) {
                ai.setPsychoTarget(nearest.getUuid());
                ai.setPsychoTargetStartTick(tick);
            }
        } else {
            // Check if target is still valid/alive
            ServerPlayerEntity target = findPlayerByUuid(ai.getPsychoTarget(), server);
            if (target == null || target.isDead()) {
                // Find new target
                ai.setPsychoTarget(null);
                ai.setPsychoTargetStartTick(0);
                return;
            }
            
            // Check if failed to reach target
            if (ai.failedToReachCurrentTargetFor(PSYCHO_TARGET_TIMEOUT_SEC, server)) {
                // Switch to new target
                System.out.println("[WatheBrains] Psycho AI switching targets (timeout)");
                ai.setPsychoTarget(null);
                ai.setPsychoTargetStartTick(0);
                return;
            }
            
            // Chase the target - move toward them
            Vec3d targetPos = target.getPos();
            Vec3d playerPos = player.getPos();
            Vec3d direction = targetPos.subtract(playerPos).normalize();
            
            // Simple move toward (needs proper pathfinding)
            double moveSpeed = 0.5;
            // player.setVelocity(direction.x * moveSpeed, direction.y, direction.z * moveSpeed);
            
            // If close enough, attack
            double distance = playerPos.distanceTo(targetPos);
            if (distance < 2.0) {
                // TODO: Bat attack - use PlayerPsychoComponent.batAttack()
                System.out.println("[WatheBrains] Psycho AI attacking " + target.getName().getString());
            }
        }
    }

    private static ServerPlayerEntity findNearestPlayer(ServerPlayerEntity self, MinecraftServer server) {
        ServerPlayerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            if (other.getUuid().equals(self.getUuid())) continue;
            if (other.isDead()) continue;
            
            double dist = self.getPos().distanceTo(other.getPos());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = other;
            }
        }
        
        return nearest;
    }

    private static ServerPlayerEntity findPlayerByUuid(java.util.UUID uuid, MinecraftServer server) {
        return server.getPlayerManager().getPlayer(uuid);
    }
}
