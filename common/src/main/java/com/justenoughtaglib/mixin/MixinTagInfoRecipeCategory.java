package com.justenoughtaglib.mixin;

import com.justenoughtaglib.tag.TagBookmarkPreferences;
import com.justenoughtaglib.tag.TagTooltipHelper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.library.plugins.jei.tags.ITagInfoRecipe;
import mezz.jei.library.plugins.jei.tags.TagInfoRecipeCategory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(value = TagInfoRecipeCategory.class, remap = false)
abstract class MixinTagInfoRecipeCategory {
	@Inject(
		method = "setRecipe(Lmezz/jei/api/gui/builder/IRecipeLayoutBuilder;Lmezz/jei/library/plugins/jei/tags/ITagInfoRecipe;Lmezz/jei/api/recipe/IFocusGroup;)V",
		at = @At("HEAD"),
		cancellable = true,
		remap = false
	)
	private void justenoughtaglib$usePreferredTagMember(
		IRecipeLayoutBuilder builder,
		ITagInfoRecipe recipe,
		IFocusGroup focuses,
		CallbackInfo ci
	) {
		Optional<ITypedIngredient<?>> preferred = TagBookmarkPreferences.findPreferred(recipe);
		if (preferred.isEmpty()) {
			return;
		}

		IRecipeSlotBuilder input = builder.addInputSlot()
			.addTypedIngredient(preferred.get())
			.setStandardSlotBackground();
		List<ItemStack> members = recipe.getTypedIngredients().stream()
			.map(ingredient -> ingredient.getIngredient(VanillaTypes.ITEM_STACK))
			.flatMap(Optional::stream)
			.toList();
		TagTooltipHelper.addTagTooltip(input, recipe.getTag(), members);

		for (ITypedIngredient<?> member : recipe.getTypedIngredients()) {
			builder.addOutputSlot().addTypedIngredient(member);
		}
		ci.cancel();
	}
}
