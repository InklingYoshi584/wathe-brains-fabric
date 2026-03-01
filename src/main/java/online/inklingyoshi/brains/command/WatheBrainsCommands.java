package online.inklingyoshi.brains.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import online.inklingyoshi.brains.ai.AiManager;
import online.inklingyoshi.brains.ai.AiProfile;
import online.inklingyoshi.brains.api.WatheApi;
import online.inklingyoshi.brains.llm.LlmEngine;

import static net.minecraft.server.command.CommandManager.literal;

/**
 * Debug commands for testing AI actions
 */
public class WatheBrainsCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("wathebrains")
            .requires(source -> source.hasPermissionLevel(2))
            
            // List AIs
            .then(literal("list")
                .executes(WatheBrainsCommands::listAis))
            
            // Spawn AI
            .then(literal("spawn")
                .then(literal("killer").executes(ctx -> spawnAi(ctx, "killer")))
                .then(literal("vigilante").executes(ctx -> spawnAi(ctx, "vigilante")))
                .then(literal("civilian").executes(ctx -> spawnAi(ctx, "civilian"))))
            
            // Test actions
            .then(literal("test")
                .then(literal("buy")
                    .then(literal("knife").executes(ctx -> testAction(ctx, "buy_knife")))
                    .then(literal("revolver").executes(ctx -> testAction(ctx, "buy_revolver")))
                    .then(literal("grenade").executes(ctx -> testAction(ctx, "buy_grenade")))
                    .then(literal("poison").executes(ctx -> testAction(ctx, "buy_poison")))
                    .then(literal("psycho").executes(ctx -> testAction(ctx, "buy_psycho"))))
                .then(literal("move")
                    .then(literal("front").executes(ctx -> testAction(ctx, "move_front")))
                    .then(literal("middle").executes(ctx -> testAction(ctx, "move_middle")))
                    .then(literal("back").executes(ctx -> testAction(ctx, "move_back"))))
                .then(literal("kill")
                    .then(literal("knife").executes(ctx -> testAction(ctx, "kill_knife")))
                    .then(literal("grenade").executes(ctx -> testAction(ctx, "kill_grenade"))))
                .then(literal("psycho").executes(ctx -> testAction(ctx, "activate_psycho")))
                .then(literal("poison")
                    .then(literal("food").executes(ctx -> testAction(ctx, "poison_food")))
                    .then(literal("bed").executes(ctx -> testAction(ctx, "poison_bed"))))
                .then(literal("blackout").executes(ctx -> testAction(ctx, "blackout")))
                .then(literal("mood")
                    .then(literal("snack").executes(ctx -> testAction(ctx, "get_snack")))
                    .then(literal("drink").executes(ctx -> testAction(ctx, "get_drink")))
                    .then(literal("air").executes(ctx -> testAction(ctx, "get_air")))
                    .then(literal("sleep").executes(ctx -> testAction(ctx, "get_sleep"))))
                .then(literal("chat")
                    .then(literal("hello").executes(ctx -> testAction(ctx, "chat_hello")))
                    .then(literal("sus").executes(ctx -> testAction(ctx, "chat_sus"))))
                .then(literal("llm")
                    .executes(ctx -> testAction(ctx, "llm_decide"))))
            
            // Get AI status
            .then(literal("status")
                .executes(WatheBrainsCommands::showStatus))
            
            // Give coins
            .then(literal("givecoins")
                .then(literal("100").executes(ctx -> giveCoins(ctx, 100)))
                .then(literal("500").executes(ctx -> giveCoins(ctx, 500)))
                .then(literal("1000").executes(ctx -> giveCoins(ctx, 1000))))
            
            // Reload config
            .then(literal("reload")
                .executes(WatheBrainsCommands::reloadConfig))
        );
    }

    private static int listAis(CommandContext<ServerCommandSource> ctx) {
        var source = ctx.getSource();
        source.sendFeedback(() -> net.minecraft.text.Text.literal("=== WatheBrains AI List ==="), true);
        
        int[] count = {0};
        for (AiProfile profile : AiManager.getAiProfiles()) {
            String info = String.format("- %s | Role: %s | InGame: %s",
                profile.getPlayer() != null ? profile.getPlayer().getName().getString() : profile.getUuid(),
                profile.getRole(),
                profile.isInGame());
            source.sendFeedback(() -> net.minecraft.text.Text.literal(info), true);
            count[0]++;
        }
        source.sendFeedback(() -> net.minecraft.text.Text.literal("Total: " + count[0] + " AI(s)"), true);
        return count[0];
    }

    private static int spawnAi(CommandContext<ServerCommandSource> ctx, String role) {
        var source = ctx.getSource();
        var server = source.getServer();
        
        long existing = AiManager.countByRole(role);
        int index = (int) existing + 1;
        
        AiManager.spawnAi(server, role, index);
        
        source.sendFeedback(() -> net.minecraft.text.Text.literal("Spawned " + role + " AI (AI_" + role + "_" + index + ")"), true);
        return 1;
    }

    private static int testAction(CommandContext<ServerCommandSource> ctx, String action) {
        var source = ctx.getSource();
        var server = source.getServer();
        
        // Get first AI player
        AiProfile ai = null;
        for (AiProfile p : AiManager.getAiProfiles()) {
            if (p.isInGame()) {
                ai = p;
                break;
            }
        }
        
        if (ai == null) {
            source.sendFeedback(() -> net.minecraft.text.Text.literal("§cNo AI found! Spawn one first."), true);
            return 0;
        }
        
        var player = ai.getPlayer();
        source.sendFeedback(() -> net.minecraft.text.Text.literal("§aTesting action: §b" + action + " §rwith §e" + player.getName().getString()), true);
        
        // Execute test action
        switch (action) {
            case "buy_knife" -> {
                WatheApi.buyKnife(player);
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aBought knife!"), true);
            }
            case "buy_revolver" -> {
                WatheApi.buyRevolver(player);
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aBought revolver!"), true);
            }
            case "buy_grenade" -> {
                WatheApi.buyGrenade(player);
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aBought grenade!"), true);
            }
            case "buy_poison" -> {
                WatheApi.buyPoisonVial(player);
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aBought poison vial!"), true);
            }
            case "buy_psycho" -> {
                WatheApi.buyPsychoMode(player);
                ai.enterPsychoState();
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aActivated psycho mode!"), true);
            }
            case "move_front" -> {
                player.requestTeleport(-150, 64, 0);
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aMoved to front!"), true);
            }
            case "move_middle" -> {
                player.requestTeleport(0, 64, 0);
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aMoved to middle!"), true);
            }
            case "move_back" -> {
                player.requestTeleport(150, 64, 0);
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aMoved to back!"), true);
            }
            case "kill_knife" -> {
                // Find nearest non-AI player to kill
                for (var p : server.getPlayerManager().getPlayerList()) {
                    if (!AiManager.isAiName(p.getName().getString()) && WatheApi.isAliveAndSurvival(p)) {
                        WatheApi.knifeKill(p, player);
                        source.sendFeedback(() -> net.minecraft.text.Text.literal("§aKnifed " + p.getName().getString() + "!"), true);
                        break;
                    }
                }
            }
            case "kill_grenade" -> {
                for (var p : server.getPlayerManager().getPlayerList()) {
                    if (!AiManager.isAiName(p.getName().getString()) && WatheApi.isAliveAndSurvival(p)) {
                        WatheApi.grenadeKill(p, player);
                        source.sendFeedback(() -> net.minecraft.text.Text.literal("§aGrenaded " + p.getName().getString() + "!"), true);
                        break;
                    }
                }
            }
            case "activate_psycho" -> {
                WatheApi.buyPsychoMode(player);
                ai.enterPsychoState();
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aActivated psycho!"), true);
            }
            case "poison_food" -> {
                var pos = WatheApi.findNearestFoodPlatter(player);
                if (pos != null) {
                    player.requestTeleport(pos.getX(), pos.getY(), pos.getZ());
                    ai.getMemory().markPoisoned(pos.toString());
                    source.sendFeedback(() -> net.minecraft.text.Text.literal("§aPoisoned food at " + pos + "!"), true);
                } else {
                    source.sendFeedback(() -> net.minecraft.text.Text.literal("§cNo food platter found!"), true);
                }
            }
            case "poison_bed" -> {
                var pos = WatheApi.findNearestBed(player);
                if (pos != null) {
                    player.requestTeleport(pos.getX(), pos.getY(), pos.getZ());
                    ai.getMemory().markPoisoned("bed:" + pos);
                    source.sendFeedback(() -> net.minecraft.text.Text.literal("§aPlaced scorpion on bed at " + pos + "!"), true);
                } else {
                    source.sendFeedback(() -> net.minecraft.text.Text.literal("§cNo bed found!"), true);
                }
            }
            case "blackout" -> {
                WatheApi.buyBlackout(player);
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aUsed blackout!"), true);
            }
            case "get_snack" -> {
                var pos = WatheApi.findNearestFoodPlatter(player);
                if (pos != null) {
                    player.requestTeleport(pos.getX(), pos.getY(), pos.getZ());
                    source.sendFeedback(() -> net.minecraft.text.Text.literal("§aWent to snack!"), true);
                }
            }
            case "get_drink" -> {
                var pos = WatheApi.findNearestDrinkTray(player);
                if (pos != null) {
                    player.requestTeleport(pos.getX(), pos.getY(), pos.getZ());
                    source.sendFeedback(() -> net.minecraft.text.Text.literal("§aWent to drink!"), true);
                }
            }
            case "get_air" -> {
                String region = WatheApi.getPlayerRegion(player);
                double x = region.equals("front") ? 150 : -150;
                player.requestTeleport(x, 64, 0);
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aWent to fresh air!"), true);
            }
            case "get_sleep" -> {
                var pos = WatheApi.findNearestBed(player);
                if (pos != null) {
                    player.requestTeleport(pos.getX(), pos.getY(), pos.getZ());
                    source.sendFeedback(() -> net.minecraft.text.Text.literal("§aWent to bed!"), true);
                }
            }
            case "chat_hello" -> {
                // Send chat message - simplified
                player.getServer().getPlayerManager().getPlayerList().forEach(p -> 
                    p.sendMessage(net.minecraft.text.Text.literal("Hello everyone!"))
                );
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aAI said hello!"), true);
            }
            case "chat_sus" -> {
                player.getServer().getPlayerManager().getPlayerList().forEach(p -> 
                    p.sendMessage(net.minecraft.text.Text.literal("That guy looks suspicious..."))
                );
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aAI said someone is sus!"), true);
            }
            case "llm_decide" -> {
                // Trigger LLM decision manually
                var state = online.inklingyoshi.brains.gamestate.GameStateBuilder.buildFor(ai, server, "manual_test");
                var decision = LlmEngine.decideAction(state, ai);
                source.sendFeedback(() -> net.minecraft.text.Text.literal("§aLLM Decision: §e" + decision.action), true);
            }
        }
        
        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> ctx) {
        var source = ctx.getSource();
        
        for (AiProfile profile : AiManager.getAiProfiles()) {
            if (!profile.isInGame()) continue;
            
            var player = profile.getPlayer();
            String status = String.format("""
                §6=== %s (AI) ===
                Role: %s
                Health: %.1f
                Coins: %d
                Mood: %.2f
                Psycho: %s
                Items: Knife=%s Revolver=%s Grenade=%s
                """,
                player.getName().getString(),
                profile.getRole(),
                player.getHealth(),
                WatheApi.getCoins(player),
                WatheApi.getMood(player),
                WatheApi.isInPsychoMode(player),
                WatheApi.hasKnife(player),
                WatheApi.hasRevolver(player),
                WatheApi.hasGrenade(player)
            );
            
            source.sendFeedback(() -> net.minecraft.text.Text.literal(status), true);
        }
        
        return 1;
    }

    private static int giveCoins(CommandContext<ServerCommandSource> ctx, int amount) {
        var source = ctx.getSource();
        
        for (AiProfile profile : AiManager.getAiProfiles()) {
            if (!profile.isInGame()) continue;
            
            var player = profile.getPlayer();
            var shop = dev.doctor4t.wathe.cca.PlayerShopComponent.KEY.get(player);
            shop.addToBalance(amount);
            source.sendFeedback(() -> net.minecraft.text.Text.literal("§aGave " + amount + " coins to " + player.getName().getString()), true);
        }
        
        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> net.minecraft.text.Text.literal("Config reloaded (placeholder)"), true);
        return 1;
    }
}
