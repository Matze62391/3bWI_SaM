package appeng.client.integrations.jei;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;

import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;

import mezz.jei.api.ingredients.IIngredientRenderer;

import appeng.client.integrations.itemlists.FluidBlockRendering;

public class FluidBlockRenderer implements IIngredientRenderer<IJeiFluidIngredient> {
    @Override
    public void render(GuiGraphicsExtractor guiGraphics, IJeiFluidIngredient ingredient) {
        var fluid = ingredient.getFluidVariant().getFluid();
        FluidBlockRendering.render(guiGraphics, fluid, 0, 0, 16, 16);
    }

    @Override
    public List<Component> getTooltip(IJeiFluidIngredient ingredient, TooltipFlag tooltipFlag) {
        // JEI adds the mod name automatically
        return List.of(FluidVariantAttributes.getName(ingredient.getFluidVariant()));
    }
}
