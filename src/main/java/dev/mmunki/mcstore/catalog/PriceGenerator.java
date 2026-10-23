package dev.mmunki.mcstore.catalog;

import dev.mmunki.mcstore.config.PriceEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the default price table from the item registry.  All decisions are
 * deterministic: the same item ID always produces the same price.
 */
public final class PriceGenerator {
    public record PriceDefaults(RarityTier rarity, long sellCents, Long buyCents) {
    }

    private static final Map<String, PriceDefaults> OVERRIDES = buildOverrides();

    private PriceGenerator() {
    }

    public static List<PriceEntry> generate(Collection<String> itemIds) {
        List<PriceEntry> result = new ArrayList<>();
        for (String itemId : itemIds) {
            if (!itemId.startsWith("minecraft:") || ItemClassifier.isBlacklisted(itemId)) {
                continue;
            }
            result.add(defaultEntry(itemId));
        }
        result.sort(Comparator.comparing(PriceEntry::itemId));
        return result;
    }

    public static PriceEntry defaultEntry(String itemId) {
        String path = ItemClassifier.path(itemId);
        PriceDefaults override = OVERRIDES.get(path);
        RarityTier rarity = override != null ? override.rarity() : ItemClassifier.rarity(itemId);
        long sell = override != null ? override.sellCents() : rarity.defaultSellCents(itemId);
        Long buy;
        if (override != null) {
            buy = override.buyCents();
        } else {
            buy = ItemClassifier.isBuyable(itemId) ? rarity.defaultBuyCents(sell) : null;
        }
        return new PriceEntry(itemId, rarity, buy, sell);
    }

    private static Map<String, PriceDefaults> buildOverrides() {
        Map<String, PriceDefaults> map = new LinkedHashMap<>();
        put(map, "coal", RarityTier.COMMON, 300L, 600L);
        put(map, "charcoal", RarityTier.COMMON, 200L, 500L);
        put(map, "copper_ingot", RarityTier.COMMON, 450L, 1_200L);
        put(map, "raw_copper", RarityTier.COMMON, 400L, 1_000L);
        put(map, "raw_copper_block", RarityTier.COMMON, 500L, 1_000L);
        put(map, "copper_block", RarityTier.COMMON, 500L, 1_000L);
        put(map, "iron_ingot", RarityTier.UNCOMMON, 1_000L, 2_500L);
        put(map, "raw_iron", RarityTier.UNCOMMON, 800L, 2_000L);
        put(map, "raw_iron_block", RarityTier.UNCOMMON, 1_200L, 3_000L);
        put(map, "iron_block", RarityTier.UNCOMMON, 1_500L, 3_750L);
        put(map, "gold_ingot", RarityTier.RARE, 2_000L, 6_000L);
        put(map, "raw_gold", RarityTier.RARE, 1_800L, 5_400L);
        put(map, "raw_gold_block", RarityTier.RARE, 3_000L, 9_000L);
        put(map, "gold_block", RarityTier.RARE, 3_500L, 10_500L);
        put(map, "redstone", RarityTier.UNCOMMON, 800L, 2_000L);
        put(map, "redstone_block", RarityTier.UNCOMMON, 1_200L, 3_000L);
        put(map, "lapis_lazuli", RarityTier.UNCOMMON, 1_000L, 2_500L);
        put(map, "lapis_block", RarityTier.UNCOMMON, 1_400L, 3_500L);
        put(map, "quartz", RarityTier.UNCOMMON, 900L, 2_250L);
        put(map, "quartz_block", RarityTier.UNCOMMON, 1_300L, 3_250L);
        put(map, "diamond", RarityTier.RARE, 4_000L, 12_000L);
        put(map, "diamond_block", RarityTier.EPIC, 9_000L, 31_500L);
        put(map, "emerald", RarityTier.RARE, 3_500L, 10_500L);
        put(map, "emerald_block", RarityTier.EPIC, 8_000L, 28_000L);
        put(map, "amethyst_shard", RarityTier.RARE, 2_000L, 6_000L);
        put(map, "amethyst_block", RarityTier.RARE, 3_800L, 11_400L);
        put(map, "sulfur", RarityTier.COMMON, 500L, 1_000L);
        put(map, "potent_sulfur", RarityTier.UNCOMMON, 1_200L, 3_000L);
        put(map, "cinnabar", RarityTier.UNCOMMON, 1_200L, 3_000L);
        put(map, "ancient_debris", RarityTier.EPIC, 9_500L, 33_250L);
        put(map, "netherite_scrap", RarityTier.EPIC, 9_000L, 31_500L);
        put(map, "netherite_ingot", RarityTier.LEGENDARY, 22_500L, 90_000L);
        put(map, "netherite_block", RarityTier.LEGENDARY, 25_000L, 100_000L);
        put(map, "elytra", RarityTier.LEGENDARY, 25_000L, null);
        put(map, "enchanted_book", RarityTier.EPIC, 7_500L, null);
        put(map, "nether_star", RarityTier.LEGENDARY, 25_000L, null);
        put(map, "dragon_egg", RarityTier.LEGENDARY, 25_000L, null);
        put(map, "totem_of_undying", RarityTier.LEGENDARY, 25_000L, null);
        put(map, "trident", RarityTier.LEGENDARY, 25_000L, null);
        put(map, "beacon", RarityTier.LEGENDARY, 20_000L, null);
        put(map, "enchanted_golden_apple", RarityTier.LEGENDARY, 25_000L, null);
        put(map, "heavy_core", RarityTier.LEGENDARY, 20_000L, null);
        put(map, "mace", RarityTier.LEGENDARY, 25_000L, null);
        put(map, "echo_shard", RarityTier.EPIC, 6_000L, null);
        put(map, "heart_of_the_sea", RarityTier.EPIC, 7_500L, null);
        put(map, "shulker_shell", RarityTier.EPIC, 8_000L, null);
        put(map, "netherite_helmet", RarityTier.EPIC, 9_000L, null);
        put(map, "netherite_chestplate", RarityTier.EPIC, 10_000L, null);
        put(map, "netherite_leggings", RarityTier.EPIC, 9_500L, null);
        put(map, "netherite_boots", RarityTier.EPIC, 8_500L, null);
        put(map, "netherite_sword", RarityTier.EPIC, 9_000L, null);
        put(map, "netherite_pickaxe", RarityTier.EPIC, 9_000L, null);
        put(map, "netherite_axe", RarityTier.EPIC, 9_000L, null);
        put(map, "netherite_shovel", RarityTier.EPIC, 8_000L, null);
        put(map, "netherite_hoe", RarityTier.EPIC, 8_000L, null);
        return Map.copyOf(map);
    }

    private static void put(Map<String, PriceDefaults> map, String id, RarityTier rarity, long sell, Long buy) {
        map.put(id, new PriceDefaults(rarity, sell, buy));
    }
}
