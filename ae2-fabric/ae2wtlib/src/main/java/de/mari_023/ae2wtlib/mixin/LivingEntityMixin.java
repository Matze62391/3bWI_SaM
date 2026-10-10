package de.mari_023.ae2wtlib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import de.mari_023.ae2wtlib.AE2wtlibEvents;

/**
 * Restocks food and other used items (NeoForge: LivingEntityUseItemEvent.Finish).
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @WrapOperation(method = "completeUsingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack ae2wtlib$restock(ItemStack stack, Level level, LivingEntity entity, Operation<ItemStack> original) {
        var used = stack.copy();
        var result = original.call(stack, level, entity);
        if (entity instanceof ServerPlayer player) {
            var holder = new ItemStack[] { result };
            AE2wtlibEvents.restock(player, used, result.getCount(), newStack -> holder[0] = newStack);
            return holder[0];
        }
        return result;
    }
}
