package rearth.oritech.port.neoforge.client.event;

import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class EntityRenderersEvent {
    private EntityRenderersEvent() {}

    public static final class RegisterRenderers {
        public <T extends Entity> void registerEntityRenderer(EntityType<? extends T> entityType,
                EntityRendererProvider<T> provider) {
            EntityRendererRegistry.register(entityType, provider);
        }

        public <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
                BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
            register(type, provider);
        }

        @SuppressWarnings("unchecked")
        private static <T extends BlockEntity, S extends BlockEntityRenderState> void register(
                BlockEntityType<? extends T> type, BlockEntityRendererProvider<T, S> provider) {
            BlockEntityRendererRegistry.register((BlockEntityType<T>) type, provider);
        }
    }
}
