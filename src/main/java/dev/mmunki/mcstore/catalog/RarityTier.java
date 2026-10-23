package dev.mmunki.mcstore.catalog;

import java.util.Locale;

/**
 * The five rarity tiers used by the shop.  Bands are sell-price bands in
 * cents; buy prices are derived from the same tier with a 2x-4x multiplier.
 */
public enum RarityTier {
    COMMON("Common", "GRAY", 100L, 500L, 200),
    UNCOMMON("Uncommon", "GREEN", 500L, 1_500L, 250),
    RARE("Rare", "BLUE", 1_500L, 4_000L, 300),
    EPIC("Epic", "LIGHT_PURPLE", 4_000L, 10_000L, 350),
    LEGENDARY("Legendary", "GOLD", 10_000L, 25_000L, 400);

    private final String displayName;
    private final String colorName;
    private final long sellMinCents;
    private final long sellMaxCents;
    private final int buyPercent;

    RarityTier(String displayName, String colorName, long sellMinCents, long sellMaxCents, int buyPercent) {
        this.displayName = displayName;
        this.colorName = colorName;
        this.sellMinCents = sellMinCents;
        this.sellMaxCents = sellMaxCents;
        this.buyPercent = buyPercent;
    }

    public String displayName() {
        return displayName;
    }

    public String colorName() {
        return colorName;
    }

    public long sellMinCents() {
        return sellMinCents;
    }

    public long sellMaxCents() {
        return sellMaxCents;
    }

    public int buyPercent() {
        return buyPercent;
    }

    public String sellBand() {
        return "%.2f-%.2f".formatted(sellMinCents / 100.0, sellMaxCents / 100.0);
    }

    public long defaultSellCents(String itemId) {
        long span = sellMaxCents - sellMinCents;
        long hash = Integer.toUnsignedLong(itemId.hashCode());
        return sellMinCents + Math.floorMod(hash, span + 1L);
    }

    public long defaultBuyCents(long sellCents) {
        long multiplied = Math.round(sellCents * (buyPercent / 100.0d));
        return Math.max(sellCents + 1L, multiplied);
    }

    public static RarityTier fromName(String name) {
        if (name == null || name.isBlank()) {
            return COMMON;
        }
        String normalized = name.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        for (RarityTier tier : values()) {
            if (tier.name().equals(normalized) || tier.displayName.equalsIgnoreCase(name.trim())) {
                return tier;
            }
        }
        return COMMON;
    }
}
