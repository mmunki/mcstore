package dev.mmunki.mcstore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mmunki.mcstore.economy.Money;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StoreConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private long startingBalanceCents = Money.STARTING_BALANCE_CENTS;
    private boolean refreshOpenGuis = true;

    public static StoreConfig loadOrCreate(Path path) throws IOException {
        StoreConfig config = new StoreConfig();
        if (Files.exists(path)) {
            try {
                JsonObject root = JsonParser.parseString(JsonFiles.read(path)).getAsJsonObject();
                if (root.has("startingBalance")) {
                    config.startingBalanceCents = Math.max(0L,
                            Money.toCents(root.get("startingBalance").getAsBigDecimal()));
                }
                if (root.has("refreshOpenGuis")) {
                    config.refreshOpenGuis = root.get("refreshOpenGuis").getAsBoolean();
                }
            } catch (RuntimeException e) {
                throw new IOException("Invalid config.json: " + e.getMessage(), e);
            }
        }
        config.save(path);
        return config;
    }

    public void save(Path path) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("_comment", "MCStore server configuration. "
                + "startingBalance is a decimal currency amount (for example 100.00); "
                + "refreshOpenGuis updates open /buy and /sell views instantly.");
        root.addProperty("startingBalance", Money.toDecimal(startingBalanceCents));
        root.addProperty("refreshOpenGuis", refreshOpenGuis);
        JsonFiles.writeAtomic(path, GSON.toJson(root) + System.lineSeparator());
    }

    public long startingBalanceCents() {
        return startingBalanceCents;
    }

    public boolean refreshOpenGuis() {
        return refreshOpenGuis;
    }

    public void setStartingBalanceCents(long cents) {
        this.startingBalanceCents = Math.max(0L, cents);
    }

    public void setRefreshOpenGuis(boolean refreshOpenGuis) {
        this.refreshOpenGuis = refreshOpenGuis;
    }

    public BigDecimal startingBalance() {
        return Money.toDecimal(startingBalanceCents);
    }
}
