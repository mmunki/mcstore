package dev.mmunki.mcstore.market;

import dev.mmunki.mcstore.catalog.RarityTier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockBookTest {
    @TempDir
    Path tempDir;

    @Test
    void absentItemsAreAtTheirRestingStock() {
        StockBook book = StockBook.empty(600);
        assertEquals(32, book.stock("minecraft:diamond", 64));
        assertEquals(256, book.stock("minecraft:coal", 512));
        assertEquals(0, book.stock("minecraft:diamond", 0));
    }

    @Test
    void everyFreshItemHasRoomToSellAndStockToBuy() {
        StockBook book = StockBook.empty(600);
        for (RarityTier tier : RarityTier.values()) {
            int capacity = tier.defaultStockCapacity();
            int stock = book.stock("minecraft:test_" + tier.name(), capacity);
            assertEquals(StockBook.restingStock(capacity), stock, tier.name());
            assertTrue(stock > 0, tier.name() + " has no stock to buy");
            assertTrue(stock < capacity, tier.name() + " has no room to sell");
        }
    }

    @Test
    void buyingConsumesAndSellingRefillsWithinTheCap() {
        StockBook book = StockBook.empty(600);
        assertEquals(20, book.consume("minecraft:diamond", 20, 64));
        assertEquals(12, book.stock("minecraft:diamond", 64));
        assertEquals(20, book.add("minecraft:diamond", 20, 64));
        assertEquals(32, book.stock("minecraft:diamond", 64));
        assertEquals(10, book.add("minecraft:diamond", 10, 64));
        assertEquals(42, book.stock("minecraft:diamond", 64));
        assertEquals(22, book.add("minecraft:diamond", 100, 64));
        assertEquals(64, book.stock("minecraft:diamond", 64));
        assertEquals(64, book.consume("minecraft:diamond", 100, 64));
        assertEquals(0, book.stock("minecraft:diamond", 64));
    }

    @Test
    void restockIsDueOnlyAfterTheConfiguredInterval() {
        StockBook book = StockBook.empty(600);
        long now = book.lastRestockEpochSecond();
        book.consume("minecraft:diamond", 10, 64);
        assertFalse(book.restockIfDue(now + 599));
        assertEquals(22, book.stock("minecraft:diamond", 64));
        assertTrue(book.restockIfDue(now + 600));
        assertEquals(32, book.stock("minecraft:diamond", 64));
        assertEquals(600, book.secondsUntilRestock(now + 600));
    }

    @Test
    void zeroIntervalDisablesRestocking() {
        StockBook book = StockBook.empty(0);
        book.consume("minecraft:diamond", 10, 64);
        assertFalse(book.restockIfDue(book.lastRestockEpochSecond() + 1_000_000));
        assertEquals(22, book.stock("minecraft:diamond", 64));
        assertEquals(-1, book.secondsUntilRestock(0));
    }

    @Test
    void restockRestoresRoomToSell() {
        StockBook book = StockBook.empty(600);
        int capacity = RarityTier.RARE.defaultStockCapacity();
        book.add("minecraft:diamond", capacity, capacity);
        assertEquals(capacity, book.stock("minecraft:diamond", capacity));
        book.restock(book.lastRestockEpochSecond() + 600);
        assertEquals(StockBook.restingStock(capacity), book.stock("minecraft:diamond", capacity));
    }

    @Test
    void persistsTradedStockAndTheClock() throws IOException {
        Path file = tempDir.resolve("stocks.json");
        StockBook book = StockBook.load(file, 600);
        book.consume("minecraft:diamond", 9, 64);
        book.consume("minecraft:coal", 512, 512);
        book.save(file);

        StockBook loaded = StockBook.load(file, 600);
        assertEquals(23, loaded.stock("minecraft:diamond", 64));
        assertEquals(0, loaded.stock("minecraft:coal", 512));
        assertEquals(book.lastRestockEpochSecond(), loaded.lastRestockEpochSecond());
        assertEquals(600, loaded.restockSeconds());
    }

    @Test
    void malformedFileKeepsValidEntriesAndUsesRestingStockForTheRest() throws IOException {
        Path file = tempDir.resolve("stocks.json");
        Files.writeString(file, """
                {
                  "restockSeconds": 120,
                  "lastRestock": 1000,
                  "stocks": {
                    "minecraft:diamond": 7,
                    "broken": "oops"
                  }
                }
                """);
        StockBook loaded = StockBook.load(file, 600);
        assertEquals(7, loaded.stock("minecraft:diamond", 64));
        assertEquals(32, loaded.stock("minecraft:emerald", 64));
        assertEquals(120, loaded.restockSeconds());
        assertEquals(1000L, loaded.lastRestockEpochSecond());
    }
}
