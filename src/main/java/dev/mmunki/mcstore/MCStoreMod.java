package dev.mmunki.mcstore;

import dev.mmunki.mcstore.command.StoreCommands;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Common entrypoint.  It is loaded on the client and on the dedicated server,
 * which is what makes the shop work in singleplayer worlds (the integrated
 * server) as well as on a server.  The shop itself uses a vanilla chest menu.
 * The only registry content is the Money Ore block/item.
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
        MCStoreContent.register();
        MCStoreContent.registerWorldgen();
        ServerLifecycleEvents.SERVER_STARTING.register(store::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> store.stop());
        ServerTickEvents.END_SERVER_TICK.register(server -> store.tick());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            StoreCommands.register(dispatcher, store);
            LOGGER.info("MCStore commands registered: /buy /sell /exchange /stock /balance /mcstore");
        });
        LOGGER.info("MCStore {} loaded (client + server, singleplayer worlds supported, Money Ore in overworld + nether)", McStore.VERSION);
    }
}
