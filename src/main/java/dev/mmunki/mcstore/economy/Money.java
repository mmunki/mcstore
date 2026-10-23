package dev.mmunki.mcstore.economy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * All money in MCStore is handled as integer cents.  The text helpers only
 * exist at the edges (commands, JSON, GUI) where a human sees a decimal.
 */
public final class Money {
    public static final long STARTING_BALANCE_CENTS = 10_000L;
    public static final long MIN_PRICE_CENTS = 1L;

    private Money() {
    }

    public static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    public static BigDecimal toDecimal(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }

    public static String format(long cents) {
        return toDecimal(cents).setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    public static String formatWithCurrency(long cents) {
        return "$" + format(cents);
    }

    /**
     * Parses {@code 12}, {@code 12.5} or {@code $12.50}.  The value is never
     * negative and at least one cent.
     */
    public static long parseCents(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("price is missing");
        }
        String cleaned = raw.trim().replace("$", "").replace(",", "");
        BigDecimal value = new BigDecimal(cleaned);
        long cents = toCents(value);
        if (cents < MIN_PRICE_CENTS) {
            throw new IllegalArgumentException("price must be at least 0.01");
        }
        return cents;
    }

    public static String debug(long cents) {
        return String.format(Locale.ROOT, "%d cents", cents);
    }
}
