package online.inklingyoshi.brains;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import online.inklingyoshi.brains.command.WatheBrainsCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.ServerCommandSource;

/**
 * WatheBrains - AI mod for Harpy Express
 * 
 * Adds AI-controlled players (killers, vigilantes, civilians) using
 * LLM-based decision making (DeepSeek via Siliconflow).
 */
public class WatheBrains implements ModInitializer {
    public static final String MOD_ID = "wathe-brains";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing WatheBrains AI Mod");
        
        // Register AI tick handler
        AiTickHandler.register();
        
        // Register event handlers
        registerEventHandlers();
        
        // Register commands
        registerCommands();
        
        LOGGER.info("WatheBrains AI Mod initialized successfully!");
        
        // Check for API key
        String apiKey = System.getenv("SILICONFLOW_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            LOGGER.warn("[WatheBrains] SILICONFLOW_API_KEY not set! AI will not function.");
            LOGGER.warn("[WatheBrains] Set it with: export SILICONFLOW_API_KEY=your_key");
        }
    }

    private void registerEventHandlers() {
        // Server started
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("[WatheBrains] Server started - AI system ready");
        });
        
        // Server stopping
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("[WatheBrains] Server stopping - cleaning up AI");
        });
        
        // Player disconnect - clean up AI profile
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            online.inklingyoshi.brains.ai.AiManager.removeProfile(handler.getPlayer().getUuid());
        });
    }

    private void registerCommands() {
        // Commands will be registered on server start
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var dispatcher = server.getCommandManager().getDispatcher();
            WatheBrainsCommands.register(dispatcher);
        });
    }
}
