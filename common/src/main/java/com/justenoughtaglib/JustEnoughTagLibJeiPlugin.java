package com.justenoughtaglib;

import com.justenoughtaglib.config.JustEnoughTagLibClientConfig;
import com.justenoughtaglib.tag.TagSlotTracker;
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
		TagSlotTracker.setRuntimeAvailable(true);
		TagSlotTracker.setIngredientManager(jeiRuntime.getIngredientManager());
		if (JustEnoughTagLibClientConfig.HIDE_JEI_BLOCK_TAG_RECIPES.get()) {
			jeiRuntime.getRecipeManager()
					.getRecipeType(BLOCK_TAG_RECIPE_TYPE_UID)
					.ifPresent(jeiRuntime.getRecipeManager()::hideRecipeCategory);
		}
	}

	@Override
	public void onRuntimeUnavailable() {
		// Recipe layouts can outlive a world screen by one client tick. Do not
		// let their custom slot handlers consult the stopped JEI runtime.
		TagSlotTracker.setRuntimeAvailable(false);
		TagSlotTracker.setIngredientManager(null);
		TagSlotTracker.clear();
	}
}
