package com.justenoughtaglib.mixin;

import mezz.jei.common.config.ClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * JEI 的 tag recipe（物品标签配方，recipe type 形如 "jei:tag_recipes/..."）
 * 默认只在开发环境启用（jei-client.ini 中 [advanced] showTagRecipesEnabled 默认 = isDev）。
 * JeiInternalPlugin.registerCategories/registerRecipes 通过
 * ClientConfig#isShowTagRecipesEnabled() 门控其注册。
 *
 * 此处强制该 getter 恒为 true，实现生产环境永远启用 tag recipe，
 * 不受配置文件影响。
 */
@Mixin(ClientConfig.class)
public class MixinClientConfig {

	@Inject(method = "isShowTagRecipesEnabled", remap = false, at = @At("HEAD"), cancellable = true)
	private void justenoughtaglib$alwaysEnableTagRecipes(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(true);
	}
}
