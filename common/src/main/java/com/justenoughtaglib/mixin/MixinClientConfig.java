package com.justenoughtaglib.mixin;

import mezz.jei.common.config.ClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientConfig.class)
public class MixinClientConfig {
	@Inject(method = "isShowTagRecipesEnabled", remap = false, at = @At("HEAD"), cancellable = true)
	private void justenoughtaglib$alwaysEnableTagRecipes(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(true);
	}
}
