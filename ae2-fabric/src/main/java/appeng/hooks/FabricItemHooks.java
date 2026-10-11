package appeng.hooks;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import appeng.items.AEBaseItem;

/**
 * Emulates NeoForge's item extensions that AE2's items rely on:
 * <ul>
 * <li>{@code onItemUseFirst}, which is called before the targeted block is used</li>
 * <li>{@code doesSneakBypassUse}, which lets the targeted block handle the interaction even while sneaking</li>
 * </ul>
 */
public final class FabricItemHooks {
    private FabricItemHooks() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(FabricItemHooks::onUseBlock);
    }

    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand,
            BlockHitResult hitResult) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }

        var stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof AEBaseItem item)) {
            return InteractionResult.PASS;
        }

        var result = item.onItemUseFirst(stack, new UseOnContext(player, hand, hitResult));
        if (result != InteractionResult.PASS) {
            return result;
        }

        if (player.isSecondaryUseActive()
                && item.doesSneakBypassUse(stack, level, hitResult.getBlockPos(), player)) {
            // Vanilla does not use the block when sneaking with an item in hand, do it here instead
            var pos = hitResult.getBlockPos();
            var state = level.getBlockState(pos);
            var useResult = state.useItemOn(stack, level, player, hand, hitResult);
            if (useResult.consumesAction()) {
                return useResult;
            }
            if (useResult instanceof InteractionResult.TryEmptyHandInteraction
                    && hand == InteractionHand.MAIN_HAND) {
                var emptyHandResult = state.useWithoutItem(level, player, hitResult);
                if (emptyHandResult.consumesAction()) {
                    return emptyHandResult;
                }
            }
        }

        return InteractionResult.PASS;
    }
}
