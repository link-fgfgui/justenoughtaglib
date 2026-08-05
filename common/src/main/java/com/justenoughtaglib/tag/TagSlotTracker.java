package com.justenoughtaglib.tag;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.library.gui.recipes.RecipeLayout;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Captures tag origins of slots during recipe-layout construction and maps
 * both full members and bookmark-narrowed members to the same tag metadata
 * for tooltip, click-navigation, and interaction queries.
 *
 * <p>Tag identity is lost when {@link Ingredient#getItems()} expands an
 * ingredient (only the item list remains after expansion). All JEI expansion
 * paths ultimately call the same {@code getItems()} method (crafting extensions,
 * {@code IIngredientConsumer}/{@code IIngredientAcceptor} default methods,
 * forging extensions, and so on). Therefore, the loader modules' (fabric/forge)
 * {@code MixinIngredient} captures the mapping between the expanded result and
 * the tag after getItems returns and during the layout-build window (see
 * {@link #beginBuild()} / {@link #associateLayout(RecipeLayout)}). Calls to
 * getItems outside the build window (recipe registration, anvil ingredients,
 * brewing-container enumeration, and so on) are excluded by the
 * {@link #isBuilding()} guard.
 *
 * <p>In 1.20.1, {@link Ingredient} has no public tag accessor ({@code getTag()}
 * was added in a later version), so the tag is restored from
 * {@link Ingredient#toJson()}: an Ingredient containing a single TagValue is
 * serialized as {@code {"tag": "ns:path"}} (matching the data-pack format),
 * which is equivalent to the getTag() check. Mixed values and explicit item
 * lists do not have a tag key and are naturally excluded.
 *
 * <p>Capture results are grouped by the "RecipeLayoutBuilder currently being
 * built" (ThreadLocal) and associated with the final RecipeLayout instance
 * when {@code RecipeLayoutBuilder.buildRecipeLayout} returns (WeakHashMap).
 * Clicks query only the mapping for the layout under the mouse, so stale entries
 * cannot leak across pages or layouts. This is the fundamental reason it is
 * more precise than "global reverse scanning".
 *
 * <p>The key is the list of actual {@link Item} instances in a slot (an
 * order-sensitive exact match). Without a bookmark binding, the full tag
 * membership is recorded; with a bookmark binding, a single-item key is also
 * recorded while the value continues to retain the full member list.
 */
public final class TagSlotTracker {
	private TagSlotTracker() {
	}

	/** Capture table for the layout currently being built; accessed only from the client thread, with ThreadLocal as a fallback for other threads. */
	private static final ThreadLocal<Map<List<Item>, TagSlotData>> CURRENT_BUILD = new ThreadLocal<>();

	/** Layout instance -> the capture table from that layout's build phase. Entries disappear automatically when the layout is collected. */
	private static final Map<RecipeLayout, Map<List<Item>, TagSlotData>> LAYOUT_TAGS =
		Collections.synchronizedMap(new WeakHashMap<>());

	public record TagSlotData(TagKey<?> tag, List<ItemStack> members, boolean preferred) {
		public TagSlotData {
			members = members.stream().map(ItemStack::copy).toList();
		}
	}

	/**
	 * Called by {@code RecipeLayoutBuilder.<init>}: starts capturing a new layout build.
	 */
	public static void beginBuild() {
		CURRENT_BUILD.set(new HashMap<>());
	}

	/** Whether the layout-build window is active (guard for the getItems funnel: capture only during construction). */
	public static boolean isBuilding() {
		return CURRENT_BUILD.get() != null;
	}

	/**
	 * Called immediately after a tag is expanded: if the Ingredient is a single
	 * tag value, records the "member list -> tag" mapping.
	 * Non-tag Ingredients (explicit item lists and mixed values) are ignored so
	 * the mapping contains only actual tags.
	 *
	 * @param ingredient     Expanded Ingredient; tag identity is restored from this value (toJson)
	 * @param expandedStacks Expansion result (the getItems return value and the same array displayed in the slot)
	 */
	public static void capture(Ingredient ingredient, ItemStack[] expandedStacks) {
		if (ingredient == null) {
			return;
		}
		Map<List<Item>, TagSlotData> build = CURRENT_BUILD.get();
		if (build == null) {
			return;
		}
		getTag(ingredient).ifPresent(tag -> capture(
			build,
			expandedStacks,
			new TagSlotData(tag, List.of(expandedStacks), false),
			false
		));
	}

	/** Captures the original tag membership, then applies any loaded tag bookmark preference. */
	public static ItemStack[] captureAndApplyPreference(Ingredient ingredient, ItemStack[] expandedStacks) {
		Optional<TagKey<Item>> tag = getTag(ingredient);
		if (tag.isEmpty()) {
			return expandedStacks;
		}

		Map<List<Item>, TagSlotData> build = CURRENT_BUILD.get();
		if (build == null) {
			return expandedStacks;
		}

		ItemStack[] selectedStacks = TagBookmarkPreferences.apply(tag.get(), expandedStacks);
		captureSelection(build, tag.get(), expandedStacks, selectedStacks);
		return selectedStacks;
	}

	static void captureSelection(
		TagKey<?> tag,
		ItemStack[] expandedStacks,
		ItemStack[] selectedStacks
	) {
		Map<List<Item>, TagSlotData> build = CURRENT_BUILD.get();
		if (build != null) {
			captureSelection(build, tag, expandedStacks, selectedStacks);
		}
	}

	private static void captureSelection(
		Map<List<Item>, TagSlotData> build,
		TagKey<?> tag,
		ItemStack[] expandedStacks,
		ItemStack[] selectedStacks
	) {
		TagSlotData original = new TagSlotData(tag, List.of(expandedStacks), false);
		capture(build, expandedStacks, original, false);
		if (selectedStacks != expandedStacks) {
			TagSlotData preferred = new TagSlotData(tag, List.of(expandedStacks), true);
			capture(build, selectedStacks, preferred, true);
		}
	}

	private static void capture(
		Map<List<Item>, TagSlotData> build,
		ItemStack[] stacks,
		TagSlotData data,
		boolean replace
	) {
		List<Item> items = Arrays.stream(stacks)
			.map(ItemStack::getItem)
			.toList();
		if (replace) {
			build.put(items, data);
		} else {
			build.putIfAbsent(items, data);
		}
	}

	/**
	 * 1.20.1 has no {@code Ingredient.getTag()}; restores the tag from the
	 * serialized form. An Ingredient containing a single TagValue is serialized
	 * as {@code {"tag": "ns:path"}}.
	 */
	private static Optional<TagKey<Item>> getTag(Ingredient ingredient) {
		JsonElement json;
		try {
			json = ingredient.toJson();
		} catch (RuntimeException e) {
			// Serialization of a mod-defined Ingredient may throw; skip it.
			return Optional.empty();
		}
		if (!json.isJsonObject()) {
			return Optional.empty();
		}
		JsonObject object = json.getAsJsonObject();
		JsonElement tag = object.get("tag");
		if (tag == null || !tag.isJsonPrimitive() || !tag.getAsJsonPrimitive().isString()) {
			return Optional.empty();
		}
		ResourceLocation location = ResourceLocation.tryParse(tag.getAsString());
		if (location == null) {
			return Optional.empty();
		}
		return Optional.of(TagKey.create(Registries.ITEM, location));
	}

	/** Called when {@code RecipeLayoutBuilder.buildRecipeLayout} returns: attaches this build's captures to the layout. */
	public static void associateLayout(RecipeLayout layout) {
		Map<List<Item>, TagSlotData> build = CURRENT_BUILD.get();
		CURRENT_BUILD.remove();
		if (layout == null || build == null || build.isEmpty()) {
			return;
		}
		LAYOUT_TAGS.put(layout, build);
	}

	/**
	 * Looks up a slot built from a tag Ingredient in a layout, including a
	 * single-item slot narrowed by a bookmark.
	 *
	 * @return the tag corresponding to the slot contents; empty when the slot was
	 * not built from a tag Ingredient.
	 */
	public static Optional<TagKey<?>> findTag(IRecipeLayoutDrawable<?> layout, List<ItemStack> itemStacks) {
		return findTagData(layout, itemStacks).map(TagSlotData::tag);
	}

	public static Optional<TagSlotData> findTagData(
		IRecipeLayoutDrawable<?> layout,
		List<ItemStack> itemStacks
	) {
		if (!(layout instanceof RecipeLayout recipeLayout)) {
			return Optional.empty();
		}
		Map<List<Item>, TagSlotData> build = LAYOUT_TAGS.get(recipeLayout);
		if (build == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(build.get(itemKey(itemStacks)));
	}

	public static Optional<TagSlotData> findCurrentTagData(List<ItemStack> itemStacks) {
		Map<List<Item>, TagSlotData> build = CURRENT_BUILD.get();
		if (build == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(build.get(itemKey(itemStacks)));
	}

	private static List<Item> itemKey(List<ItemStack> itemStacks) {
		return itemStacks.stream().map(ItemStack::getItem).toList();
	}
}
