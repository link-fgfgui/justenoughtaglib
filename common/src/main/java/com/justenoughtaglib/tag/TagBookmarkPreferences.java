package com.justenoughtaglib.tag;

import mezz.jei.gui.bookmarks.IBookmark;
import mezz.jei.gui.bookmarks.RecipeBookmark;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.library.plugins.jei.tags.ITagInfoRecipe;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Keeps the displayed item selected by each item-tag recipe bookmark. */
public final class TagBookmarkPreferences {
	private static volatile Map<TagKey<?>, ItemStack> preferredStacks = Map.of();

	private TagBookmarkPreferences() {
	}

	public static void refresh(Collection<IBookmark> bookmarks) {
		Map<TagKey<?>, ItemStack> refreshed = new HashMap<>();
		for (IBookmark bookmark : bookmarks) {
			if (!(bookmark instanceof RecipeBookmark<?, ?> recipeBookmark)) {
				continue;
			}
			if (!(recipeBookmark.getRecipe() instanceof ITagInfoRecipe tagRecipe)) {
				continue;
			}
			Object ingredient = recipeBookmark.getRecipeOutput().getIngredient();
			if (ingredient instanceof ItemStack stack && !stack.isEmpty()) {
				refreshed.putIfAbsent(tagRecipe.getTag(), stack.copy());
			}
		}
		preferredStacks = Map.copyOf(refreshed);
	}

	public static ItemStack[] apply(TagKey<?> tag, ItemStack[] expandedStacks) {
		ItemStack preferred = preferredStacks.get(tag);
		return selectPreferred(preferred, expandedStacks);
	}

	public static Optional<ITypedIngredient<?>> findPreferred(ITagInfoRecipe recipe) {
		ItemStack preferred = preferredStacks.get(recipe.getTag());
		if (preferred == null || preferred.isEmpty()) {
			return Optional.empty();
		}
		return recipe.getTypedIngredients().stream()
			.filter(candidate -> candidate.getIngredient() instanceof ItemStack stack &&
				ItemStack.isSameItemSameTags(preferred, stack))
			.findFirst();
	}

	static ItemStack[] selectPreferred(ItemStack preferred, ItemStack[] expandedStacks) {
		if (preferred == null || preferred.isEmpty()) {
			return expandedStacks;
		}
		for (ItemStack expandedStack : expandedStacks) {
			if (ItemStack.isSameItemSameTags(preferred, expandedStack)) {
				return new ItemStack[]{expandedStack};
			}
		}
		return expandedStacks;
	}
}
