package dev.mmunki.mcstore.market;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mmunki.mcstore.config.JsonFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Persistent stock levels for the shop, plus the restock clock.
 *
 * <p>An item with no entry in the map is at its resting stock: half of its
 * cap.  This is deliberate.  A fresh world (and every restock) must have room
 * for players to sell, so the shelves cannot start completely full, but they
 * also need enough stock to buy from.  At half stock the supply factor is
 * exactly 1.0, so the live price equals the base price after a restock.</p>
 */
public final class StockBook {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String COMMENT = "MCStore stock. An item that is not listed is at its resting stock "
            + "(half of its cap). lastRestock is Unix epoch seconds; restockSeconds=0 disables automatic restocking.";

    private final Map<String, Integer> stocks = new TreeMap<>();
    private long lastRestockEpochSecond;
    private int restockSeconds;
    private Path path;

    private StockBook(int restockSeconds) {
        this.restockSeconds = Math.max(0, restockSeconds);
    }

    public static StockBook empty(int restockSeconds) {
        StockBook book = new StockBook(restockSeconds);
        book.lastRestockEpochSecond = Instant.now().getEpochSecond();
        return book;
    }

    public static StockBook load(Path path, int restockSeconds) throws IOException {
        StockBook book = new StockBook(restockSeconds);
        if (Files.exists(path)) {
            try {
                JsonObject root = JsonParser.parseString(JsonFiles.read(path)).getAsJsonObject();
                if (root.has("lastRestock") && !root.get("lastRestock").isJsonNull()) {
                    book.lastRestockEpochSecond = root.get("lastRestock").getAsLong();
                }
                if (root.has("restockSeconds")) {
                    book.restockSeconds = Math.max(0, root.get("restockSeconds").getAsInt());
                }
                JsonObject values = root.has("stocks") && root.get("stocks").isJsonObject()
                        ? root.getAsJsonObject("stocks")
                        : new JsonObject();
                for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
                    if (entry.getKey().startsWith("_")) {
                        continue;
                    }
                    try {
                        int value = Math.max(0, entry.getValue().getAsInt());
                        book.stocks.put(entry.getKey(), value);
                    } catch (RuntimeException ignored) {
                        // a single malformed stock entry must not stop the server
                    }
                }
            } catch (RuntimeException e) {
                throw new IOException("Invalid stocks.json: " + e.getMessage(), e);
            }
        }
        if (book.lastRestockEpochSecond <= 0L) {
            book.lastRestockEpochSecond = Instant.now().getEpochSecond();
        }
        book.path = path;
        return book;
    }

    public synchronized int stock(String itemId, int capacity) {
        int cap = Math.max(0, capacity);
        Integer value = stocks.get(itemId);
        if (value == null) {
            return restingStock(cap);
        }
        return Math.max(0, Math.min(cap, value));
    }

    /**
     * The stock an item has on a fresh world and after every restock.  Half
     * of the cap leaves room to sell and still leaves stock to buy.
     */
    public static int restingStock(int capacity) {
        return Math.max(0, capacity) / 2;
    }

    public synchronized int add(String itemId, int amount, int capacity) {
        if (amount <= 0) {
            return 0;
        }
        int cap = Math.max(0, capacity);
        int current = stock(itemId, cap);
        int next = Math.min(cap, current + amount);
        int accepted = next - current;
        if (accepted > 0) {
            stocks.put(itemId, next);
            saveQuietly();
        }
        return accepted;
    }

    public synchronized int consume(String itemId, int amount, int capacity) {
        if (amount <= 0) {
            return 0;
        }
        int cap = Math.max(0, capacity);
        int current = stock(itemId, cap);
        int next = Math.max(0, current - amount);
        int removed = current - next;
        if (removed > 0) {
            stocks.put(itemId, next);
            saveQuietly();
        }
        return removed;
    }

    public synchronized boolean restockIfDue(long nowEpochSecond) {
        if (restockSeconds <= 0) {
            return false;
        }
        if (nowEpochSecond - lastRestockEpochSecond < restockSeconds) {
            return false;
        }
        restock(nowEpochSecond);
        return true;
    }

    public synchronized void restock(long nowEpochSecond) {
        stocks.clear();
        lastRestockEpochSecond = nowEpochSecond;
        saveQuietly();
    }

    public synchronized int secondsUntilRestock(long nowEpochSecond) {
        if (restockSeconds <= 0) {
            return -1;
        }
        long remaining = lastRestockEpochSecond + restockSeconds - nowEpochSecond;
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, remaining));
    }

    public synchronized long lastRestockEpochSecond() {
        return lastRestockEpochSecond;
    }

    public synchronized int restockSeconds() {
        return restockSeconds;
    }

    public synchronized void setRestockSeconds(int restockSeconds) {
        this.restockSeconds = Math.max(0, restockSeconds);
        saveQuietly();
    }

    public synchronized Map<String, Integer> snapshot() {
        return Collections.unmodifiableMap(new TreeMap<>(stocks));
    }

    public synchronized void save(Path target) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("_comment", COMMENT);
        root.addProperty("restockSeconds", restockSeconds);
        root.addProperty("lastRestock", lastRestockEpochSecond);
        JsonObject values = new JsonObject();
        for (Map.Entry<String, Integer> entry : stocks.entrySet()) {
            values.addProperty(entry.getKey(), entry.getValue());
        }
        root.add("stocks", values);
        JsonFiles.writeAtomic(target, GSON.toJson(root) + System.lineSeparator());
        this.path = target;
    }

    public synchronized void save() throws IOException {
        if (path != null) {
            save(path);
        }
    }

    private void saveQuietly() {
        if (path == null) {
            return;
        }
        try {
            save(path);
        } catch (IOException e) {
            System.err.println("MCStore: could not save stocks.json: " + e.getMessage());
        }
    }
}
