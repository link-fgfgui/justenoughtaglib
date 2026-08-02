package com.justenoughtaglib.mixin;

import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.gui.bookmarks.RecipeBookmark;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修正 tag recipe 书签写入的物品。
 *
 * 原逻辑 RecipeBookmark.create 取布局中第一个 OUTPUT 槽的物品
 * （tag recipe 的成员顺序是数据包声明序），因此书签文件里
 * {@code "R:...tag_recipes/...#...#item_stack&<物品>"} 写的是 tag 首成员，
 * 而不是用户按 U 时聚焦的那个物品。
 *
 * 这里在 create 入口拦截：当 recipe type 属于 tag_recipes/ 且布局带 focus 时，
 * 用 focus 的物品重建书签（displayRole 固定 OUTPUT，反序列化时按 OUTPUT
 * focus 即可匹配 tag 布局的 output 槽）；其余情况一律走原逻辑。
 *
 * remap = false：JEI 的类/方法在 Forge 与 Fabric 运行时均不重映射。
 */
@Mixin(RecipeBookmark.class)
public class MixinRecipeBookmark {

	@Inject(method = "create", remap = false, at = @At("HEAD"), cancellable = true)
	private static <T> void justenoughtaglib$bookmarkFocusedTagItem(
		IRecipeLayoutDrawable<T> recipeLayoutDrawable,
		IIngredientManager ingredientManager,
		CallbackInfoReturnable<RecipeBookmark<T, ?>> cir
	) {
		ResourceLocation recipeTypeUid = recipeLayoutDrawable.getRecipeCategory().getRecipeType().getUid();
		if (!recipeTypeUid.getPath().startsWith("tag_recipes/")) {
			return; // 非 tag recipe 走原逻辑
		}
		if (!(recipeLayoutDrawable instanceof MixinRecipeLayoutAccessor accessor)) {
			return;
		}
		IFocusGroup focuses = accessor.justenoughtaglib$getFocuses();
		if (focuses == null) {
			return; // 无 focus（直接浏览 tag 页）：原逻辑写 tag 首成员
		}
		for (IFocus<?> focus : focuses.getAllFocuses()) {
			ResourceLocation recipeUid = recipeLayoutDrawable.getRecipeCategory()
				.getRegistryName(recipeLayoutDrawable.getRecipe());
			if (recipeUid == null) {
				return; // 与原逻辑一致：无 registryName 则放弃书签
			}
			ITypedIngredient<?> typed = ingredientManager.normalizeTypedIngredient(focus.getTypedValue());
			cir.setReturnValue(new RecipeBookmark<>(
				recipeLayoutDrawable.getRecipeCategory(),
				recipeLayoutDrawable.getRecipe(),
				recipeUid,
				typed,
				RecipeIngredientRole.OUTPUT
			));
			return;
		}
	}
}
