package com.justenoughtaglib.mixin;

import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.library.gui.recipes.RecipeLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the focus group held internally by RecipeLayout.
 *
 * JEI's IRecipeLayoutDrawable API does not expose the focus group,
 * but RecipeBookmark.create needs to know which item the user used
 * to enter this layout when creating a tag recipe bookmark.
 * {@code @Accessor} exposes the private focuses field as a getter.
 *
 * remap = false: JEI classes and fields are not remapped at runtime on Forge
 * or Fabric; their development names are also their runtime names (the same
 * pattern as MixinClientConfig).
 */
@Mixin(RecipeLayout.class)
public interface MixinRecipeLayoutAccessor {

	@Accessor(value = "focuses", remap = false)
	IFocusGroup justenoughtaglib$getFocuses();
}
