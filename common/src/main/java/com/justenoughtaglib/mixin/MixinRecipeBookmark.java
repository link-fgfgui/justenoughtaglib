package com.justenoughtaglib.mixin;

import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.gui.bookmarks.RecipeBookmark;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes the item stored by a recipe bookmark when a recipe is opened from a focus.
 *
 * <p>The original {@code RecipeBookmark.create} takes the item from the first
 * slot it finds (OUTPUT, then INPUT), so for recipes with several output slots —
 * a tag recipe puts each member in its own output slot, and multi-output recipes
 * simply have more than one — the bookmark stored the first output even when the
 * user entered the layout from a later one via R/U.</p>
 *
 * This intercepts create at its entry point for every recipe. When the layout
 * carries a focus whose item appears in an output slot, the bookmark is rebuilt
 * with that focused item. Layouts without such a focus keep JEI's original logic
 * (first OUTPUT slot, then first INPUT slot).
 *
 * remap = false: JEI classes and methods are not remapped at runtime on Forge
 * or Fabric.
 */
@Mixin(RecipeBookmark.class)
public class MixinRecipeBookmark {

	@Inject(method = "create", remap = false, at = @At("HEAD"), cancellable = true)
	private static <T> void justenoughtaglib$bookmarkFocusedItem(
		IRecipeLayoutDrawable<T> recipeLayoutDrawable,
		IIngredientManager ingredientManager,
		CallbackInfoReturnable<RecipeBookmark<T, ?>> cir
	) {
		if (!(recipeLayoutDrawable instanceof MixinRecipeLayoutAccessor accessor)) {
			return;
		}
		IFocusGroup focuses = accessor.justenoughtaglib$getFocuses();
		if (focuses == null) {
			return; // No focus (directly browsing a page); use the original logic.
		}
		ResourceLocation recipeUid = recipeLayoutDrawable.getRecipeCategory()
			.getRegistryName(recipeLayoutDrawable.getRecipe());
		for (IFocus<?> focus : focuses.getAllFocuses()) {
			if (recipeUid == null) {
				return; // Match the original logic: abandon the bookmark without a registry name.
			}
			ITypedIngredient<?> typed = ingredientManager.normalizeTypedIngredient(focus.getTypedValue());
			if (!justenoughtaglib$isInOutputSlot(recipeLayoutDrawable, typed, ingredientManager)) {
				continue;
			}
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

	/**
	 * Whether any OUTPUT slot of the layout contains the focused ingredient.
	 */
	@Unique
	private static boolean justenoughtaglib$isInOutputSlot(
		IRecipeLayoutDrawable<?> recipeLayoutDrawable,
		ITypedIngredient<?> focused,
		IIngredientManager ingredientManager
	) {
		IRecipeSlotsView slotsView = recipeLayoutDrawable.getRecipeSlotsView();
		for (IRecipeSlotView slotView : slotsView.getSlotViews(RecipeIngredientRole.OUTPUT)) {
			for (ITypedIngredient<?> candidate : slotView.getAllIngredients().toList()) {
				if (justenoughtaglib$isSameIngredient(candidate, focused, ingredientManager)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Compares two typed ingredients of the same type, ignoring normalization
	 * differences (JEI stores bookmark ids with {@link UidContext#Recipe}).
	 */
	@Unique
	@SuppressWarnings("unchecked")
	private static boolean justenoughtaglib$isSameIngredient(
		ITypedIngredient<?> a,
		ITypedIngredient<?> b,
		IIngredientManager ingredientManager
	) {
		if (!a.getType().equals(b.getType())) {
			return false;
		}
		Object aValue = a.getIngredient();
		Object bValue = b.getIngredient();
		if (aValue == bValue) {
			return true;
		}
		IIngredientHelper<Object> helper = (IIngredientHelper<Object>) ingredientManager.getIngredientHelper(a.getType());
		if (!helper.isValidIngredient(aValue) || !helper.isValidIngredient(bValue)) {
			return false;
		}
		return helper.getUniqueId(aValue, UidContext.Recipe).equals(helper.getUniqueId(bValue, UidContext.Recipe));
	}
}
