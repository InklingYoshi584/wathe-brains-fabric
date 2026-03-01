package online.inklingyoshi.brains.ai;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Represents an AI player profile with role, memory, and state tracking.
 */
public class AiProfile {
    private final UUID uuid;
    private final String role; // "killer", "vigilante", "civilian"
    private final AiMemory memory;
    private long lastDecisionTick = 0;
    private String lastTriggerReason = "periodic";
    private boolean importantTriggerPending = false;
    private UUID followTarget = null;
    
    // Psycho chase state (killer only)
    private UUID psychoTarget = null;
    private long psychoTargetStartTick = 0;
    private boolean inPsychoState = false;
    
    // Reference to the fake player entity
    private ServerPlayerEntity player;

    public AiProfile(UUID uuid, String role) {
        this.uuid = uuid;
        this.role = role.toLowerCase();
        this.memory = new AiMemory();
    }

    public UUID getUuid() { return uuid; }
    public String getRole() { return role; }
    public AiMemory getMemory() { return memory; }
    public long getLastDecisionTick() { return lastDecisionTick; }
    public void setLastDecisionTick(long tick) { this.lastDecisionTick = tick; }
    public String getLastTriggerReason() { return lastTriggerReason; }
    public void setLastTriggerReason(String reason) { this.lastTriggerReason = reason; }
    public boolean isImportantTriggerPending() { return importantTriggerPending; }
    public void markImportantTrigger(String reason) {
        this.importantTriggerPending = true;
        this.lastTriggerReason = reason;
    }
    public boolean consumeImportantTrigger() {
        boolean result = importantTriggerPending;
        importantTriggerPending = false;
        return result;
    }
    public UUID getFollowTarget() { return followTarget; }
    public void setFollowTarget(UUID uuid) { this.followTarget = uuid; }
    public void clearFollowTarget() { this.followTarget = null; }
    public ServerPlayerEntity getPlayer() { return player; }
    public void setPlayer(ServerPlayerEntity player) { this.player = player; }
    public boolean isInGame() { return player != null && !player.isDisconnected(); }

    // Psycho state methods (killer only)
    public boolean isInPsychoState() { return inPsychoState; }
    public void enterPsychoState() { this.inPsychoState = true; this.psychoTargetStartTick = 0; }
    public void exitPsychoState() { this.inPsychoState = false; this.psychoTarget = null; this.psychoTargetStartTick = 0; }
    public UUID getPsychoTarget() { return psychoTarget; }
    public void setPsychoTarget(UUID uuid) { this.psychoTarget = uuid; }
    public long getPsychoTargetStartTick() { return psychoTargetStartTick; }
    public void setPsychoTargetStartTick(long tick) { this.psychoTargetStartTick = tick; }

    public boolean failedToReachCurrentTargetFor(double seconds, MinecraftServer server) {
        if (psychoTarget == null || psychoTargetStartTick == 0) return false;
        long ticksElapsed = server.getTicks() - psychoTargetStartTick;
        return ticksElapsed > (seconds * 20);
    }
}
