package com.justenoughtaglib.tag;

import com.justenoughtaglib.mixin.MixinRecipeSlotAccessor;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.library.gui.recipes.RecipeLayout;
import mezz.jei.library.plugins.jei.tags.ITagInfoRecipe;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.BiFunction;

/** Resolves tag-equivalent JEI slots and applies bookmark-driven display overrides. */
public final class TagSlotTracker {
	private static final Map<RecipeLayout<?>, Map<IRecipeSlotView, TagSlotData>> LAYOUT_TAGS =
		Collections.synchronizedMap(new WeakHashMap<>());

	private TagSlotTracker() {
	}

	public record TagSlotData(TagKey<?> tag, List<ItemStack> members, boolean preferred) {
		public TagSlotData {
			members = members.stream().map(ItemStack::copy).toList();
		}
	}

	public static void prepareLayout(
		RecipeLayout<?> layout,
		IIngredientHelper<ItemStack> itemStackHelper
	) {
		prepareLayout(layout, itemStackHelper, TagBookmarkPreferences::findPreferred);
	}

	static void prepareLayout(
		RecipeLayout<?> layout,
		IIngredientHelper<ItemStack> itemStackHelper,
		BiFunction<TagKey<?>, List<ItemStack>, Optional<ItemStack>> preferenceLookup
	) {
		Map<IRecipeSlotView, TagSlotData> tagSlots = new IdentityHashMap<>();
		TagKey<?> recipeTag = layout.getRecipe() instanceof ITagInfoRecipe tagRecipe ? tagRecipe.getTag() : null;

		for (IRecipeSlotDrawable slot : layout.getRecipeSlots().getSlots(RecipeIngredientRole.INPUT)) {
			List<ItemStack> members = slot.getItemStacks().toList();
			Optional<TagKey<?>> tag = recipeTag == null
				? itemStackHelper.getTagKeyEquivalent(members)
				: Optional.of(recipeTag);
			if (tag.isEmpty()) {
				continue;
			}

			Optional<ItemStack> preferred = preferenceLookup.apply(tag.get(), members);
			TagSlotData data = new TagSlotData(tag.get(), members, preferred.isPresent());
			tagSlots.put(slot, data);
			applyPreferenceIfUnclaimed(slot, preferred);
		}

		if (!tagSlots.isEmpty()) {
			LAYOUT_TAGS.put(layout, Collections.unmodifiableMap(tagSlots));
		}
	}

	public static Optional<TagSlotData> findTagData(
		IRecipeLayoutDrawable<?> layout,
		IRecipeSlotView slot
	) {
		if (!(layout instanceof RecipeLayout<?> recipeLayout)) {
			return Optional.empty();
		}
		Map<IRecipeSlotView, TagSlotData> tagSlots = LAYOUT_TAGS.get(recipeLayout);
		if (tagSlots == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(tagSlots.get(slot));
	}

	public static void reapplyDisplayOverrides(RecipeLayout<?> layout) {
		reapplyDisplayOverrides(layout, TagBookmarkPreferences::findPreferred);
	}

	static void reapplyDisplayOverrides(
		RecipeLayout<?> layout,
		BiFunction<TagKey<?>, List<ItemStack>, Optional<ItemStack>> preferenceLookup
	) {
		Map<IRecipeSlotView, TagSlotData> tagSlots = LAYOUT_TAGS.get(layout);
		if (tagSlots == null) {
			return;
		}

		for (Map.Entry<IRecipeSlotView, TagSlotData> entry : tagSlots.entrySet()) {
			if (!(entry.getKey() instanceof IRecipeSlotDrawable slot)) {
				continue;
			}
			TagSlotData data = entry.getValue();
			applyPreferenceIfUnclaimed(slot, preferenceLookup.apply(data.tag(), data.members()));
		}
	}

	private static void applyPreferenceIfUnclaimed(
		IRecipeSlotDrawable slot,
		Optional<ItemStack> preferred
	) {
		if (preferred.isEmpty()) {
			return;
		}
		if (slot instanceof MixinRecipeSlotAccessor accessor &&
			accessor.justenoughtaglib$getDisplayOverrides() != null) {
			return;
		}
		slot.createDisplayOverrides().addItemStack(preferred.get().copy());
	}
}
