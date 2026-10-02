package com.justenoughtaglib.tag;

import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagIngredientsTest {

	private static <T> ITypedIngredient<T> typed(IIngredientType<T> type, T ingredient) {
		return new ITypedIngredient<>() {
			@Override
			public IIngredientType<T> getType() {
				return type;
			}

			@Override
			public T getIngredient() {
				return ingredient;
			}
		};
	}

	@Test
	void testIsSameIdentityAndNull() {
		assertTrue(TagIngredients.isSame(null, (ITypedIngredient<?>) null, null));

		IIngredientType<String> type = () -> String.class;
		ITypedIngredient<String> typedA = typed(type, "test");
		assertTrue(TagIngredients.isSame(null, typedA, typedA));
		assertFalse(TagIngredients.isSame(null, typedA, null));
		assertFalse(TagIngredients.isSame(null, null, typedA));
	}

	@Test
	void testIsSameDifferentTypes() {
		IIngredientType<String> type1 = () -> String.class;
		IIngredientType<Integer> type2 = () -> Integer.class;

		ITypedIngredient<String> typedA = typed(type1, "42");
		ITypedIngredient<Integer> typedB = typed(type2, 42);

		assertFalse(TagIngredients.isSame(null, typedA, typedB));
	}
}
