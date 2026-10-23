package dev.mmunki.mcstore.economy;

import dev.mmunki.mcstore.McStore;
import dev.mmunki.mcstore.config.PriceBook;
import dev.mmunki.mcstore.config.PriceEntry;
import dev.mmunki.mcstore.market.MarketQuote;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class StoreService {
    private static final SimpleContainer NO_EXTRA_CONTAINER = new SimpleContainer(0);

    private final McStore store;

    public StoreService(McStore store) {
        this.store = store;
    }

    public boolean buy(ServerPlayer player, String rawItemId, int amount) {
        String itemId = PriceBook.normalizeId(rawItemId);
        PriceEntry entry = store.prices().get(itemId);
        if (entry == null || !entry.buyable()) {
            player.sendSystemMessage(error("MCStore does not sell that item."));
            return false;
        }
        Item item = item(itemId);
        if (item == null) {
            player.sendSystemMessage(error("Unknown item: " + itemId));
            return false;
        }
        MarketQuote quote = store.quote(entry);
        int quantity = Math.max(1, amount);
        ItemStack stack = new ItemStack(item, quantity);
        quantity = stack.getCount();
        if (quote.stock() < quantity) {
            player.sendSystemMessage(error("Only " + quote.stock() + "x " + displayName(stack)
                    + " left in stock. The market restocks in "
                    + McStore.formatDuration(store.secondsUntilRestock()) + "."));
            return false;
        }
        long cost = Math.multiplyExact(quote.buyCents(), (long) quantity);
        long balance = store.balances().balance(player.getUUID());
        if (balance < cost) {
            player.sendSystemMessage(error("Not enough money. That costs " + Money.format(cost)
                    + ", your balance is " + Money.format(balance) + "."));
            return false;
        }
        if (!canFit(player, stack)) {
            player.sendSystemMessage(error("Your inventory is full. Make room for "
                    + quantity + "x " + displayName(stack) + "."));
            return false;
        }
        if (!store.balances().tryWithdraw(player.getUUID(), cost)) {
            player.sendSystemMessage(error("Not enough money."));
            return false;
        }
        int removed = store.stocks().consume(itemId, quantity, quote.capacity());
        if (removed < quantity) {
            store.balances().add(player.getUUID(), cost);
            player.sendSystemMessage(error("The market could not fill that order. The purchase was refunded."));
            return false;
        }
        if (!player.getInventory().add(stack)) {
            store.balances().add(player.getUUID(), cost);
            store.stocks().add(itemId, removed, quote.capacity());
            player.sendSystemMessage(error("Your inventory is full. The purchase was refunded."));
            return false;
        }
        player.sendSystemMessage(success("Bought " + quantity + "x " + displayName(stack)
                + " at " + Money.format(quote.buyCents()) + " each for " + Money.format(cost)
                + ". Stock left: " + quote.stock() + " -> " + (quote.stock() - quantity)
                + " · Balance: " + Money.format(store.balances().balance(player.getUUID()))));
        return true;
    }

    public boolean sell(ServerPlayer player, String rawItemId, int amountOrAll) {
        String itemId = PriceBook.normalizeId(rawItemId);
        PriceEntry entry = store.prices().get(itemId);
        if (entry == null) {
            player.sendSystemMessage(error("MCStore does not buy that item."));
            return false;
        }
        MarketQuote quote = store.quote(entry);
        if (quote.availableToSell() <= 0) {
            player.sendSystemMessage(error("The store is fully stocked with " + prettyId(itemId)
                    + " and is not buying more right now."));
            return false;
        }
        int available = countItem(player, itemId);
        if (available < 1) {
            player.sendSystemMessage(error("You do not have " + prettyId(itemId) + " in your inventory."));
            return false;
        }
        int amount = amountOrAll <= 0 ? available : Math.min(amountOrAll, available);
        amount = Math.min(amount, quote.availableToSell());
        int removed = removeItem(player, itemId, amount);
        if (removed < 1) {
            player.sendSystemMessage(error("You do not have enough of that item."));
            return false;
        }
        int added = store.stocks().add(itemId, removed, quote.capacity());
        long earned = Math.multiplyExact(quote.sellCents(), (long) added);
        store.balances().add(player.getUUID(), earned);
        ItemStack sample = stack(itemId);
        String name = sample == null ? prettyId(itemId) : displayName(sample);
        player.sendSystemMessage(success("Sold " + added + "x " + name + " at "
                + Money.format(quote.sellCents()) + " each for " + Money.format(earned)
                + ". Store stock: " + quote.stock() + " -> " + (quote.stock() + added)
                + " · Balance: " + Money.format(store.balances().balance(player.getUUID()))));
        return true;
    }

    public boolean sellHand(ServerPlayer player) {
        ItemStack held = player.getInventory().getSelectedItem();
        if (held.isEmpty()) {
            player.sendSystemMessage(error("You are not holding an item."));
            return false;
        }
        String itemId = idOf(held);
        int amount = held.getCount();
        return sell(player, itemId, amount);
    }

    public boolean sellAll(ServerPlayer player) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty()) {
                continue;
            }
            String id = idOf(stack);
            PriceEntry entry = store.prices().get(id);
            if (entry != null) {
                counts.merge(id, stack.getCount(), Integer::sum);
            }
        }
        if (counts.isEmpty()) {
            player.sendSystemMessage(error("Nothing in your inventory can be sold."));
            return false;
        }
        long total = 0L;
        int totalItems = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            PriceEntry price = store.prices().get(entry.getKey());
            if (price == null) {
                continue;
            }
            MarketQuote quote = store.quote(price);
            int space = quote.availableToSell();
            if (space <= 0) {
                continue;
            }
            int wanted = Math.min(entry.getValue(), space);
            int removed = removeItem(player, entry.getKey(), wanted);
            if (removed > 0) {
                int added = store.stocks().add(entry.getKey(), removed, quote.capacity());
                total = Math.addExact(total, Math.multiplyExact(quote.sellCents(), (long) added));
                totalItems += added;
            }
        }
        if (totalItems == 0) {
            player.sendSystemMessage(error("Everything you can sell is already fully stocked at the store."));
            return false;
        }
        store.balances().add(player.getUUID(), total);
        player.sendSystemMessage(success("Sold " + totalItems + " items for " + Money.format(total)
                + ". Balance: " + Money.format(store.balances().balance(player.getUUID()))));
        return true;
    }

    public static String idOf(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public static Item item(String itemId) {
        try {
            Identifier id = Identifier.tryParse(PriceBook.normalizeId(itemId));
            if (id == null) {
                return null;
            }
            Optional<Item> value = BuiltInRegistries.ITEM.getOptional(id);
            return value.orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static ItemStack stack(String itemId) {
        Item item = item(itemId);
        return item == null ? null : new ItemStack(item);
    }

    public static String displayName(ItemStack stack) {
        return stack.getHoverName().getString();
    }

    public static String prettyId(String itemId) {
        String path = PriceBook.normalizeId(itemId);
        int colon = path.indexOf(':');
        if (colon >= 0) {
            path = path.substring(colon + 1);
        }
        String[] words = path.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private static int countItem(ServerPlayer player, String itemId) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty() && idOf(stack).equals(itemId)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int removeItem(ServerPlayer player, String itemId, int amount) {
        return player.getInventory().clearOrCountMatchingItems(
                stack -> !stack.isEmpty() && idOf(stack).equals(itemId),
                amount,
                NO_EXTRA_CONTAINER);
    }

    private static boolean canFit(ServerPlayer player, ItemStack stack) {
        int capacity = 0;
        int max = stack.getMaxStackSize();
        for (ItemStack slot : player.getInventory().getNonEquipmentItems()) {
            if (slot.isEmpty()) {
                capacity += max;
            } else if (ItemStack.isSameItemSameComponents(slot, stack)) {
                capacity += Math.max(0, slot.getMaxStackSize() - slot.getCount());
            }
            if (capacity >= stack.getCount()) {
                return true;
            }
        }
        return capacity >= stack.getCount();
    }

    private static Component success(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GREEN);
    }

    private static Component error(String text) {
        return Component.literal(text).withStyle(ChatFormatting.RED);
    }
}
