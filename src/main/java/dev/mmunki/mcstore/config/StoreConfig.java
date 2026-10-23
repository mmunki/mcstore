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
    private int restockSeconds = 600;
    private long xpCentsPerLevel = 100L;

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
                if (root.has("restockSeconds")) {
                    config.restockSeconds = Math.max(0, root.get("restockSeconds").getAsInt());
                }
                if (root.has("xpCentsPerLevel")) {
                    config.xpCentsPerLevel = Math.max(1L,
                            Money.toCents(root.get("xpCentsPerLevel").getAsBigDecimal()));
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
                + "refreshOpenGuis updates open /buy and /sell views instantly; "
                + "restockSeconds is the market restock cycle (0 disables it); "
                + "xpCentsPerLevel is how much one experience level is worth when exchanged.");
        root.addProperty("startingBalance", Money.toDecimal(startingBalanceCents));
        root.addProperty("refreshOpenGuis", refreshOpenGuis);
        root.addProperty("restockSeconds", restockSeconds);
        root.addProperty("xpCentsPerLevel", Money.toDecimal(xpCentsPerLevel));
        JsonFiles.writeAtomic(path, GSON.toJson(root) + System.lineSeparator());
    }

    public long startingBalanceCents() {
        return startingBalanceCents;
    }

    public boolean refreshOpenGuis() {
        return refreshOpenGuis;
    }

    public int restockSeconds() {
        return restockSeconds;
    }

    public long xpCentsPerLevel() {
        return xpCentsPerLevel;
    }

    public void setStartingBalanceCents(long cents) {
        this.startingBalanceCents = Math.max(0L, cents);
    }

    public void setRefreshOpenGuis(boolean refreshOpenGuis) {
        this.refreshOpenGuis = refreshOpenGuis;
    }

    public void setRestockSeconds(int restockSeconds) {
        this.restockSeconds = Math.max(0, restockSeconds);
    }

    public void setXpCentsPerLevel(long xpCentsPerLevel) {
        this.xpCentsPerLevel = Math.max(1L, xpCentsPerLevel);
    }

    public BigDecimal startingBalance() {
        return Money.toDecimal(startingBalanceCents);
    }

    public BigDecimal xpCentsPerLevelDecimal() {
        return Money.toDecimal(xpCentsPerLevel);
    }
}
