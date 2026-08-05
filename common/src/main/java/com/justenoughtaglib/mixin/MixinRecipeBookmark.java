package com.justenoughtaglib.mixin;

import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.gui.bookmarks.RecipeBookmark;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes the item stored by tag recipe bookmarks.
 *
 * The original RecipeBookmark.create takes the item from the first OUTPUT
 * slot in the layout (tag recipe members are ordered according to their data
 * pack declaration), so the bookmark file stores the first tag member in
 * {@code "R:...tag_recipes/...#...#item_stack&<item>"} instead of the item
 * focused when the user pressed U.
 *
 * This intercepts create at its entry point. When the recipe type belongs to
 * tag_recipes/ and the layout has a focus, it reconstructs the bookmark with
 * the focused item (displayRole is fixed to OUTPUT, and an OUTPUT focus matches
 * the tag layout's output slot during deserialization). All other cases use
 * the original logic.
 *
 * remap = false: JEI classes and methods are not remapped at runtime on Forge
 * or Fabric.
 */
@Mixin(RecipeBookmark.class)
public class MixinRecipeBookmark {

	@Inject(method = "create", remap = false, at = @At("HEAD"), cancellable = true)
	private static <T> void justenoughtaglib$bookmarkFocusedTagItem(
		IRecipeLayoutDrawable<T> recipeLayoutDrawable,
		IIngredientManager ingredientManager,
		CallbackInfoReturnable<RecipeBookmark<T, ?>> cir
	) {
		ResourceLocation recipeTypeUid = recipeLayoutDrawable.getRecipeCategory().getRecipeType().getUid();
		if (!recipeTypeUid.getPath().startsWith("tag_recipes/")) {
			return; // Not a tag recipe; use the original logic.
		}
		if (!(recipeLayoutDrawable instanceof MixinRecipeLayoutAccessor accessor)) {
			return;
		}
		IFocusGroup focuses = accessor.justenoughtaglib$getFocuses();
		if (focuses == null) {
			return; // No focus (directly browsing the tag page); the original logic stores the first tag member.
		}
		for (IFocus<?> focus : focuses.getAllFocuses()) {
			ResourceLocation recipeUid = recipeLayoutDrawable.getRecipeCategory()
				.getRegistryName(recipeLayoutDrawable.getRecipe());
			if (recipeUid == null) {
				return; // Match the original logic: abandon the bookmark without a registry name.
			}
			ITypedIngredient<?> typed = ingredientManager.normalizeTypedIngredient(focus.getTypedValue());
			cir.setReturnValue(new RecipeBookmark<>(
				recipeLayoutDrawable.getRecipeCategory(),
				recipeLayoutDrawable.getRecipe(),
				recipeUid,
				typed,
				RecipeIngredientRole.OUTPUT
			));
			return;
		}
	}
}
