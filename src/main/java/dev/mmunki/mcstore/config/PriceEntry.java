package dev.mmunki.mcstore.config;

import dev.mmunki.mcstore.catalog.RarityTier;

import java.util.Objects;

public record PriceEntry(String itemId, RarityTier rarity, Long buyCents, long sellCents) {
    public PriceEntry {
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(rarity, "rarity");
        if (sellCents < 1L) {
            throw new IllegalArgumentException("sell price must be positive: " + itemId);
        }
        if (buyCents != null && buyCents < 1L) {
            throw new IllegalArgumentException("buy price must be positive: " + itemId);
        }
    }

    public boolean buyable() {
        return buyCents != null;
    }

    public PriceEntry withBuyCents(Long buy) {
        return new PriceEntry(itemId, rarity, buy, sellCents);
    }

    public PriceEntry withSellCents(long sell) {
        return new PriceEntry(itemId, rarity, buyCents, sell);
    }

    public PriceEntry withRarity(RarityTier tier) {
        return new PriceEntry(itemId, tier, buyCents, sellCents);
    }
}
