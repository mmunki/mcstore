package dev.mmunki.mcstore.market;

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
    void absentItemsAreAtFullStock() {
        StockBook book = StockBook.empty(600);
        assertEquals(64, book.stock("minecraft:diamond", 64));
        assertEquals(512, book.stock("minecraft:coal", 512));
    }

    @Test
    void buyingConsumesAndSellingRefillsWithinTheCap() {
        StockBook book = StockBook.empty(600);
        assertEquals(20, book.consume("minecraft:diamond", 20, 64));
        assertEquals(44, book.stock("minecraft:diamond", 64));
        assertEquals(20, book.add("minecraft:diamond", 20, 64));
        assertEquals(64, book.stock("minecraft:diamond", 64));
        assertEquals(0, book.add("minecraft:diamond", 10, 64));
        assertEquals(64, book.consume("minecraft:diamond", 100, 64));
        assertEquals(0, book.stock("minecraft:diamond", 64));
    }

    @Test
    void restockIsDueOnlyAfterTheConfiguredInterval() {
        StockBook book = StockBook.empty(600);
        long now = book.lastRestockEpochSecond();
        book.consume("minecraft:diamond", 10, 64);
        assertFalse(book.restockIfDue(now + 599));
        assertEquals(54, book.stock("minecraft:diamond", 64));
        assertTrue(book.restockIfDue(now + 600));
        assertEquals(64, book.stock("minecraft:diamond", 64));
        assertEquals(600, book.secondsUntilRestock(now + 600));
    }

    @Test
    void zeroIntervalDisablesRestocking() {
        StockBook book = StockBook.empty(0);
        book.consume("minecraft:diamond", 10, 64);
        assertFalse(book.restockIfDue(book.lastRestockEpochSecond() + 1_000_000));
        assertEquals(54, book.stock("minecraft:diamond", 64));
        assertEquals(-1, book.secondsUntilRestock(0));
    }

    @Test
    void persistsTradedStockAndTheClock() throws IOException {
        Path file = tempDir.resolve("stocks.json");
        StockBook book = StockBook.load(file, 600);
        book.consume("minecraft:diamond", 9, 64);
        book.consume("minecraft:coal", 512, 512);
        book.save(file);

        StockBook loaded = StockBook.load(file, 600);
        assertEquals(55, loaded.stock("minecraft:diamond", 64));
        assertEquals(0, loaded.stock("minecraft:coal", 512));
        assertEquals(book.lastRestockEpochSecond(), loaded.lastRestockEpochSecond());
        assertEquals(600, loaded.restockSeconds());
    }

    @Test
    void malformedFileFallsBackToFullMarket() throws IOException {
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
        assertEquals(120, loaded.restockSeconds());
        assertEquals(1000L, loaded.lastRestockEpochSecond());
    }
}
