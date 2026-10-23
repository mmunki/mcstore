package dev.mmunki.mcstore.market;

import dev.mmunki.mcstore.config.PriceEntry;
import dev.mmunki.mcstore.economy.Money;

/**
 * A live quote for one item.  The shop never trades at the raw price from
 * prices.json: the price moves with the stock level, the same way a real
 * market moves with supply.  Buying consumes stock and pushes the price up;
 * selling refills stock and pushes the price down.
 */
public record MarketQuote(String itemId, int stock, int capacity,
                          Long buyCents, long sellCents, int trendPercent) {
    public static MarketQuote of(PriceEntry entry, int stock) {
        int capacity = entry.rarity().defaultStockCapacity();
        int clamped = Math.max(0, Math.min(capacity, stock));
        double fill = capacity <= 0 ? 0.0d : (double) clamped / (double) capacity;
        double scarcity = 1.0d - fill;
        double factor = 0.75d + 0.50d * scarcity;

        long sell = Math.max(Money.MIN_PRICE_CENTS, Math.round(entry.sellCents() * factor));
        Long buy = null;
        if (entry.buyCents() != null) {
            buy = Math.max(sell + 1L, Math.round(entry.buyCents() * factor));
        }
        int trend = (int) Math.round((factor - 1.0d) * 100.0d);
        return new MarketQuote(entry.itemId(), clamped, capacity, buy, sell, trend);
    }

    public boolean buyable() {
        return buyCents != null;
    }

    public int fillPercent() {
        return capacity <= 0 ? 0 : (int) Math.round(100.0d * stock / capacity);
    }

    public int availableToSell() {
        return Math.max(0, capacity - stock);
    }

    public String trendLabel() {
        if (trendPercent > 1) {
            return "+" + trendPercent + "% scarce";
        }
        if (trendPercent < -1) {
            return trendPercent + "% oversupplied";
        }
        return "stable";
    }
}
