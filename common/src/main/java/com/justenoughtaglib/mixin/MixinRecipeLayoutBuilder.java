package com.justenoughtaglib.mixin;

import com.justenoughtaglib.tag.TagSlotTracker;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.library.gui.recipes.RecipeLayout;
import mezz.jei.library.gui.recipes.layout.builder.RecipeLayoutBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Resolves tag-equivalent input slots after JEI has finished building them and
 * applies display-only bookmark preferences without changing slot ingredients.
 */
@Mixin(RecipeLayoutBuilder.class)
public abstract class MixinRecipeLayoutBuilder<T> {
	@Shadow(remap = false)
	@Final
	private IIngredientManager ingredientManager;

	@Inject(method = "buildRecipeLayout", remap = false, at = @At("RETURN"))
	private void justenoughtaglib$prepareTagSlots(CallbackInfoReturnable<RecipeLayout<T>> cir) {
		TagSlotTracker.prepareLayout(cir.getReturnValue(), ingredientManager);
	}
}
