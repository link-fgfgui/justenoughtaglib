package com.justenoughtaglib.mixin;

import com.justenoughtaglib.bookmark.RecipeContextElement;
import com.justenoughtaglib.tag.TagBookmarkPreferences;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.bookmarks.IBookmark;
import mezz.jei.gui.bookmarks.RecipeBookmark;
import mezz.jei.gui.input.UserInput;
import mezz.jei.gui.overlay.bookmarks.BookmarkOverlay;
import mezz.jei.gui.overlay.elements.IElement;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Converts clicks on recipe output slots into JEI's standard recipe bookmarks.
 */
@Mixin(BookmarkList.class)
public abstract class MixinBookmarkList {
	@Shadow(remap = false)
	public abstract boolean add(IBookmark value);
	@Shadow(remap = false)
	@Final
	private IIngredientManager ingredientManager;
	@Shadow(remap = false)
	@Final
	private List<IBookmark> bookmarksList;

	@Inject(method = "notifyListenersOfChange", remap = false, at = @At("HEAD"))
	private void justenoughtaglib$refreshTagBookmarkPreferences(CallbackInfo ci) {
		TagBookmarkPreferences.refresh(bookmarksList);
	}

	@Inject(method = "onElementBookmarked", remap = false, at = @At("HEAD"), cancellable = true)
	private <T> void justenoughtaglib$bookmarkRecipeOutput(
		IElement<T> element,
		UserInput input,
		BookmarkOverlay bookmarkOverlay,
		CallbackInfoReturnable<Boolean> cir
	) {
		// not a bookmark screen
		if (bookmarkOverlay.isMouseOver(input.getMouseX(), input.getMouseY())) {
			return;
		}
		// need a recipe screen
		if (!(element instanceof RecipeContextElement<?> recipeContextElement)) {
			return;
		}
		if (!recipeContextElement.getRecipeLayout().getRecipeCategory().getRecipeType().getUid().getPath().startsWith("tag_recipes/")) {
			return;
		}
		// ensure a output slot
		if (recipeContextElement.getRole() != RecipeIngredientRole.OUTPUT) {
			return;
		}
		// catch it, add a recipe bookmark. do not use jei internal api because i want to bookmark the item user focused
		RecipeBookmark<?, ?> recipeBookmark = createOutputRecipeBookmark(recipeContextElement, ingredientManager);
		if (recipeBookmark != null) {
			cir.setReturnValue(add(recipeBookmark));
		}
	}

	@SuppressWarnings("unchecked")
	private static RecipeBookmark<?, ?> createOutputRecipeBookmark(
		RecipeContextElement<?> element,
		IIngredientManager ingredientManager
	) {
		IRecipeLayoutDrawable<Object> recipeLayout = (IRecipeLayoutDrawable<Object>) element.getRecipeLayout();
		return createOutputRecipeBookmark(
			recipeLayout,
			element.getTypedIngredient(),
			ingredientManager
		);
	}

	private static <R, I> RecipeBookmark<R, I> createOutputRecipeBookmark(
		IRecipeLayoutDrawable<R> recipeLayout,
		ITypedIngredient<I> displayedIngredient,
		IIngredientManager ingredientManager
	) {
		IRecipeCategory<R> recipeCategory = recipeLayout.getRecipeCategory();
		R recipe = recipeLayout.getRecipe();
		ResourceLocation recipeUid = recipeCategory.getRegistryName(recipe);
		if (recipeUid == null) {
			return null;
		}

		ITypedIngredient<I> normalizedIngredient = ingredientManager.normalizeTypedIngredient(displayedIngredient);
		return new RecipeBookmark<>(
			recipeCategory,
			recipe,
			recipeUid,
			normalizedIngredient,
			RecipeIngredientRole.OUTPUT
		);
	}
}
