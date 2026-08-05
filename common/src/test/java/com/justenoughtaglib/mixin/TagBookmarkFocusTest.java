package com.justenoughtaglib.mixin;

import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.library.plugins.jei.tags.ITagInfoRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagBookmarkFocusTest {
	private static final IIngredientType<String> ITEM_TYPE = () -> String.class;
	private static final IIngredientType<String> OTHER_TYPE = () -> String.class;
	private static final IIngredientHelper<String> INGREDIENT_HELPER = new StringIngredientHelper();
	private static final IIngredientManager INGREDIENT_MANAGER = createIngredientManager();

	@Test
	void emptyGroupDoesNotSelectAMember() {
		FakeTypedIngredient oak = item("oak_planks");
		FakeTypedIngredient jungle = item("jungle_planks");

		Optional<ITypedIngredient<?>> selected = TagBookmarkFocus.find(
			recipe(oak, jungle),
			focuses(),
			INGREDIENT_MANAGER
		);

		assertTrue(selected.isEmpty());
	}

	@Test
	void outputOnlyGroupDoesNotSelectAMember() {
		FakeTypedIngredient oak = item("oak_planks");
		FakeTypedIngredient jungle = item("jungle_planks");

		Optional<ITypedIngredient<?>> selected = TagBookmarkFocus.find(
			recipe(oak, jungle),
			focuses(focus(RecipeIngredientRole.OUTPUT, jungle)),
			INGREDIENT_MANAGER
		);

		assertTrue(selected.isEmpty());
	}

	@Test
	void matchingRolesIgnoreUnrelatedFocusAndReturnCurrentMember() {
		FakeTypedIngredient oak = item("oak_planks");
		FakeTypedIngredient jungle = item("jungle_planks");
		FakeTypedIngredient unrelated = new FakeTypedIngredient(OTHER_TYPE, "water");

		Optional<ITypedIngredient<?>> selected = TagBookmarkFocus.find(
			recipe(oak, jungle),
			focuses(
				focus(RecipeIngredientRole.INPUT, unrelated),
				focus(RecipeIngredientRole.INPUT, jungle),
				focus(RecipeIngredientRole.OUTPUT, jungle)
			),
			INGREDIENT_MANAGER
		);

		assertTrue(selected.isPresent());
		assertSame(jungle, selected.orElseThrow());
	}

	@Test
	void disagreeingRolesDoNotSelectEitherMember() {
		FakeTypedIngredient oak = item("oak_planks");
		FakeTypedIngredient jungle = item("jungle_planks");

		Optional<ITypedIngredient<?>> selected = TagBookmarkFocus.find(
			recipe(oak, jungle),
			focuses(
				focus(RecipeIngredientRole.INPUT, oak),
				focus(RecipeIngredientRole.OUTPUT, jungle)
			),
			INGREDIENT_MANAGER
		);

		assertTrue(selected.isEmpty());
	}

	@Test
	void staleValueDoesNotGetInjectedIntoRecipe() {
		FakeTypedIngredient oak = item("oak_planks");
		FakeTypedIngredient jungle = item("jungle_planks");
		FakeTypedIngredient stale = item("birch_planks");

		Optional<ITypedIngredient<?>> selected = TagBookmarkFocus.find(
			recipe(oak, jungle),
			focuses(
				focus(RecipeIngredientRole.INPUT, stale),
				focus(RecipeIngredientRole.OUTPUT, stale)
			),
			INGREDIENT_MANAGER
		);

		assertTrue(selected.isEmpty());
	}

	@Test
	void preferredTransferLayoutWinsWhenAllowed() {
		Object preferred = new Object();
		boolean[] fallbackCalled = {false};
		Supplier<Optional<Object>> fallback = () -> {
			fallbackCalled[0] = true;
			return Optional.of(new Object());
		};
		Predicate<Object> allowed = value -> value == preferred;

		Optional<Object> selected = TransferLayoutPolicy.justenoughtaglib$selectPreferredTransferLayout(
			preferred,
			fallback,
			allowed
		);

		assertTrue(selected.isPresent());
		assertSame(preferred, selected.orElseThrow());
		assertFalse(fallbackCalled[0]);
	}

	@Test
	void disallowedPreferredLayoutUsesAllowedFallback() {
		Object preferred = new Object();
		Object fallbackLayout = new Object();
		boolean[] fallbackCalled = {false};
		Supplier<Optional<Object>> fallback = () -> {
			fallbackCalled[0] = true;
			return Optional.of(fallbackLayout);
		};
		Predicate<Object> allowed = value -> value == fallbackLayout;

		Optional<Object> selected = TransferLayoutPolicy.justenoughtaglib$selectPreferredTransferLayout(
			preferred,
			fallback,
			allowed
		);

		assertTrue(selected.isPresent());
		assertSame(fallbackLayout, selected.orElseThrow());
		assertTrue(fallbackCalled[0]);
	}

	@Test
	void disallowedPreferredAndFallbackLayoutsProduceEmptySelection() {
		Object preferred = new Object();
		Object fallbackLayout = new Object();
		boolean[] fallbackCalled = {false};
		Supplier<Optional<Object>> fallback = () -> {
			fallbackCalled[0] = true;
			return Optional.of(fallbackLayout);
		};
		Predicate<Object> allowed = value -> false;

		Optional<Object> selected = TransferLayoutPolicy.justenoughtaglib$selectPreferredTransferLayout(
			preferred,
			fallback,
			allowed
		);

		assertTrue(selected.isEmpty());
		assertTrue(fallbackCalled[0]);
	}

	private static FakeTypedIngredient item(String id) {
		return new FakeTypedIngredient(ITEM_TYPE, id);
	}

	private static FakeTagInfoRecipe recipe(FakeTypedIngredient... members) {
		return new FakeTagInfoRecipe(Arrays.asList(members));
	}

	private static FakeFocusGroup focuses(IFocus<?>... focuses) {
		return new FakeFocusGroup(Arrays.asList(focuses));
	}

	private static FakeFocus focus(RecipeIngredientRole role, FakeTypedIngredient ingredient) {
		return new FakeFocus(role, ingredient);
	}

	private static IIngredientManager createIngredientManager() {
		return (IIngredientManager) Proxy.newProxyInstance(
			IIngredientManager.class.getClassLoader(),
			new Class<?>[]{IIngredientManager.class},
			(proxy, method, args) -> {
				if (method.getName().equals("getIngredientHelper")) {
					return INGREDIENT_HELPER;
				}
				throw new AssertionError("Unexpected ingredient manager call: " + method);
			}
		);
	}

	private record FakeTypedIngredient(
		IIngredientType<String> type,
		String ingredient
	) implements ITypedIngredient<String> {
		@Override
		public IIngredientType<String> getType() {
			return type;
		}

		@Override
		public String getIngredient() {
			return ingredient;
		}
	}

	private record FakeFocus(
		RecipeIngredientRole role,
		FakeTypedIngredient typedValue
	) implements IFocus<String> {
		@Override
		public ITypedIngredient<String> getTypedValue() {
			return typedValue;
		}

		@Override
		public RecipeIngredientRole getRole() {
			return role;
		}

		@Override
		public <T> Optional<IFocus<T>> checkedCast(IIngredientType<T> ingredientType) {
			if (!typedValue.getType().equals(ingredientType)) {
				return Optional.empty();
			}
			@SuppressWarnings("unchecked")
			IFocus<T> cast = (IFocus<T>) this;
			return Optional.of(cast);
		}
	}

	private static final class FakeFocusGroup implements IFocusGroup {
		private final List<IFocus<?>> focuses;

		private FakeFocusGroup(List<IFocus<?>> focuses) {
			this.focuses = List.copyOf(focuses);
		}

		@Override
		public boolean isEmpty() {
			return focuses.isEmpty();
		}

		@Override
		public List<IFocus<?>> getAllFocuses() {
			return focuses;
		}

		@Override
		public Stream<IFocus<?>> getFocuses(RecipeIngredientRole role) {
			return focuses.stream().filter(focus -> focus.getRole() == role);
		}

		@Override
		public <T> Stream<IFocus<T>> getFocuses(IIngredientType<T> ingredientType) {
			return focuses.stream()
				.filter(focus -> focus.getTypedValue().getType().equals(ingredientType))
				.map(FakeFocusGroup::castFocus);
		}

		@Override
		public <T> Stream<IFocus<T>> getFocuses(IIngredientType<T> ingredientType, RecipeIngredientRole role) {
			return getFocuses(ingredientType)
				.filter(focus -> focus.getRole() == role);
		}

		@SuppressWarnings("unchecked")
		private static <T> IFocus<T> castFocus(IFocus<?> focus) {
			return (IFocus<T>) focus;
		}
	}

	private static final class FakeTagInfoRecipe implements ITagInfoRecipe {
		private final List<ITypedIngredient<?>> members;

		private FakeTagInfoRecipe(List<? extends ITypedIngredient<?>> members) {
			this.members = List.copyOf(members);
		}

		@Override
		public TagKey<?> getTag() {
			return TagKey.create(Registries.ITEM, new ResourceLocation("test", "planks"));
		}

		@Override
		public List<ITypedIngredient<?>> getTypedIngredients() {
			return members;
		}
	}

	private static final class StringIngredientHelper implements IIngredientHelper<String> {
		@Override
		public IIngredientType<String> getIngredientType() {
			return ITEM_TYPE;
		}

		@Override
		public String getDisplayName(String ingredient) {
			return ingredient;
		}

		@Override
		public String getUniqueId(String ingredient, UidContext context) {
			return ingredient;
		}

		@Override
		public ResourceLocation getResourceLocation(String ingredient) {
			return new ResourceLocation("test", ingredient);
		}

		@Override
		public String copyIngredient(String ingredient) {
			return ingredient;
		}

		@Override
		public String getErrorInfo(String ingredient) {
			return ingredient;
		}
	}
}
