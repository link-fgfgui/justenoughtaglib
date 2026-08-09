package com.justenoughtaglib.tag;

import com.justenoughtaglib.mixin.MixinRecipeSlotAccessor;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.library.gui.recipes.RecipeLayout;
import mezz.jei.library.plugins.jei.tags.ITagInfoRecipe;
import net.minecraft.tags.TagKey;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Tracks which input slots of an open recipe layout are bound to a tag and keeps the
 * member displayed there aligned with the player's bookmark for that tag.
 *
 * <p>Only <em>ordinary</em> recipe layouts are tracked. Tag-recipe pages are built from
 * a single input slot holding the whole tag plus a scrollable grid of member outputs;
 * their interaction is entirely the bookmark mixins' job and they never enter the
 * slot-tag map, so the all-member input slot keeps JEI's native full cycling instead of
 * being narrowed to one bookmarked member.
 *
 * <p>Two concerns are deliberately kept apart:
 *
 * <p><b>1. Click/lookup snapshot ({@link TagSlotData}).</b> Captured once per layout in
 * {@link #prepareLayout(RecipeLayout, IIngredientManager)}, {@code preferred()} records
 * whether a bookmark binding existed <em>at build time</em>. Slot click handling
 * (see {@code MixinRecipeGuiLayouts}) reads only that snapshot. A bookmark added or
 * removed while a layout is on screen therefore never rewires that layout's clicks —
 * the change takes effect when a layout is next built. That is intended: interactions
 * are a per-layout decision, and JEI rebuilds layouts whenever it re-renders the page.
 *
 * <p><b>2. Live display pin.</b> JEI's {@code RecipeLayout.tick()} clears every slot's
 * display overrides at the start of each ingredient cycle, then lets the recipe
 * category re-assert its own overrides. The mixin on {@code RecipeLayout#tick} runs
 * right after that category hook and re-pins the displayed member here, from a fresh
 * {@link TagBookmarkPreferences} lookup. So a binding added mid-view is shown from the
 * very next cycle, and a binding removed mid-view is no longer re-pinned — JEI
 * clearing the overrides on that cycle makes the slot revert to its normal member
 * cycling by itself. Nothing is ever "unpinned" manually: not re-pinning is what lets
 * the next cycle clean up.
 *
 * <p>Which of the two concerns a flow belongs to comes from one place:
 * {@link #decideTagBehavior}, the single decision point for a tag-resolved slot.
 *
 * <p>Type-generic. Members are read through {@link IRecipeSlotView#getIngredients} and
 * ingredient identity is delegated to JEI's {@code IIngredientHelper} machinery, so any
 * JEI ingredient type (item stacks, fluid stacks, modded types) is handled alike.
 */
public final class TagSlotTracker {
	private static volatile boolean runtimeAvailable;
	private static volatile IIngredientManager ingredientManager;
	/** Layout → ({@link TagSlotData} per tag-bound input slot). Weak keys so completed layouts are reaped. */
	private static final Map<RecipeLayout<?>, Map<IRecipeSlotView, TagSlotData>> LAYOUT_TAGS =
		Collections.synchronizedMap(new WeakHashMap<>());

	private TagSlotTracker() {
	}

	public static void setRuntimeAvailable(boolean available) {
		runtimeAvailable = available;
	}

	public static boolean isRuntimeAvailable() {
		return runtimeAvailable;
	}

	public static void setIngredientManager(IIngredientManager manager) {
		ingredientManager = manager;
	}

	/**
	 * Discards all snapshots. Called by the plugin when the JEI runtime stops: recipe
	 * layouts can outlive a world screen by one client tick, and clearing here makes
	 * those leftover layouts a no-op instead of consulting the stopped runtime.
	 */
	public static void clear() {
		LAYOUT_TAGS.clear();
	}

	/**
	 * Immutable per-layout capture of one tag-bound input slot.
	 *
	 * @param tag            the tag the slot is guaranteed equivalent to
	 * @param ingredientType the ingredient flavor the members belong to
	 * @param members        snapshot of the slot's members at layout-build time. Copied
	 *                       because JEI recycles its slot ingredient lists, and later pins
	 *                       must not alias a list JEI may mutate.
	 * @param preferred      whether the bookmark preference for {@code tag} held an
	 *                       ingredient when the layout was built. A snapshot for click
	 *                       decisions; not updated while the layout is open (see class javadoc).
	 */
	public record TagSlotData(TagKey<?> tag, IIngredientType<?> ingredientType, List<?> members, boolean preferred) {
		public TagSlotData {
			members = List.copyOf(members);
		}
	}

	/** What a tag-resolved input slot of an ordinary recipe should do, decided in one place. */
	enum SlotBehavior {
		/** Show the bookmarked member as a display-only override. */
		NARROW,
		/** Let the slot's click jump to its tag's listing page. */
		JUMP,
		/** Leave the slot to JEI's native behavior. */
		INERT
	}

	/**
	 * The single decision point for a tag-resolved input slot: whether to narrow its
	 * display to the bookmarked member, let it click into the tag listing, or do nothing.
	 *
	 * <p>{@code ownedByCategory} tells whether the recipe category already set its own
	 * display override for the slot this cycle; only narrows that do not preempt JEI's
	 * display are honored, while clicks (a pure interaction) are unaffected by it. The
	 * preference input is the caller's to choose: the layout-build snapshot
	 * ({@code data.preferred()}) for click decisions, the live
	 * {@link TagBookmarkPreferences} lookup for the display pin.
	 */
	static SlotBehavior decideTagBehavior(TagSlotData data, boolean ownedByCategory, boolean hasPreference) {
		if (data == null) {
			return SlotBehavior.INERT;
		}
		if (hasPreference && !ownedByCategory) {
			return SlotBehavior.NARROW;
		}
		if (!hasPreference && isUsefulJump(data)) {
			return SlotBehavior.JUMP;
		}
		return SlotBehavior.INERT;
	}

	/** A jump into the tag listing is only worth it when the tag spans more than one member. */
	private static boolean isUsefulJump(TagSlotData data) {
		return data.members().size() > 1;
	}

	private static boolean isSlotOwnedByCategory(IRecipeSlotView slot) {
		return slot instanceof MixinRecipeSlotAccessor accessor &&
			accessor.justenoughtaglib$getDisplayOverrides() != null;
	}

	/**
	 * First pass, once per freshly built layout (hooked after
	 * {@code RecipeLayoutBuilder#buildRecipeLayout}): resolve which INPUT slots are
	 * tag-equivalent, snapshot them into {@link TagSlotData}, and apply the initial
	 * display pin so a bookmarked tag shows its preferred member immediately, before
	 * the first JEI ingredient cycle. Tag-recipe pages are skipped entirely (class javadoc).
	 */
	public static void prepareLayout(RecipeLayout<?> layout, IIngredientManager manager) {
		prepareLayout(layout, manager, TagBookmarkPreferences::findPreferred);
	}

	/**
	 * Body of {@link #prepareLayout(RecipeLayout, IIngredientManager)} with an injectable
	 * preference lookup, so tests can drive it without a live bookmark list.
	 */
	static void prepareLayout(
		RecipeLayout<?> layout,
		IIngredientManager manager,
		TagPreferenceLookup preferenceLookup
	) {
		// Tag-recipe pages are one whole-tag input slot plus a scrollable grid of member
		// outputs; their clicks are served purely by the bookmark mixins, so they are not
		// part of this normalization and never land in LAYOUT_TAGS (direction A).
		if (layout.getRecipe() instanceof ITagInfoRecipe) {
			return;
		}
		Map<IRecipeSlotView, TagSlotData> tagSlots = new IdentityHashMap<>();

		for (IRecipeSlotDrawable slot : layout.getRecipeSlots().getSlots(RecipeIngredientRole.INPUT)) {
			prepareOrdinarySlot(tagSlots, slot, manager, preferenceLookup);
		}

		if (!tagSlots.isEmpty()) {
			LAYOUT_TAGS.put(layout, Collections.unmodifiableMap(tagSlots));
		}
	}

	private static void prepareOrdinarySlot(
		Map<IRecipeSlotView, TagSlotData> tagSlots,
		IRecipeSlotDrawable slot,
		IIngredientManager manager,
		TagPreferenceLookup preferenceLookup
	) {
		for (IIngredientType<?> type : manager.getRegisteredIngredientTypes()) {
			List<?> members = slot.getIngredients(asType(type)).toList();
			if (members.isEmpty()) {
				continue;
			}
			Optional<TagKey<?>> tag = TagIngredients.getTagKeyEquivalent(manager, type, members);
			if (tag.isEmpty()) {
				continue;
			}
			Optional<Object> preferred = preferenceLookup.apply(manager, tag.get(), type, members);
			TagSlotData data = new TagSlotData(tag.get(), type, copiedMembers(manager, type, members), preferred.isPresent());
			tagSlots.put(slot, data);
			if (decideTagBehavior(data, isSlotOwnedByCategory(slot), preferred.isPresent()) == SlotBehavior.NARROW) {
				applyPin(slot, data.ingredientType(), preferred);
			}
			break;
		}
	}

	private static List<?> copiedMembers(IIngredientManager manager, IIngredientType<?> type, List<?> members) {
		return members.stream()
			.map(member -> TagIngredients.copy(manager, type, member))
			.toList();
	}

	/**
	 * Queries the click/lookup snapshot (class javadoc, concern 1). Used by
	 * {@code MixinRecipeGuiLayouts} to decide whether an unbound tag input should jump
	 * to its tag listing.
	 */
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

	/**
	 * The tag-resolved slot whose click should open the tag listing: an unbound tag with
	 * more than one member (snapshot decision — one place, {@link #decideTagBehavior}).
	 */
	public static Optional<TagSlotData> findJumpSlotData(
		IRecipeLayoutDrawable<?> layout,
		IRecipeSlotView slot
	) {
		return findTagData(layout, slot)
			.filter(data -> decideTagBehavior(data, isSlotOwnedByCategory(slot), data.preferred()) == SlotBehavior.JUMP);
	}

	/**
	 * Re-pins the displayed member for each recorded tag slot. Called from the
	 * {@code RecipeLayout#tick} mixin <em>after</em> JEI's {@code onDisplayedIngredientsUpdate}
	 * hook, at which point JEI has already cleared the previous cycle's overrides and the
	 * category has re-asserted its own.
	 *
	 * <p>Each cycle re-derives the pin from the live preference map ({@code TagBookmarkPreferences}),
	 * which is rebuilt on bookmark add/remove/reorder. So the <em>value</em> follows the
	 * bookmarks — a change made while the view is open is visible from the next cycle —
	 * but the lookup is stable while the bookmarks are. Re-running the lookup every cycle
	 * is not about getting a new value; it is what re-asserts the (possibly identical)
	 * pin after JEI wiped the previous one. The <em>matching basis</em> it runs against,
	 * {@link TagSlotData#members()}, is the layout-build snapshot and never changes.
	 * Slots left unpinned simply revert to JEI's normal cycling. See class javadoc, concern 2.
	 */
	public static void reapplyDisplayOverrides(RecipeLayout<?> layout) {
		reapplyDisplayOverrides(layout, TagBookmarkPreferences::findPreferred);
	}

	/**
	 * Body of {@link #reapplyDisplayOverrides(RecipeLayout)} with an injectable
	 * preference lookup, for the same test seam as {@link #prepareLayout}.
	 */
	static void reapplyDisplayOverrides(
		RecipeLayout<?> layout,
		TagPreferenceLookup preferenceLookup
	) {
		IIngredientManager manager = ingredientManager;
		if (manager == null) {
			return;
		}
		Map<IRecipeSlotView, TagSlotData> tagSlots = LAYOUT_TAGS.get(layout);
		if (tagSlots == null) {
			return;
		}
		for (Map.Entry<IRecipeSlotView, TagSlotData> entry : tagSlots.entrySet()) {
			if (!(entry.getKey() instanceof IRecipeSlotDrawable slot)) {
				continue;
			}
			TagSlotData data = entry.getValue();
			Optional<Object> preferred = preferenceLookup.apply(manager, data.tag(), data.ingredientType(), data.members());
			if (decideTagBehavior(data, isSlotOwnedByCategory(slot), preferred.isPresent()) == SlotBehavior.NARROW) {
				applyPin(slot, data.ingredientType(), preferred);
			}
		}
	}

	/**
	 * Pins {@code preferred} as the slot's displayed ingredient. The pin is additive and
	 * display-only: the slot's real ingredients are untouched. Whether the pin is due is
	 * decided upstream by {@link #decideTagBehavior}; this only applies the decision.
	 */
	private static void applyPin(
		IRecipeSlotDrawable slot,
		IIngredientType<?> type,
		Optional<Object> preferred
	) {
		if (preferred.isEmpty()) {
			return;
		}
		@SuppressWarnings({"rawtypes", "unchecked"})
		IIngredientType<Object> rawType = (IIngredientType<Object>) (IIngredientType) type;
		slot.createDisplayOverrides().addIngredient(rawType, preferred.get());
	}

	@SuppressWarnings("unchecked")
	private static <I> IIngredientType<I> asType(IIngredientType<?> type) {
		return (IIngredientType<I>) type;
	}

	@FunctionalInterface
	public interface TagPreferenceLookup {
		Optional<Object> apply(IIngredientManager ingredientManager, TagKey<?> tag, IIngredientType<?> type, List<?> members);
	}
}