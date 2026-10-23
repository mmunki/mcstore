package dev.mmunki.mcstore.catalog;

import java.util.Collection;
import java.util.Set;

public record Coverage(int registryTotal, int filteredTotal, int pricedTotal) {
    public static final double TARGET = 0.80d;

    public static Coverage of(Collection<String> registryIds, Set<String> pricedIds,
                              Collection<String> filteredIds) {
        int priced = 0;
        for (String id : filteredIds) {
            if (pricedIds.contains(id)) {
                priced++;
            }
        }
        return new Coverage(registryIds.size(), filteredIds.size(), priced);
    }

    public double fractionOfFiltered() {
        return filteredTotal == 0 ? 0.0d : (double) pricedTotal / (double) filteredTotal;
    }

    public double fractionOfRegistry() {
        return registryTotal == 0 ? 0.0d : (double) pricedTotal / (double) registryTotal;
    }

    public int percentOfFiltered() {
        return (int) Math.floor(fractionOfFiltered() * 100.0d);
    }

    public int percentOfRegistry() {
        return (int) Math.floor(fractionOfRegistry() * 100.0d);
    }

    public boolean meetsTarget() {
        return fractionOfFiltered() >= TARGET;
    }
}
