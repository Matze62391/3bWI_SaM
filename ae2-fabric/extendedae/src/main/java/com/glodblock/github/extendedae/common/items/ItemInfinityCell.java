package com.glodblock.github.extendedae.common.items;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.items.AEBaseItem;
import appeng.items.storage.StorageCellTooltipComponent;
import com.glodblock.github.extendedae.common.EAESingletons;
import com.glodblock.github.extendedae.util.FCUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ItemInfinityCell extends AEBaseItem {

    @Nullable
    private final Supplier<AEKey> record;

    public ItemInfinityCell(@NotNull Supplier<AEKey> type, Properties properties) {
        super(properties.stacksTo(1));
        this.record = FCUtil.memoize(type);
    }

    /**
     * An infinity cell whose content is stored on the stack ({@link EAESingletons#INFINITY_RECORD}). The creative tab
     * has one for every fluid.
     */
    public ItemInfinityCell(Properties properties) {
        super(properties.stacksTo(1));
        this.record = null;
    }

    @Nullable
    public AEKey getRecord() {
        return this.record != null ? this.record.get() : null;
    }

    @Nullable
    public AEKey getRecord(ItemStack stack) {
        var stored = stack.get(EAESingletons.INFINITY_RECORD);
        return stored != null ? stored : getRecord();
    }

    public static ItemStack withRecord(ItemInfinityCell cell, AEKey record) {
        var stack = new ItemStack(cell);
        stack.set(EAESingletons.INFINITY_RECORD, record);
        return stack;
    }

    @Override
    public void addToMainCreativeTab(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        if (this.record != null) {
            super.addToMainCreativeTab(parameters, output);
            return;
        }
        for (var fluid : BuiltInRegistries.FLUID) {
            if (fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState())) {
                output.accept(withRecord(this, AEFluidKey.of(fluid)));
            }
        }
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack is) {
        var key = getRecord(is);
        if (key == null) {
            return Component.translatable("item.extendedae.infinity_cell");
        }
        return Component.translatable("item.extendedae.infinity_cell_name", key.getDisplayName());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull TooltipDisplay tooltipDisplay, @NotNull Consumer<Component> lines, @NotNull TooltipFlag tooltipFlags) {
        super.appendHoverText(stack, context, tooltipDisplay, lines, tooltipFlags);
        lines.accept(Component.translatable("infinity.tooltip").withStyle(ChatFormatting.GREEN));
    }

    @NotNull
    @Override
    public Optional<TooltipComponent> getTooltipImage(@NotNull ItemStack stack) {
        var key = getRecord(stack);
        if (key == null) {
            return Optional.empty();
        }
        var content = Collections.singletonList(new GenericStack(key, getAsIntMax(key)));
        return Optional.of(new StorageCellTooltipComponent(List.of(), content, false, true));
    }

    public static long getAsIntMax(AEKey key) {
        return (long) Integer.MAX_VALUE * key.getAmountPerUnit();
    }

}
