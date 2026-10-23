package dev.mmunki.mcstore;

import dev.mmunki.mcstore.command.StoreCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Common entrypoint.  It is loaded on the client and on the dedicated server,
 * which is what makes the shop work in singleplayer worlds (the integrated
 * server) as well as on a server.  There is deliberately no blocks, items or
 * custom menu type: a vanilla client can join a server and use the shop.
 */
public final class MCStoreMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("mcstore");

    private final McStore store;

    public MCStoreMod() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("mcstore");
        this.store = new McStore(configDir);
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(store::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> store.stop());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            StoreCommands.register(dispatcher, store);
            LOGGER.info("MCStore commands registered: /buy /sell /balance /mcstore");
        });
        LOGGER.info("MCStore {} loaded (client + server, singleplayer worlds supported)", McStore.VERSION);
    }
}
