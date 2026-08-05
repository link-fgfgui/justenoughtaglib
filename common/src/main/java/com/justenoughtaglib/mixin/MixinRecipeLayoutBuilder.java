package com.justenoughtaglib.mixin;

import com.justenoughtaglib.tag.TagSlotTracker;
import mezz.jei.library.gui.recipes.RecipeLayout;
import mezz.jei.library.gui.recipes.layout.builder.RecipeLayoutBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Associates tag mappings captured during construction with the final
 * RecipeLayout instance. Each layout gets an independent mapping (grouped with
 * ThreadLocal), which is attached when buildRecipeLayout returns. Click queries
 * read only the current layout's data, so stale entries from other pages or
 * recipes cannot match accidentally.
 */
@Mixin(RecipeLayoutBuilder.class)
public abstract class MixinRecipeLayoutBuilder<T> {

	@Inject(method = "<init>", remap = false, at = @At("TAIL"))
	private void justenoughtaglib$beginTagBuild(CallbackInfo ci) {
		TagSlotTracker.beginBuild();
	}

	@Inject(method = "buildRecipeLayout", remap = false, at = @At("RETURN"))
	private void justenoughtaglib$associateTagBuild(CallbackInfoReturnable<RecipeLayout<T>> cir) {
		TagSlotTracker.associateLayout(cir.getReturnValue());
	}
}
