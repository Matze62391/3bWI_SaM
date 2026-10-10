package net.pedroksl.advanced_ae.common.helpers;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.pedroksl.advanced_ae.AdvancedAE;

/**
 * Replaces NeoForge's {@code Entity#getPersistentData()}: a mutable compound tag that is saved with the entity.
 */
public final class AAEPersistentData {
    public static final AttachmentType<CompoundTag> TYPE = AttachmentRegistry.create(
            AdvancedAE.makeId("persistent_data"),
            builder -> builder.initializer(CompoundTag::new).persistent(CompoundTag.CODEC).copyOnDeath());

    private AAEPersistentData() {}

    public static void register() {
        // Registered when the class is loaded
    }

    public static CompoundTag get(Entity entity) {
        return entity.getAttachedOrCreate(TYPE);
    }
}
