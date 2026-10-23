package dev.mmunki.mcstore.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemClassifierTest {
    @Test
    void rarityOverridesCoverTheImportantItems() {
        assertEquals(RarityTier.RARE, ItemClassifier.rarity("minecraft:diamond"));
        assertEquals(RarityTier.EPIC, ItemClassifier.rarity("minecraft:enchanted_book"));
        assertEquals(RarityTier.LEGENDARY, ItemClassifier.rarity("minecraft:netherite_ingot"));
        assertEquals(RarityTier.LEGENDARY, ItemClassifier.rarity("minecraft:elytra"));
        assertEquals(RarityTier.EPIC, ItemClassifier.rarity("minecraft:netherite_scrap"));
        assertEquals(RarityTier.RARE, ItemClassifier.rarity("minecraft:gold_ingot"));
        assertEquals(RarityTier.UNCOMMON, ItemClassifier.rarity("minecraft:iron_ingot"));
        assertEquals(RarityTier.COMMON, ItemClassifier.rarity("minecraft:dirt"));
    }

    @Test
    void oreAndMineralFormsAreBuyable() {
        assertTrue(ItemClassifier.isBuyable("minecraft:coal"));
        assertTrue(ItemClassifier.isBuyable("minecraft:raw_iron"));
        assertTrue(ItemClassifier.isBuyable("minecraft:deepslate_diamond_ore"));
        assertTrue(ItemClassifier.isBuyable("minecraft:netherite_scrap"));
        assertTrue(ItemClassifier.isBuyable("minecraft:copper_ingot"));
        assertTrue(ItemClassifier.isBuyable("minecraft:ancient_debris"));
        assertTrue(ItemClassifier.isBuyable("mcstore:money_ore"));
        assertEquals(RarityTier.RARE, ItemClassifier.rarity("mcstore:money_ore"));
        assertFalse(ItemClassifier.isBuyable("minecraft:dirt"));
        assertFalse(ItemClassifier.isBuyable("minecraft:oak_log"));
        assertFalse(ItemClassifier.isBuyable("minecraft:diamond_sword"));
    }

    @Test
    void technicalAndCreativeOnlyItemsAreBlacklisted() {
        for (String id : new String[]{
                "minecraft:air", "minecraft:barrier", "minecraft:command_block",
                "minecraft:chain_command_block", "minecraft:structure_block",
                "minecraft:debug_stick", "minecraft:knowledge_book",
                "minecraft:zombie_spawn_egg", "minecraft:infested_stone",
                "minecraft:bedrock", "minecraft:light", "minecraft:budding_amethyst"
        }) {
            assertTrue(ItemClassifier.isBlacklisted(id), id);
        }
        assertFalse(ItemClassifier.isBlacklisted("minecraft:oak_log"));
        assertFalse(ItemClassifier.isBlacklisted("minecraft:diamond"));
        assertFalse(ItemClassifier.isBlacklisted("minecraft:spawner"));
    }

    @Test
    void namespaceIsOptional() {
        assertEquals("diamond", ItemClassifier.path("minecraft:diamond"));
        assertEquals("diamond", ItemClassifier.path("diamond"));
        assertTrue(ItemClassifier.isBuyable("diamond"));
    }
}
