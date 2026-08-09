package com.justenoughtaglib.tag;

import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.gui.bookmarks.IBookmark;
import mezz.jei.gui.bookmarks.RecipeBookmark;
import mezz.jei.library.plugins.jei.tags.ITagInfoRecipe;
import net.minecraft.tags.TagKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Live, whole-map source of truth for which ingredient the player wants shown for
 * each tag. {@link #refresh} recomputes the map from scratch on every bookmark-list
 * change, so a dropped bookmark immediately stops being offered. Both the layout-build
 * snapshot in {@code TagSlotTracker} and its per-cycle display pin consult
 * {@link #findPreferred} against the same map.
 *
 * <p>The stored value is the bookmark's typed ingredient, which carries its own
 * {@link mezz.jei.api.ingredients.IIngredientType}, so item and fluid tag
 * recipes on either loader share one map.</p>
 */
public final class TagBookmarkPreferences {
	private static volatile Map<TagKey<?>, ITypedIngredient<?>> preferences = Map.of();

	private TagBookmarkPreferences() {
	}

	public static void refresh(Collection<IBookmark> bookmarks) {
		Map<TagKey<?>, ITypedIngredient<?>> refreshed = new HashMap<>();
		for (IBookmark bookmark : bookmarks) {
			if (!(bookmark instanceof RecipeBookmark<?, ?> recipeBookmark)) {
				continue;
			}
			if (!(recipeBookmark.getRecipe() instanceof ITagInfoRecipe tagRecipe)) {
				continue;
			}
			ITypedIngredient<?> output = recipeBookmark.getRecipeOutput();
			if (output == null || TagIngredients.isEmpty(output.getType(), output.getIngredient())) {
				continue;
			}
			refreshed.putIfAbsent(tagRecipe.getTag(), output);
		}
		preferences = Map.copyOf(refreshed);
	}

	/**
	 * Finds the first {@code members} entry that equals the bookmarked ingredient for
	 * {@code tag}. Returns empty when the tag has no preference or the stored
	 * preference belongs to a different ingredient flavor (e.g. an item preference
	 * queried for a fluid tag recipe).
	 */
	public static Optional<Object> findPreferred(
		IIngredientManager ingredientManager,
		TagKey<?> tag,
		mezz.jei.api.ingredients.IIngredientType<?> type,
		List<?> members
	) {
		ITypedIngredient<?> stored = preferences.get(tag);
		if (stored == null || stored.getType() != type) {
			return Optional.empty();
		}
		Object preferred = stored.getIngredient();
		return members.stream()
			.map(member -> (Object) member)
			.filter(member -> TagIngredients.isSame(ingredientManager, type, preferred, member))
			.findFirst();
	}
}