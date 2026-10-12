package rearth.oritech.init.compat.jei;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.recipe.v1.sync.ClientRecipeSynchronizedEvent;
import net.fabricmc.fabric.api.recipe.v1.sync.SynchronizedRecipes;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import rearth.oritech.init.recipes.OritechRecipe;

import java.util.Collection;
import java.util.List;

/**
 * Keeps the latest client-side copy of Oritech's server-side recipes for JEI.
 */
public final class OritechJeiRecipeSync {

    private static volatile SynchronizedRecipes clientRecipes;

    private OritechJeiRecipeSync() {
    }

    public static void register() {
        ClientRecipeSynchronizedEvent.EVENT.register((client, recipes) -> clientRecipes = recipes);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clientRecipes = null);
    }

    static Collection<RecipeHolder<OritechRecipe>> getRecipes(RecipeType<OritechRecipe> recipeType) {
        var recipes = clientRecipes;
        return recipes == null ? List.of() : recipes.getAllOfType(recipeType);
    }
}
