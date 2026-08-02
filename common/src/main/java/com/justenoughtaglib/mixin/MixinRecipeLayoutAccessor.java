package com.justenoughtaglib.mixin;

import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.library.gui.recipes.RecipeLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 RecipeLayout 内部持有的 focus 组。
 *
 * JEI 的 API 接口 IRecipeLayoutDrawable 不提供 focus 访问，
 * 而 RecipeBookmark.create 生成 tag recipe 书签时需要知道
 * 用户是从哪个物品（focus）进入该布局的。
 * 通过 @Accessor 直接把私有字段 focuses 暴露成 getter。
 *
 * remap = false：JEI 的类/字段在 Forge 与 Fabric 运行时均不重映射，
 * dev 名即运行时名（与 MixinClientConfig 同一模式）。
 */
@Mixin(RecipeLayout.class)
public interface MixinRecipeLayoutAccessor {

	@Accessor(value = "focuses", remap = false)
	IFocusGroup justenoughtaglib$getFocuses();
}
