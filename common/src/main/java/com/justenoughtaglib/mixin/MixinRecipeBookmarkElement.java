package com.justenoughtaglib.mixin;

import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferManager;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.JeiTooltip;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.common.transfer.RecipeTransferUtil;
import mezz.jei.gui.bookmarks.RecipeBookmark;
import mezz.jei.gui.input.UserInput;
import mezz.jei.gui.overlay.elements.RecipeBookmarkElement;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.gui.util.FocusUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Gives JEI recipe bookmarks a recipe marker and preserves JEI's tooltip feature flow.
 */
@Mixin(RecipeBookmarkElement.class)
public abstract class MixinRecipeBookmarkElement<R, I> {
	@Shadow(remap = false)
	@Final
	private RecipeBookmark<R, I> recipeBookmark;

	@Shadow(remap = false)
	private Optional<IRecipeLayoutDrawable<R>> getRecipeLayoutDrawable() {
		throw new AssertionError();
	}

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
				focusFactory.createFocus(RecipeIngredientRole.INPUT, recipeBookmark.getRecipeOutput()),
				focusFactory.createFocus(RecipeIngredientRole.OUTPUT, recipeBookmark.getRecipeOutput())
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
			// U / 右键查用途：等价于在显示物品上按 U，列出所有用该物品作为输入的配方
			recipesGui.show(focusUtil.createFocuses(recipeBookmark.getRecipeOutput(), roles));
			ci.cancel();
			return;
		}

		recipesGui.showRecipes(
			recipeBookmark.getRecipeCategory(),
			List.of(recipeBookmark.getRecipe()),
			focusUtil.createFocuses(
				recipeBookmark.getRecipeOutput(),
				List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.OUTPUT)
			)
		);
		ci.cancel();
	}

	@Inject(method = "createRenderOverlay", remap = false, at = @At("HEAD"), cancellable = true)
	private void justenoughtaglib$useRecipeFavoriteOverlay(CallbackInfoReturnable<IDrawable> cir) {
		cir.setReturnValue(new RecipeFavoriteOverlay());
	}

	// Keep JEI's original getTooltip flow so its Shift-gated preview and transfer
	// hints remain additive instead of replacing the whole tooltip.
	@Redirect(
		method = "getTooltip",
		at = @At(
			value = "INVOKE",
			target = "Lmezz/jei/common/gui/JeiTooltip;add(Lnet/minecraft/network/chat/FormattedText;)V",
			ordinal = 0
		),
		remap = false
	)
	private void justenoughtaglib$replaceBookmarkTitle(
		JeiTooltip tooltip,
		FormattedText originalTitle
	) {
		tooltip.add(Component.literal(justenoughtaglib$getRecipeOutputDisplayName(

				)
		));
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
		tooltip.add(Component.translatable(
				"jei.tooltip.bookmarks.recipe",recipeBookmark.getRecipeCategory().getTitle()));
	}

	@Unique
	private String justenoughtaglib$getRecipeOutputDisplayName() {
		ITypedIngredient<I> recipeOutput = recipeBookmark.getRecipeOutput();
		IIngredientManager ingredientManager = Internal.getJeiRuntime().getIngredientManager();
		IIngredientHelper<I> ingredientHelper = ingredientManager.getIngredientHelper(recipeOutput.getType());
		return ingredientHelper.getDisplayName(recipeOutput.getIngredient());
	}

	@Inject(method = "handleClick", remap = false, at = @At("HEAD"), cancellable = true)
	private void justenoughtaglib$handleLeftClick(
		UserInput input,
		IInternalKeyMappings keyBindings,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (input.is(keyBindings.getLeftClick()) && justenoughtaglib$tryTransferRecipe(input)) {
			cir.setReturnValue(true);
		}
	}

	@Unique
	private boolean justenoughtaglib$tryTransferRecipe(UserInput input) {
		Minecraft minecraft = Minecraft.getInstance();
		Screen screen = minecraft.screen;
		Player player = minecraft.player;
		if (player == null || !(screen instanceof AbstractContainerScreen<?> containerScreen)) {
			return false;
		}

		IRecipeLayoutDrawable<R> focusedLayout = getRecipeLayoutDrawable().orElse(null);
		if (focusedLayout == null) {
			return false;
		}

		IRecipeTransferManager recipeTransferManager = Internal.getJeiRuntime().getRecipeTransferManager();
		AbstractContainerMenu container = containerScreen.getMenu();
		boolean simulate = input.isSimulate();
		Optional<IRecipeLayoutDrawable<R>> selectedLayout = justenoughtaglib$selectTransferLayout(
			focusedLayout,
			recipeTransferManager,
			container,
			player,
			simulate
		);

		if (simulate) {
			return selectedLayout.isPresent();
		}
		return selectedLayout
			.map(layout -> RecipeTransferUtil.transferRecipe(
				recipeTransferManager,
				container,
				layout,
				player,
				Screen.hasShiftDown()
			))
			.orElse(false);
	}

	@Unique
	private Optional<IRecipeLayoutDrawable<R>> justenoughtaglib$selectTransferLayout(
		IRecipeLayoutDrawable<R> focusedLayout,
		IRecipeTransferManager recipeTransferManager,
		AbstractContainerMenu container,
		Player player,
		boolean simulate
	) {
		Predicate<IRecipeLayoutDrawable<R>> allowed = layout -> {
			IRecipeTransferError error = RecipeTransferUtil
				.getTransferRecipeError(recipeTransferManager, container, layout, player)
				.orElse(null);
			return error == null || error.getType().allowsTransfer;
		};

		if (!justenoughtaglib$isTagRecipe()) {
			return !simulate || allowed.test(focusedLayout)
				? Optional.of(focusedLayout)
				: Optional.empty();
		}

		return TransferLayoutPolicy.justenoughtaglib$selectPreferredTransferLayout(
			focusedLayout,
			this::justenoughtaglib$createUnfocusedTagRecipeLayout,
			allowed
		);
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


	private static final class RecipeFavoriteOverlay implements IDrawable {
		private final IDrawable icon = Internal.getTextures().getRecipeBookmark();

		@Override
		public int getWidth() {
			return 16;
		}

		@Override
		public int getHeight() {
			return 16;
		}

		@Override
		public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
			var poseStack = guiGraphics.pose();
			poseStack.pushPose();
			poseStack.translate(xOffset + 11, yOffset + 11, 200);
			poseStack.scale(0.5F, 0.5F, 0.5F);
			icon.draw(guiGraphics);
			poseStack.popPose();
		}
	}
}
