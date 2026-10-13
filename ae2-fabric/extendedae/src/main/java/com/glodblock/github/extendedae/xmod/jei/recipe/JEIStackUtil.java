package com.glodblock.github.extendedae.xmod.jei.recipe;

import com.glodblock.github.glodium.recipe.stack.IngredientStack;
import guideme.internal.util.Platform;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;

public class JEIStackUtil {

    public static void addItem(IngredientStack.Item stack, IRecipeSlotBuilder slot) {
        for (var item : stack.getIngredient().display().resolveForStacks(Platform.getSlotDisplayContext())) {
            slot.add(item.copyWithCount(stack.getAmount()));
        }
    }

    public static void addFluid(IngredientStack.Fluid stack, IRecipeSlotBuilder slot) {
        slot.setFluidRenderer(stack.getAmount(), false, 16, 16);
        for (var fluid : stack.getIngredient().getStacks(stack.getAmount())) {
            slot.add(fluid.getFluid(), stack.getAmount());
        }
    }

}
