package dev.mmunki.mcstore;

import dev.mmunki.mcstore.command.StoreCommands;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Server entrypoint.  There is deliberately no client entrypoint, no blocks,
 * no items and no custom menu type: a vanilla client can join and use the
 * shop, and removing the mod leaves no registry content behind.
 */
public final class MCStoreMod implements DedicatedServerModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("mcstore");

    private final McStore store;

    public MCStoreMod() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("mcstore");
        this.store = new McStore(configDir);
    }

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(store::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> store.stop());
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            StoreCommands.register(dispatcher, store);
            LOGGER.info("MCStore commands registered: /buy /sell /balance /mcstore");
        });
        LOGGER.info("MCStore {} loaded (server-side only, no client mod required)", McStore.VERSION);
    }
}
