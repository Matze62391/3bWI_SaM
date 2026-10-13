package rearth.oritech.init.compat.jei;

import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import rearth.oritech.init.recipes.RecipeContent;

/**
 * Vanilla no longer sends recipes to clients; JEI and the machine screens need Oritech's recipes there.
 */
public final class OritechJeiRecipeServerSync {

    private OritechJeiRecipeServerSync() {
    }

    public static void register() {
        for (var serializer : RecipeContent.RECIPE_SERIALIZERS.getEntries()) {
            RecipeSynchronization.synchronizeRecipeSerializer(serializer.get());
        }
    }
}
