package com.justenoughtaglib.tag;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IScalableDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.common.util.ImmutablePoint2i;
import mezz.jei.library.gui.ingredients.CycleTicker;
import mezz.jei.library.gui.recipes.RecipeLayout;
import mezz.jei.library.gui.recipes.ShapelessIcon;
import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.DataVersion;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TagSlotTracker 捕获/查询契约的无头单元测试：
 * 不启动游戏，用原版物品与手工绑定的 tag 内容验证映射语义。
 */
class TagSlotTrackerTest {
	private static Item OAK;
	private static Item SPRUCE;

	static {
		// 1.20.1 的 MappedRegistry 构造要求 Bootstrap 已启动（checkBootstrapCalled），
		// bootStrap() 纯代码初始化注册表，无需资源包；DataFixers 初始化要求
		// SharedConstants 已设置版本（getDataVersion）。
		// Item 构造会 createIntrusiveHolder，bootStrap 后注册表已定型无法再创建
		// 自定义物品，直接用 bootStrap 已注册的原版物品。
		SharedConstants.setVersion(new WorldVersion() {
			@Override
			public DataVersion getDataVersion() {
				return new DataVersion(3465, "1.20.1");
			}

			@Override
			public String getId() {
				return "1.20.1";
			}

			@Override
			public String getName() {
				return "1.20.1";
			}

			@Override
			public int getProtocolVersion() {
				return 763;
			}

			@Override
			public int getPackVersion(PackType type) {
				return 15;
			}

			@Override
			public Date getBuildTime() {
				return new Date(0);
			}

			@Override
			public boolean isStable() {
				return true;
			}
		});
		Bootstrap.bootStrap();
		OAK = Items.OAK_PLANKS;
		SPRUCE = Items.SPRUCE_PLANKS;
	}

	private static TagKey<Item> tag(String path) {
		return TagKey.create(Registries.ITEM, new ResourceLocation("test", path));
	}

	private static Holder<Item> holderOf(Item item) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(item));
		return BuiltInRegistries.ITEM.getHolderOrThrow(key);
	}

	/** 模拟 MixinIngredient 的 getItems RETURN 捕获：传展开结果。 */
	private static void capture(Ingredient ingredient) {
		TagSlotTracker.capture(ingredient, ingredient.getItems());
	}

	private static RecipeLayout layout() {
		return new RecipeLayout<>(
			stubCategory(),
			List.of(),
			null,
			(IScalableDrawable) null,
			0,
			(ShapelessIcon) null,
			new ImmutablePoint2i(0, 0),
			List.<IRecipeSlotDrawable>of(),
			List.<IRecipeSlotDrawable>of(),
			(CycleTicker) null,
			null
		);
	}

	private static IRecipeCategory<Object> stubCategory() {
		return new IRecipeCategory<>() {
			@Override
			public RecipeType<Object> getRecipeType() {
				return null;
			}

			@Override
			public Component getTitle() {
				return null;
			}

			@Override
			public IDrawable getIcon() {
				return null;
			}

			@Override
			public void setRecipe(IRecipeLayoutBuilder builder, Object recipe, IFocusGroup focuses) {
			}

			@Override
			public int getWidth() {
				return 100;
			}

			@Override
			public int getHeight() {
				return 100;
			}
		};
	}

	/** tag Ingredient 在展开后（无头环境下展开为空）仍能按空内容列表查回。 */
	@Test
	void tagIngredientIsCapturedAndFound() {
		TagKey<Item> tag = tag("planks");
		RecipeLayout layout = layout();

		TagSlotTracker.beginBuild();
		capture(Ingredient.of(tag));
		TagSlotTracker.associateLayout(layout);

		assertEquals(Optional.of(tag), TagSlotTracker.findTag(layout, List.of()));
	}

	/** 显式物品列表的 Ingredient（非 tag）不被捕获。 */
	@Test
	void plainItemIngredientIsNotCaptured() {
		RecipeLayout layout = layout();

		TagSlotTracker.beginBuild();
		capture(Ingredient.of(new ItemStack(OAK)));
		TagSlotTracker.associateLayout(layout);

		assertEquals(Optional.empty(), TagSlotTracker.findTag(layout, List.of(new ItemStack(OAK))));
	}

	/** 展开按注册表实际绑定的 tag 内容，且键为顺序敏感的全等匹配。 */
	@Test
	void expansionUsesBoundTagContentsAndIsOrderSensitive() {
		TagKey<Item> tag = tag("planks");
		BuiltInRegistries.ITEM.bindTags(Map.of(
			tag,
			List.of(holderOf(OAK), holderOf(SPRUCE))
		));
		RecipeLayout layout = layout();

		TagSlotTracker.beginBuild();
		capture(Ingredient.of(tag));
		TagSlotTracker.associateLayout(layout);

		assertEquals(
			Optional.of(tag),
			TagSlotTracker.findTag(layout, List.of(new ItemStack(OAK), new ItemStack(SPRUCE)))
		);
		assertEquals(
			Optional.empty(),
			TagSlotTracker.findTag(layout, List.of(new ItemStack(SPRUCE), new ItemStack(OAK)))
		);
		assertEquals(Optional.empty(), TagSlotTracker.findTag(layout, List.of(new ItemStack(OAK))));
	}

	/** 同一内容先捕获者胜（与 JEI 反查 findFirst 语义一致）。 */
	@Test
	void firstCaptureWinsForIdenticalContent() {
		TagKey<Item> tagA = tag("a");
		TagKey<Item> tagB = tag("b");
		BuiltInRegistries.ITEM.bindTags(Map.of(
			tagA, List.of(holderOf(OAK)),
			tagB, List.of(holderOf(OAK))
		));
		RecipeLayout layout = layout();

		TagSlotTracker.beginBuild();
		capture(Ingredient.of(tagA));
		capture(Ingredient.of(tagB));
		TagSlotTracker.associateLayout(layout);

		assertEquals(Optional.of(tagA), TagSlotTracker.findTag(layout, List.of(new ItemStack(OAK))));
	}

	/** 不同布局的捕获互不串扰；同内容在不同布局可指向不同 tag。 */
	@Test
	void layoutsAreIsolated() {
		TagKey<Item> tagA = tag("a");
		TagKey<Item> tagB = tag("b");
		BuiltInRegistries.ITEM.bindTags(Map.of(
			tagA, List.of(holderOf(OAK)),
			tagB, List.of(holderOf(OAK))
		));
		RecipeLayout layoutA = layout();
		RecipeLayout layoutB = layout();

		TagSlotTracker.beginBuild();
		capture(Ingredient.of(tagA));
		TagSlotTracker.associateLayout(layoutA);
		TagSlotTracker.beginBuild();
		capture(Ingredient.of(tagB));
		TagSlotTracker.associateLayout(layoutB);

		assertEquals(Optional.of(tagA), TagSlotTracker.findTag(layoutA, List.of(new ItemStack(OAK))));
		assertEquals(Optional.of(tagB), TagSlotTracker.findTag(layoutB, List.of(new ItemStack(OAK))));
	}
}
