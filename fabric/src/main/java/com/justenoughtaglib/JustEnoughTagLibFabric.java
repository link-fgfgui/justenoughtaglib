package com.justenoughtaglib;

import com.justenoughtaglib.config.JustEnoughTagLibClientConfig;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraftforge.fml.config.ModConfig;

public class JustEnoughTagLibFabric implements ModInitializer {
    
    @Override
    public void onInitialize() {
        ForgeConfigRegistry.INSTANCE.register(
            Constants.MOD_ID,
            ModConfig.Type.CLIENT,
            JustEnoughTagLibClientConfig.SPEC
        );
    }
}
