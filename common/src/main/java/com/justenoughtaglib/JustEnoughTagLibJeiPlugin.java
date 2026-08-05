package com.justenoughtaglib;

import com.justenoughtaglib.config.JustEnoughTagLibClientConfig;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

public class JustEnoughTagLibJeiPlugin implements IModPlugin {
	private static final ResourceLocation PLUGIN_UID =
		new ResourceLocation(Constants.MOD_ID, "jei_plugin");
	private static final ResourceLocation BLOCK_TAG_RECIPE_TYPE_UID =
		new ResourceLocation("minecraft", "tag_recipes/block");

	@Override
	public ResourceLocation getPluginUid() {
		return PLUGIN_UID;
	}

	@Override
	public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
		if (JustEnoughTagLibClientConfig.HIDE_JEI_BLOCK_TAG_RECIPES.get()) {
			jeiRuntime.getRecipeManager()
					.getRecipeType(BLOCK_TAG_RECIPE_TYPE_UID)
					.ifPresent(jeiRuntime.getRecipeManager()::hideRecipeCategory);
		}
	}
}
