package dev.mmunki.mcstore.catalog;

import java.util.Locale;

public enum SellCategory {
    ALL,
    ORES,
    BLOCKS,
    MINERALS;

    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
    }

    public boolean matches(String itemId) {
        return switch (this) {
            case ALL -> true;
            case ORES -> ItemClassifier.isOre(itemId);
            case BLOCKS -> ItemClassifier.isMineralBlock(itemId);
            case MINERALS -> ItemClassifier.isMineral(itemId)
                    && !ItemClassifier.isOre(itemId)
                    && !ItemClassifier.isMineralBlock(itemId);
        };
    }
}
