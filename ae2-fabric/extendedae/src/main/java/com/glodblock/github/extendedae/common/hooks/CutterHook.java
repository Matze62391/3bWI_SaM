package com.glodblock.github.extendedae.common.hooks;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.items.tools.quartz.QuartzCuttingKnifeItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.util.InteractionUtil;
import com.glodblock.github.extendedae.container.ContainerRenamer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import java.util.List;

public final class CutterHook {

    public static final CutterHook INSTANCE = new CutterHook();

    private CutterHook() {
        // NO-OP
    }

    public static void addTooltip() {
        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> hookTooltip(stack, lines));
    }

    private static void hookTooltip(ItemStack stack, List<Component> tooltip) {
        if (stack.getItem() instanceof QuartzCuttingKnifeItem) {
            tooltip.add(Component.translatable("cutter.tooltip").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public static void register() {
        UseBlockCallback.EVENT.register(INSTANCE::onPlayerUseBlock);
    }

    public InteractionResult onPlayerUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isSpectator() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        var itemStack = player.getItemInHand(hand);
        if (!InteractionUtil.isInAlternateUseMode(player) && itemStack.getItem() instanceof QuartzCuttingKnifeItem) {
            var pos = hitResult.getBlockPos();
            var tile = level.getBlockEntity(pos);
            if (tile instanceof AEBaseBlockEntity) {
                if (tile instanceof CableBusBlockEntity cable) {
                    var hitVec = hitResult.getLocation();
                    Vec3 hitInBlock = new Vec3(hitVec.x - pos.getX(), hitVec.y - pos.getY(), hitVec.z - pos.getZ());
                    var part = cable.selectPartLocal(hitInBlock).part;
                    if (part instanceof AEBasePart p) {
                        if (!level.isClientSide()) {
                            MenuOpener.open(ContainerRenamer.TYPE, player, MenuLocators.forPart(p));
                        }
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    if (!level.isClientSide()) {
                        MenuOpener.open(ContainerRenamer.TYPE, player, MenuLocators.forBlockEntity(tile));
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

}
