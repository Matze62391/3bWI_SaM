package gripe._90.megacells.integration;

import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import com.google.common.base.Suppliers;

import net.fabricmc.loader.api.FabricLoader;

import gripe._90.megacells.definition.MEGATranslations;
import gripe._90.megacells.integration.ae2wt.AE2WTIntegration;
import gripe._90.megacells.integration.appflux.AppFluxIntegration;

@SuppressWarnings("Convert2MethodRef")
public enum Addons {
    AE2WTLIB("AE2WTLib", () -> new AE2WTIntegration(), true),
    // Applied Mekanistics and Applied Botanics don't exist for Fabric 26.3
    APPMEK("Applied Mekanistics"),
    APPBOT("Applied Botanics"),
    ARSENG("Ars Énergistique"),
    APPEX("Applied Experienced"),
    APPFLUX("Applied Flux", () -> new AppFluxIntegration(), true),
    // AppliedE stays disabled even as a compile-only stub: MEGA's own MEGAEMCInterfaceBlockEntity/
    // MEGAEMCInterfacePart directly `extends` AppliedE's own classes (not just reference them), so the
    // JVM eagerly resolves that superclass the moment MEGA's own classes are loaded - crashing the
    // WHOLE mod's construction, not just this integration, regardless of Addons#isLoaded gating.
    // Restore only once AppliedE ships a real 26.1 build to be present at runtime too.
    APPLIEDE("AppliedE"),
    APPLIEDSOUL("Applied Soul"),
// APPELEM("Applied Elemental"),
;

    private final String modName;
    private final Supplier<IntegrationHelper> helper;
    private final boolean enabled; // TODO: Needs a more dynamic approach than this...

    Addons(String modName) {
        this(modName, () -> new IntegrationHelper() {}, false);
    }

    Addons(String modName, Supplier<IntegrationHelper> helper, boolean enabled) {
        this.modName = modName;
        this.helper = Suppliers.memoize(helper::get);
        this.enabled = enabled;
    }

    public String getModId() {
        return name().toLowerCase();
    }

    public IntegrationHelper getHelper() {
        return helper.get();
    }

    public boolean isLoaded() {
        return enabled && FabricLoader.getInstance().isModLoaded(getModId());
    }

    public Component getUnavailableTooltip() {
        return (enabled ? MEGATranslations.NotInstalled.text(modName) : MEGATranslations.NotYetAvailable.text())
                .withStyle(ChatFormatting.GRAY);
    }
}
