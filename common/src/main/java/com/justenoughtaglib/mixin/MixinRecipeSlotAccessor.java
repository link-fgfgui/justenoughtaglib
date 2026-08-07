package com.justenoughtaglib.mixin;

import mezz.jei.library.gui.ingredients.RecipeSlot;
import mezz.jei.library.ingredients.DisplayIngredientAcceptor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes whether a recipe category already owns a slot's display override. */
@Mixin(value = RecipeSlot.class, remap = false)
public interface MixinRecipeSlotAccessor {
	@Accessor("displayOverrides")
	DisplayIngredientAcceptor justenoughtaglib$getDisplayOverrides();
}
