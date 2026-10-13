package net.pedroksl.advanced_ae.common.definitions;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class AAETags {
    public static final TagKey<Item> ADV_PATTERN_PROVIDER = TagKey.create(Registries.ITEM, AAEBlocks.ADV_PATTERN_PROVIDER.id());
}
