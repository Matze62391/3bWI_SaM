package de.mari_023.ae2wtlib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import de.mari_023.ae2wtlib.AE2wtlibEvents;

/**
 * Restocks arrows when a bow is drawn and released (NeoForge: ArrowNockEvent and ArrowLooseEvent).
 */
@Mixin(BowItem.class)
public abstract class BowItemMixin {
    @Inject(method = "use", at = @At("HEAD"))
    private void ae2wtlib$restockOnNock(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        ae2wtlib$restockArrows(player, player.getItemInHand(hand));
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void ae2wtlib$restockOnLoose(ItemStack bow, Level level, LivingEntity entity, int remainingTime,
            CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player player) {
            ae2wtlib$restockArrows(player, bow);
        }
    }

    private static void ae2wtlib$restockArrows(Player player, ItemStack bow) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack projectile = player.getProjectile(bow);
        if (projectile.isEmpty()) {
            return;
        }
        AE2wtlibEvents.restock(serverPlayer, projectile, projectile.getCount(), stack -> {
        });
    }
}
