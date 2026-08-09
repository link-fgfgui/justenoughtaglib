package com.justenoughtaglib.mixin;

import com.justenoughtaglib.bookmark.RecipeContextElement;
import com.justenoughtaglib.tag.TagRecipeJumpElement;
import com.justenoughtaglib.tag.TagSlotTracker;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;
import mezz.jei.gui.input.ClickableIngredientInternal;
import mezz.jei.gui.input.IClickableIngredientInternal;
import mezz.jei.gui.overlay.elements.IElement;
import mezz.jei.gui.recipes.RecipeGuiLayouts;
import mezz.jei.gui.recipes.RecipeLayoutWithButtons;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Keeps recipe context on output-slot bookmark clicks. Unbound tag inputs jump
 * to their tag recipe; bookmark-bound inputs use normal item R/U behavior.
 */
@Mixin(RecipeGuiLayouts.class)
public abstract class MixinRecipeGuiLayouts {
	@Shadow(remap = false)
	private List<RecipeLayoutWithButtons<?>> recipeLayoutsWithButtons;

	@Inject(method = "getIngredientUnderMouse", remap = false, at = @At("RETURN"), cancellable = true)
	private void justenoughtaglib$addRecipeContext(
		double mouseX,
		double mouseY,
		CallbackInfoReturnable<Stream<IClickableIngredientInternal<?>>> cir
	) {
		if (!TagSlotTracker.isRuntimeAvailable()) {
			return;
		}
		List<IClickableIngredientInternal<?>> customIngredients = recipeLayoutsWithButtons.stream()
			.map(RecipeLayoutWithButtons::recipeLayout)
			.flatMap(recipeLayout -> getCustomClickedIngredients(recipeLayout, mouseX, mouseY))
			.toList();

		// Nothing for us to change: keep JEI's original stream untouched.
		if (customIngredients.isEmpty()) {
			return;
		}

		// Only replace the slots we actually overrode, keeping every other
		// original clickable (JEI's own candidates for other slots, plus any
		// added by other mods) intact. The key is identity of the slot's
		// displayed ITypedIngredient: the same slot returns the same cached
		// instance through both the original path and ours, while distinct
		// slots (even for an identical item) produce distinct instances.
		// TypedIngredient has no equals(), so we make the reference guarantee
		// explicit with an IdentityHashMap-backed set.
		Set<Object> capturedSlots = Collections.newSetFromMap(new IdentityHashMap<>());
		for (IClickableIngredientInternal<?> custom : customIngredients) {
			capturedSlots.add(custom.getTypedIngredient());
		}

		Stream<IClickableIngredientInternal<?>> remainingOriginal = cir.getReturnValue()
			.filter(ingredient -> !capturedSlots.contains(ingredient.getTypedIngredient()));

		cir.setReturnValue(Stream.concat(remainingOriginal, customIngredients.stream()));
	}

	private static Stream<IClickableIngredientInternal<?>> getCustomClickedIngredients(
		IRecipeLayoutDrawable<?> recipeLayout,
		double mouseX,
		double mouseY
	) {
		return recipeLayout.getSlotUnderMouse(mouseX, mouseY)
			.stream()
			.flatMap(slotUnderMouse -> slotUnderMouse.slot().getDisplayedIngredient()
				.flatMap(displayedIngredient -> createCustomClickableIngredient(recipeLayout, slotUnderMouse, displayedIngredient))
			.stream()
			);
	}

	private static Optional<IClickableIngredientInternal<?>> createCustomClickableIngredient(
		IRecipeLayoutDrawable<?> recipeLayout,
		RecipeSlotUnderMouse slotUnderMouse,
		ITypedIngredient<?> displayedIngredient
	) {
		RecipeIngredientRole role = slotUnderMouse.slot().getRole();
		Optional<IElement<?>> element;
		if (role == RecipeIngredientRole.OUTPUT && isTagRecipe(recipeLayout)) {
			element = Optional.of(new RecipeContextElement<>(recipeLayout, displayedIngredient, role));
		} else if (role == RecipeIngredientRole.INPUT && !isTagRecipe(recipeLayout)) {
			// Click decisions use the layout-build snapshot (TagSlotTracker.preferred()):
			// a tag the player already bookmarks when the layout was built routes through
			// normal item R/U; an unbound tag input with several members jumps to the tag
			// listing. A bookmark change while this layout is open only affects the next
			// layout (and the live display pin), not these clicks.
			element = TagSlotTracker.findJumpSlotData(recipeLayout, slotUnderMouse.slot())
				.<IElement<?>>map(data -> new TagRecipeJumpElement<>(displayedIngredient, data.tag()));
		} else {
			element = Optional.empty();
		}
		return element.<IClickableIngredientInternal<?>>map(
			value -> new ClickableIngredientInternal<>(value, slotUnderMouse::isMouseOver, false, true)
		);
	}

	@Unique
	private static boolean isTagRecipe(IRecipeLayoutDrawable<?> recipeLayout) {
		return recipeLayout.getRecipeCategory().getRecipeType().getUid().getPath().startsWith("tag_recipes/");
	}
}
