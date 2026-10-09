package appeng.recipes.conditions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.TagKey;

import appeng.core.AppEng;

/**
 * Passes if an item tag exists and contains at least one entry. This replaces NeoForge's
 * {@code neoforge:not(neoforge:tag_empty)} condition, which AE2 uses for optional recipes (i.e. matter cannon ammo for
 * metals added by other mods).
 */
public record TagNotEmptyCondition(Identifier tag) implements ResourceCondition {
    public static final MapCodec<TagNotEmptyCondition> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
            Identifier.CODEC.fieldOf("tag").forGetter(TagNotEmptyCondition::tag))
            .apply(builder, TagNotEmptyCondition::new));

    public static final ResourceConditionType<TagNotEmptyCondition> TYPE = ResourceConditionType
            .create(AppEng.makeId("tag_not_empty"), CODEC);

    public static void register() {
        ResourceConditions.register(TYPE);
    }

    @Override
    public ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(RegistryOps.@Nullable RegistryInfoLookup registryInfo) {
        if (registryInfo == null) {
            return false;
        }
        var items = registryInfo.lookup(Registries.ITEM);
        if (items.isEmpty()) {
            return false;
        }
        return items.get().get(TagKey.create(Registries.ITEM, tag))
                .map(set -> set.size() > 0)
                .orElse(false);
    }
}
