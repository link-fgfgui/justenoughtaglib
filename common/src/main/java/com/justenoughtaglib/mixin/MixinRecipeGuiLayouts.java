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
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Keeps recipe layout context on output-slot bookmark clicks and tag-input
 * navigation clicks, and jumps to the tag recipe page when a non-tag recipe's
 * input slot content exactly matches a captured tag's member list.
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
		if (role == RecipeIngredientRole.OUTPUT || (role == RecipeIngredientRole.INPUT && isTagRecipe(recipeLayout))) {
			element = new RecipeContextElement<>(recipeLayout, displayedIngredient, role);
		} else if (role == RecipeIngredientRole.INPUT) {
			// 输入槽内容恰好是构建期捕获的某个 tag 成员集 → 精确跳转该 tag 页面；
			// 未捕获（自定义分类、直喂列表等）回退原版物品页。
			List<ItemStack> slotStacks = slotUnderMouse.slot().getItemStacks().toList();
			element = TagSlotTracker.findTag(recipeLayout, slotStacks)
				.<IElement<?>>map(tag -> new TagRecipeJumpElement<>(displayedIngredient, tag))
				.orElseGet(() -> new IngredientElement<>(displayedIngredient));
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
