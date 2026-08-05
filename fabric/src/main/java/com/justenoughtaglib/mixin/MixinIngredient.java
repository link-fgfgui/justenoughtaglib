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
 * RETURN 处捕获"展开结果 → tag"，键与槽内实际展示内容（同一份缓存数组）一致；
 * 窗口外的 getItems（配方注册、铁砧材料、酿造容器枚举等）由
 * {@link TagSlotTracker#isBuilding()} 守卫排除。
 *
 * <p>目标是 vanilla 类，必须放在 loader 模块（fabric/forge 各一份）：
 * common 模块无 refmap，不能混入需要 SRG/intermediary 重映射的 vanilla 目标；
 * 各 loader 的 mixin AP 会自动生成重映射条目。
 */
@Mixin(Ingredient.class)
public abstract class MixinIngredient {

	@Inject(method = "getItems", at = @At("RETURN"))
	private void justenoughtaglib$captureTag(CallbackInfoReturnable<ItemStack[]> cir) {
		if (TagSlotTracker.isBuilding()) {
			TagSlotTracker.capture((Ingredient) (Object) this, cir.getReturnValue());
		}
	}
}
