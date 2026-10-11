package com.glodblock.github.glodium.xmod;

import com.glodblock.github.glodium.Glodium;
import com.glodblock.github.glodium.reflect.moon.Moon;
import com.glodblock.github.glodium.registry.RegistryHandler;
import com.glodblock.github.glodium.util.GlodUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.CustomValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads the integrations of a mod with other mods. On NeoForge, the plugins are found through their
 * {@link ThirdParty} annotation. On Fabric, a mod lists them in its {@code fabric.mod.json}:
 * <pre>
 * "custom": {
 *   "glodium:xmod": { "&lt;other mod id&gt;": "&lt;plugin class&gt;" }
 * }
 * </pre>
 * The plugin is only loaded if the other mod is installed.
 */
public final class XModManager {

    public static final String CUSTOM_KEY = "glodium:xmod";
    private static final List<XMod> LOADERS = new ArrayList<>();
    private static boolean loaded;

    /**
     * Scans and instantiates the plugins once. Mods may register their content (e.g. in AE2's addon entrypoint) before
     * Glodium's own entrypoint ran, so every entry point calls this.
     */
    public static synchronized void load() {
        if (!loaded) {
            loaded = true;
            scan();
            init();
        }
    }

    private static void scan() {
        LOADERS.clear();
        for (var mod : FabricLoader.getInstance().getAllMods()) {
            var meta = mod.getMetadata();
            if (!meta.containsCustomValue(CUSTOM_KEY)) {
                continue;
            }
            var value = meta.getCustomValue(CUSTOM_KEY);
            if (value.getType() != CustomValue.CvType.OBJECT) {
                Glodium.LOGGER.error("Invalid {} in mod {}", CUSTOM_KEY, meta.getId());
                continue;
            }
            for (var entry : value.getAsObject()) {
                LOADERS.add(new XMod(meta.getId(), entry.getKey(), entry.getValue().getAsString()));
            }
        }
    }

    private static void init() {
        for (var loader: LOADERS) {
            if (GlodUtil.checkMod(loader.xmod)) {
                try {
                    loader.instance = (XModLoader) Moon.instantiate(Class.forName(loader.pluginPath));
                } catch (Exception e) {
                    Glodium.LOGGER.error("Unable to load third party plugin: {} for mod: {}", loader.pluginPath, loader.xmod, e);
                }
            }
        }
    }

    /**
     * Runs the common setup of the plugins of the given mod. Called by the mod once its content is registered (NeoForge
     * does this in the common setup event).
     */
    public static void common(String host) {
        load();
        for (var loader: LOADERS) {
            if (loader.instance != null && loader.host.equals(host)) {
                loader.instance.loadCommon();
            }
        }
    }

    /**
     * Runs the client setup of the plugins of the given mod.
     */
    public static void client(String host) {
        load();
        for (var loader: LOADERS) {
            if (loader.instance != null && loader.host.equals(host)) {
                loader.instance.loadClient();
            }
        }
    }

    public static void register(String host, RegistryHandler handler) {
        load();
        for (var loader: LOADERS) {
            if (loader.instance != null && loader.host.equals(host)) {
                loader.instance.onRegister(handler);
            }
        }
    }

    static class XMod {
        private final String host;
        private final String xmod;
        private final String pluginPath;
        private XModLoader instance;

        XMod(String host, String xmod, String pluginPath) {
            this.host = host;
            this.xmod = xmod;
            this.pluginPath = pluginPath;
        }
    }

}
