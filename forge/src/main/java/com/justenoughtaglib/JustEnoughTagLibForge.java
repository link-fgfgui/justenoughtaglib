package com.justenoughtaglib;

import com.justenoughtaglib.config.JustEnoughTagLibClientConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(Constants.MOD_ID)
public class JustEnoughTagLibForge {
    
    public JustEnoughTagLibForge() {
        ModLoadingContext.get().registerConfig(
            ModConfig.Type.CLIENT,
            JustEnoughTagLibClientConfig.SPEC
        );
    }
}
