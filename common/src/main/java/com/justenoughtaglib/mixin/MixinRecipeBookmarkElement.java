package com.justenoughtaglib.mixin;

import com.justenoughtaglib.transfer.TransferLayoutPolicy;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferManager;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.JeiTooltip;
import mezz.jei.common.transfer.RecipeTransferUtil;
import mezz.jei.common.util.SafeIngredientUtil;
import mezz.jei.gui.bookmarks.RecipeBookmark;
import mezz.jei.gui.overlay.elements.RecipeBookmarkElement;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.gui.util.FocusUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Gives JEI recipe bookmarks a recipe marker and preserves JEI's native interactions.
 */
@Mixin(RecipeBookmarkElement.class)
public abstract class MixinRecipeBookmarkElement<R, I> {
	@Shadow(remap = false)
	@Final
	private RecipeBookmark<R, I> recipeBookmark;

	@Unique
	private boolean justenoughtaglib$isTagRecipe() {
		return recipeBookmark.getRecipeCategory().getRecipeType().getUid().getPath().startsWith("tag_recipes/");
	}

	@Redirect(
		method = "getRecipeLayoutDrawable",
		at = @At(
			value = "INVOKE",
			target = "Lmezz/jei/api/recipe/IFocusFactory;getEmptyFocusGroup()Lmezz/jei/api/recipe/IFocusGroup;"
		),
		remap = false
	)
	private IFocusGroup justenoughtaglib$focusTagRecipe(IFocusFactory focusFactory) {
		if (justenoughtaglib$isTagRecipe()) {
			return focusFactory.createFocusGroup(List.of(
				focusFactory.createFocus(RecipeIngredientRole.INPUT, recipeBookmark.getRecipeOutput())
			));
		}
		return focusFactory.getEmptyFocusGroup();
	}

	@Inject(method = "show", remap = false, at = @At("HEAD"), cancellable = true)
	private void justenoughtaglib$showFocusedTagRecipe(
		IRecipesGui recipesGui,
		FocusUtil focusUtil,
		List<RecipeIngredientRole> roles,
		CallbackInfo ci
	) {
		if (!justenoughtaglib$isTagRecipe()) {
			return;
		}

		if (roles.contains(RecipeIngredientRole.INPUT)) {
			// U / right-click for uses: equivalent to pressing U on the displayed item,
			// listing all recipes that use it as an input.
			recipesGui.show(focusUtil.createFocuses(recipeBookmark.getRecipeOutput(), roles));
			ci.cancel();
			return;
		}

		recipesGui.showRecipes(
			recipeBookmark.getRecipeCategory(),
			List.of(recipeBookmark.getRecipe()),
			focusUtil.createFocuses(
				recipeBookmark.getRecipeOutput(),
				List.of(RecipeIngredientRole.INPUT)
			)
		);
		ci.cancel();
	}

	// Keep JEI's original getTooltip flow so its Shift-gated preview and transfer
	// hints remain additive instead of replacing the whole tooltip.
	@ModifyArg(
		method = "getTooltip",
		at = @At(
			value = "INVOKE",
			target = "Lmezz/jei/common/gui/JeiTooltip;add(Lnet/minecraft/network/chat/FormattedText;)V",
			ordinal = 0
		),
		remap = false,
		index = 0
	)
	private FormattedText justenoughtaglib$replaceBookmarkTitle(FormattedText originalTitle) {
		return justenoughtaglib$isTagRecipe()
			? Component.literal(justenoughtaglib$getRecipeOutputDisplayName())
			: originalTitle;
	}

	@Redirect(
		method = "getTooltip",
		at = @At(
			value = "INVOKE",
			target = "Lmezz/jei/common/util/SafeIngredientUtil;getTooltip(Lmezz/jei/api/gui/builder/ITooltipBuilder;Lmezz/jei/api/runtime/IIngredientManager;Lmezz/jei/api/ingredients/IIngredientRenderer;Lmezz/jei/api/ingredients/ITypedIngredient;)V",
			ordinal = 0
		),
		remap = false
	)
	private <T> void justenoughtaglib$replaceIngredientTooltip(
		ITooltipBuilder tooltip,
		IIngredientManager ingredientManager,
		IIngredientRenderer<T> ingredientRenderer,
		ITypedIngredient<T> typedIngredient
	) {
		if (justenoughtaglib$isTagRecipe()) {
			tooltip.add(Component.translatable(
				"jei.tooltip.bookmarks.recipe",
				recipeBookmark.getRecipeCategory().getTitle()
			));
		} else {
			SafeIngredientUtil.getTooltip(tooltip, ingredientManager, ingredientRenderer, typedIngredient);
		}
	}

	@Unique
	private String justenoughtaglib$getRecipeOutputDisplayName() {
		ITypedIngredient<I> recipeOutput = recipeBookmark.getRecipeOutput();
		IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();
		IIngredientHelper<I> ingredientHelper = ingredientManager.getIngredientHelper(recipeOutput.getType());
		return ingredientHelper.getDisplayName(recipeOutput.getIngredient());
	}

	@Redirect(
		method = "handleClick",
		at = @At(
			value = "INVOKE",
			target = "Lmezz/jei/common/transfer/RecipeTransferUtil;getTransferRecipeError(Lmezz/jei/api/recipe/transfer/IRecipeTransferManager;Lnet/minecraft/world/inventory/AbstractContainerMenu;Lmezz/jei/api/gui/IRecipeLayoutDrawable;Lnet/minecraft/world/entity/player/Player;)Ljava/util/Optional;"
		),
		remap = false
	)
	private Optional<IRecipeTransferError> justenoughtaglib$getPreferredTransferError(
		IRecipeTransferManager recipeTransferManager,
		AbstractContainerMenu container,
		IRecipeLayoutDrawable<R> focusedLayout,
		Player player
	) {
		IRecipeLayoutDrawable<R> selectedLayout = justenoughtaglib$selectTransferLayout(
			focusedLayout,
			recipeTransferManager,
			container,
			player
		);
		return RecipeTransferUtil.getTransferRecipeError(
			recipeTransferManager,
			container,
			selectedLayout,
			player
		);
	}

	@Redirect(
		method = "handleClick",
		at = @At(
			value = "INVOKE",
			target = "Lmezz/jei/common/transfer/RecipeTransferUtil;transferRecipe(Lmezz/jei/api/recipe/transfer/IRecipeTransferManager;Lnet/minecraft/world/inventory/AbstractContainerMenu;Lmezz/jei/api/gui/IRecipeLayoutDrawable;Lnet/minecraft/world/entity/player/Player;Z)Z"
		),
		remap = false
	)
	private boolean justenoughtaglib$transferPreferredLayout(
		IRecipeTransferManager recipeTransferManager,
		AbstractContainerMenu container,
		IRecipeLayoutDrawable<R> focusedLayout,
		Player player,
		boolean transferMax
	) {
		IRecipeLayoutDrawable<R> selectedLayout = justenoughtaglib$selectTransferLayout(
			focusedLayout,
			recipeTransferManager,
			container,
			player
		);
		return RecipeTransferUtil.transferRecipe(
			recipeTransferManager,
			container,
			selectedLayout,
			player,
			transferMax
		);
	}

	@Unique
	private IRecipeLayoutDrawable<R> justenoughtaglib$selectTransferLayout(
		IRecipeLayoutDrawable<R> focusedLayout,
		IRecipeTransferManager recipeTransferManager,
		AbstractContainerMenu container,
		Player player
	) {
		if (!justenoughtaglib$isTagRecipe()) {
			return focusedLayout;
		}

		Predicate<IRecipeLayoutDrawable<R>> allowed = layout -> {
			IRecipeTransferError error = RecipeTransferUtil
				.getTransferRecipeError(recipeTransferManager, container, layout, player)
				.orElse(null);
			return error == null || error.getType().allowsTransfer;
		};

		return TransferLayoutPolicy.selectPreferredTransferLayout(
			focusedLayout,
			this::justenoughtaglib$createUnfocusedTagRecipeLayout,
			allowed
		).orElse(focusedLayout);
	}

	@Unique
	private Optional<IRecipeLayoutDrawable<R>> justenoughtaglib$createUnfocusedTagRecipeLayout() {
		IFocusFactory focusFactory = Internal.getJeiRuntime().getJeiHelpers().getFocusFactory();
		return Internal.getJeiRuntime().getRecipeManager().createRecipeLayoutDrawable(
			recipeBookmark.getRecipeCategory(),
			recipeBookmark.getRecipe(),
			focusFactory.getEmptyFocusGroup(),
			Internal.getTextures().getRecipePreviewBackground(),
			4
		);
	}
}
