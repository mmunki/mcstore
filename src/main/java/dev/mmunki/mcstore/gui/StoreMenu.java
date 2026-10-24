package dev.mmunki.mcstore.gui;

import dev.mmunki.mcstore.McStore;
import dev.mmunki.mcstore.catalog.ItemClassifier;
import dev.mmunki.mcstore.catalog.RarityTier;
import dev.mmunki.mcstore.catalog.SellCategory;
import dev.mmunki.mcstore.config.PriceEntry;
import dev.mmunki.mcstore.economy.ExchangeService;
import dev.mmunki.mcstore.economy.Money;
import dev.mmunki.mcstore.economy.StoreService;
import dev.mmunki.mcstore.market.MarketQuote;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A read-only chest menu.  Every click is handled here and never reaches
 * ChestMenu's item-moving code, so a client cannot take items out of the shop.
 */
public final class StoreMenu extends ChestMenu {
    public enum Mode {
        BUY("Buy"),
        SELL("Sell"),
        SELL_CHEST("Sell Chest"),
        EXCHANGE("XP Exchange"),
        EDIT("Price Editor");

        private final String title;

        Mode(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    private static final int PAGE_SIZE = 45;
    private static final int DEPOSIT_SLOTS = 45;
    private static final int PLAYER_SLOT_START = 54;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_INFO = 46;
    private static final int SLOT_MODE = 47;
    private static final int SLOT_ACTION = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_STATUS = 50;
    private static final int SLOT_NEXT = 53;

    private final McStore store;
    private final StoreService service;
    private final ExchangeService exchangeService;
    private final ServerPlayer player;
    private final Mode mode;
    private final SimpleContainer container;

    private final Map<Integer, MenuAction> actions = new HashMap<>();
    private final Map<String, PriceEntry> draft = new LinkedHashMap<>();
    private boolean draftDirty;
    private boolean editingBuy = true;
    private SellCategory sellCategory = SellCategory.ALL;
    private int page;

    public StoreMenu(int syncId, Inventory playerInventory, ServerPlayer player,
                     McStore store, Mode mode) {
        super(MenuType.GENERIC_9x6, syncId, playerInventory, new SimpleContainer(54), 6);
        this.store = store;
        this.service = new StoreService(store);
        this.exchangeService = new ExchangeService(store);
        this.player = player;
        this.mode = mode;
        this.container = (SimpleContainer) getContainer();
        if (mode == Mode.EDIT) {
            loadDraft();
        }
        rebuild();
    }

    public static void open(ServerPlayer player, McStore store, Mode mode) {
        Component title = Component.literal("MCStore · " + mode.title());
        player.openMenu(new SimpleMenuProvider(
                (syncId, inventory, p) -> new StoreMenu(syncId, inventory, player, store, mode),
                title));
    }

    public void refresh() {
        if (mode == Mode.EDIT && !draftDirty) {
            loadDraft();
        }
        rebuild();
        broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player clickingPlayer) {
        if (!(clickingPlayer instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (mode == Mode.SELL_CHEST) {
            MenuAction action = slotId >= 0 && slotId < 54 ? actions.get(slotId) : null;
            if (action != null) {
                if (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE) {
                    action.run(serverPlayer, button, input == ContainerInput.QUICK_MOVE);
                }
                return;
            }
            super.clicked(slotId, button, input, clickingPlayer);
            return;
        }
        if (slotId < 0 || slotId >= 54) {
            return;
        }
        if (input != ContainerInput.PICKUP && input != ContainerInput.QUICK_MOVE) {
            return;
        }
        MenuAction action = actions.get(slotId);
        if (action == null) {
            return;
        }
        boolean shift = input == ContainerInput.QUICK_MOVE;
        action.run(serverPlayer, button, shift);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (mode != Mode.SELL_CHEST) {
            return ItemStack.EMPTY;
        }
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < DEPOSIT_SLOTS) {
            if (!moveItemStackTo(stack, PLAYER_SLOT_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, DEPOSIT_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public void removed(Player player) {
        if (mode == Mode.SELL_CHEST) {
            returnContents(player);
        }
        super.removed(player);
    }

    private void rebuild() {
        actions.clear();
        if (mode == Mode.SELL_CHEST) {
            buildSellChest();
            return;
        }
        for (int slot = 0; slot < 54; slot++) {
            container.setItem(slot, GuiUtil.filler());
        }

        if (mode == Mode.EXCHANGE) {
            buildExchange();
            return;
        }

        List<PriceEntry> entries = entriesForMode();
        int maxPage = Math.max(0, (entries.size() - 1) / PAGE_SIZE);
        if (page > maxPage) {
            page = maxPage;
        }
        if (page < 0) {
            page = 0;
        }
        int start = page * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        for (int index = start; index < end; index++) {
            PriceEntry entry = entries.get(index);
            int slot = index - start;
            ItemStack icon = itemIcon(entry);
            if (icon == null) {
                continue;
            }
            container.setItem(slot, icon);
            actions.put(slot, (clicker, button, shift) -> handleEntry(clicker, entry, button, shift));
        }

        container.setItem(SLOT_INFO, pageInfo(entries.size(), maxPage));
        container.setItem(SLOT_CLOSE, GuiUtil.icon(Items.BARRIER, "Close", ChatFormatting.RED,
                List.of("Return to the game.")));
        actions.put(SLOT_CLOSE, (clicker, button, shift) -> clicker.closeContainer());

        if (page > 0) {
            container.setItem(SLOT_PREV, GuiUtil.icon(Items.ARROW, "Previous page", ChatFormatting.WHITE,
                    List.of("Page " + page + " of " + (maxPage + 1))));
            actions.put(SLOT_PREV, (clicker, button, shift) -> {
                page--;
                rebuild();
                broadcastChanges();
            });
        }
        if (page < maxPage) {
            container.setItem(SLOT_NEXT, GuiUtil.icon(Items.ARROW, "Next page", ChatFormatting.WHITE,
                    List.of("Page " + (page + 2) + " of " + (maxPage + 1))));
            actions.put(SLOT_NEXT, (clicker, button, shift) -> {
                page++;
                rebuild();
                broadcastChanges();
            });
        }

        if (mode == Mode.EDIT) {
            buildEditFooter();
        } else if (mode == Mode.SELL) {
            buildSellFooter(entries.size());
        } else {
            buildStoreFooter(entries.size());
        }
    }

    private void buildStoreFooter(int count) {
        long balance = store.balances().balance(player.getUUID());
        int maxPage = Math.max(0, (count - 1) / PAGE_SIZE);
        container.setItem(SLOT_INFO, GuiUtil.icon(Items.GOLD_INGOT, "Balance: " + Money.format(balance),
                ChatFormatting.GOLD,
                List.of(mode == Mode.BUY ? "Buying spends this balance." : "Selling adds to this balance.")));
        container.setItem(SLOT_MODE, GuiUtil.icon(Items.PAPER,
                "Page " + (page + 1) + " / " + (maxPage + 1),
                ChatFormatting.WHITE,
                List.of(count + " " + (mode == Mode.BUY ? "tradable entries" : "sellable items"),
                        "Use the arrows to page.")));
        container.setItem(SLOT_ACTION, GuiUtil.icon(Items.EXPERIENCE_BOTTLE,
                "Exchange XP for money", ChatFormatting.AQUA,
                List.of("1 level = " + Money.format(store.config().xpCentsPerLevel()),
                        "Click to open the exchange.")));
        actions.put(SLOT_ACTION, (clicker, button, shift) -> open(clicker, store, Mode.EXCHANGE));
        container.setItem(SLOT_STATUS, GuiUtil.icon(Items.CLOCK,
                "Next restock: " + McStore.formatDuration(store.secondsUntilRestock()),
                ChatFormatting.YELLOW,
                List.of("Stock is consumed by buying and replenished by selling.",
                        "A restock returns every market item to half of its cap.")));
    }

    private void buildSellFooter(int count) {
        long balance = store.balances().balance(player.getUUID());
        int maxPage = Math.max(0, (count - 1) / PAGE_SIZE);
        container.setItem(SLOT_INFO, GuiUtil.icon(Items.GOLD_INGOT, "Balance: " + Money.format(balance),
                ChatFormatting.GOLD,
                List.of(count + " " + sellCategory.displayName().toLowerCase(java.util.Locale.ROOT)
                                + " items shown",
                        "Page " + (page + 1) + " / " + (maxPage + 1))));
        categoryButton(SLOT_MODE, SellCategory.ORES, Items.RAW_IRON);
        categoryButton(SLOT_ACTION, SellCategory.BLOCKS, Items.IRON_BLOCK);
        categoryButton(SLOT_CLOSE, SellCategory.MINERALS, Items.DIAMOND);
        categoryButton(SLOT_STATUS, SellCategory.ALL, Items.CHEST);
        container.setItem(51, GuiUtil.icon(Items.ENDER_CHEST, "Open sell chest", ChatFormatting.AQUA,
                List.of("Deposit any blocks, ores or items.",
                        "Sell one, a stack, the chest or your inventory.")));
        actions.put(51, (clicker, button, shift) -> open(clicker, store, Mode.SELL_CHEST));
        container.setItem(52, GuiUtil.icon(Items.BARRIER, "Close", ChatFormatting.RED,
                List.of("Return to the game.")));
        actions.put(52, (clicker, button, shift) -> clicker.closeContainer());
    }

    private void categoryButton(int slot, SellCategory category, Item item) {
        boolean selected = sellCategory == category;
        ChatFormatting color = selected ? ChatFormatting.AQUA : ChatFormatting.WHITE;
        container.setItem(slot, GuiUtil.icon(item,
                category.displayName() + (selected ? " (selected)" : ""), color,
                List.of(selected ? "Showing this category." : "Click to show this category.",
                        "Categories: ores, blocks, minerals, all.")));
        actions.put(slot, (clicker, button, shift) -> {
            sellCategory = category;
            page = 0;
            rebuild();
            broadcastChanges();
        });
    }

    private void buildSellChest() {
        for (int slot = DEPOSIT_SLOTS; slot < 54; slot++) {
            container.setItem(slot, GuiUtil.filler());
        }
        container.setItem(45, GuiUtil.icon(Items.ARROW, "Back to Sell", ChatFormatting.WHITE,
                List.of("Return to the category menu.",
                        "Items left in the chest go back to you.")));
        actions.put(45, (clicker, button, shift) -> {
            returnContents(clicker);
            open(clicker, store, Mode.SELL);
        });
        sellChestButton(46, Items.GOLD_NUGGET, "Sell 1 of each", 1,
                "Sell a single unit from every item type in the chest.");
        sellChestButton(47, Items.GOLD_INGOT, "Sell a stack of each", 64,
                "Sell up to one stack of every item type in the chest.");
        sellChestButton(48, Items.CHEST, "Sell everything in the chest", 0,
                "Sell every sellable item deposited in the chest.");
        container.setItem(49, GuiUtil.icon(Items.ENDER_CHEST, "Sell entire inventory",
                ChatFormatting.LIGHT_PURPLE,
                List.of("Sells every sellable item you carry.",
                        "The chest contents stay in the chest.")));
        actions.put(49, (clicker, button, shift) -> {
            if (service.sellAll(clicker)) {
                rebuild();
                broadcastChanges();
            }
        });
        container.setItem(50, GuiUtil.icon(Items.DIAMOND, "Sell held item", ChatFormatting.AQUA,
                List.of("Sell the stack in your main hand.")));
        actions.put(50, (clicker, button, shift) -> {
            if (service.sellHand(clicker)) {
                rebuild();
                broadcastChanges();
            }
        });
        container.setItem(51, GuiUtil.icon(Items.HOPPER, "Return chest items", ChatFormatting.YELLOW,
                List.of("Move the chest contents back to your inventory.")));
        actions.put(51, (clicker, button, shift) -> {
            returnContents(clicker);
            rebuild();
            broadcastChanges();
        });
        container.setItem(52, GuiUtil.icon(Items.PAPER, "Sell chest", ChatFormatting.WHITE,
                List.of("Put items in the top 45 slots.",
                        "Nothing is sold until you click a button.")));
        container.setItem(53, GuiUtil.icon(Items.BARRIER, "Close", ChatFormatting.RED,
                List.of("Return to the game.",
                        "Any remaining items go back to you.")));
        actions.put(53, (clicker, button, shift) -> {
            returnContents(clicker);
            clicker.closeContainer();
        });
    }

    private void sellChestButton(int slot, Item item, String name, int limit, String description) {
        container.setItem(slot, GuiUtil.icon(item, name, ChatFormatting.GREEN,
                List.of(description, "Only items the market is buying are sold.")));
        actions.put(slot, (clicker, button, shift) -> {
            if (service.sellContainer(clicker, container, limit)) {
                rebuild();
                broadcastChanges();
            }
        });
    }

    private void returnContents(Player player) {
        for (int slot = 0; slot < DEPOSIT_SLOTS; slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            container.setItem(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        container.setChanged();
    }

    private void buildExchange() {
        long balance = store.balances().balance(player.getUUID());
        long perLevel = store.config().xpCentsPerLevel();
        int levels = player.experienceLevel;

        container.setItem(4, GuiUtil.icon(Items.GOLD_INGOT, "Balance: " + Money.format(balance),
                ChatFormatting.GOLD,
                List.of("Exchange experience levels for money.")));
        container.setItem(13, GuiUtil.icon(Items.EXPERIENCE_BOTTLE,
                "Your experience: " + levels + " levels", ChatFormatting.GREEN,
                List.of("1 level = " + Money.format(perLevel),
                        "The exchange is instant and irreversible.")));

        int[] amounts = {1, 5, 10, 25};
        int[] slots = {20, 21, 22, 23};
        for (int i = 0; i < amounts.length; i++) {
            exchangeOffer(slots[i], amounts[i], levels, perLevel);
        }
        exchangeOffer(24, Math.max(0, levels), levels, perLevel);

        container.setItem(SLOT_INFO, GuiUtil.icon(Items.CLOCK,
                "Market restock: " + McStore.formatDuration(store.secondsUntilRestock()),
                ChatFormatting.YELLOW,
                List.of("Buy when stock is high, sell when stock is low.")));
        container.setItem(SLOT_CLOSE, GuiUtil.icon(Items.BARRIER, "Close", ChatFormatting.RED,
                List.of("Return to the game.")));
        actions.put(SLOT_CLOSE, (clicker, button, shift) -> clicker.closeContainer());
    }

    private void exchangeOffer(int slot, int amount, int levels, long perLevel) {
        if (amount <= 0 || levels < amount) {
            container.setItem(slot, GuiUtil.icon(Items.GLASS_BOTTLE,
                    amount == 0 ? "All levels" : amount + " levels",
                    ChatFormatting.DARK_GRAY,
                    List.of("Not enough experience.",
                            "You have " + levels + " levels.")));
            return;
        }
        long cents = amount * perLevel;
        String label = amount == levels ? "All " + amount + " levels" : amount + " level" + (amount == 1 ? "" : "s");
        container.setItem(slot, GuiUtil.icon(Items.EXPERIENCE_BOTTLE, label,
                ChatFormatting.GREEN,
                List.of("Receive: " + Money.format(cents),
                        "Current level: " + levels,
                        "Click to exchange.")));
        actions.put(slot, (clicker, button, shift) -> {
            if (exchangeService.exchange(clicker, amount)) {
                rebuild();
                broadcastChanges();
            }
        });
    }

    private void buildEditFooter() {
        container.setItem(SLOT_MODE, GuiUtil.icon(Items.COMPARATOR,
                "Editing: " + (editingBuy ? "BUY" : "SELL") + " price",
                editingBuy ? ChatFormatting.AQUA : ChatFormatting.GOLD,
                List.of("Click to switch between buy and sell.",
                        "LMB +1 · RMB -1 · Shift +10 / -10")));
        actions.put(SLOT_MODE, (clicker, button, shift) -> {
            editingBuy = !editingBuy;
            rebuild();
            broadcastChanges();
        });

        container.setItem(SLOT_ACTION, GuiUtil.icon(Items.EMERALD,
                "Save & Reload", ChatFormatting.GREEN,
                List.of("Writes prices.json immediately.",
                        "Refreshes every open /buy and /sell view.",
                        draftDirty ? "You have unsaved changes." : "No unsaved changes.")));
        actions.put(SLOT_ACTION, (clicker, button, shift) -> saveDraft());

        container.setItem(SLOT_STATUS, GuiUtil.icon(Items.HOPPER, "Reload prices.json", ChatFormatting.YELLOW,
                List.of("Discard this editor's draft and read the file again.")));
        actions.put(SLOT_STATUS, (clicker, button, shift) -> {
            loadDraft();
            draftDirty = false;
            rebuild();
            broadcastChanges();
            clicker.sendSystemMessage(Component.literal("MCStore price editor reloaded from disk.")
                    .withStyle(ChatFormatting.YELLOW));
        });
    }

    private void saveDraft() {
        store.prices().replaceAll(draft.values());
        boolean saved = store.savePrices();
        if (!saved) {
            player.sendSystemMessage(Component.literal("MCStore could not write prices.json.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        draftDirty = false;
        store.refreshOpenGuis();
        loadDraft();
        rebuild();
        broadcastChanges();
        player.sendSystemMessage(Component.literal("MCStore prices saved. /buy and /sell now use the new prices.")
                .withStyle(ChatFormatting.GREEN));
    }

    private void handleEntry(ServerPlayer clicker, PriceEntry entry, int button, boolean shift) {
        switch (mode) {
            case BUY -> {
                int amount = shift ? Math.max(1, new ItemStack(requireItem(entry)).getMaxStackSize()) : 1;
                service.buy(clicker, entry.itemId(), amount);
                refresh();
            }
            case SELL -> {
                service.sell(clicker, entry.itemId(), shift ? -1 : 1);
                refresh();
            }
            case EDIT -> adjust(entry, button, shift);
            case EXCHANGE -> {
                // handled by the exchange offers, not by item entries
            }
            case SELL_CHEST -> {
            }
        }
    }

    private void adjust(PriceEntry entry, int button, boolean shift) {
        long delta = (shift ? 1_000L : 100L) * (button == 1 ? -1L : 1L);
        long current;
        if (editingBuy) {
            if (entry.buyCents() == null) {
                if (!ItemClassifier.isBuyable(entry.itemId())) {
                    player.sendSystemMessage(Component.literal("That item is sell-only. Switch to SELL to edit it.")
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                current = entry.rarity().defaultBuyCents(entry.sellCents());
            } else {
                current = entry.buyCents();
            }
        } else {
            current = entry.sellCents();
        }
        long next = Math.max(Money.MIN_PRICE_CENTS, current + delta);
        PriceEntry updated = editingBuy ? entry.withBuyCents(next) : entry.withSellCents(next);
        draft.put(entry.itemId(), updated);
        draftDirty = true;
        rebuild();
        broadcastChanges();
    }

    private ItemStack itemIcon(PriceEntry entry) {
        Item item = StoreService.item(entry.itemId());
        if (item == null) {
            return null;
        }
        ItemStack stack = new ItemStack(item);
        ChatFormatting color = GuiUtil.color(entry.rarity());
        Component name = stack.getHoverName().copy().withStyle(color);
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("Rarity: " + entry.rarity().displayName()).withStyle(color));

        MarketQuote quote = store.quote(entry);
        if (mode == Mode.EDIT) {
            if (entry.buyCents() != null) {
                lore.add(Component.literal("Base buy: " + Money.format(entry.buyCents())).withStyle(ChatFormatting.GREEN));
            } else {
                lore.add(Component.literal("Buy: not for sale").withStyle(ChatFormatting.DARK_GRAY));
            }
            lore.add(Component.literal("Base sell: " + Money.format(entry.sellCents())).withStyle(ChatFormatting.GOLD));
            lore.add(Component.literal("Editing " + (editingBuy ? "BUY" : "SELL")
                    + " · LMB +1 · RMB -1 · Shift +10/-10").withStyle(ChatFormatting.AQUA));
            if (draftDirty) {
                lore.add(Component.literal("Unsaved draft").withStyle(ChatFormatting.YELLOW));
            }
        } else {
            if (mode == Mode.BUY && quote.buyable()) {
                lore.add(Component.literal("Market buy: " + Money.format(quote.buyCents())).withStyle(ChatFormatting.GREEN));
            }
            lore.add(Component.literal("Market sell: " + Money.format(quote.sellCents())).withStyle(ChatFormatting.GOLD));
            lore.add(Component.literal("Stock: " + stockBar(quote) + " " + quote.stock() + "/" + quote.capacity())
                    .withStyle(ChatFormatting.AQUA));
            lore.add(Component.literal("Trend: " + quote.trendLabel() + " · restock in "
                    + McStore.formatDuration(store.secondsUntilRestock())).withStyle(ChatFormatting.DARK_AQUA));
            lore.add(Component.literal(mode == Mode.BUY
                    ? "Click: buy 1 · Shift-click: buy a stack"
                    : "Click: sell 1 · Shift-click: sell everything").withStyle(ChatFormatting.GRAY));
        }
        return GuiUtil.icon(item, name, lore);
    }

    private static String stockBar(MarketQuote quote) {
        int slots = 10;
        int filled = Math.round(slots * quote.fillPercent() / 100.0f);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < slots; i++) {
            bar.append(i < filled ? '#' : '-');
        }
        return bar.append(']').toString();
    }

    private ItemStack pageInfo(int count, int maxPage) {
        ChatFormatting color = page < maxPage ? ChatFormatting.WHITE : ChatFormatting.GRAY;
        return GuiUtil.icon(Items.PAPER,
                "Page " + (page + 1) + " / " + (maxPage + 1),
                color,
                List.of(count + " entries", "Use the arrows to page."));
    }

    private void loadDraft() {
        draft.clear();
        for (PriceEntry entry : store.prices().all()) {
            if (store.isRegistered(entry.itemId())) {
                draft.put(entry.itemId(), entry);
            }
        }
    }

    private List<PriceEntry> entriesForMode() {
        return switch (mode) {
            case BUY -> store.buyableEntries();
            case SELL -> store.sellableEntries().stream()
                    .filter(entry -> sellCategory.matches(entry.itemId()))
                    .toList();
            case SELL_CHEST -> List.of();
            case EXCHANGE -> List.of();
            case EDIT -> new ArrayList<>(draft.values());
        };
    }

    private Item requireItem(PriceEntry entry) {
        Item item = StoreService.item(entry.itemId());
        if (item == null) {
            throw new IllegalStateException("missing item " + entry.itemId());
        }
        return item;
    }

    @FunctionalInterface
    private interface MenuAction {
        void run(ServerPlayer player, int button, boolean shift);
    }
}
