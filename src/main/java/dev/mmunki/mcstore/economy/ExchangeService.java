package dev.mmunki.mcstore.economy;

import dev.mmunki.mcstore.McStore;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Converts experience levels into the server currency.  Experience is removed
 * first and the money is credited immediately, so a conversion can never
 * create money without spending the levels.
 */
public final class ExchangeService {
    private final McStore store;

    public ExchangeService(McStore store) {
        this.store = store;
    }

    public boolean exchange(ServerPlayer player, int levels) {
        int available = player.experienceLevel;
        if (levels <= 0) {
            player.sendSystemMessage(error("Enter a positive number of experience levels."));
            return false;
        }
        if (available < levels) {
            player.sendSystemMessage(error("You only have " + available + " experience levels."));
            return false;
        }
        long cents;
        try {
            cents = Math.multiplyExact(store.config().xpCentsPerLevel(), (long) levels);
        } catch (ArithmeticException e) {
            player.sendSystemMessage(error("That exchange is too large."));
            return false;
        }
        player.giveExperienceLevels(-levels);
        store.balances().add(player.getUUID(), cents);
        player.sendSystemMessage(success("Exchanged " + levels + " experience level"
                + (levels == 1 ? "" : "s") + " for " + Money.format(cents)
                + ". Balance: " + Money.format(store.balances().balance(player.getUUID()))));
        return true;
    }

    private static Component success(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GREEN);
    }

    private static Component error(String text) {
        return Component.literal(text).withStyle(ChatFormatting.RED);
    }
}
