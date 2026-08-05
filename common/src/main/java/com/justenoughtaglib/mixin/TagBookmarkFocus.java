package com.justenoughtaglib.mixin;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.library.plugins.jei.tags.ITagInfoRecipe;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class TagBookmarkFocus {
	private TagBookmarkFocus() {
	}

	static Optional<ITypedIngredient<?>> find(
		ITagInfoRecipe recipe,
		IFocusGroup focuses,
		IIngredientManager ingredientManager
	) {
		List<ITypedIngredient<?>> members = recipe.getTypedIngredients();
		Optional<ITypedIngredient<?>> input = findRoleMember(
			focuses,
			RecipeIngredientRole.INPUT,
			members,
			ingredientManager
		);
		Optional<ITypedIngredient<?>> output = findRoleMember(
			focuses,
			RecipeIngredientRole.OUTPUT,
			members,
			ingredientManager
		);

		if (input.isEmpty() || output.isEmpty()) {
			return Optional.empty();
		}
		if (!sameIngredient(input.get(), output.get(), ingredientManager)) {
			return Optional.empty();
		}
		return input;
	}

	private static Optional<ITypedIngredient<?>> findRoleMember(
		IFocusGroup focuses,
		RecipeIngredientRole role,
		List<ITypedIngredient<?>> members,
		IIngredientManager ingredientManager
	) {
		for (IFocus<?> focus : focuses.getAllFocuses()) {
			if (focus.getRole() != role) {
				continue;
			}
			ITypedIngredient<?> focusedIngredient = focus.getTypedValue();
			if (focusedIngredient == null) {
				continue;
			}
			Optional<ITypedIngredient<?>> member = findMember(
				focusedIngredient,
				members,
				ingredientManager
			);
			if (member.isPresent()) {
				return member;
			}
		}
		return Optional.empty();
	}

	private static Optional<ITypedIngredient<?>> findMember(
		ITypedIngredient<?> focusedIngredient,
		List<ITypedIngredient<?>> members,
		IIngredientManager ingredientManager
	) {
		return members.stream()
			.filter(member -> sameIngredient(focusedIngredient, member, ingredientManager))
			.findFirst();
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static boolean sameIngredient(
		ITypedIngredient<?> first,
		ITypedIngredient<?> second,
		IIngredientManager ingredientManager
	) {
		if (!Objects.equals(first.getType(), second.getType())) {
			return false;
		}
		IIngredientHelper helper = ingredientManager.getIngredientHelper(first.getType());
		String firstUid = helper.getUniqueId(first.getIngredient(), UidContext.Ingredient);
		String secondUid = helper.getUniqueId(second.getIngredient(), UidContext.Ingredient);
		return Objects.equals(firstUid, secondUid);
	}
}
