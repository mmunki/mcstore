package dev.mmunki.mcstore.economy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {
    @Test
    void formatsAndParsesCentsExactly() {
        assertEquals("12.50", Money.format(1_250L));
        assertEquals("$100.00", Money.formatWithCurrency(10_000L));
        assertEquals(1_250L, Money.parseCents("12.50"));
        assertEquals(1_250L, Money.parseCents("$12.50"));
        assertEquals(1_200L, Money.parseCents("12"));
    }

    @Test
    void roundsHalfUp() {
        assertEquals(1_235L, Money.toCents(new BigDecimal("12.345")));
        assertEquals(1_234L, Money.toCents(new BigDecimal("12.344")));
    }

    @Test
    void rejectsNonPositivePrices() {
        assertThrows(IllegalArgumentException.class, () -> Money.parseCents("0"));
        assertThrows(IllegalArgumentException.class, () -> Money.parseCents("-1.00"));
    }
}
