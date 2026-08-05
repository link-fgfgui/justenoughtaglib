package com.justenoughtaglib.mixin;

import com.justenoughtaglib.tag.TagSlotTracker;
import mezz.jei.library.gui.recipes.RecipeLayout;
import mezz.jei.library.gui.recipes.layout.builder.RecipeLayoutBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 把构建期捕获的 tag 映射与最终 RecipeLayout 实例关联：
 * 每个布局一个独立映射（ThreadLocal 归组），buildRecipeLayout 返回时挂到布局上，
 * 点击查询只读当前布局的数据——跨页面/跨配方的陈旧条目不可能误命中。
 */
@Mixin(RecipeLayoutBuilder.class)
public abstract class MixinRecipeLayoutBuilder<T> {

	@Inject(method = "<init>", remap = false, at = @At("TAIL"))
	private void justenoughtaglib$beginTagBuild(CallbackInfo ci) {
		TagSlotTracker.beginBuild();
	}

	@Inject(method = "buildRecipeLayout", remap = false, at = @At("RETURN"))
	private void justenoughtaglib$associateTagBuild(CallbackInfoReturnable<RecipeLayout<T>> cir) {
		TagSlotTracker.associateLayout(cir.getReturnValue());
	}
}
