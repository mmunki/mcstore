package dev.mmunki.mcstore.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mmunki.mcstore.catalog.Coverage;
import dev.mmunki.mcstore.catalog.ItemClassifier;
import dev.mmunki.mcstore.catalog.PriceGenerator;
import dev.mmunki.mcstore.catalog.RarityTier;
import dev.mmunki.mcstore.economy.Money;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The live price table.  The in-memory model is always the source of truth for
 * the shop; prices.json is only a persistence format and is rewritten
 * atomically after every admin change.
 */
public final class PriceBook {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final int CURRENT_VERSION = 2;
    private static final String COMMENT = "MCStore prices. Money is a decimal amount; "
            + "the runtime stores integer cents. Rarity is one of COMMON, UNCOMMON, RARE, EPIC, LEGENDARY. "
            + "Remove the buy field to make an item sell-only.";

    private final Map<String, PriceEntry> entries = new TreeMap<>();
    private int version = CURRENT_VERSION;

    public static PriceBook load(Path path) throws IOException {
        PriceBook book = new PriceBook();
        if (!Files.exists(path)) {
            return book;
        }
        String json = JsonFiles.read(path);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        int version = root.has("_version") ? root.get("_version").getAsInt() : 1;
        JsonObject items;
        if (root.has("items") && root.get("items").isJsonObject()) {
            items = root.getAsJsonObject("items");
        } else {
            items = root;
        }
        for (Map.Entry<String, JsonElement> element : items.entrySet()) {
            if (element.getKey().startsWith("_") || !element.getValue().isJsonObject()) {
                continue;
            }
            String id = normalizeId(element.getKey());
            JsonObject value = element.getValue().getAsJsonObject();
            RarityTier rarity = value.has("rarity")
                    ? RarityTier.fromName(value.get("rarity").getAsString())
                    : ItemClassifier.rarity(id);
            long sell = readCents(value.get("sell"), rarity.defaultSellCents(id));
            Long buy = value.has("buy") && !value.get("buy").isJsonNull()
                    ? readCents(value.get("buy"), 0L)
                    : null;
            if (buy != null && buy < Money.MIN_PRICE_CENTS) {
                buy = null;
            }
            if (sell < Money.MIN_PRICE_CENTS) {
                sell = rarity.defaultSellCents(id);
            }
            book.entries.put(id, new PriceEntry(id, rarity, buy, sell));
        }
        if (version < CURRENT_VERSION) {
            book.upgradeMineralBlocks();
        }
        book.version = CURRENT_VERSION;
        return book;
    }

    private void upgradeMineralBlocks() {
        for (String id : List.copyOf(entries.keySet())) {
            if (ItemClassifier.isMineralBlock(id)) {
                entries.put(id, PriceGenerator.defaultEntry(id));
            }
        }
    }

    private static long readCents(JsonElement element, long fallback) {
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            if (element.getAsJsonPrimitive().isString()) {
                return Money.parseCents(element.getAsString());
            }
            return Money.toCents(element.getAsBigDecimal());
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    public void save(Path path) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("_version", version);
        root.addProperty("_comment", COMMENT);
        JsonObject items = new JsonObject();
        for (PriceEntry entry : entries.values()) {
            JsonObject value = new JsonObject();
            value.addProperty("rarity", entry.rarity().name());
            if (entry.buyCents() != null) {
                value.addProperty("buy", Money.toDecimal(entry.buyCents()));
            }
            value.addProperty("sell", Money.toDecimal(entry.sellCents()));
            items.add(entry.itemId(), value);
        }
        root.add("items", items);
        JsonFiles.writeAtomic(path, GSON.toJson(root) + System.lineSeparator());
    }

    public void replaceAll(Collection<PriceEntry> newEntries) {
        entries.clear();
        for (PriceEntry entry : newEntries) {
            entries.put(entry.itemId(), entry);
        }
    }

    public static PriceBook generate(Collection<String> registryItemIds) {
        PriceBook book = new PriceBook();
        for (PriceEntry entry : PriceGenerator.generate(registryItemIds)) {
            book.entries.put(entry.itemId(), entry);
        }
        return book;
    }

    /**
     * Adds an entry only when the id is missing.  Existing files, including
     * admin-edited ones, are never overwritten by a built-in entry.
     */
    public void ensure(PriceEntry entry) {
        entries.putIfAbsent(entry.itemId(), entry);
    }

    public PriceEntry get(String itemId) {
        return entries.get(normalizeId(itemId));
    }

    public boolean contains(String itemId) {
        return entries.containsKey(normalizeId(itemId));
    }

    public PriceEntry setBuy(String itemId, long cents) {
        String id = normalizeId(itemId);
        PriceEntry current = entries.get(id);
        if (current == null) {
            current = PriceGenerator.defaultEntry(id);
        }
        PriceEntry next = current.withBuyCents(cents);
        entries.put(id, next);
        return next;
    }

    public PriceEntry setSell(String itemId, long cents) {
        String id = normalizeId(itemId);
        PriceEntry current = entries.get(id);
        if (current == null) {
            current = PriceGenerator.defaultEntry(id);
        }
        PriceEntry next = current.withSellCents(cents);
        entries.put(id, next);
        return next;
    }

    public List<PriceEntry> all() {
        return List.copyOf(entries.values());
    }

    public List<PriceEntry> sellable() {
        return entries.values().stream().filter(entry -> entry.sellCents() >= 1L).toList();
    }

    public List<PriceEntry> buyable() {
        return entries.values().stream().filter(PriceEntry::buyable).toList();
    }

    public List<String> buyableIds() {
        List<String> result = new ArrayList<>();
        for (PriceEntry entry : buyable()) {
            result.add(entry.itemId());
        }
        return Collections.unmodifiableList(result);
    }

    public Set<String> pricedIds() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    public Coverage coverage(Collection<String> registryIds, Collection<String> filteredIds) {
        return Coverage.of(registryIds, entries.keySet(), filteredIds);
    }

    public static String normalizeId(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("item id is missing");
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("item id is empty");
        }
        return trimmed.contains(":") ? trimmed : "minecraft:" + trimmed;
    }

    public Map<String, PriceEntry> snapshot() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }
}
