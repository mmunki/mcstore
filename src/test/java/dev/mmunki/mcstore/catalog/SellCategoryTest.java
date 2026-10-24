package dev.mmunki.mcstore.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SellCategoryTest {
    @Test
    void oresCategoryContainsOresAndRawMaterials() {
        assertTrue(SellCategory.ORES.matches("minecraft:iron_ore"));
        assertTrue(SellCategory.ORES.matches("minecraft:raw_iron"));
        assertTrue(SellCategory.ORES.matches("minecraft:ancient_debris"));
        assertFalse(SellCategory.ORES.matches("minecraft:iron_block"));
        assertFalse(SellCategory.ORES.matches("minecraft:iron_ingot"));
    }

    @Test
    void blocksCategoryContainsMineralStorageBlocks() {
        assertTrue(SellCategory.BLOCKS.matches("minecraft:iron_block"));
        assertTrue(SellCategory.BLOCKS.matches("minecraft:raw_iron_block"));
        assertTrue(SellCategory.BLOCKS.matches("minecraft:diamond_block"));
        assertFalse(SellCategory.BLOCKS.matches("minecraft:iron_ore"));
        assertFalse(SellCategory.BLOCKS.matches("minecraft:stone"));
    }

    @Test
    void mineralsCategoryContainsRefinedMinerals() {
        assertTrue(SellCategory.MINERALS.matches("minecraft:iron_ingot"));
        assertTrue(SellCategory.MINERALS.matches("minecraft:diamond"));
        assertTrue(SellCategory.MINERALS.matches("minecraft:netherite_scrap"));
        assertFalse(SellCategory.MINERALS.matches("minecraft:iron_ore"));
        assertFalse(SellCategory.MINERALS.matches("minecraft:iron_block"));
    }

    @Test
    void allCategoryContainsEverything() {
        assertTrue(SellCategory.ALL.matches("minecraft:oak_log"));
        assertTrue(SellCategory.ALL.matches("minecraft:diamond"));
    }
}
