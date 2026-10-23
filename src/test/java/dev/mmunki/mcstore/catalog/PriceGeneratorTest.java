package dev.mmunki.mcstore.catalog;

import dev.mmunki.mcstore.config.PriceEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceGeneratorTest {
    @Test
    void diamondHasAnExplicitOverride() {
        PriceEntry diamond = PriceGenerator.defaultEntry("minecraft:diamond");
        assertEquals(4_000L, diamond.sellCents());
        assertEquals(12_000L, diamond.buyCents());
        assertEquals(RarityTier.RARE, diamond.rarity());
    }

    @Test
    void netheriteElytraAndBooksHaveOverrides() {
        assertEquals(22_500L, PriceGenerator.defaultEntry("minecraft:netherite_ingot").sellCents());
        assertEquals(25_000L, PriceGenerator.defaultEntry("minecraft:elytra").sellCents());
        assertEquals(7_500L, PriceGenerator.defaultEntry("minecraft:enchanted_book").sellCents());
        assertNull(PriceGenerator.defaultEntry("minecraft:elytra").buyCents());
    }

    @Test
    void generatedEntriesAreDeterministicAndFiltered() {
        List<String> ids = List.of(
                "minecraft:diamond", "minecraft:coal", "minecraft:oak_log",
                "minecraft:air", "minecraft:barrier", "minecraft:zombie_spawn_egg",
                "minecraft:wheat", "minecraft:redstone", "minecraft:iron_ingot"
        );
        List<PriceEntry> first = PriceGenerator.generate(ids);
        List<PriceEntry> second = PriceGenerator.generate(ids);
        assertEquals(first, second);
        assertEquals(6, first.size());
        assertFalse(first.stream().anyMatch(entry -> entry.itemId().equals("minecraft:air")));
        assertTrue(first.stream().allMatch(entry -> entry.sellCents() >= 1L));
        assertTrue(first.stream().filter(entry -> entry.itemId().equals("minecraft:diamond"))
                .allMatch(PriceEntry::buyable));
    }

    @Test
    void explicitOverridesStayInsideTheirRarityBand() {
        List<String> ids = List.of(
                "minecraft:coal", "minecraft:charcoal", "minecraft:copper_ingot",
                "minecraft:raw_copper", "minecraft:raw_copper_block", "minecraft:copper_block",
                "minecraft:iron_ingot", "minecraft:raw_iron", "minecraft:raw_iron_block",
                "minecraft:iron_block", "minecraft:gold_ingot", "minecraft:raw_gold",
                "minecraft:raw_gold_block", "minecraft:gold_block", "minecraft:redstone",
                "minecraft:redstone_block", "minecraft:lapis_lazuli", "minecraft:lapis_block",
                "minecraft:quartz", "minecraft:quartz_block", "minecraft:diamond",
                "minecraft:diamond_block", "minecraft:emerald", "minecraft:emerald_block",
                "minecraft:amethyst_shard", "minecraft:amethyst_block", "minecraft:sulfur",
                "minecraft:potent_sulfur", "minecraft:cinnabar", "minecraft:ancient_debris",
                "minecraft:netherite_scrap", "minecraft:netherite_ingot", "minecraft:netherite_block",
                "minecraft:elytra", "minecraft:enchanted_book", "minecraft:nether_star",
                "minecraft:dragon_egg", "minecraft:totem_of_undying", "minecraft:trident",
                "minecraft:beacon", "minecraft:enchanted_golden_apple", "minecraft:heavy_core",
                "minecraft:mace", "minecraft:echo_shard", "minecraft:heart_of_the_sea",
                "minecraft:shulker_shell", "minecraft:netherite_helmet",
                "minecraft:netherite_chestplate", "minecraft:netherite_leggings",
                "minecraft:netherite_boots", "minecraft:netherite_sword",
                "minecraft:netherite_pickaxe", "minecraft:netherite_axe",
                "minecraft:netherite_shovel", "minecraft:netherite_hoe");
        for (String id : ids) {
            PriceEntry entry = PriceGenerator.defaultEntry(id);
            assertTrue(entry.sellCents() >= entry.rarity().sellMinCents(),
                    id + " sell below " + entry.rarity());
            assertTrue(entry.sellCents() <= entry.rarity().sellMaxCents(),
                    id + " sell above " + entry.rarity());
            if (entry.buyable()) {
                assertTrue(entry.buyCents() >= entry.sellCents() * 2L,
                        id + " buy below 2x sell");
                assertTrue(entry.buyCents() <= entry.sellCents() * 4L,
                        id + " buy above 4x sell");
            }
        }
    }

    @Test
    void sellOnlyItemsDoNotGetABuyPrice() {
        PriceEntry log = PriceGenerator.defaultEntry("minecraft:oak_log");
        assertFalse(log.buyable());
        assertNull(log.buyCents());
    }
}
