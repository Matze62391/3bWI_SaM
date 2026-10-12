package rearth.oritech.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rearth.oritech.init.ToolsContent;

/** The enderic railgun is held like a loaded crossbow. */
@Mixin(AvatarRenderer.class)
public class RailgunArmPoseMixin {

    @Inject(method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At("HEAD"), cancellable = true)
    private static void oritech$railgunPose(Avatar avatar, ItemStack stack, InteractionHand hand,
                                            CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (hand == InteractionHand.MAIN_HAND && stack.is(ToolsContent.ENDERIC_RAILGUN.get())) {
            cir.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_HOLD);
        }
    }
}
