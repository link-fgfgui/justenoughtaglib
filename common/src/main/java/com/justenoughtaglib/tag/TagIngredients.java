package com.justenoughtaglib.tag;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Small helpers on top of the JEI {@link IIngredientHelper} API. The tag feature
 * deliberately avoids per-type logic: every operation delegates to JEI, so item,
 * fluid and any other registered ingredient type behave alike.
 */
public final class TagIngredients {
	private TagIngredients() {
	}

	/**
	 * Whether two ingredients of {@code type} are the same entity. Reuses JEI's own
	 * ingredient identity, {@code IIngredientHelper#getUniqueId(ingredient, UidContext.Ingredient)},
	 * which is the same context JEI uses to match an ingredient against another one
	 * ({@code ElementSearch}, {@code IngredientBookmark}, {@code DisplayIngredientAcceptor}).
	 * {@link UidContext#Recipe} is only for serializing an ingredient, never for in-memory
	 * identity, so it is deliberately not used here — {@code MixinRecipeBookmark} follows
	 * the same rule. Amount and count are ignored; whether NBT matters is decided by JEI's
	 * subtype system.
	 */
	public static boolean isSame(IIngredientManager ingredientManager, IIngredientType<?> type, Object a, Object b) {
		if (a == b) {
			return true;
		}
		IIngredientHelper<Object> helper = helper(ingredientManager, type);
		if (!helper.isValidIngredient(a) || !helper.isValidIngredient(b)) {
			return false;
		}
		return helper.getUniqueId(a, UidContext.Ingredient).equals(helper.getUniqueId(b, UidContext.Ingredient));
	}

	public static boolean isSame(IIngredientManager ingredientManager, ITypedIngredient<?> a, ITypedIngredient<?> b) {
		if (a == b) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (!a.getType().equals(b.getType())) {
			return false;
		}
		return isSame(ingredientManager, a.getType(), a.getIngredient(), b.getIngredient());
	}

	/**
	 * Whether an ingredient is empty. Item stacks have a vanilla empty state; other
	 * ingredient types are produced and validated by JEI, so an empty instance cannot
	 * end up bookmarked.
	 */
	static boolean isEmpty(IIngredientType<?> type, Object ingredient) {
		if (type != VanillaTypes.ITEM_STACK) {
			return false;
		}
		return !(ingredient instanceof ItemStack stack) || stack.isEmpty();
	}

	static Object copy(IIngredientManager ingredientManager, IIngredientType<?> type, Object ingredient) {
		return helper(ingredientManager, type).copyIngredient(ingredient);
	}

	static Optional<TagKey<?>> getTagKeyEquivalent(IIngredientManager ingredientManager, IIngredientType<?> type, List<?> members) {
		IIngredientHelper<Object> helper = helper(ingredientManager, type);
		return helper.getTagKeyEquivalent((List<Object>) members);
	}

	private static IIngredientHelper<Object> helper(IIngredientManager ingredientManager, IIngredientType<?> type) {
		@SuppressWarnings({"rawtypes", "unchecked"})
		IIngredientHelper<Object> helper =
			(IIngredientHelper<Object>) (IIngredientHelper) ingredientManager.getIngredientHelper((IIngredientType) type);
		return helper;
	}
}