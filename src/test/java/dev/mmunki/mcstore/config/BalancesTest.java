package dev.mmunki.mcstore.config;

import dev.mmunki.mcstore.economy.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalancesTest {
    @TempDir
    Path tempDir;

    @Test
    void startsAtOneHundred() {
        Balances balances = Balances.empty(Money.STARTING_BALANCE_CENTS);
        UUID player = UUID.randomUUID();
        assertEquals(10_000L, balances.balance(player));
        assertEquals(100.0d, Money.toDecimal(balances.balance(player)).doubleValue());
    }

    @Test
    void withdrawsAndCreditsWithoutFloatingPointDrift() {
        Balances balances = Balances.empty(10_000L);
        UUID player = UUID.randomUUID();
        assertTrue(balances.tryWithdraw(player, 2_550L));
        assertEquals(7_450L, balances.balance(player));
        balances.add(player, 1_275L);
        assertEquals(8_725L, balances.balance(player));
        assertFalse(balances.tryWithdraw(player, 10_000L));
        assertEquals(8_725L, balances.balance(player));
    }

    @Test
    void persistsAcrossReload() throws IOException {
        Path file = tempDir.resolve("balances.json");
        Balances balances = Balances.load(file, 10_000L);
        UUID player = UUID.randomUUID();
        balances.add(player, 12_345L);
        balances.save();

        Balances loaded = Balances.load(file, 10_000L);
        assertEquals(22_345L, loaded.balance(player));
        assertEquals(10_000L, loaded.balance(UUID.randomUUID()));
    }

    @Test
    void savesOnEveryChange() throws IOException {
        Path file = tempDir.resolve("balances.json");
        Balances balances = Balances.load(file, 10_000L);
        UUID player = UUID.randomUUID();
        balances.add(player, 500L);
        assertTrue(file.toFile().exists());
        Balances loaded = Balances.load(file, 10_000L);
        assertEquals(10_500L, loaded.balance(player));
    }
}
