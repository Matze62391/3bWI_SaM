package net.pedroksl.advanced_ae.recipes;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import appeng.core.registries.DeferredRegister;
import net.pedroksl.advanced_ae.AdvancedAE;

public final class AAERecipeTypes {
    private AAERecipeTypes() {}

    public static final DeferredRegister<RecipeType<?>> DR =
            DeferredRegister.create(Registries.RECIPE_TYPE, AdvancedAE.MOD_ID);

    public static final RecipeType<ReactionChamberRecipe> REACTION_CHAMBER = register("react");

    private static <T extends Recipe<?>> RecipeType<T> register(String id) {
        var typeId = AdvancedAE.makeId(id);
        RecipeType<T> type = new RecipeType<>() {
            @Override
            public String toString() {
                return typeId.toString();
            }
        };
        DR.register(id, () -> type);
        return type;
    }
}
