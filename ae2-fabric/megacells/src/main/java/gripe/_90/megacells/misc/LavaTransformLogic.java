package gripe._90.megacells.misc;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import appeng.recipes.AERecipeTypes;

public final class LavaTransformLogic {
    private static final Set<Item> lavaCache = new HashSet<>();

    static {
        ServerLifecycleEvents.SERVER_STARTED.register(_ -> lavaCache.clear());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((_, _, _) -> lavaCache.clear());
    }

    @SuppressWarnings("resource")
    public static boolean canTransformInLava(ItemEntity entity) {
        return entity.level() instanceof ServerLevel level
                && getLavaTransformableItems(level).contains(entity.getItem().getItem());
    }

    @SuppressWarnings("resource")
    public static boolean allIngredientsPresent(ItemEntity entity) {
        var x = entity.getX();
        var y = entity.getY();
        var z = entity.getZ();
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }

        var items = level.getEntities(null, new AABB(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1)).stream()
                .filter(e -> e instanceof ItemEntity && !e.isRemoved())
                .map(e -> ((ItemEntity) e).getItem().getItem())
                .toList();

        for (var recipe : level.recipeAccess().recipes.byType(AERecipeTypes.TRANSFORM)) {
            if (recipe.value().circumstance.isFluidTag(FluidTags.LAVA)) {
                return recipe.value().ingredients.stream()
                        .allMatch(ingredient -> ingredient.items().anyMatch(holder -> items.contains(holder.value())));
            }
        }

        return false;
    }

    @SuppressWarnings("SameReturnValue")
    private static Set<Item> getLavaTransformableItems(ServerLevel level) {
        if (lavaCache.isEmpty()) {
            for (var recipe : level.recipeAccess().recipes.byType(AERecipeTypes.TRANSFORM)) {
                if (!recipe.value().circumstance.isFluidTag(FluidTags.LAVA)) {
                    continue;
                }

                for (var ingredient : recipe.value().ingredients) {
                    ingredient.items().forEach(holder -> lavaCache.add(holder.value()));

                    // Don't break here unlike AE2's TransformLogic, otherwise unprocessed items will burn up
                }
            }
        }

        return lavaCache;
    }
}
