package rearth.oritech.init;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import rearth.oritech.Oritech;
import rearth.oritech.api.networking.NetworkManager;
import rearth.oritech.block.entity.augmenter.api.Augment;

import java.util.HashMap;
import java.util.Map;

public class AttachmentContent {

    public static final AttachmentType<GlobalPos> PORTAL_TARGET = AttachmentRegistry.<GlobalPos>builder()
            .initializer(() -> GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO))
            .syncWith(GlobalPos.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
            .persistent(GlobalPos.CODEC)
            .copyOnDeath()
            .buildAndRegister(Oritech.id("portal_target"));

    @SuppressWarnings("unchecked")
    public static final AttachmentType<Map<Identifier, Augment.AugmentState>> ACTIVE_AUGMENTS = AttachmentRegistry.<Map<Identifier, Augment.AugmentState>>builder()
            .initializer(HashMap::new)
            .syncWith(ByteBufCodecs.map(HashMap::new, Identifier.STREAM_CODEC, NetworkManager.getAutoCodec(Augment.AugmentState.class)), AttachmentSyncPredicate.targetOnly())
            .persistent(Codec.unboundedMap(Identifier.CODEC, Augment.AugmentState.CODEC))
            .copyOnDeath()
            .buildAndRegister(Oritech.id("playeraugments"));

    public static final AttachmentType<Boolean> ZIPLINING_STATE = AttachmentRegistry.<Boolean>builder()
            .initializer(() -> false)
            .syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
            .persistent(Codec.BOOL)
            .copyOnDeath()
            .buildAndRegister(Oritech.id("ziplining"));

    public static void init() {
    }
}
