package net.pedroksl.advanced_ae.xmod.jei;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import mezz.jei.api.fabric.ingredients.fluids.IJeiFluidIngredient;
import mezz.jei.api.fabric.ingredients.fluids.JeiFluidIngredient;
import net.pedroksl.ae2addonlib.fluid.FluidIngredient;
import net.pedroksl.advanced_ae.AdvancedAE;
import net.pedroksl.advanced_ae.client.AAEClient;
import net.pedroksl.advanced_ae.client.gui.ReactionChamberScreen;
import net.pedroksl.advanced_ae.common.definitions.AAEBlocks;
import net.pedroksl.advanced_ae.common.definitions.AAEItems;
import net.pedroksl.advanced_ae.common.definitions.AAEText;
import net.pedroksl.advanced_ae.recipes.AAERecipeTypes;
import net.pedroksl.ae2addonlib.recipes.IngredientStack;

import appeng.api.ids.AEComponents;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    public static final Identifier TEXTURE = AdvancedAE.makeId("textures/guis/emi.png");

    private static final Identifier ID = AdvancedAE.makeId("core");

    public JEIPlugin() {}

    @Override
    public Identifier getPluginUid() {
        return ID;
    }

    /**
     * Powered items are shown both empty and fully charged in the creative tab. Like AE2, the stored energy makes them
     * different subtypes in the ingredient list (but not in recipes).
     */
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> byStoredEnergy = (stack, context) ->
                context == UidContext.Recipe ? null : stack.get(AEComponents.STORED_ENERGY);
        for (var item : List.of(
                AAEItems.QUANTUM_HELMET,
                AAEItems.QUANTUM_CHESTPLATE,
                AAEItems.QUANTUM_LEGGINGS,
                AAEItems.QUANTUM_BOOTS,
                AAEItems.QUANTUM_CRAFTER_WIRELESS_TERMINAL)) {
            registration.registerSubtypeInterpreter(item.asItem(), byStoredEnergy);
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        var jeiHelpers = registry.getJeiHelpers();
        registry.addRecipeCategories(new ReactionChamberCategory(jeiHelpers));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        addSyncedRecipes(registration, ReactionChamberCategory.RECIPE_TYPE, AAERecipeTypes.REACTION_CHAMBER);

        // The descriptions the NeoForge version shows in EMI and REI
        registration.addItemStackInfo(
                AAEItems.SHATTERED_SINGULARITY.stack(), AAEText.ShatteredSingularityDescription.text());
        registration.addItemStackInfo(
                AAEBlocks.ADV_PATTERN_PROVIDER.stack(), AAEText.AdvPatternProviderEmiDesc.text());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Clicking the arrow of the reaction chamber shows its recipes
        registration.addRecipeClickArea(
                ReactionChamberScreen.class, 97, 44, 20, 14, ReactionChamberCategory.RECIPE_TYPE);
    }

    private static <I extends RecipeInput, T extends Recipe<I>> void addSyncedRecipes(
            IRecipeRegistration registration,
            IRecipeType<RecipeHolder<T>> recipeType,
            RecipeType<T> vanillaRecipeType) {
        var recipes = AAEClient.instance().getRecipeMapForType(Minecraft.getInstance().level, vanillaRecipeType);
        var recipeHolders = List.copyOf(recipes.byType(vanillaRecipeType));
        registration.addRecipes(recipeType, recipeHolders);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        var chamber = AAEBlocks.REACTION_CHAMBER.stack();
        registration.addCraftingStation(ReactionChamberCategory.RECIPE_TYPE, chamber);
    }

    public static List<ItemStack> stackOf(IngredientStack.Item stack) {
        if (!stack.isEmpty()) {
            return stack.getIngredient().items()
                    .map(item -> new ItemStack(item, stack.getAmount()))
                    .toList();
        }
        return List.of();
    }

    public static List<IJeiFluidIngredient> stackOf(IngredientStack.Fluid stack) {
        FluidIngredient ingredient = stack.getIngredient();
        return ingredient.getStacks(stack.getAmount()).stream()
                .map(fluid -> (IJeiFluidIngredient) new JeiFluidIngredient(fluid.getVariant(), fluid.getAmountLong()))
                .toList();
    }
}
