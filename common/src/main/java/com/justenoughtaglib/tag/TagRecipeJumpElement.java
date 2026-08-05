package com.justenoughtaglib.tag;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.JeiTooltip;
import mezz.jei.gui.bookmarks.IBookmark;
import mezz.jei.gui.overlay.IngredientGridTooltipHelper;
import mezz.jei.gui.overlay.elements.IElement;
import mezz.jei.gui.util.FocusUtil;
import mezz.jei.library.plugins.jei.tags.ITagInfoRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.List;
import java.util.Optional;

public final class TagRecipeJumpElement<T> implements IElement<T> {
	private static final ResourceLocation TAG_RECIPE_TYPE_UID = new ResourceLocation("minecraft", "tag_recipes/item");

	private final ITypedIngredient<T> ingredient;
	private final TagKey<?> tag;

	public TagRecipeJumpElement(ITypedIngredient<T> ingredient, TagKey<?> tag) {
		this.ingredient = ingredient;
		this.tag = tag;
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
		Optional<RecipeType<ITagInfoRecipe>> recipeType = Internal.getJeiRuntime().getRecipeManager()
			.getRecipeType(TAG_RECIPE_TYPE_UID, ITagInfoRecipe.class);
		if (recipeType.isEmpty()) {
			showItemRecipes(recipesGui, focusUtil, roles);
			return;
		}
		ITagInfoRecipe tagRecipe = Internal.getJeiRuntime().getRecipeManager()
			.createRecipeLookup(recipeType.get())
			.get()
			.filter(recipe -> recipe.getTag().equals(tag))
			.findFirst()
			.orElse(null);
		if (tagRecipe == null) {
			showItemRecipes(recipesGui, focusUtil, roles);
			return;
		}
		List<IFocus<?>> focuses = focusUtil.createFocuses(ingredient, List.of(RecipeIngredientRole.INPUT));
		IRecipeCategory<ITagInfoRecipe> category = Internal.getJeiRuntime().getRecipeManager().getRecipeCategory(recipeType.get());
		recipesGui.showRecipes(category, List.of(tagRecipe), focuses);
	}

	private void showItemRecipes(IRecipesGui recipesGui, FocusUtil focusUtil, List<RecipeIngredientRole> roles) {
		List<IFocus<?>> focuses = focusUtil.createFocuses(ingredient, roles);
		recipesGui.show(focuses);
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
