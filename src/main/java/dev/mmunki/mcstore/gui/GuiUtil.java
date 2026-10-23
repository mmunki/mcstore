package dev.mmunki.mcstore.gui;

import dev.mmunki.mcstore.catalog.RarityTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

final class GuiUtil {
    private GuiUtil() {
    }

    static ChatFormatting color(RarityTier tier) {
        return switch (tier) {
            case COMMON -> ChatFormatting.GRAY;
            case UNCOMMON -> ChatFormatting.GREEN;
            case RARE -> ChatFormatting.BLUE;
            case EPIC -> ChatFormatting.LIGHT_PURPLE;
            case LEGENDARY -> ChatFormatting.GOLD;
        };
    }

    static ItemStack filler() {
        ItemStack stack = new ItemStack(Items.STAINED_GLASS_PANE.pick(DyeColor.GRAY));
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(" ").withStyle(ChatFormatting.DARK_GRAY));
        return stack;
    }

    static ItemStack icon(Item item, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, name);
        if (lore != null && !lore.isEmpty()) {
            stack.set(DataComponents.LORE, new ItemLore(lore));
        }
        return stack;
    }

    static ItemStack icon(Item item, String name, ChatFormatting color, List<String> lore) {
        List<Component> lines = new ArrayList<>();
        if (lore != null) {
            for (String line : lore) {
                lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
        return icon(item, Component.literal(name).withStyle(color), lines);
    }

    static Component text(String value, ChatFormatting color) {
        return Component.literal(value).withStyle(color);
    }
}
