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
import mezz.jei.gui.overlay.elements.IngredientElement;
import mezz.jei.gui.recipes.RecipeGuiLayouts;
import mezz.jei.gui.recipes.RecipeLayoutWithButtons;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Stream;

/**
 * Keeps recipe context on output-slot bookmark clicks. Unbound tag inputs jump
 * to their tag recipe; bookmark-bound inputs use normal item R/U behavior.
 */
@Mixin(RecipeGuiLayouts.class)
public abstract class MixinRecipeGuiLayouts {
	@Shadow(remap = false)
	private List<RecipeLayoutWithButtons<?>> recipeLayoutsWithButtons;

	@Inject(method = "getIngredientUnderMouse", remap = false, at = @At("HEAD"), cancellable = true)
	private void justenoughtaglib$addRecipeContext(
		double mouseX,
		double mouseY,
		CallbackInfoReturnable<Stream<IClickableIngredientInternal<?>>> cir
	) {
		Stream<IClickableIngredientInternal<?>> clickableIngredients = recipeLayoutsWithButtons.stream()
			.map(RecipeLayoutWithButtons::recipeLayout)
			.flatMap(recipeLayout -> getClickedIngredients(recipeLayout, mouseX, mouseY));
		cir.setReturnValue(clickableIngredients);
	}

	private static Stream<IClickableIngredientInternal<?>> getClickedIngredients(
		IRecipeLayoutDrawable<?> recipeLayout,
		double mouseX,
		double mouseY
	) {
		return recipeLayout.getSlotUnderMouse(mouseX, mouseY)
			.stream()
			.flatMap(slotUnderMouse -> slotUnderMouse.slot().getDisplayedIngredient()
				.map(displayedIngredient -> createClickableIngredient(recipeLayout, slotUnderMouse, displayedIngredient))
			.stream()
			);
	}

	private static IClickableIngredientInternal<?> createClickableIngredient(
		IRecipeLayoutDrawable<?> recipeLayout,
		RecipeSlotUnderMouse slotUnderMouse,
		ITypedIngredient<?> displayedIngredient
	) {
		RecipeIngredientRole role = slotUnderMouse.slot().getRole();
		IElement<?> element;
		if (role == RecipeIngredientRole.OUTPUT) {
			element = new RecipeContextElement<>(recipeLayout, displayedIngredient, role);
		} else if (role == RecipeIngredientRole.INPUT) {
			if (isTagRecipe(recipeLayout)) {
				element = new IngredientElement<>(displayedIngredient);
			} else {
				List<ItemStack> slotStacks = slotUnderMouse.slot().getItemStacks().toList();
				element = TagSlotTracker.findTagData(recipeLayout, slotStacks)
					.filter(data -> !data.preferred())
					.<IElement<?>>map(data -> new TagRecipeJumpElement<>(displayedIngredient, data.tag()))
					.orElseGet(() -> new IngredientElement<>(displayedIngredient));
			}
		} else {
			element = new IngredientElement<>(displayedIngredient);
		}
		return new ClickableIngredientInternal<>(element, slotUnderMouse::isMouseOver, false, true);
	}

	@Unique
	private static boolean isTagRecipe(IRecipeLayoutDrawable<?> recipeLayout) {
		return recipeLayout.getRecipeCategory().getRecipeType().getUid().getPath().startsWith("tag_recipes/");
	}
}
