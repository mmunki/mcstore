package dev.mmunki.mcstore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.mmunki.mcstore.McStore;
import dev.mmunki.mcstore.catalog.ItemClassifier;
import dev.mmunki.mcstore.config.PriceBook;
import dev.mmunki.mcstore.config.PriceEntry;
import dev.mmunki.mcstore.economy.Money;
import dev.mmunki.mcstore.economy.StoreService;
import dev.mmunki.mcstore.gui.StoreMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class StoreCommands {
    private StoreCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, McStore store) {
        StoreService service = new StoreService(store);

        dispatcher.register(Commands.literal("buy")
                .executes(context -> openStore(context.getSource(), store, StoreMenu.Mode.BUY))
                .then(Commands.argument("material", IdentifierArgument.id())
                        .suggests((context, builder) -> suggestIds(builder, buyableIds(store)))
                        .executes(context -> directBuy(context, store, service))));

        dispatcher.register(Commands.literal("sell")
                .executes(context -> openStore(context.getSource(), store, StoreMenu.Mode.SELL))
                .then(Commands.literal("hand").executes(context -> sellHand(context.getSource(), service)))
                .then(Commands.literal("all").executes(context -> sellAll(context.getSource(), service))));

        dispatcher.register(Commands.literal("balance")
                .executes(context -> balance(context.getSource(), store)));

        dispatcher.register(Commands.literal("mcstore")
                .requires(store::isAdmin)
                .executes(context -> status(context.getSource(), store))
                .then(Commands.literal("status")
                        .executes(context -> status(context.getSource(), store)))
                .then(Commands.literal("reload")
                        .executes(context -> reload(context.getSource(), store)))
                .then(Commands.literal("edit")
                        .executes(context -> edit(context.getSource(), store)))
                .then(Commands.literal("setprice")
                        .then(Commands.argument("material", IdentifierArgument.id())
                                .suggests((context, builder) -> suggestIds(builder, allIds(store)))
                                .then(Commands.argument("side", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            builder.suggest("buy");
                                            builder.suggest("sell");
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.01))
                                                .executes(context -> setPrice(context, store)))))));
    }

    private static int openStore(CommandSourceStack source, McStore store, StoreMenu.Mode mode) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can open the store."));
            return 0;
        }
        if (!store.ready()) {
            source.sendFailure(Component.literal("MCStore is still starting. Try again in a moment."));
            return 0;
        }
        StoreMenu.open(player, store, mode);
        return 1;
    }

    private static int directBuy(CommandContext<CommandSourceStack> context, McStore store, StoreService service) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.literal("Only a player can buy items."));
            return 0;
        }
        if (!store.ready()) {
            context.getSource().sendFailure(Component.literal("MCStore is still starting. Try again in a moment."));
            return 0;
        }
        String id = PriceBook.normalizeId(IdentifierArgument.getId(context, "material").toString());
        if (!store.isRegistered(id)) {
            context.getSource().sendFailure(Component.literal("Unknown item: " + id));
            return 0;
        }
        if (!ItemClassifier.isBuyable(id)) {
            context.getSource().sendFailure(Component.literal("Only ores and minerals are buyable. Open /buy to browse."));
            return 0;
        }
        return service.buy(player, id, 1) ? 1 : 0;
    }

    private static int sellHand(CommandSourceStack source, StoreService service) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can sell items."));
            return 0;
        }
        return service.sellHand(player) ? 1 : 0;
    }

    private static int sellAll(CommandSourceStack source, StoreService service) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can sell items."));
            return 0;
        }
        return service.sellAll(player) ? 1 : 0;
    }

    private static int balance(CommandSourceStack source, McStore store) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only a player has a balance."));
            return 0;
        }
        long balance = store.balances().balance(player.getUUID());
        source.sendSuccess(() -> Component.literal("Balance: " + Money.format(balance))
                .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int status(CommandSourceStack source, McStore store) {
        String status = store.status();
        boolean pass = store.coverage().meetsTarget();
        source.sendSuccess(() -> Component.literal(status)
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Coverage target 80%: " + (pass ? "PASS" : "FAIL")
                + " · /buy /sell /balance · /mcstore setprice|reload|edit")
                .withStyle(pass ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        return 1;
    }

    private static int reload(CommandSourceStack source, McStore store) {
        store.refreshRegistryIds();
        store.reload();
        source.sendSuccess(() -> Component.literal("MCStore reloaded prices.json and config.json.")
                .withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.literal(store.status()).withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int edit(CommandSourceStack source, McStore store) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("The price editor is a chest GUI; run it as a player."));
            return 0;
        }
        StoreMenu.open(player, store, StoreMenu.Mode.EDIT);
        return 1;
    }

    private static int setPrice(CommandContext<CommandSourceStack> context, McStore store) {
        String raw = IdentifierArgument.getId(context, "material").toString();
        String side = StringArgumentType.getString(context, "side").toLowerCase(Locale.ROOT);
        double price = DoubleArgumentType.getDouble(context, "price");
        String id = PriceBook.normalizeId(raw);

        if (!side.equals("buy") && !side.equals("sell")) {
            context.getSource().sendFailure(Component.literal("The side must be buy or sell."));
            return 0;
        }
        if (!store.isRegistered(id)) {
            context.getSource().sendFailure(Component.literal("Unknown item: " + id));
            return 0;
        }
        PriceEntry current = store.prices().get(id);
        if (current == null) {
            context.getSource().sendFailure(Component.literal("There is no price entry for " + id + "."));
            return 0;
        }
        boolean buy = side.equals("buy");
        if (buy && !ItemClassifier.isBuyable(id)) {
            context.getSource().sendFailure(Component.literal("Only ores and minerals can have a buy price."));
            return 0;
        }
        long cents;
        try {
            cents = Money.toCents(BigDecimal.valueOf(price));
        } catch (RuntimeException e) {
            context.getSource().sendFailure(Component.literal("Invalid price."));
            return 0;
        }
        if (cents < Money.MIN_PRICE_CENTS) {
            context.getSource().sendFailure(Component.literal("The price must be at least 0.01."));
            return 0;
        }
        PriceEntry updated = store.setPrice(id, buy, cents);
        context.getSource().sendSuccess(() -> Component.literal("Set " + side + " price of " + id + " to "
                + Money.format(cents) + " (rarity " + updated.rarity().displayName() + "). "
                + "Open store views were refreshed.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static Collection<String> buyableIds(McStore store) {
        return store.prices().buyableIds();
    }

    private static Collection<String> allIds(McStore store) {
        return store.prices().pricedIds();
    }

    private static CompletableFuture<Suggestions> suggestIds(SuggestionsBuilder builder, Collection<String> ids) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String id : ids) {
            if (id.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(id);
            }
        }
        return builder.buildFuture();
    }
}
