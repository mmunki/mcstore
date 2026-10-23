package dev.mmunki.mcstore.catalog;

import dev.mmunki.mcstore.config.PriceBook;
import dev.mmunki.mcstore.config.PriceEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverageTest {
    @Test
    void coverageCountsOnlyFilteredItems() {
        List<String> registry = List.of("minecraft:a", "minecraft:b", "minecraft:barrier", "minecraft:c");
        List<String> filtered = List.of("minecraft:a", "minecraft:b", "minecraft:c");
        Set<String> priced = Set.of("minecraft:a", "minecraft:b");
        Coverage coverage = Coverage.of(registry, priced, filtered);
        assertEquals(4, coverage.registryTotal());
        assertEquals(3, coverage.filteredTotal());
        assertEquals(2, coverage.pricedTotal());
        assertEquals(66, coverage.percentOfFiltered());
        assertFalse(coverage.meetsTarget());
    }

    @Test
    void generatedDefaultsReachTheCoverageTarget() {
        List<String> registry = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            registry.add("minecraft:test_item_" + i);
        }
        registry.add("minecraft:air");
        registry.add("minecraft:barrier");
        registry.add("minecraft:command_block");
        List<String> filtered = registry.stream().filter(id -> !ItemClassifier.isBlacklisted(id)).toList();
        PriceBook book = PriceBook.generate(registry);
        Coverage coverage = book.coverage(registry, filtered);
        assertEquals(100, coverage.filteredTotal());
        assertEquals(100, coverage.pricedTotal());
        assertEquals(100, coverage.percentOfFiltered());
        assertTrue(coverage.meetsTarget());
    }

    @Test
    void aPartialPriceFileCanStillFailTheTarget() {
        List<String> registry = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            registry.add("minecraft:test_item_" + i);
        }
        PriceBook book = new PriceBook();
        book.replaceAll(List.of(new PriceEntry("minecraft:test_item_0", RarityTier.COMMON, null, 100L)));
        Coverage coverage = book.coverage(registry, registry);
        assertEquals(1, coverage.pricedTotal());
        assertEquals(1, coverage.percentOfFiltered());
        assertFalse(coverage.meetsTarget());
    }
}
