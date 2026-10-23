package dev.mmunki.mcstore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Integer-cent balances keyed by player UUID.  Every mutation is written to
 * disk immediately; the server-stop hook writes one last consistent snapshot.
 */
public final class Balances {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Map<UUID, Long> balances = new TreeMap<>();
    private long startingBalanceCents;
    private Path path;

    private Balances(long startingBalanceCents, Path path) {
        this.startingBalanceCents = startingBalanceCents;
        this.path = path;
    }

    public static Balances empty(long startingBalanceCents) {
        return new Balances(startingBalanceCents, null);
    }

    public static Balances load(Path path, long startingBalanceCents) throws IOException {
        Balances balances = new Balances(startingBalanceCents, path);
        if (Files.exists(path)) {
            try {
                JsonObject root = JsonParser.parseString(JsonFiles.read(path)).getAsJsonObject();
                JsonObject values = root.has("balances") && root.get("balances").isJsonObject()
                        ? root.getAsJsonObject("balances")
                        : root;
                for (Map.Entry<String, com.google.gson.JsonElement> entry : values.entrySet()) {
                    if (entry.getKey().startsWith("_")) {
                        continue;
                    }
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        long cents = entry.getValue().getAsLong();
                        if (cents >= 0L) {
                            balances.balances.put(uuid, cents);
                        }
                    } catch (RuntimeException ignored) {
                        // ignore a single malformed account, keep the rest
                    }
                }
            } catch (RuntimeException e) {
                throw new IOException("Invalid balances.json: " + e.getMessage(), e);
            }
        }
        return balances;
    }

    public synchronized void setStartingBalanceCents(long startingBalanceCents) {
        this.startingBalanceCents = Math.max(0L, startingBalanceCents);
    }

    public long startingBalanceCents() {
        return startingBalanceCents;
    }

    public synchronized long balance(UUID uuid) {
        Long value = balances.get(uuid);
        if (value != null) {
            return value;
        }
        balances.put(uuid, startingBalanceCents);
        saveQuietly();
        return startingBalanceCents;
    }

    public synchronized void add(UUID uuid, long cents) {
        if (cents <= 0L) {
            return;
        }
        long current = balances.getOrDefault(uuid, startingBalanceCents);
        long next;
        try {
            next = Math.addExact(current, cents);
        } catch (ArithmeticException e) {
            next = Long.MAX_VALUE;
        }
        balances.put(uuid, next);
        saveQuietly();
    }

    public synchronized boolean tryWithdraw(UUID uuid, long cents) {
        if (cents <= 0L) {
            return false;
        }
        long current = balances.getOrDefault(uuid, startingBalanceCents);
        if (current < cents) {
            return false;
        }
        balances.put(uuid, current - cents);
        saveQuietly();
        return true;
    }

    public synchronized void set(UUID uuid, long cents) {
        balances.put(uuid, Math.max(0L, cents));
        saveQuietly();
    }

    public synchronized Map<UUID, Long> snapshot() {
        return Collections.unmodifiableMap(new TreeMap<>(balances));
    }

    public synchronized void save(Path target) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("_comment", "Balances are stored as integer cents (100 = 1.00).");
        JsonObject values = new JsonObject();
        for (Map.Entry<UUID, Long> entry : balances.entrySet()) {
            values.addProperty(entry.getKey().toString(), entry.getValue());
        }
        root.add("balances", values);
        JsonFiles.writeAtomic(target, GSON.toJson(root) + System.lineSeparator());
        this.path = target;
    }

    public synchronized void save() throws IOException {
        save(path);
    }

    private void saveQuietly() {
        if (path == null) {
            return;
        }
        try {
            save(path);
        } catch (IOException e) {
            System.err.println("MCStore: could not save balances.json: " + e.getMessage());
        }
    }
}
