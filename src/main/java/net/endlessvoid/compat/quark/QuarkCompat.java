package net.endlessvoid.compat.quark;

import net.endlessvoid.VoidDimensions;
import net.endlessvoid.VoidMod;

import java.util.List;

/**
 * Quark integration, called only from mixins that apply when Quark is installed. Quark's types are reached
 * reflectively through public fields, so this mod needs no compile dependency on Quark or its library Zeta.
 */
public final class QuarkCompat {
    private static final String WORLD_MODULES = "org.violetmoon.quark.content.world.module.";

    /** Exposes Zeta's DimensionConfig dimension list (added by {@code ZetaDimensionConfigMixin}). */
    public interface DimensionList {
        List<String> endless_void$dimensions();
    }

    /**
     * Quark hard-codes chorus vegetation to the End in its generator's constructor, outside its config.
     * Adds the Void to that generator's dimension whitelist ({@code dimConfig}, public in Zeta's Generator).
     */
    public static void allowGeneratorInVoid(Object generator) {
        try {
            addVoid(generator.getClass().getField("dimConfig").get(generator));
        } catch (ReflectiveOperationException | RuntimeException e) {
            VoidMod.LOGGER.warn("Couldn't enable {} in the Void", generator.getClass().getSimpleName(), e);
        }
    }

    /**
     * Adds the Void to the default dimension list of Quark's configurable End features. Runs as the module class
     * initializes, before Zeta reads these fields as config defaults, so new config files list the Void and saved
     * config files keep their own values.
     */
    public static void defaultEndFeaturesToVoid(Class<?> module) {
        String[] path = switch (module.getName()) {
            case WORLD_MODULES + "SpiralSpiresModule" -> new String[]{"dimensions"};
            case WORLD_MODULES + "NewStoneTypesModule", WORLD_MODULES + "BigStoneClustersModule" -> new String[]{"myalite", "dimensions"};
            default -> null;
        };
        if (path == null) return;
        try {
            Object value = module.getField(path[0]).get(null);
            for (int i = 1; i < path.length; i++) value = value.getClass().getField(path[i]).get(value);
            addVoid(value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            VoidMod.LOGGER.warn("Couldn't add the Void to {} defaults", module.getSimpleName(), e);
        }
    }

    private static void addVoid(Object dimensionConfig) {
        if (!(dimensionConfig instanceof DimensionList config)) return;
        List<String> dimensions = config.endless_void$dimensions();
        String voidId = VoidDimensions.THE_VOID.location().toString();
        if (!dimensions.contains(voidId)) dimensions.add(voidId);
    }

    private QuarkCompat() {}
}
