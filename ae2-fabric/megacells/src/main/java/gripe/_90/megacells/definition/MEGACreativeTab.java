package gripe._90.megacells.definition;

import java.util.ArrayList;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseBlockItem;
import appeng.core.definitions.BlockDefinition;
import appeng.core.definitions.ItemDefinition;
import appeng.items.AEBaseItem;

import gripe._90.megacells.MEGACells;

public final class MEGACreativeTab {
    public static void register() {
        var tab = net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder()
                .title(MEGATranslations.ModName.text())
                .icon(MEGAItems.ITEM_CELL_256M::stack)
                .displayItems(MEGACreativeTab::populateTab)
                .build();
        net.minecraft.core.Registry.register(
                net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB, MEGACells.makeId("tab"), tab);
    }

    private static void populateTab(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output tabOutput) {
        // Block items and their blocks can both add the same stack, so only add each item once
        var added = new java.util.HashSet<net.minecraft.world.item.Item>();
        CreativeModeTab.Output output = (stack, visibility) -> {
            if (added.add(stack.getItem())) {
                tabOutput.accept(stack, visibility);
            }
        };
        var itemDefs = new ArrayList<ItemDefinition<?>>();
        itemDefs.addAll(MEGAItems.getItems());
        itemDefs.addAll(
                MEGABlocks.getBlocks().stream().map(BlockDefinition::item).toList());

        for (var itemDef : itemDefs) {
            var item = itemDef.asItem();

            // For block items, the block controls the creative tab
            if (item instanceof AEBaseBlockItem baseItem && baseItem.getBlock() instanceof AEBaseBlock baseBlock) {
                baseBlock.addToMainCreativeTab(params, output);
            } else if (item instanceof AEBaseItem baseItem) {
                baseItem.addToMainCreativeTab(params, output);
            } else {
                output.accept(itemDef);
            }
        }
    }
}
