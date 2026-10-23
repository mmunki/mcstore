package dev.mmunki.mcstore;

import dev.mmunki.mcstore.catalog.Coverage;
import dev.mmunki.mcstore.catalog.ItemClassifier;
import dev.mmunki.mcstore.config.AdminList;
import dev.mmunki.mcstore.config.Balances;
import dev.mmunki.mcstore.config.PriceBook;
import dev.mmunki.mcstore.config.PriceEntry;
import dev.mmunki.mcstore.config.StoreConfig;
import dev.mmunki.mcstore.economy.Money;
import dev.mmunki.mcstore.gui.StoreMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class McStore {
    public static final String VERSION = "26.2.1";
    private static final Logger LOGGER = LoggerFactory.getLogger("mcstore");

    private final Path configDir;
    private final Path configPath;
    private final Path pricesPath;
    private final Path balancesPath;
    private final Path adminsPath;

    private StoreConfig config = new StoreConfig();
    private AdminList admins = new AdminList();
    private PriceBook prices = new PriceBook();
    private Balances balances;

    private MinecraftServer server;
    private List<String> registryIds = List.of();
    private List<String> filteredIds = List.of();
    private volatile boolean ready;

    public McStore(Path configDir) {
        this.configDir = configDir;
        this.configPath = configDir.resolve("config.json");
        this.pricesPath = configDir.resolve("prices.json");
        this.balancesPath = configDir.resolve("balances.json");
        this.adminsPath = configDir.resolve("admins.json");
        this.balances = Balances.empty(Money.STARTING_BALANCE_CENTS);
    }

    public void start(MinecraftServer server) {
        this.server = server;
        try {
            Files.createDirectories(configDir);
        } catch (IOException e) {
            LOGGER.error("MCStore could not create {}", configDir, e);
        }

        try {
            config = StoreConfig.loadOrCreate(configPath);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("MCStore could not load config.json, using defaults", e);
            config = new StoreConfig();
        }

        try {
            admins = AdminList.loadOrCreate(adminsPath);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("MCStore could not load admins.json, using an empty list", e);
            admins = new AdminList();
        }

        refreshRegistryIds();

        boolean generated = false;
        try {
            if (Files.exists(pricesPath) && Files.size(pricesPath) > 2L) {
                prices = PriceBook.load(pricesPath);
            }
            if (prices.all().isEmpty()) {
                prices = PriceBook.generate(registryIds);
                generated = true;
            }
            prices.save(pricesPath);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("MCStore could not load prices.json, regenerating defaults", e);
            prices = PriceBook.generate(registryIds);
            generated = true;
            try {
                prices.save(pricesPath);
            } catch (IOException saveError) {
                LOGGER.error("MCStore could not write prices.json", saveError);
            }
        }

        try {
            balances = Balances.load(balancesPath, config.startingBalanceCents());
            balances.save();
        } catch (IOException | RuntimeException e) {
            LOGGER.error("MCStore could not load balances.json, starting with an empty ledger", e);
            balances = Balances.empty(config.startingBalanceCents());
        }

        ready = true;
        Coverage coverage = coverage();
        LOGGER.info("MCStore {} initialized on Minecraft 26.2 (Fabric)", VERSION);
        LOGGER.info("MCStore prices {} ({} default entries), {} sellable of {} filtered items ({}%), {} of {} registry items ({}%), {} buyable ores",
                generated ? "generated" : "loaded",
                prices.all().size(),
                coverage.pricedTotal(), coverage.filteredTotal(), coverage.percentOfFiltered(),
                coverage.pricedTotal(), coverage.registryTotal(), coverage.percentOfRegistry(),
                buyableEntries().size());
    }

    public void stop() {
        if (balances != null) {
            try {
                balances.save();
            } catch (IOException e) {
                LOGGER.error("MCStore could not save balances.json on shutdown", e);
            }
        }
        this.server = null;
        this.ready = false;
    }

    public void reload() {
        try {
            config = StoreConfig.loadOrCreate(configPath);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("MCStore reload: config.json is invalid, keeping the previous config", e);
        }
        try {
            admins = AdminList.loadOrCreate(adminsPath);
        } catch (IOException | RuntimeException e) {
            LOGGER.error("MCStore reload: admins.json is invalid, keeping the previous list", e);
        }
        try {
            prices = PriceBook.load(pricesPath);
            if (prices.all().isEmpty()) {
                prices = PriceBook.generate(registryIds);
                prices.save(pricesPath);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("MCStore reload: prices.json is invalid, keeping the previous prices", e);
        }
        if (balances != null) {
            balances.setStartingBalanceCents(config.startingBalanceCents());
        }
        refreshOpenGuis();
    }

    public boolean savePrices() {
        try {
            prices.save(pricesPath);
            return true;
        } catch (IOException e) {
            LOGGER.error("MCStore could not write prices.json", e);
            return false;
        }
    }

    public PriceEntry setPrice(String itemId, boolean buy, long cents) {
        PriceEntry entry = buy ? prices.setBuy(itemId, cents) : prices.setSell(itemId, cents);
        savePrices();
        refreshOpenGuis();
        return entry;
    }

    public void refreshOpenGuis() {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof StoreMenu menu) {
                if (config.refreshOpenGuis()) {
                    menu.refresh();
                    player.sendSystemMessage(Component.literal("MCStore prices updated.").withStyle(net.minecraft.ChatFormatting.YELLOW));
                } else {
                    player.closeContainer();
                    player.sendSystemMessage(Component.literal("MCStore prices changed. The store was closed; use /buy or /sell again.")
                            .withStyle(net.minecraft.ChatFormatting.YELLOW));
                }
            }
        }
    }

    public void refreshRegistryIds() {
        List<String> registry = new ArrayList<>();
        List<String> filtered = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (!"minecraft".equals(id.getNamespace())) {
                continue;
            }
            String value = id.toString();
            registry.add(value);
            if (!ItemClassifier.isBlacklisted(value)) {
                filtered.add(value);
            }
        }
        registry.sort(String::compareTo);
        filtered.sort(String::compareTo);
        registryIds = Collections.unmodifiableList(registry);
        filteredIds = Collections.unmodifiableList(filtered);
    }

    public List<PriceEntry> sellableEntries() {
        List<PriceEntry> result = new ArrayList<>();
        for (PriceEntry entry : prices.sellable()) {
            if (isRegistered(entry.itemId())) {
                result.add(entry);
            }
        }
        return result;
    }

    public List<PriceEntry> buyableEntries() {
        List<PriceEntry> result = new ArrayList<>();
        for (PriceEntry entry : prices.buyable()) {
            if (isRegistered(entry.itemId()) && ItemClassifier.isBuyable(entry.itemId())) {
                result.add(entry);
            }
        }
        return result;
    }

    public boolean isRegistered(String itemId) {
        try {
            return BuiltInRegistries.ITEM.containsKey(Identifier.parse(PriceBook.normalizeId(itemId)));
        } catch (RuntimeException e) {
            return false;
        }
    }

    public Coverage coverage() {
        return prices.coverage(registryIds, filteredIds);
    }

    public String status() {
        Coverage coverage = coverage();
        return "MCStore %s · %d sellable / %d filtered items (%d%%) · %d buyable ores · %d registry items"
                .formatted(VERSION, coverage.pricedTotal(), coverage.filteredTotal(),
                        coverage.percentOfFiltered(), buyableEntries().size(), coverage.registryTotal());
    }

    public boolean isAdmin(CommandSourceStack source) {
        if (source == null) {
            return false;
        }
        if (source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) {
            return true;
        }
        ServerPlayer player = source.getPlayer();
        return player != null && admins.contains(player.getUUID());
    }

    public StoreConfig config() {
        return config;
    }

    public AdminList admins() {
        return admins;
    }

    public PriceBook prices() {
        return prices;
    }

    public Balances balances() {
        return balances;
    }

    public boolean ready() {
        return ready;
    }

    public Path configDir() {
        return configDir;
    }

    public Path pricesPath() {
        return pricesPath;
    }

    public Path balancesPath() {
        return balancesPath;
    }

    public MinecraftServer server() {
        return server;
    }

    public Collection<String> registryIds() {
        return registryIds;
    }

    public Collection<String> filteredIds() {
        return filteredIds;
    }

    public static Logger logger() {
        return LOGGER;
    }
}
