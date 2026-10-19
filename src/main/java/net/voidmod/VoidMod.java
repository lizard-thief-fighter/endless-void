package net.voidmod;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(VoidMod.MOD_ID)
public class VoidMod {
    public static final String MOD_ID = "void";
    public static final Logger LOGGER = LogUtils.getLogger();

    public VoidMod(IEventBus modEventBus) {
    }
}
