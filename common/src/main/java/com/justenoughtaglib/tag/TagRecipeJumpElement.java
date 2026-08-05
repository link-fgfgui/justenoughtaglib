package com.justenoughtaglib.tag;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.common.Internal;
import mezz.jei.common.gui.JeiTooltip;
import mezz.jei.gui.bookmarks.IBookmark;
import mezz.jei.gui.overlay.IngredientGridTooltipHelper;
import mezz.jei.gui.overlay.elements.IElement;
import mezz.jei.gui.util.FocusUtil;
import mezz.jei.library.plugins.jei.tags.ITagInfoRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.util.List;
import java.util.Optional;

/**
 * 点击"内容恰好是某个 tag 成员集"的配方输入槽时，跳转到该 tag 的 recipe 页面。
 *
 * <p>仅当 {@link TagSlotTracker} 在布局构建期捕获到该槽的 tag 时才创建（精确匹配），
 * 跳转目标复用 JEI 内置的 tag 分类（{@code minecraft:tag_recipes/item}）与其中的
 * {@link ITagInfoRecipe} 实例——与 tag 书签点击同一条展示路径（
 * {@code showRecipes(分类, 配方, OUTPUT focus)}），因此页面内容、书签、焦点行为一致。
 *
 * <p>解析失败（分类未注册、该 tag 没有 recipe 实例等异常场景）时回退为原版行为：
 * 打开点击物品的 recipe 页面，绝不吞掉点击。
 */
public final class TagRecipeJumpElement<T> implements IElement<T> {
	private static final ResourceLocation TAG_RECIPE_TYPE_UID = new ResourceLocation("minecraft", "tag_recipes/item");

	private final ITypedIngredient<T> ingredient;
	private final TagKey<?> tag;

	public TagRecipeJumpElement(ITypedIngredient<T> ingredient, TagKey<?> tag) {
		this.ingredient = ingredient;
		this.tag = tag;
	}

	@Override
	public ITypedIngredient<T> getTypedIngredient() {
		return ingredient;
	}

	@Override
	public Optional<IBookmark> getBookmark() {
		return Optional.empty();
	}

	@Override
	public IDrawable createRenderOverlay() {
		return null;
	}

	@Override
	public void show(IRecipesGui recipesGui, FocusUtil focusUtil, List<RecipeIngredientRole> roles) {
		Optional<RecipeType<ITagInfoRecipe>> recipeType = Internal.getJeiRuntime().getRecipeManager()
			.getRecipeType(TAG_RECIPE_TYPE_UID, ITagInfoRecipe.class);
		if (recipeType.isEmpty()) {
			showItemRecipes(recipesGui, focusUtil, roles);
			return;
		}
		ITagInfoRecipe tagRecipe = Internal.getJeiRuntime().getRecipeManager()
			.createRecipeLookup(recipeType.get())
			.get()
			.filter(recipe -> recipe.getTag().equals(tag))
			.findFirst()
			.orElse(null);
		if (tagRecipe == null) {
			showItemRecipes(recipesGui, focusUtil, roles);
			return;
		}
		List<IFocus<?>> focuses = focusUtil.createFocuses(ingredient, List.of(RecipeIngredientRole.OUTPUT));
		IRecipeCategory<ITagInfoRecipe> category = Internal.getJeiRuntime().getRecipeManager().getRecipeCategory(recipeType.get());
		recipesGui.showRecipes(category, List.of(tagRecipe), focuses);
	}

	/** 回退：与 {@code IngredientElement} 行为一致，按点击物品打开 recipe 页面。 */
	private void showItemRecipes(IRecipesGui recipesGui, FocusUtil focusUtil, List<RecipeIngredientRole> roles) {
		List<IFocus<?>> focuses = focusUtil.createFocuses(ingredient, roles);
		recipesGui.show(focuses);
	}

	@Override
	public void getTooltip(
		JeiTooltip tooltip,
		IngredientGridTooltipHelper tooltipHelper,
		IIngredientRenderer<T> ingredientRenderer,
		IIngredientHelper<T> ingredientHelper
	) {
		tooltipHelper.getIngredientTooltip(tooltip, ingredient, ingredientRenderer, ingredientHelper);
	}

	@Override
	public boolean isVisible() {
		return true;
	}
}
