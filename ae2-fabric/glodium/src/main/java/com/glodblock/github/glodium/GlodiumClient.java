package com.glodblock.github.glodium;

import com.glodblock.github.glodium.client.render.highlight.HighlightRender;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public class GlodiumClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        LevelRenderEvents.COLLECT_SUBMITS.register(HighlightRender::hook);
    }

}
