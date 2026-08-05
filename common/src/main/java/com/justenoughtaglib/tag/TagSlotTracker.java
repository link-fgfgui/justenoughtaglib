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
 * 在配方布局构建期捕获槽的 tag 来源，并把完整成员与书签收窄后的成员
 * 映射到同一份 tag 元数据，供 tooltip、点击导航和交互语义查询。
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
 * <p>键是槽内实际物品的 {@link Item} 列表（顺序敏感的全等匹配）。未绑定时记录
 * 完整 tag 成员；有 bookmark 绑定时还记录单物品键，但值中继续保存完整成员列表。
 */
public final class TagSlotTracker {
	private TagSlotTracker() {
	}

	/** 当前正在构建的布局的捕获表；仅客户端线程访问，ThreadLocal 兜底其它线程。 */
	private static final ThreadLocal<Map<List<Item>, TagSlotData>> CURRENT_BUILD = new ThreadLocal<>();

	/** 布局实例 → 该布局构建期捕获的表。布局被回收后条目自动消失。 */
	private static final Map<RecipeLayout, Map<List<Item>, TagSlotData>> LAYOUT_TAGS =
		Collections.synchronizedMap(new WeakHashMap<>());

	public record TagSlotData(TagKey<?> tag, List<ItemStack> members, boolean preferred) {
		public TagSlotData {
			members = members.stream().map(ItemStack::copy).toList();
		}
	}

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
		Map<List<Item>, TagSlotData> build = CURRENT_BUILD.get();
		if (build == null) {
			return;
		}
		getTag(ingredient).ifPresent(tag -> capture(
			build,
			expandedStacks,
			new TagSlotData(tag, List.of(expandedStacks), false),
			false
		));
	}

	/** Captures the original tag membership, then applies any loaded tag bookmark preference. */
	public static ItemStack[] captureAndApplyPreference(Ingredient ingredient, ItemStack[] expandedStacks) {
		Optional<TagKey<Item>> tag = getTag(ingredient);
		if (tag.isEmpty()) {
			return expandedStacks;
		}

		Map<List<Item>, TagSlotData> build = CURRENT_BUILD.get();
		if (build == null) {
			return expandedStacks;
		}

		ItemStack[] selectedStacks = TagBookmarkPreferences.apply(tag.get(), expandedStacks);
		captureSelection(build, tag.get(), expandedStacks, selectedStacks);
		return selectedStacks;
	}

	static void captureSelection(
		TagKey<?> tag,
		ItemStack[] expandedStacks,
		ItemStack[] selectedStacks
	) {
		Map<List<Item>, TagSlotData> build = CURRENT_BUILD.get();
		if (build != null) {
			captureSelection(build, tag, expandedStacks, selectedStacks);
		}
	}

	private static void captureSelection(
		Map<List<Item>, TagSlotData> build,
		TagKey<?> tag,
		ItemStack[] expandedStacks,
		ItemStack[] selectedStacks
	) {
		TagSlotData original = new TagSlotData(tag, List.of(expandedStacks), false);
		capture(build, expandedStacks, original, false);
		if (selectedStacks != expandedStacks) {
			TagSlotData preferred = new TagSlotData(tag, List.of(expandedStacks), true);
			capture(build, selectedStacks, preferred, true);
		}
	}

	private static void capture(
		Map<List<Item>, TagSlotData> build,
		ItemStack[] stacks,
		TagSlotData data,
		boolean replace
	) {
		List<Item> items = Arrays.stream(stacks)
			.map(ItemStack::getItem)
			.toList();
		if (replace) {
			build.put(items, data);
		} else {
			build.putIfAbsent(items, data);
		}
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
		Map<List<Item>, TagSlotData> build = CURRENT_BUILD.get();
		CURRENT_BUILD.remove();
		if (layout == null || build == null || build.isEmpty()) {
			return;
		}
		LAYOUT_TAGS.put(layout, build);
	}

	/**
	 * 查询某个布局内由 tag Ingredient 构建的槽，包括 bookmark 收窄后的单物品槽。
	 *
	 * @return 槽内容对应的 tag；该槽不是由 tag Ingredient 构建时为空。
	 */
	public static Optional<TagKey<?>> findTag(IRecipeLayoutDrawable<?> layout, List<ItemStack> itemStacks) {
		return findTagData(layout, itemStacks).map(TagSlotData::tag);
	}

	public static Optional<TagSlotData> findTagData(
		IRecipeLayoutDrawable<?> layout,
		List<ItemStack> itemStacks
	) {
		if (!(layout instanceof RecipeLayout recipeLayout)) {
			return Optional.empty();
		}
		Map<List<Item>, TagSlotData> build = LAYOUT_TAGS.get(recipeLayout);
		if (build == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(build.get(itemKey(itemStacks)));
	}

	public static Optional<TagSlotData> findCurrentTagData(List<ItemStack> itemStacks) {
		Map<List<Item>, TagSlotData> build = CURRENT_BUILD.get();
		if (build == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(build.get(itemKey(itemStacks)));
	}

	private static List<Item> itemKey(List<ItemStack> itemStacks) {
		return itemStacks.stream().map(ItemStack::getItem).toList();
	}
}
