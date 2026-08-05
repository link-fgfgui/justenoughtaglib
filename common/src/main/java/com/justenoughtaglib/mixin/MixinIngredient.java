package com.justenoughtaglib.mixin;

import com.justenoughtaglib.tag.TagSlotTracker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Single funnel: every JEI path that expands a raw Ingredient into an item
 * list ultimately calls {@code Ingredient.getItems()} (crafting extensions,
 * {@code IIngredientConsumer} / {@code IIngredientAcceptor} default methods,
 * forging extensions, and so on). During the layout-build window (between
 * {@link TagSlotTracker#beginBuild()} and {@code associateLayout}), capture
 * the "expanded result -> tag" mapping at RETURN and narrow the displayed
 * members according to loaded tag recipe bookmarks. Calls to getItems outside
 * the window (recipe registration, anvil ingredients, brewing-container
 * enumeration, and so on) are excluded by the {@link TagSlotTracker#isBuilding()}
 * guard.
 */
@Mixin(Ingredient.class)
public abstract class MixinIngredient {

	@Inject(method = "getItems", at = @At("RETURN"), cancellable = true)
	private void justenoughtaglib$captureTag(CallbackInfoReturnable<ItemStack[]> cir) {
		if (TagSlotTracker.isBuilding()) {
			cir.setReturnValue(TagSlotTracker.captureAndApplyPreference(
				(Ingredient) (Object) this,
				cir.getReturnValue()
			));
		}
	}
}
