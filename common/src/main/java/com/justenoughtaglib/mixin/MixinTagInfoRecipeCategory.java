package com.justenoughtaglib.mixin;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.common.Internal;
import mezz.jei.library.plugins.jei.tags.ITagInfoRecipe;
import mezz.jei.library.plugins.jei.tags.TagInfoRecipeCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = TagInfoRecipeCategory.class, remap = false)
abstract class MixinTagInfoRecipeCategory {
	@Inject(
		method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lmezz/jei/library/plugins/jei/tags/ITagInfoRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V",
		at = @At("HEAD"),
		cancellable = true,
		remap = false
	)
	private void justenoughtaglib$setFocusedRecipe(
		IRecipeLayoutBuilder builder,
		ITagInfoRecipe recipe,
		IFocusGroup focuses,
		CallbackInfo ci
	) {
		IJeiRuntime runtime;
		try {
			runtime = Internal.getJeiRuntime();
		} catch (IllegalStateException e) {
			// JEI 注册配方阶段（PluginLoader.createRecipeManager，早于运行时创建）
			// 也会调用 setRecipe 构建 ingredient supplier；此时 getJeiRuntime()
			// 必然抛 ISE（仅当 jeiRuntime == null 时抛），且 focuses 恒为空，
			// 放行原方法——否则异常会让所有 tag recipe 注册失败、整个 tag 分类消失。
			return;
		}
		Optional<ITypedIngredient<?>> focusedMember = TagBookmarkFocus.find(
			recipe,
			focuses,
			runtime.getIngredientManager()
		);
		if (focusedMember.isEmpty()) {
			return;
		}

		builder.addInputSlot()
			.addTypedIngredient(focusedMember.get())
			.setStandardSlotBackground();
		builder.addOutputSlot()
			.addTypedIngredient(focusedMember.get());
		ci.cancel();
	}
}
