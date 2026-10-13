package gripe._90.megacells.definition;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class MEGATags {
    public static final TagKey<Item> SKY_STEEL_INGOT = TagKey.create(Registries.ITEM, Identifier.parse("c:ingots/sky_steel"));
    public static final TagKey<Item> SKY_BRONZE_INGOT = TagKey.create(Registries.ITEM, Identifier.parse("c:ingots/sky_bronze"));
    public static final TagKey<Item> SKY_OSMIUM_INGOT = TagKey.create(Registries.ITEM, Identifier.parse("c:ingots/sky_osmium"));

    public static final TagKey<Block> SKY_STEEL_BLOCK =
            TagKey.create(Registries.BLOCK, Identifier.parse("c:storage_blocks/sky_steel"));
    public static final TagKey<Block> SKY_BRONZE_BLOCK =
            TagKey.create(Registries.BLOCK, Identifier.parse("c:storage_blocks/sky_bronze"));
    public static final TagKey<Block> SKY_OSMIUM_BLOCK =
            TagKey.create(Registries.BLOCK, Identifier.parse("c:storage_blocks/sky_osmium"));

    public static final TagKey<Item> MEGA_INTERFACE = TagKey.create(Registries.ITEM, MEGABlocks.MEGA_INTERFACE.id());
    public static final TagKey<Item> MEGA_PATTERN_PROVIDER = TagKey.create(Registries.ITEM, MEGABlocks.MEGA_PATTERN_PROVIDER.id());
}
