package com.justenoughtaglib.mixin;

import com.justenoughtaglib.tag.TagSlotTracker;
import com.justenoughtaglib.tag.TagTooltipHelper;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.library.gui.ingredients.ICycler;
import mezz.jei.library.gui.recipes.layout.builder.RecipeSlotBuilder;
import mezz.jei.library.ingredients.DisplayIngredientAcceptor;
import mezz.jei.core.util.Pair;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mixin(value = RecipeSlotBuilder.class, remap = false)
public abstract class MixinRecipeSlotBuilder {
	@Shadow(remap = false)
	public abstract DisplayIngredientAcceptor getIngredientAcceptor();

	@Inject(
		method = "build(Ljava/util/Set;Lmezz/jei/library/gui/ingredients/ICycler;)Lmezz/jei/core/util/Pair;",
		at = @At("HEAD"),
		remap = false
	)
	private void justenoughtaglib$restoreTagTooltip(
		Set<Integer> focusMatches,
		ICycler cycler,
		CallbackInfoReturnable<Pair<Integer, IRecipeSlotDrawable>> cir
	) {
		List<ItemStack> stacks = getIngredientAcceptor().getAllIngredients().stream()
			.flatMap(Optional::stream)
			.map(ingredient -> ingredient.getIngredient(VanillaTypes.ITEM_STACK))
			.flatMap(Optional::stream)
			.toList();
		TagSlotTracker.findCurrentTagData(stacks)
			.filter(TagSlotTracker.TagSlotData::preferred)
			.ifPresent(data -> TagTooltipHelper.addTagTooltip(
				(RecipeSlotBuilder) (Object) this,
				data.tag(),
				data.members()
			));
	}
}
