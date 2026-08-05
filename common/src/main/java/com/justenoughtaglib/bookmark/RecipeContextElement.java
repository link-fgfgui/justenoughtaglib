package com.justenoughtaglib.bookmark;

import com.justenoughtaglib.mixin.MixinRecipesGuiAccessor;
import com.justenoughtaglib.tag.RecipeGuiSelection;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.common.gui.JeiTooltip;
import mezz.jei.gui.bookmarks.IBookmark;
import mezz.jei.gui.overlay.IngredientGridTooltipHelper;
import mezz.jei.gui.overlay.elements.IElement;
import mezz.jei.gui.util.FocusUtil;

import java.util.List;
import java.util.Optional;

/**
 * Carries the recipe layout and slot role needed for output bookmark creation.
 *
 * <p>The element deliberately is not itself a bookmark. It is only used while JEI
 * handles the click on a displayed recipe ingredient.</p>
 */
public final class RecipeContextElement<T> implements IElement<T> {
	private final IRecipeLayoutDrawable<?> recipeLayout;
	private final ITypedIngredient<T> ingredient;
	private final RecipeIngredientRole role;

	public RecipeContextElement(
		IRecipeLayoutDrawable<?> recipeLayout,
		ITypedIngredient<T> ingredient,
		RecipeIngredientRole role
	) {
		this.recipeLayout = recipeLayout;
		this.ingredient = ingredient;
		this.role = role;
	}

	public IRecipeLayoutDrawable<?> getRecipeLayout() {
		return recipeLayout;
	}

	public RecipeIngredientRole getRole() {
		return role;
	}

	@Override
	public ITypedIngredient<T> getTypedIngredient() {
		return ingredient;
	}

	@Override
	public Optional<IBookmark> getBookmark() {
		return Optional.empty();
	}

	@Override
	public IDrawable createRenderOverlay() {
		return null;
	}

	@Override
	public void show(IRecipesGui recipesGui, FocusUtil focusUtil, List<RecipeIngredientRole> roles) {
		ITypedIngredient<?> ingredient = getTypedIngredient();
		List<IFocus<?>> focuses = focusUtil.createFocuses(ingredient, roles);
		recipesGui.show(focuses);

		if (role == RecipeIngredientRole.OUTPUT &&
			recipeLayout.getRecipeCategory().getRecipeType().getUid().getPath().startsWith("tag_recipes/") &&
			roles.contains(RecipeIngredientRole.INPUT) &&
			recipesGui instanceof MixinRecipesGuiAccessor accessor &&
			accessor.justenoughtaglib$getLogic() instanceof RecipeGuiSelection selection) {
			selection.justenoughtaglib$selectRecipe(
				recipeLayout.getRecipeCategory(),
				recipeLayout.getRecipe()
			);
		}
	}

	@Override
	public void getTooltip(
		JeiTooltip tooltip,
		IngredientGridTooltipHelper tooltipHelper,
		IIngredientRenderer<T> ingredientRenderer,
		IIngredientHelper<T> ingredientHelper
	) {
		tooltipHelper.getIngredientTooltip(tooltip, ingredient, ingredientRenderer, ingredientHelper);
	}

	@Override
	public boolean isVisible() {
		return true;
	}
}
