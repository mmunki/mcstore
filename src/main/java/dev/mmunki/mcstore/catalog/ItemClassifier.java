package dev.mmunki.mcstore.catalog;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic, registry-independent heuristics.  Keeping this class free of
 * Minecraft types makes the price rules unit-testable without a server.
 */
public final class ItemClassifier {
    private static final Set<String> BLACKLIST = Set.of(
            "air", "cave_air", "void_air",
            "barrier", "bedrock", "light",
            "command_block", "chain_command_block", "repeating_command_block",
            "command_block_minecart", "structure_block", "structure_void", "jigsaw",
            "debug_stick", "knowledge_book",
            "trial_spawner", "vault",
            "end_portal_frame", "end_portal", "end_gateway", "nether_portal",
            "reinforced_deepslate", "budding_amethyst", "petrified_oak_slab",
            "fire", "soul_fire", "frosted_ice", "bubble_column",
            "test_block", "test_instance_block"
    );

    private static final Set<String> BUYABLE_EXACT = Set.of(
            "coal", "charcoal", "raw_coal",
            "copper_ingot", "raw_copper", "raw_copper_block", "copper_block",
            "iron_ingot", "raw_iron", "raw_iron_block", "iron_block", "iron_nugget",
            "gold_ingot", "raw_gold", "raw_gold_block", "gold_block", "gold_nugget",
            "redstone", "redstone_block",
            "lapis_lazuli", "lapis_block",
            "quartz", "quartz_block",
            "diamond", "diamond_block",
            "emerald", "emerald_block",
            "amethyst_shard", "amethyst_block",
            "ancient_debris", "netherite_scrap", "netherite_ingot", "netherite_block",
            "sulfur", "potent_sulfur", "cinnabar",
            "glowstone_dust", "glowstone",
            "money_ore"
    );

    private static final Map<String, RarityTier> RARITY_OVERRIDES = Map.ofEntries(
            Map.entry("nether_star", RarityTier.LEGENDARY),
            Map.entry("dragon_egg", RarityTier.LEGENDARY),
            Map.entry("elytra", RarityTier.LEGENDARY),
            Map.entry("trident", RarityTier.LEGENDARY),
            Map.entry("totem_of_undying", RarityTier.LEGENDARY),
            Map.entry("enchanted_golden_apple", RarityTier.LEGENDARY),
            Map.entry("beacon", RarityTier.LEGENDARY),
            Map.entry("netherite_ingot", RarityTier.LEGENDARY),
            Map.entry("netherite_block", RarityTier.LEGENDARY),
            Map.entry("heavy_core", RarityTier.LEGENDARY),
            Map.entry("mace", RarityTier.LEGENDARY),
            Map.entry("ancient_debris", RarityTier.EPIC),
            Map.entry("netherite_scrap", RarityTier.EPIC),
            Map.entry("diamond", RarityTier.RARE),
            Map.entry("money_ore", RarityTier.RARE),
            Map.entry("diamond_block", RarityTier.EPIC),
            Map.entry("emerald", RarityTier.RARE),
            Map.entry("emerald_block", RarityTier.EPIC),
            Map.entry("enchanted_book", RarityTier.EPIC),
            Map.entry("echo_shard", RarityTier.EPIC),
            Map.entry("heart_of_the_sea", RarityTier.EPIC),
            Map.entry("shulker_shell", RarityTier.EPIC),
            Map.entry("dragon_breath", RarityTier.EPIC),
            Map.entry("conduit", RarityTier.EPIC),
            Map.entry("end_crystal", RarityTier.EPIC),
            Map.entry("respawn_anchor", RarityTier.EPIC),
            Map.entry("wither_skeleton_skull", RarityTier.EPIC),
            Map.entry("sniffer_egg", RarityTier.EPIC),
            Map.entry("netherite_helmet", RarityTier.EPIC),
            Map.entry("netherite_chestplate", RarityTier.EPIC),
            Map.entry("netherite_leggings", RarityTier.EPIC),
            Map.entry("netherite_boots", RarityTier.EPIC),
            Map.entry("netherite_sword", RarityTier.EPIC),
            Map.entry("netherite_pickaxe", RarityTier.EPIC),
            Map.entry("netherite_axe", RarityTier.EPIC),
            Map.entry("netherite_shovel", RarityTier.EPIC),
            Map.entry("netherite_hoe", RarityTier.EPIC),
            Map.entry("diamond_horse_armor", RarityTier.EPIC),
            Map.entry("golden_horse_armor", RarityTier.RARE),
            Map.entry("iron_horse_armor", RarityTier.UNCOMMON),
            Map.entry("gold_ingot", RarityTier.RARE),
            Map.entry("gold_block", RarityTier.RARE),
            Map.entry("raw_gold", RarityTier.RARE),
            Map.entry("raw_gold_block", RarityTier.RARE),
            Map.entry("nether_gold_ore", RarityTier.RARE),
            Map.entry("golden_apple", RarityTier.RARE),
            Map.entry("ender_pearl", RarityTier.RARE),
            Map.entry("ender_eye", RarityTier.RARE),
            Map.entry("blaze_rod", RarityTier.RARE),
            Map.entry("blaze_powder", RarityTier.RARE),
            Map.entry("ghast_tear", RarityTier.RARE),
            Map.entry("magma_cream", RarityTier.RARE),
            Map.entry("obsidian", RarityTier.RARE),
            Map.entry("crying_obsidian", RarityTier.RARE),
            Map.entry("amethyst_shard", RarityTier.RARE),
            Map.entry("redstone", RarityTier.UNCOMMON),
            Map.entry("redstone_block", RarityTier.UNCOMMON),
            Map.entry("lapis_lazuli", RarityTier.UNCOMMON),
            Map.entry("lapis_block", RarityTier.UNCOMMON),
            Map.entry("quartz", RarityTier.UNCOMMON),
            Map.entry("quartz_block", RarityTier.UNCOMMON),
            Map.entry("iron_ingot", RarityTier.UNCOMMON),
            Map.entry("iron_block", RarityTier.UNCOMMON),
            Map.entry("raw_iron", RarityTier.UNCOMMON),
            Map.entry("raw_iron_block", RarityTier.UNCOMMON),
            Map.entry("copper_ingot", RarityTier.COMMON),
            Map.entry("copper_block", RarityTier.COMMON),
            Map.entry("raw_copper", RarityTier.COMMON),
            Map.entry("raw_copper_block", RarityTier.COMMON),
            Map.entry("coal", RarityTier.COMMON),
            Map.entry("charcoal", RarityTier.COMMON),
            Map.entry("sulfur", RarityTier.COMMON),
            Map.entry("potent_sulfur", RarityTier.UNCOMMON),
            Map.entry("cinnabar", RarityTier.UNCOMMON)
    );

    private ItemClassifier() {
    }

    public static String path(String itemId) {
        int colon = itemId.indexOf(':');
        return (colon >= 0 ? itemId.substring(colon + 1) : itemId).toLowerCase(Locale.ROOT);
    }

    public static boolean isBlacklisted(String itemId) {
        String path = path(itemId);
        if (BLACKLIST.contains(path)) {
            return true;
        }
        if (path.startsWith("infested_") || path.startsWith("debug_")) {
            return true;
        }
        if (path.endsWith("_spawn_egg")) {
            return true;
        }
        return path.endsWith("_command_block");
    }

    public static boolean isBuyable(String itemId) {
        String path = path(itemId);
        if (isBlacklisted(itemId)) {
            return false;
        }
        if (BUYABLE_EXACT.contains(path)) {
            return true;
        }
        if (path.endsWith("_ore") || path.startsWith("raw_")) {
            return true;
        }
        if (path.endsWith("_ingot") || path.endsWith("_nugget") || path.endsWith("_scrap")) {
            return true;
        }
        return path.contains("ancient_debris");
    }

    public static RarityTier rarity(String itemId) {
        String path = path(itemId);
        RarityTier exact = RARITY_OVERRIDES.get(path);
        if (exact != null) {
            return exact;
        }
        if (path.contains("netherite")) {
            return path.contains("scrap") ? RarityTier.EPIC : RarityTier.LEGENDARY;
        }
        if (path.contains("ancient_debris")) {
            return RarityTier.EPIC;
        }
        if (path.contains("diamond") || path.contains("emerald")) {
            return RarityTier.RARE;
        }
        if (path.contains("gold")) {
            return RarityTier.RARE;
        }
        if (path.contains("iron")) {
            return RarityTier.UNCOMMON;
        }
        if (path.contains("redstone") || path.contains("lapis")
                || path.contains("amethyst") || path.contains("quartz")) {
            return RarityTier.UNCOMMON;
        }
        if (path.contains("copper") || path.contains("coal")
                || path.contains("sulfur") || path.contains("cinnabar")) {
            return RarityTier.COMMON;
        }
        return RarityTier.COMMON;
    }
}
