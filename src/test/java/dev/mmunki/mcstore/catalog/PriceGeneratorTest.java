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
    void moneyOreIsARareBuyableCommodity() {
        PriceEntry moneyOre = PriceGenerator.defaultEntry("mcstore:money_ore");
        assertEquals(RarityTier.RARE, moneyOre.rarity());
        assertEquals(5_000L, moneyOre.sellCents());
        assertEquals(15_000L, moneyOre.buyCents());
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
    void mineralBlocksAreEightPointNineTimesTheirBaseMaterial() {
        PriceEntry ironBlock = PriceGenerator.defaultEntry("minecraft:iron_block");
        assertEquals(8_900L, ironBlock.sellCents());
        assertEquals(22_250L, ironBlock.buyCents());
        assertEquals(RarityTier.UNCOMMON, ironBlock.rarity());

        PriceEntry diamondBlock = PriceGenerator.defaultEntry("minecraft:diamond_block");
        assertEquals(35_600L, diamondBlock.sellCents());
        assertEquals(106_800L, diamondBlock.buyCents());
        assertEquals(RarityTier.RARE, diamondBlock.rarity());

        PriceEntry rawIronBlock = PriceGenerator.defaultEntry("minecraft:raw_iron_block");
        assertEquals(7_120L, rawIronBlock.sellCents());
        assertEquals(17_800L, rawIronBlock.buyCents());

        PriceEntry netheriteBlock = PriceGenerator.defaultEntry("minecraft:netherite_block");
        assertEquals(200_250L, netheriteBlock.sellCents());
        assertEquals(801_000L, netheriteBlock.buyCents());
        assertEquals(RarityTier.LEGENDARY, netheriteBlock.rarity());
    }

    @Test
    void explicitOverridesStayInsideTheirRarityBand() {
        List<String> ids = List.of(
                "minecraft:coal", "minecraft:charcoal", "minecraft:copper_ingot",
                "minecraft:raw_copper", "minecraft:iron_ingot", "minecraft:raw_iron",
                "minecraft:gold_ingot", "minecraft:raw_gold", "minecraft:redstone",
                "minecraft:lapis_lazuli", "minecraft:quartz", "minecraft:diamond",
                "minecraft:emerald", "minecraft:amethyst_shard", "minecraft:sulfur",
                "minecraft:potent_sulfur", "minecraft:cinnabar", "minecraft:ancient_debris",
                "minecraft:netherite_scrap", "minecraft:netherite_ingot",
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
    void storageBlocksOfOresAreBuyable() {
        assertTrue(PriceGenerator.defaultEntry("minecraft:coal_block").buyable());
        assertTrue(PriceGenerator.defaultEntry("minecraft:raw_iron_block").buyable());
        assertTrue(PriceGenerator.defaultEntry("minecraft:netherite_block").buyable());
    }

    @Test
    void sellOnlyItemsDoNotGetABuyPrice() {
        PriceEntry log = PriceGenerator.defaultEntry("minecraft:oak_log");
        assertFalse(log.buyable());
        assertNull(log.buyCents());
    }
}
