package dev.mmunki.mcstore.config;

import dev.mmunki.mcstore.catalog.RarityTier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceBookTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsHumanReadablePrices() throws IOException {
        PriceBook book = PriceBook.generate(List.of(
                "minecraft:diamond", "minecraft:oak_log", "minecraft:air"));
        Path file = tempDir.resolve("prices.json");
        book.save(file);

        String json = Files.readString(file);
        assertTrue(json.contains("\"minecraft:diamond\""));
        assertTrue(json.contains("\"sell\": 40.00"));
        assertTrue(json.contains("\"rarity\": \"RARE\""));

        PriceBook loaded = PriceBook.load(file);
        PriceEntry diamond = loaded.get("diamond");
        assertNotNull(diamond);
        assertEquals(4_000L, diamond.sellCents());
        assertEquals(12_000L, diamond.buyCents());
        assertNull(loaded.get("air"));
    }

    @Test
    void setPriceUpdatesBothSidesAndSurvivesReload() throws IOException {
        PriceBook book = PriceBook.generate(List.of("minecraft:diamond", "minecraft:stone"));
        Path file = tempDir.resolve("prices.json");
        book.setSell("minecraft:diamond", 5_500L);
        book.setBuy("minecraft:stone", 250L);
        book.save(file);

        PriceBook loaded = PriceBook.load(file);
        assertEquals(5_500L, loaded.get("minecraft:diamond").sellCents());
        assertEquals(12_000L, loaded.get("minecraft:diamond").buyCents());
        assertEquals(250L, loaded.get("minecraft:stone").buyCents());
        assertEquals(RarityTier.COMMON, loaded.get("minecraft:stone").rarity());
    }

    @Test
    void bareMapFormatIsAccepted() throws IOException {
        Path file = tempDir.resolve("prices.json");
        Files.writeString(file, """
                {
                  "minecraft:stick": {"rarity": "COMMON", "sell": 2.50}
                }
                """);
        PriceBook book = PriceBook.load(file);
        PriceEntry stick = book.get("stick");
        assertNotNull(stick);
        assertEquals(250L, stick.sellCents());
        assertFalse(stick.buyable());
    }

    @Test
    void normalizeIdAddsTheDefaultNamespace() {
        assertEquals("minecraft:diamond", PriceBook.normalizeId("diamond"));
        assertEquals("minecraft:diamond", PriceBook.normalizeId("Diamond"));
        assertEquals("mod:thing", PriceBook.normalizeId("mod:thing"));
    }
}
