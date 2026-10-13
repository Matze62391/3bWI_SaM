package rearth.oritech.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public class PermissionHelpers {
    // Permission checks post another break event;
    // to avoid recusion on neoforge we add this flag
    public static final ThreadLocal<Boolean> CHECKING_OFFSET_BREAK_PERMISSION = ThreadLocal.withInitial(() -> false);

    public static boolean CanPlayerBreakBlock(Level level, BlockPos blockPos, BlockState blockState, ServerPlayer player) {
        var wasCheckingOffsetBreakPermission = CHECKING_OFFSET_BREAK_PERMISSION.get();
        CHECKING_OFFSET_BREAK_PERMISSION.set(true);
        try {
            return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, blockPos, blockState, level.getBlockEntity(blockPos));
        } finally {
            CHECKING_OFFSET_BREAK_PERMISSION.set(wasCheckingOffsetBreakPermission);
        }
    }

    public static boolean CanPlayerAttackEntity(LivingEntity target, Level level, float amount, DamageSource damageSource) {

        if (!(level instanceof ServerLevel serverLevel))
            return true;

        return ServerLivingEntityEvents.ALLOW_DAMAGE.invoker().allowDamage(target, damageSource, amount);
    }

}
