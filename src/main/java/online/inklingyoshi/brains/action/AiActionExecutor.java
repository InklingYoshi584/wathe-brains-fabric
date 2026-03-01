package online.inklingyoshi.brains.action;

import online.inklingyoshi.brains.ai.AiProfile;
import online.inklingyoshi.brains.api.WatheApi;
import online.inklingyoshi.brains.llm.LlmEngine;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Executes AI decisions in the game world using Wathe API.
 */
public class AiActionExecutor {

    public static void apply(LlmEngine.AiDecision decision, AiProfile ai, MinecraftServer server) {
        if (decision == null) return;
        
        String role = ai.getRole().toLowerCase();
        ServerPlayerEntity self = ai.getPlayer();
        
        if (self == null) return;
        
        switch (decision.action) {
            case "move_to" -> handleMoveTo(decision, ai, server);
            case "chat" -> handleChat(decision, ai);
            case "follow_player" -> handleFollow(decision, ai, server);
            case "get_snack" -> handleGetSnack(ai);
            case "get_drink" -> handleGetDrink(ai);
            case "get_air" -> handleGetAir(ai, server);
            case "get_sleep" -> handleGetSleep(ai, server);
            case "buy_item" -> handleBuyItem(decision, ai);
            case "knife_kill" -> handleKnifeKill(ai, server, decision.target_player);
            case "grenade_kill" -> handleGrenadeKill(ai, decision.target_player);
            case "activate_psycho" -> handleActivatePsycho(ai, server);
            case "poison_food" -> handlePoisonFood(ai);
            case "poison_bed" -> handlePoisonBed(ai);
            case "blackout" -> handleBlackout(ai);
            case "do_nothing" -> { }
            default -> System.err.println("[WatheBrains] Unknown action: " + decision.action);
        }
    }

    private static void handleMoveTo(LlmEngine.AiDecision decision, AiProfile ai, MinecraftServer server) {
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null || decision.target_location == null) return;
        
        Vec3d targetPos = getLocationCoords(decision.target_location, server);
        if (targetPos != null) {
            player.requestTeleport(targetPos.x, targetPos.y, targetPos.z);
            System.out.println("[WatheBrains] AI " + player.getName().getString() + " moving to: " + decision.target_location);
        }
    }

    private static Vec3d getLocationCoords(String location, MinecraftServer server) {
        return switch (location.toLowerCase()) {
            case "front", "car_1" -> new Vec3d(-150, 64, 0);
            case "middle", "car_2" -> new Vec3d(0, 64, 0);
            case "back", "car_3" -> new Vec3d(150, 64, 0);
            default -> null;
        };
    }

    private static void handleChat(LlmEngine.AiDecision decision, AiProfile ai) {
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null || decision.message == null) return;
        
        player.getServer().getPlayerManager().broadcast(
            net.minecraft.text.Text.literal(decision.message),
            net.minecraft.message.MessageType.CHAT,
            player.getUuid()
        );
        
        System.out.println("[WatheBrains] AI " + player.getName().getString() + " chats: " + decision.message);
    }

    private static void handleFollow(LlmEngine.AiDecision decision, AiProfile ai, MinecraftServer server) {
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null || decision.target_player == null) return;
        
        ServerPlayerEntity target = findPlayerByName(decision.target_player, server);
        if (target == null) {
            System.out.println("[WatheBrains] AI can't find target: " + decision.target_player);
            return;
        }
        
        ai.setFollowTarget(target.getUuid());
        
        Vec3d targetPos = target.getPos();
        Vec3d direction = targetPos.subtract(player.getPos()).normalize();
        double followDistance = 3.0;
        Vec3d newPos = targetPos.subtract(direction.multiply(followDistance));
        
        player.requestTeleport(newPos.x, newPos.y, newPos.z);
        
        System.out.println("[WatheBrains] AI " + player.getName().getString() + " following: " + decision.target_player);
    }

    private static void handleGetSnack(AiProfile ai) {
        if (!isCivilianLike(ai)) return;
        
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null) return;
        
        BlockPos platterPos = WatheApi.findNearestFoodPlatter(player);
        if (platterPos != null) {
            player.requestTeleport(platterPos.getX(), platterPos.getY(), platterPos.getZ());
            System.out.println("[WatheBrains] Civilian " + player.getName().getString() + " getting snack at " + platterPos);
        }
    }

    private static void handleGetDrink(AiProfile ai) {
        if (!isCivilianLike(ai)) return;
        
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null) return;
        
        BlockPos trayPos = WatheApi.findNearestDrinkTray(player);
        if (trayPos != null) {
            player.requestTeleport(trayPos.getX(), trayPos.getY(), trayPos.getZ());
            System.out.println("[WatheBrains] Civilian " + player.getName().getString() + " getting drink at " + trayPos);
        }
    }

    private static void handleGetAir(AiProfile ai, MinecraftServer server) {
        if (!isCivilianLike(ai)) return;
        
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null) return;
        
        String region = WatheApi.getPlayerRegion(player);
        String targetRegion = region.equals("front") ? "back" : "front";
        
        Vec3d targetPos = getLocationCoords(targetRegion, server);
        if (targetPos != null) {
            player.requestTeleport(targetPos.x, targetPos.y, targetPos.z);
            System.out.println("[WatheBrains] Civilian " + player.getName().getString() + " getting fresh air at " + targetRegion);
        }
    }

    private static void handleGetSleep(AiProfile ai, MinecraftServer server) {
        if (!isCivilianLike(ai)) return;
        
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null) return;
        
        BlockPos bedPos = WatheApi.findNearestBed(player);
        if (bedPos != null) {
            player.requestTeleport(bedPos.getX(), bedPos.getY(), bedPos.getZ());
            System.out.println("[WatheBrains] Civilian " + player.getName().getString() + " going to sleep at " + bedPos);
        }
    }

    private static void handleBuyItem(LlmEngine.AiDecision decision, AiProfile ai) {
        if (!ai.getRole().equals("killer")) return;
        
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null || decision.item == null) return;
        
        int shopIndex = switch (decision.item.toLowerCase()) {
            case "knife" -> WatheApi.SHOP_KNIFE;
            case "revolver" -> WatheApi.SHOP_REVOLVER;
            case "grenade" -> WatheApi.SHOP_GRENADE;
            case "poison_vial" -> WatheApi.SHOP_POISON_VIAL;
            case "scorpion" -> WatheApi.SHOP_SCORPION;
            case "lockpick" -> WatheApi.SHOP_LOCKPICK;
            default -> {
                System.out.println("[WatheBrains] Unknown item to buy: " + decision.item);
                yield -1;
            }
        };
        
        if (shopIndex >= 0) {
            WatheApi.buyItem(player, shopIndex);
            System.out.println("[WatheBrains] Killer " + player.getName().getString() + " bought: " + decision.item);
        }
    }

    private static void handleKnifeKill(AiProfile ai, MinecraftServer server, String targetName) {
        if (!ai.getRole().equals("killer")) return;
        
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null) return;
        
        ServerPlayerEntity target = targetName != null ? 
            findPlayerByName(targetName, server) : 
            findNearestValidTarget(player, server);
        
        if (target == null) {
            System.out.println("[WatheBrains] Killer can't find target to knife");
            return;
        }
        
        if (!WatheApi.hasKnife(player)) WatheApi.buyKnife(player);
        
        Vec3d targetPos = target.getPos();
        player.requestTeleport(targetPos.x, targetPos.y, targetPos.z);
        
        WatheApi.knifeKill(target, player);
        
        System.out.println("[WatheBrains] Killer " + player.getName().getString() + " knifed " + target.getName().getString());
    }

    private static void handleGrenadeKill(AiProfile ai, String targetName) {
        if (!ai.getRole().equals("killer")) return;
        
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null) return;
        
        ServerPlayerEntity target = findNearestValidTarget(player, player.getServer());
        
        if (target == null) {
            System.out.println("[WatheBrains] Killer can't find target for grenade");
            return;
        }
        
        if (!WatheApi.hasGrenade(player)) WatheApi.buyGrenade(player);
        
        Vec3d targetPos = target.getPos();
        player.requestTeleport(targetPos.x - 5, targetPos.y, targetPos.z);
        
        WatheApi.grenadeKill(target, player);
        
        System.out.println("[WatheBrains] Killer " + player.getName().getString() + " grenaded " + target.getName().getString());
    }

    private static void handleActivatePsycho(AiProfile ai, MinecraftServer server) {
        if (!ai.getRole().equals("killer")) return;
        
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null) return;
        
        if (WatheApi.isInPsychoMode(player)) return;
        
        WatheApi.buyPsychoMode(player);
        ai.enterPsychoState();
        
        System.out.println("[WatheBrains] Killer " + player.getName().getString() + " activated psycho mode");
    }

    private static void handlePoisonFood(AiProfile ai) {
        if (!ai.getRole().equals("killer")) return;
        
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null) return;
        
        if (!WatheApi.hasPoisonVial(player)) WatheApi.buyPoisonVial(player);
        
        BlockPos foodPos = WatheApi.findNearestFoodPlatter(player);
        BlockPos drinkPos = WatheApi.findNearestDrinkTray(player);
        
        BlockPos targetPos = foodPos;
        if (drinkPos != null && (foodPos == null || 
            player.getPos().distanceTo(new Vec3d(drinkPos.getX(), drinkPos.getY(), drinkPos.getZ())) <
            player.getPos().distanceTo(new Vec3d(foodPos.getX(), foodPos.getY(), foodPos.getZ())))) {
            targetPos = drinkPos;
        }
        
        if (targetPos != null) {
            player.requestTeleport(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            ai.getMemory().markPoisoned(targetPos.toString());
            System.out.println("[WatheBrains] Killer " + player.getName().getString() + " poisoned food at " + targetPos);
        }
    }

    private static void handlePoisonBed(AiProfile ai) {
        if (!ai.getRole().equals("killer")) return;
        
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null) return;
        
        if (!WatheApi.hasScorpion(player)) WatheApi.buyScorpion(player);
        
        BlockPos bedPos = WatheApi.findNearestBed(player);
        
        if (bedPos != null) {
            player.requestTeleport(bedPos.getX(), bedPos.getY(), bedPos.getZ());
            ai.getMemory().markPoisoned("bed:" + bedPos);
            System.out.println("[WatheBrains] Killer " + player.getName().getString() + " placed scorpion on bed at " + bedPos);
        }
    }

    private static void handleBlackout(AiProfile ai) {
        if (!ai.getRole().equals("killer")) return;
        
        ServerPlayerEntity player = ai.getPlayer();
        if (player == null) return;
        
        WatheApi.buyBlackout(player);
        
        System.out.println("[WatheBrains] Killer " + player.getName().getString() + " used blackout");
    }

    private static boolean isCivilianLike(AiProfile ai) {
        String r = ai.getRole().toLowerCase();
        return r.equals("civilian") || r.equals("vigilante");
    }

    private static ServerPlayerEntity findPlayerByName(String name, MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getName().getString().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    private static ServerPlayerEntity findNearestValidTarget(ServerPlayerEntity self, MinecraftServer server) {
        ServerPlayerEntity nearest = null;
        double nearestDist = Double.MAX_VALUE;
        
        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            if (other.getUuid().equals(self.getUuid())) continue;
            if (!WatheApi.isAliveAndSurvival(other)) continue;
            if (WatheApi.isKiller(other)) continue;
            
            double dist = self.getPos().distanceTo(other.getPos());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = other;
            }
        }
        
        return nearest;
    }
}
