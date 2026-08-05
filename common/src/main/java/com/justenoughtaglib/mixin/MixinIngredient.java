package com.justenoughtaglib.mixin;

import com.justenoughtaglib.tag.TagSlotTracker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 唯一漏斗：JEI 所有把原始 Ingredient 展开成物品列表的路径最终都调用
 * {@code Ingredient.getItems()}（crafting 扩展、IIngredientConsumer /
 * IIngredientAcceptor 默认方法、锻造扩展等）。在布局构建窗口内
 * （{@link TagSlotTracker#beginBuild()} 与 {@code associateLayout} 之间）的
 * RETURN 处捕获"展开结果 → tag"，并按已加载的 tag recipe 书签收窄展示成员；
 * 窗口外的 getItems（配方注册、铁砧材料、酿造容器枚举等）由
 * {@link TagSlotTracker#isBuilding()} 守卫排除。
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
