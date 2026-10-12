package rearth.oritech.port.neoforge.client.event;

import java.util.function.Supplier;

import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

public final class RegisterPictureInPictureRenderersEvent {
    public <T extends PictureInPictureRenderState> void register(Class<T> stateClass,
            Supplier<PictureInPictureRenderer<T>> factory) {
        PictureInPictureRendererRegistry.register(context -> factory.get());
    }
}
