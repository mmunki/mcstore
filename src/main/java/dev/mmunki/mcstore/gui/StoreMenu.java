package dev.mmunki.mcstore.gui;

import dev.mmunki.mcstore.McStore;
import dev.mmunki.mcstore.catalog.ItemClassifier;
import dev.mmunki.mcstore.catalog.RarityTier;
import dev.mmunki.mcstore.config.PriceEntry;
import dev.mmunki.mcstore.economy.Money;
import dev.mmunki.mcstore.economy.StoreService;
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
    private static final int SLOT_PREV = 45;
    private static final int SLOT_INFO = 46;
    private static final int SLOT_MODE = 47;
    private static final int SLOT_ACTION = 48;
    private static final int SLOT_CLOSE = 49;
    private static final int SLOT_STATUS = 50;
    private static final int SLOT_NEXT = 53;

    private final McStore store;
    private final StoreService service;
    private final ServerPlayer player;
    private final Mode mode;
    private final SimpleContainer container;

    private final Map<Integer, MenuAction> actions = new HashMap<>();
    private final Map<String, PriceEntry> draft = new LinkedHashMap<>();
    private boolean draftDirty;
    private boolean editingBuy = true;
    private int page;

    public StoreMenu(int syncId, Inventory playerInventory, ServerPlayer player,
                     McStore store, Mode mode) {
        super(MenuType.GENERIC_9x6, syncId, playerInventory, new SimpleContainer(54), 6);
        this.store = store;
        this.service = new StoreService(store);
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
        return ItemStack.EMPTY;
    }

    private void rebuild() {
        actions.clear();
        for (int slot = 0; slot < 54; slot++) {
            container.setItem(slot, GuiUtil.filler());
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
                List.of(count + " " + (mode == Mode.BUY ? "buyable ores" : "sellable items"),
                        "Use the arrows to page.")));
        container.setItem(SLOT_STATUS, GuiUtil.icon(Items.CHEST,
                mode == Mode.BUY ? "Buy one, or shift-click for a stack" : "Sell one, or shift-click for everything",
                ChatFormatting.YELLOW,
                List.of("Every click is safe: the server verifies funds",
                        "and inventory before anything changes.")));
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
            case EDIT -> {
                adjust(entry, button, shift);
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
        if (entry.buyCents() != null) {
            lore.add(Component.literal("Buy: " + Money.format(entry.buyCents())).withStyle(ChatFormatting.GREEN));
        } else {
            lore.add(Component.literal("Buy: not for sale").withStyle(ChatFormatting.DARK_GRAY));
        }
        lore.add(Component.literal("Sell: " + Money.format(entry.sellCents())).withStyle(ChatFormatting.GOLD));
        switch (mode) {
            case BUY -> lore.add(Component.literal("Click: buy 1 · Shift-click: buy a stack")
                    .withStyle(ChatFormatting.GRAY));
            case SELL -> lore.add(Component.literal("Click: sell 1 · Shift-click: sell all")
                    .withStyle(ChatFormatting.GRAY));
            case EDIT -> {
                lore.add(Component.literal("Editing " + (editingBuy ? "BUY" : "SELL")
                        + " · LMB +1 · RMB -1 · Shift +10/-10").withStyle(ChatFormatting.AQUA));
                if (draftDirty) {
                    lore.add(Component.literal("Unsaved draft").withStyle(ChatFormatting.YELLOW));
                }
            }
        }
        return GuiUtil.icon(item, name, lore);
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
            case SELL -> store.sellableEntries();
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
