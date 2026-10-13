package rearth.oritech.port.neoforge.fluids;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * Physical properties of a fluid. Fabric has no fluid type registry: Oritech's fluids carry their type
 * ({@link BaseFlowingFluid#getFluidType()}), other fluids get a type with vanilla-like defaults.
 */
public class FluidType {
    public static final int BUCKET_VOLUME = 1000;

    private static final Map<Fluid, FluidType> DEFAULTS = new ConcurrentHashMap<>();

    private final Properties properties;
    private String descriptionId;

    public FluidType(Properties properties) {
        this.properties = properties;
    }

    public static FluidType of(Fluid fluid) {
        if (fluid instanceof BaseFlowingFluid flowing) {
            return flowing.getFluidType();
        }
        return DEFAULTS.computeIfAbsent(fluid, f -> {
            var props = Properties.create();
            if (f == Fluids.LAVA || f == Fluids.FLOWING_LAVA) {
                props.temperature(1300).density(3000).viscosity(6000).lightLevel(15);
            }
            var type = new FluidType(props);
            var id = BuiltInRegistries.FLUID.getKey(f);
            type.descriptionId = "block." + id.getNamespace() + "." + id.getPath().replace("flowing_", "");
            return type;
        });
    }

    void setDescriptionIdIfAbsent(String id) {
        if (descriptionId == null) {
            descriptionId = id;
        }
    }

    public String getDescriptionId() {
        return descriptionId != null ? descriptionId : "fluid_type.unknown";
    }

    public Component getDescription() {
        return Component.translatable(getDescriptionId());
    }

    public int getTemperature() {
        return properties.temperature;
    }

    public int getDensity() {
        return properties.density;
    }

    public int getViscosity() {
        return properties.viscosity;
    }

    public int getLightLevel() {
        return properties.lightLevel;
    }

    public Rarity getRarity() {
        return properties.rarity;
    }

    public boolean isLighterThanAir() {
        return properties.density <= 0;
    }

    public double motionScale() {
        return properties.motionScale;
    }

    public boolean canSwim() {
        return properties.canSwim;
    }

    public boolean canDrown() {
        return properties.canDrown;
    }

    public boolean canPushEntity() {
        return properties.canPushEntity;
    }

    public float getFallDistanceModifier() {
        return properties.fallDistanceModifier;
    }

    /**
     * Custom movement of entities in the fluid. NeoForge calls this from its entity patches; on Fabric fluids move
     * entities like water does, so this is not called.
     */
    public boolean move(LivingEntity entity, Vec3 movementVector, double gravity) {
        return false;
    }

    public void setItemMovement(ItemEntity entity) {
    }

    public static final class Properties {
        int temperature = 300;
        int density = 1000;
        int viscosity = 1000;
        int lightLevel = 0;
        double motionScale = 0.014;
        boolean canSwim = true;
        boolean canDrown = true;
        boolean canPushEntity = true;
        float fallDistanceModifier = 0.5F;
        Rarity rarity = Rarity.COMMON;
        String descriptionId;

        private Properties() {}

        public static Properties create() {
            return new Properties();
        }

        public Properties descriptionId(String descriptionId) {
            this.descriptionId = descriptionId;
            return this;
        }

        public Properties temperature(int temperature) {
            this.temperature = temperature;
            return this;
        }

        public Properties density(int density) {
            this.density = density;
            return this;
        }

        public Properties viscosity(int viscosity) {
            this.viscosity = viscosity;
            return this;
        }

        public Properties lightLevel(int lightLevel) {
            this.lightLevel = lightLevel;
            return this;
        }

        public Properties motionScale(double motionScale) {
            this.motionScale = motionScale;
            return this;
        }

        public Properties canSwim(boolean canSwim) {
            this.canSwim = canSwim;
            return this;
        }

        public Properties canDrown(boolean canDrown) {
            this.canDrown = canDrown;
            return this;
        }

        public Properties canPushEntity(boolean canPushEntity) {
            this.canPushEntity = canPushEntity;
            return this;
        }

        public Properties fallDistanceModifier(float fallDistanceModifier) {
            this.fallDistanceModifier = fallDistanceModifier;
            return this;
        }

        public Properties rarity(Rarity rarity) {
            this.rarity = rarity;
            return this;
        }

        // accepted for source compatibility, Fabric handles these through fluid tags and the fluid block
        public Properties canExtinguish(boolean value) {
            return this;
        }

        public Properties canConvertToSource(boolean value) {
            return this;
        }

        public Properties isWaterLike(boolean value) {
            return this;
        }

        public Properties supportsBoating(boolean value) {
            return this;
        }

        public Properties canHydrate(boolean value) {
            return this;
        }

        public Properties sound(Object action, Object sound) {
            return this;
        }

        public Properties pathType(Object pathType) {
            return this;
        }

        public Properties adjacentPathType(Object pathType) {
            return this;
        }
    }
}
