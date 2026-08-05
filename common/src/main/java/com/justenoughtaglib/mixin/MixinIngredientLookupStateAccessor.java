package com.justenoughtaglib.mixin;

import mezz.jei.gui.recipes.lookups.IngredientLookupState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = IngredientLookupState.class, remap = false)
public interface MixinIngredientLookupStateAccessor {
	@Accessor("recipeIndex")
	void justenoughtaglib$setRecipeIndex(int recipeIndex);
}
