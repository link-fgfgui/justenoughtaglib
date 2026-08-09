package com.justenoughtaglib.mixin;

import com.justenoughtaglib.tag.TagSlotTracker;
import mezz.jei.library.gui.recipes.RecipeLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps the tag-preferred member on screen while JEI advances its ingredient cycles.
 *
 * <p>JEI's {@code tick()} clears every slot's display overrides at the start of a cycle,
 * calls the category's {@code onDisplayedIngredientsUpdate}, and only then may we run —
 * so this injection point <em>after</em> that hook is the correct place to re-pin.
 * Re-pinning each cycle is what keeps a bookmarked tag on its preferred member;
 * not re-pinning is what lets an un-bookmarked tag revert on its own. See
 * {@link TagSlotTracker}.
 */
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
