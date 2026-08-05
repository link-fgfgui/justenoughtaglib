package com.justenoughtaglib.tag;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.library.gui.recipes.RecipeLayout;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * 在配方布局构建期捕获"槽内容 == tag 成员集"的事实，供点击跳转等场景精确查询。
 *
 * <p>tag 身份在 {@link Ingredient#getItems()} 展开的那一刻丢失（展开后只剩物品列表）。
 * JEI 所有展开路径最终都调用同一个 {@code getItems()}（crafting 扩展、
 * {@code IIngredientConsumer}/{@code IIngredientAcceptor} 默认方法、锻造扩展等），
 * 因此 loader 模块（fabric/forge）的 {@code MixinIngredient} 在 getItems 返回后、
 * 布局构建窗口内（见 {@link #beginBuild()} / {@link #associateLayout(RecipeLayout)}）
 * 捕获展开结果与 tag 的对应关系。构建窗口外的 getItems（配方注册、铁砧材料、
 * 酿造容器枚举等）被 {@link #isBuilding()} 守卫排除。
 *
 * <p>1.20.1 的 {@link Ingredient} 没有公开的 tag 访问器（{@code getTag()} 是后续版本
 * 才加入的），用 {@link Ingredient#toJson()} 恢复：单一 TagValue 的 Ingredient 序列化
 * 为 {@code {"tag": "ns:path"}}（与数据包格式一致），与 getTag() 的判定语义等价——
 * 混合值/显式物品列表都没有 tag 键，被天然排除。
 *
 * <p>捕获结果按"构建中的 RecipeLayoutBuilder"归组（ThreadLocal），在
 * {@code RecipeLayoutBuilder.buildRecipeLayout} 返回时与最终的 RecipeLayout 实例关联
 * （WeakHashMap）。点击时只查询鼠标所在布局自己的映射，因此不存在跨页面/跨布局的
 * 陈旧条目——这是比"全局反向扫描"精确的根本原因。
 *
 * <p>键是展开后物品的 {@link Item} 列表（顺序敏感的全等匹配）。tag 展开恒为 count=1
 * 的无 NBT 栈，按物品身份比较即可，且与槽内实际展示的内容（同一份缓存展开结果）
 * 天然一致。
 */
public final class TagSlotTracker {
	private TagSlotTracker() {
	}

	/** 当前正在构建的布局的捕获表；仅客户端线程访问，ThreadLocal 兜底其它线程。 */
	private static final ThreadLocal<Map<List<Item>, TagKey<?>>> CURRENT_BUILD = new ThreadLocal<>();

	/** 布局实例 → 该布局构建期捕获的表。布局被回收后条目自动消失。 */
	private static final Map<RecipeLayout, Map<List<Item>, TagKey<?>>> LAYOUT_TAGS =
		Collections.synchronizedMap(new WeakHashMap<>());

	/**
	 * 由 {@code RecipeLayoutBuilder.<init>} 调用：开始一次新的布局构建捕获。
	 */
	public static void beginBuild() {
		CURRENT_BUILD.set(new HashMap<>());
	}

	/** 是否处于布局构建窗口内（getItems 漏斗的守卫：仅构建期捕获）。 */
	public static boolean isBuilding() {
		return CURRENT_BUILD.get() != null;
	}

	/**
	 * 在 tag 被展开后立刻调用：若 Ingredient 是单一 tag 值，记录"成员列表 → tag"。
	 * 非 tag Ingredient（显式物品列表、混合值）直接忽略，保证映射里只有真实的 tag。
	 *
	 * @param ingredient    被展开的 Ingredient；tag 身份从这里恢复（toJson）
	 * @param expandedStacks 展开结果（getItems 的返回值，与槽内展示内容同一份数组）
	 */
	public static void capture(Ingredient ingredient, ItemStack[] expandedStacks) {
		if (ingredient == null) {
			return;
		}
		Map<List<Item>, TagKey<?>> build = CURRENT_BUILD.get();
		if (build == null) {
			return;
		}
		getTag(ingredient).ifPresent(tag -> {
			List<Item> items = Arrays.stream(expandedStacks)
				.map(ItemStack::getItem)
				.toList();
			// 同内容重复捕获（同一 tag 被多次展开、或同页两个配方共用）
			// 保留首次结果，与 JEI 反查的 findFirst 语义一致。
			build.putIfAbsent(items, tag);
		});
	}

	/**
	 * 1.20.1 无 {@code Ingredient.getTag()}，从序列化结果恢复：
	 * 单一 TagValue 的 Ingredient 序列化为 {@code {"tag": "ns:path"}}。
	 */
	private static Optional<TagKey<Item>> getTag(Ingredient ingredient) {
		JsonElement json;
		try {
			json = ingredient.toJson();
		} catch (RuntimeException e) {
			// 模组自定义 Ingredient 的序列化可能抛异常，跳过即可。
			return Optional.empty();
		}
		if (!json.isJsonObject()) {
			return Optional.empty();
		}
		JsonObject object = json.getAsJsonObject();
		JsonElement tag = object.get("tag");
		if (tag == null || !tag.isJsonPrimitive() || !tag.getAsJsonPrimitive().isString()) {
			return Optional.empty();
		}
		ResourceLocation location = ResourceLocation.tryParse(tag.getAsString());
		if (location == null) {
			return Optional.empty();
		}
		return Optional.of(TagKey.create(Registries.ITEM, location));
	}

	/** 由 {@code RecipeLayoutBuilder.buildRecipeLayout} 返回时调用：把本次构建的捕获挂到布局上。 */
	public static void associateLayout(RecipeLayout layout) {
		Map<List<Item>, TagKey<?>> build = CURRENT_BUILD.get();
		CURRENT_BUILD.remove();
		if (layout == null || build == null || build.isEmpty()) {
			return;
		}
		LAYOUT_TAGS.put(layout, build);
	}

	/**
	 * 查询某个布局内"内容恰好为某个 tag 成员集"的槽。
	 *
	 * @return 槽内容对应的 tag；该槽不是由 tag Ingredient 构建时为空。
	 */
	public static Optional<TagKey<?>> findTag(IRecipeLayoutDrawable<?> layout, List<ItemStack> itemStacks) {
		if (!(layout instanceof RecipeLayout recipeLayout)) {
			return Optional.empty();
		}
		Map<List<Item>, TagKey<?>> build = LAYOUT_TAGS.get(recipeLayout);
		if (build == null) {
			return Optional.empty();
		}
		List<Item> items = itemStacks.stream()
			.map(ItemStack::getItem)
			.toList();
		return Optional.ofNullable(build.get(items));
	}
}
