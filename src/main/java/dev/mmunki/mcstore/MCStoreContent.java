package dev.mmunki.mcstore;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;

/**
 * The only registry content in MCStore: the Money Ore.
 *
 * <p>The ore is the same block in both dimensions.  The configured feature and
 * the placed feature are data-pack JSON in this jar; this class only registers
 * the block/item and attaches the two placed features to the overworld and
 * nether biomes.</p>
 */
public final class MCStoreContent {
    private static final String NAMESPACE = "mcstore";
    public static final Identifier MONEY_ORE_ID = Identifier.fromNamespaceAndPath(NAMESPACE, "money_ore");
    public static final String MONEY_ORE_ITEM_ID = NAMESPACE + ":money_ore";
    public static final ResourceKey<Block> MONEY_ORE_BLOCK_KEY =
            ResourceKey.create(Registries.BLOCK, MONEY_ORE_ID);
    public static final ResourceKey<Item> MONEY_ORE_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, MONEY_ORE_ID);

    private MCStoreContent() {
    }

    public static void register() {
        Block moneyOre = new DropExperienceBlock(UniformInt.of(2, 5),
                BlockBehaviour.Properties.of()
                        .setId(MONEY_ORE_BLOCK_KEY)
                        .mapColor(MapColor.STONE)
                        .strength(3.0F, 3.0F)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.STONE));
        Registry.register(BuiltInRegistries.BLOCK, MONEY_ORE_ID, moneyOre);
        Registry.register(BuiltInRegistries.ITEM, MONEY_ORE_ID,
                new BlockItem(moneyOre, new Item.Properties().setId(MONEY_ORE_ITEM_KEY)));
    }

    public static void registerWorldgen() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES, placedFeature("money_ore_overworld"));
        BiomeModifications.addFeature(BiomeSelectors.foundInTheNether(),
                GenerationStep.Decoration.UNDERGROUND_ORES, placedFeature("money_ore_nether"));
    }

    private static ResourceKey<PlacedFeature> placedFeature(String path) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                Identifier.fromNamespaceAndPath(NAMESPACE, path));
    }
}
