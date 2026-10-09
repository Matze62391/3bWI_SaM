package appeng.client.renderer.parts;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import appeng.api.parts.IPart;
import appeng.client.api.renderer.parts.PartRenderer;
import appeng.client.api.renderer.parts.RegisterPartRendererEvent;
import appeng.core.AppEng;

/**
 * Registration facility for associating {@link PartRenderer} with {@link IPart} classes.
 */
public class PartRendererDispatcher implements ResourceManagerReloadListener {
    public static final Identifier ID = AppEng.makeId("part_renderer_dispatcher");
    private Map<Class<?>, Registration<?>> registrations = Map.of();

    public PartRendererDispatcher() {
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        var tempMap = new ConcurrentHashMap<Class<?>, Registration<?>>();
        RegisterPartRendererEvent.EVENT.invoker()
                .registerPartRenderers(new RegisterPartRendererEvent(makeRegistrationSink(tempMap)));

        registrations = Collections.unmodifiableMap(new IdentityHashMap<>(tempMap));
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends IPart> PartRenderer<T, ?> getRenderer(Class<T> partClass) {
        var registration = registrations.get(partClass);
        if (registration != null) {
            return (PartRenderer<T, ?>) registration.renderer;
        }
        return null;
    }

    private RegisterPartRendererEvent.PartRegistrationSink makeRegistrationSink(
            Map<Class<?>, Registration<?>> registrations) {
        return new RegisterPartRendererEvent.PartRegistrationSink() {
            @Override
            public <T extends IPart> void register(Class<T> partClass, PartRenderer<? super T, ?> renderer) {
                registrations.put(partClass, new Registration<>(partClass, renderer));
            }
        };
    }

    private record Registration<T extends IPart>(Class<T> partClass,
            PartRenderer<? super T, ?> renderer) {
    }
}
