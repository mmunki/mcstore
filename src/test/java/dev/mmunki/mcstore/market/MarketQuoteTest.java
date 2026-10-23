package dev.mmunki.mcstore.market;

import dev.mmunki.mcstore.catalog.RarityTier;
import dev.mmunki.mcstore.config.PriceEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketQuoteTest {
    private static final PriceEntry DIAMOND = new PriceEntry("minecraft:diamond", RarityTier.RARE, 12_000L, 4_000L);

    @Test
    void fullStockQuotesAreCheaperThanScarceStock() {
        MarketQuote full = MarketQuote.of(DIAMOND, RarityTier.RARE.defaultStockCapacity());
        MarketQuote empty = MarketQuote.of(DIAMOND, 0);
        assertTrue(full.sellCents() < empty.sellCents());
        assertTrue(full.buyCents() < empty.buyCents());
        assertTrue(full.trendPercent() < empty.trendPercent());
        assertTrue(full.trendLabel().contains("oversupplied"));
        assertTrue(empty.trendLabel().contains("scarce"));
    }

    @Test
    void buyAlwaysStaysAboveSell() {
        for (int stock = 0; stock <= RarityTier.RARE.defaultStockCapacity(); stock++) {
            MarketQuote quote = MarketQuote.of(DIAMOND, stock);
            assertTrue(quote.buyCents() > quote.sellCents(), "stock " + stock);
            assertTrue(quote.sellCents() >= 1L);
        }
    }

    @Test
    void stockIsClampedAndCapacityDependsOnRarity() {
        MarketQuote quote = MarketQuote.of(DIAMOND, 10_000);
        assertEquals(RarityTier.RARE.defaultStockCapacity(), quote.stock());
        assertEquals(RarityTier.RARE.defaultStockCapacity(), quote.capacity());
        assertEquals(100, quote.fillPercent());
        assertEquals(0, quote.availableToSell());
    }

    @Test
    void sellOnlyItemsHaveNoBuyQuote() {
        PriceEntry log = new PriceEntry("minecraft:oak_log", RarityTier.COMMON, null, 250L);
        MarketQuote quote = MarketQuote.of(log, 0);
        assertFalse(quote.buyable());
        assertEquals(null, quote.buyCents());
        assertTrue(quote.sellCents() >= 1L);
    }

    @Test
    void rarityCapsAreOrdered() {
        assertTrue(RarityTier.COMMON.defaultStockCapacity() > RarityTier.UNCOMMON.defaultStockCapacity());
        assertTrue(RarityTier.UNCOMMON.defaultStockCapacity() > RarityTier.RARE.defaultStockCapacity());
        assertTrue(RarityTier.RARE.defaultStockCapacity() > RarityTier.EPIC.defaultStockCapacity());
        assertTrue(RarityTier.EPIC.defaultStockCapacity() > RarityTier.LEGENDARY.defaultStockCapacity());
    }
}
