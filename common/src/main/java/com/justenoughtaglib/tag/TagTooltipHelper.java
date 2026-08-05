package com.justenoughtaglib.tag;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.common.Internal;
import mezz.jei.common.platform.Services;
import mezz.jei.library.gui.ingredients.TagContentTooltipComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class TagTooltipHelper {
	private TagTooltipHelper() {
	}

	public static void addTagTooltip(
		IRecipeSlotBuilder slot,
		TagKey<?> tag,
		List<ItemStack> members
	) {
		List<ItemStack> copiedMembers = members.stream().map(ItemStack::copy).toList();
		slot.addRichTooltipCallback((recipeSlot, tooltip) -> {
			tooltip.add(Component.translatable("jei.tooltip.recipe.tag", "")
				.withStyle(ChatFormatting.GRAY));
			tooltip.add(Services.PLATFORM.getRenderHelper().getName(tag)
				.copy()
				.withStyle(ChatFormatting.GRAY));

			if (copiedMembers.size() > 1 && Internal.getJeiClientConfigs()
				.getClientConfig()
				.isTagContentTooltipEnabled()) {
				IIngredientRenderer<ItemStack> renderer = Internal.getJeiRuntime()
					.getIngredientManager()
					.getIngredientRenderer(VanillaTypes.ITEM_STACK);
				tooltip.add(new TagContentTooltipComponent<>(renderer, copiedMembers));
			}
		});
	}
}
