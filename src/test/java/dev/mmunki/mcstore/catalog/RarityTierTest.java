package dev.mmunki.mcstore.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RarityTierTest {
    @Test
    void bandsMatchTheSpecification() {
        assertEquals(100L, RarityTier.COMMON.sellMinCents());
        assertEquals(500L, RarityTier.COMMON.sellMaxCents());
        assertEquals(500L, RarityTier.UNCOMMON.sellMinCents());
        assertEquals(1_500L, RarityTier.UNCOMMON.sellMaxCents());
        assertEquals(1_500L, RarityTier.RARE.sellMinCents());
        assertEquals(4_000L, RarityTier.RARE.sellMaxCents());
        assertEquals(4_000L, RarityTier.EPIC.sellMinCents());
        assertEquals(10_000L, RarityTier.EPIC.sellMaxCents());
        assertEquals(10_000L, RarityTier.LEGENDARY.sellMinCents());
        assertEquals(25_000L, RarityTier.LEGENDARY.sellMaxCents());
    }

    @Test
    void defaultSellStaysInsideTheBandAndIsDeterministic() {
        for (RarityTier tier : RarityTier.values()) {
            for (String id : new String[]{"minecraft:coal", "minecraft:diamond", "minecraft:stick", "minecraft:elytra"}) {
                long first = tier.defaultSellCents(id);
                long second = tier.defaultSellCents(id);
                assertEquals(first, second);
                assertTrue(first >= tier.sellMinCents() && first <= tier.sellMaxCents(),
                        tier + " " + id + " -> " + first);
            }
        }
    }

    @Test
    void defaultBuyIsBetweenTwoAndFourTimesSell() {
        for (RarityTier tier : RarityTier.values()) {
            long sell = tier.defaultSellCents("minecraft:example");
            long buy = tier.defaultBuyCents(sell);
            assertTrue(buy >= sell * 2L, tier + " buy should be at least 2x sell");
            assertTrue(buy <= sell * 4L, tier + " buy should be at most 4x sell");
        }
    }

    @Test
    void namesAreCaseInsensitive() {
        assertEquals(RarityTier.LEGENDARY, RarityTier.fromName("legendary"));
        assertEquals(RarityTier.UNCOMMON, RarityTier.fromName("Uncommon"));
        assertEquals(RarityTier.COMMON, RarityTier.fromName(null));
    }
}
