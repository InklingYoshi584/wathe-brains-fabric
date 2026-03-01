package online.inklingyoshi.brains.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import online.inklingyoshi.brains.ai.AiManager;
import online.inklingyoshi.brains.ai.AiProfile;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Debug commands for WatheBrains AI mod
 */
public class WatheBrainsCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("wathebrains")
            .requires(source -> source.hasPermissionLevel(2)) // OP only
            .then(literal("list")
                .executes(WatheBrainsCommands::listAis))
            .then(literal("spawn")
                .then(literal("killer")
                    .executes(ctx -> spawnAi(ctx, "killer")))
                .then(literal("vigilante")
                    .executes(ctx -> spawnAi(ctx, "vigilante")))
                .then(literal("civilian")
                    .executes(ctx -> spawnAi(ctx, "civilian"))))
            .then(literal("reload")
                .executes(WatheBrainsCommands::reloadConfig))
        );
    }

    private static int listAis(CommandContext<ServerCommandSource> ctx) {
        var source = ctx.getSource();
        var server = source.getServer();
        
        source.sendFeedback(() -> net.minecraft.text.Text.literal("=== WatheBrains AI List ==="), true);
        
        long count = 0;
        for (AiProfile profile : AiManager.getAiProfiles()) {
            String info = String.format("- %s | Role: %s | InGame: %s",
                profile.getPlayer() != null ? profile.getPlayer().getName().getString() : profile.getUuid(),
                profile.getRole(),
                profile.isInGame());
            
            source.sendFeedback(() -> net.minecraft.text.Text.literal(info), true);
            count++;
        }
        
        source.sendFeedback(() -> net.minecraft.text.Text.literal("Total: " + count + " AI(s)"), true);
        return (int) count;
    }

    private static int spawnAi(CommandContext<ServerCommandSource> ctx, String role) {
        var source = ctx.getSource();
        var server = source.getServer();
        
        // Count existing AI of this role
        long existing = AiManager.countByRole(role);
        int index = (int) existing + 1;
        
        // Spawn using Carpet command
        AiManager.spawnAi(server, role, index);
        
        source.sendFeedback(() -> net.minecraft.text.Text.literal("Spawned " + role + " AI (AI_" + role + "_" + index + ")"), true);
        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> ctx) {
        var source = ctx.getSource();
        
        // Reload config (future use)
        source.sendFeedback(() -> net.minecraft.text.Text.literal("Config reloaded (placeholder)"), true);
        return 1;
    }
}
