package online.inklingyoshi.brains.api;

import dev.doctor4t.wathe.cca.*;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheBlocks;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Wrapper API for Wathe game features.
 */
public class WatheApi {

    public static final int SHOP_KNIFE = 0;
    public static final int SHOP_REVOLVER = 1;
    public static final int SHOP_GRENADE = 2;
    public static final int SHOP_PSYCHO_MODE = 3;
    public static final int SHOP_POISON_VIAL = 4;
    public static final int SHOP_SCORPION = 5;
    public static final int SHOP_FIRECRACKER = 6;
    public static final int SHOP_LOCKPICK = 7;
    public static final int SHOP_CROWBAR = 8;
    public static final int SHOP_BODY_BAG = 9;
    public static final int SHOP_BLACKOUT = 10;
    public static final int SHOP_NOTE = 11;

    public static int getCoins(ServerPlayerEntity player) {
        return PlayerShopComponent.KEY.get(player).balance;
    }

    public static float getMood(ServerPlayerEntity player) {
        return PlayerMoodComponent.KEY.get(player).getMood();
    }

    public static boolean hasTask(ServerPlayerEntity player, PlayerMoodComponent.Task task) {
        return PlayerMoodComponent.KEY.get(player).tasks.containsKey(task);
    }

    public static boolean isKiller(ServerPlayerEntity player) {
        World world = player.getWorld();
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        return !game.isInnocent(player);
    }

    public static boolean isAliveAndSurvival(ServerPlayerEntity player) {
        return GameFunctions.isPlayerAliveAndSurvival(player);
    }

    public static boolean isInPsychoMode(ServerPlayerEntity player) {
        return PlayerPsychoComponent.KEY.get(player).getPsychoTicks() > 0;
    }

    public static int getPsychoTicks(ServerPlayerEntity player) {
        return PlayerPsychoComponent.KEY.get(player).getPsychoTicks();
    }

    public static double getPsychoRemainingSec(ServerPlayerEntity player) {
        return PlayerPsychoComponent.KEY.get(player).getPsychoTicks() / 20.0;
    }

    public static boolean hasItem(ServerPlayerEntity player, Item item) {
        return player.getInventory().contains(new ItemStack(item));
    }

    public static boolean hasKnife(ServerPlayerEntity player) { return hasItem(player, WatheItems.KNIFE); }
    public static boolean hasRevolver(ServerPlayerEntity player) { return hasItem(player, WatheItems.REVOLVER); }
    public static boolean hasGrenade(ServerPlayerEntity player) { return hasItem(player, WatheItems.GRENADE); }
    public static boolean hasLockpick(ServerPlayerEntity player) { return hasItem(player, WatheItems.LOCKPICK); }
    public static boolean hasPoisonVial(ServerPlayerEntity player) { return hasItem(player, WatheItems.POISON_VIAL); }
    public static boolean hasScorpion(ServerPlayerEntity player) { return hasItem(player, WatheItems.SCORPION); }
    public static boolean hasBodyBag(ServerPlayerEntity player) { return hasItem(player, WatheItems.BODY_BAG); }
    public static boolean hasBlackout(ServerPlayerEntity player) { return hasItem(player, WatheItems.BLACKOUT); }

    public static boolean buyItem(ServerPlayerEntity player, int shopIndex) {
        PlayerShopComponent shop = PlayerShopComponent.KEY.get(player);
        shop.tryBuy(shopIndex);
        return true;
    }

    public static boolean buyKnife(ServerPlayerEntity player) { return buyItem(player, SHOP_KNIFE); }
    public static boolean buyRevolver(ServerPlayerEntity player) { return buyItem(player, SHOP_REVOLVER); }
    public static boolean buyGrenade(ServerPlayerEntity player) { return buyItem(player, SHOP_GRENADE); }
    public static boolean buyPsychoMode(ServerPlayerEntity player) { return buyItem(player, SHOP_PSYCHO_MODE); }
    public static boolean buyPoisonVial(ServerPlayerEntity player) { return buyItem(player, SHOP_POISON_VIAL); }
    public static boolean buyScorpion(ServerPlayerEntity player) { return buyItem(player, SHOP_SCORPION); }
    public static boolean buyLockpick(ServerPlayerEntity player) { return buyItem(player, SHOP_LOCKPICK); }
    public static boolean buyBlackout(ServerPlayerEntity player) { return buyItem(player, SHOP_BLACKOUT); }

    public static void knifeKill(PlayerEntity victim, PlayerEntity killer) {
        GameFunctions.killPlayer(victim, true, killer, GameConstants.DeathReasons.KNIFE);
    }

    public static void gunKill(PlayerEntity victim, PlayerEntity killer) {
        GameFunctions.killPlayer(victim, true, killer, GameConstants.DeathReasons.GUN);
    }

    public static void grenadeKill(PlayerEntity victim, PlayerEntity killer) {
        GameFunctions.killPlayer(victim, true, killer, GameConstants.DeathReasons.GRENADE);
    }

    public static void batKill(PlayerEntity victim, PlayerEntity killer) {
        GameFunctions.killPlayer(victim, true, killer, GameConstants.DeathReasons.BAT);
    }

    public static String getRegion(Vec3d pos) {
        double x = pos.x;
        if (x < -100) return "front";
        else if (x < 100) return "middle";
        else return "back";
    }

    public static String getPlayerRegion(ServerPlayerEntity player) {
        return getRegion(player.getPos());
    }

    public static BlockPos findNearestFoodPlatter(ServerPlayerEntity player) {
        return findNearestBlock(player, WatheBlocks.FOOD_PLATTER, 30);
    }

    public static BlockPos findNearestDrinkTray(ServerPlayerEntity player) {
        return findNearestBlock(player, WatheBlocks.DRINK_TRAY, 30);
    }

    public static BlockPos findNearestBed(ServerPlayerEntity player) {
        return findNearestBlock(player, net.minecraft.block.Blocks.BED, 30);
    }

    public static BlockPos findNearestBlock(ServerPlayerEntity player, Block targetBlock, double radius) {
        World world = player.getWorld();
        Vec3d playerPos = player.getPos();
        
        int searchRadius = (int) radius;
        BlockPos playerPosBlock = BlockPos.ofFloored(playerPos);
        
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    BlockPos pos = playerPosBlock.add(dx, dy, dz);
                    BlockState state = world.getBlockState(pos);
                    if (state.getBlock() == targetBlock) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    public static boolean isGameRunning(World world) {
        GameWorldComponent game = GameWorldComponent.KEY.get(world);
        return game.getGameStatus() == GameWorldComponent.GameStatus.ACTIVE;
    }
}
