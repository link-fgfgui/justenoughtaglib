package com.justenoughtaglib.tag;

import mezz.jei.gui.bookmarks.IBookmark;
import mezz.jei.gui.bookmarks.RecipeBookmark;
import mezz.jei.library.plugins.jei.tags.ITagInfoRecipe;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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

	public static Optional<ItemStack> findPreferred(TagKey<?> tag, List<ItemStack> members) {
		return findMatching(preferredStacks.get(tag), members);
	}

	static Optional<ItemStack> findMatching(ItemStack preferred, List<ItemStack> members) {
		if (preferred == null || preferred.isEmpty()) {
			return Optional.empty();
		}
		return members.stream()
			.filter(member -> ItemStack.isSameItemSameTags(preferred, member))
			.findFirst();
	}
}
