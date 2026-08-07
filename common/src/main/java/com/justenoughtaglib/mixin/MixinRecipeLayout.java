package com.justenoughtaglib.mixin;

import com.justenoughtaglib.tag.TagSlotTracker;
import mezz.jei.library.gui.recipes.RecipeLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps tag bookmark display preferences after JEI advances ingredient cycles. */
@Mixin(value = RecipeLayout.class, remap = false)
public class MixinRecipeLayout {
	@Inject(
		method = "tick",
		at = @At(
			value = "INVOKE",
			target = "Lmezz/jei/api/recipe/category/IRecipeCategory;onDisplayedIngredientsUpdate(Ljava/lang/Object;Ljava/util/List;Lmezz/jei/api/recipe/IFocusGroup;)V",
			shift = At.Shift.AFTER
		),
		remap = false
	)
	private void justenoughtaglib$reapplyTagDisplayOverrides(CallbackInfo ci) {
		TagSlotTracker.reapplyDisplayOverrides((RecipeLayout<?>) (Object) this);
	}
}
