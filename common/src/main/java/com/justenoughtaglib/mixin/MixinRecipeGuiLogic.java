package com.justenoughtaglib.mixin;

import com.justenoughtaglib.tag.RecipeGuiSelection;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.gui.recipes.IRecipeLogicStateListener;
import mezz.jei.gui.recipes.RecipeGuiLogic;
import mezz.jei.gui.recipes.lookups.ILookupState;
import mezz.jei.gui.recipes.lookups.IngredientLookupState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(value = RecipeGuiLogic.class, remap = false)
public abstract class MixinRecipeGuiLogic implements RecipeGuiSelection {
	@Shadow
	private ILookupState state;

	@Shadow
	@Final
	private IRecipeLogicStateListener stateListener;

	@Override
	@Unique
	public boolean justenoughtaglib$selectRecipe(IRecipeCategory<?> category, Object recipe) {
		if (!(state instanceof IngredientLookupState)) {
			return false;
		}

		IRecipeCategory<?> previousCategory = state.getFocusedRecipes().getRecipeCategory();
		if (!state.moveToRecipeCategory(category)) {
			return false;
		}
		List<?> recipes = state.getFocusedRecipes().getRecipes();
		int recipeIndex = recipes.indexOf(recipe);
		if (recipeIndex < 0) {
			state.moveToRecipeCategory(previousCategory);
			return false;
		}

		((MixinIngredientLookupStateAccessor) state).justenoughtaglib$setRecipeIndex(recipeIndex);
		stateListener.onStateChange();
		return true;
	}
}
