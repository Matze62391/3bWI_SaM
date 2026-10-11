package appeng.client.integrations.jei;


import org.jetbrains.annotations.Nullable;


import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.fabric.constants.FabricTypes;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.fabric.ingredients.fluids.JeiFluidIngredient;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.client.api.integrations.jei.IngredientConverter;

public class FluidIngredientConverter implements IngredientConverter<IJeiFluidIngredient> {
    @Override
    public IIngredientType<IJeiFluidIngredient> getIngredientType() {
        return FabricTypes.FLUID_STACK;
    }

    @Nullable
    @Override
    public IJeiFluidIngredient getIngredientFromStack(GenericStack stack) {
        if (stack.what() instanceof AEFluidKey fluidKey) {
            return new JeiFluidIngredient(fluidKey.toVariant(), Math.max(1, stack.amount()));
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public GenericStack getStackFromIngredient(IJeiFluidIngredient ingredient) {
        return GenericStack.from(ingredient.getFluidVariant(), ingredient.getAmount());
    }
}
