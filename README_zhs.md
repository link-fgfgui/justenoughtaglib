# JustEnoughTagLib

一个面向 **Minecraft 1.20.1 + JEI** 的客户端物品标签配方（Tag Recipe）增强 Mod，同时支持 **Forge** 和 **Fabric**。

## 功能特性

- **强制启用标签配方页面**：JEI 的物品标签配方分类（recipe type 形如 `minecraft:tag_recipes/item`，例如查看 `#minecraft:planks` 包含哪些物品）默认只在开发环境开放。本 Mod 通过改写 `ClientConfig#isShowTagRecipesEnabled()`，在生产环境下也始终启用。（`MixinClientConfig`）
- **默认隐藏方块标签配方**：实用性较低的"方块标签配方"分类（`minecraft:tag_recipes/block`）默认隐藏，可通过客户端配置 `hideJeiBlockTagRecipes` 设为 `false` 恢复。（`JustEnoughTagLibJeiPlugin`）
- **点击标签输入槽跳转**：在普通配方中点击由 tag 构造的输入槽时，直接跳到对应的标签配方页面，而不只是按物品查配方。（`TagRecipeJumpElement` / `MixinRecipeGuiLayouts`）
- **配方输出槽收藏为书签**：普通配方输出槽（不只是物品）可以收藏为 JEI 的**配方书签**，点击输出槽即可加入 JEI 书签栏，并带星标覆盖图与"配方分类"提示。（`RecipeContextElement` / `MixinBookmarkList` / `MixinRecipeBookmarkElement`）
- **修复标签配方书签记录物品错误**：从某个具体物品进入标签页面后收藏，书签记录的是**该聚焦物品**，而不是 tag 成员列表的第一个物品。（`MixinRecipeBookmark`）
- **书签重载后收窄展示**：重新加载书签后，使用同一 tag 的配方输入槽会优先显示书签选定的物品，同时保留原 tag 名称与完整成员列表的 tooltip 提示。（`TagBookmarkPreferences` / `MixinTagInfoRecipeCategory` / `MixinRecipeSlotBuilder` / `TagTooltipHelper`）
- **配方书签完整交互**：标签配方书签支持常规的配方/用途查询（R/U 与右键），以及从书签直接执行 JEI 配方转移；当聚焦成员无法转移时会自动回退使用完整标签配方进行转移。（`MixinRecipeBookmarkElement` / `TransferLayoutPolicy`）
- **更明确的提示**：配方书签的 tooltip 显示物品名称与配方分类，并使用 JEI 原生的书签图标（半角星标）作为覆盖图。

## 工作原理

标签身份在 `Ingredient#getItems()` 展开时丢失（展开后只剩物品列表）。Mod 在布局构建窗口内通过各加载器模块的 `MixinIngredient` 拦截 `getItems()` 的返回，用 `Ingredient#toJson()` 恢复 tag 来源，并把"展开结果 → tag"映射关联到当前正在构建的 `RecipeLayout`（`TagSlotTracker`）。这样点击、tooltip、书签收窄和转移都能基于**当前布局自己的数据**精确判定，避免跨页面、跨配方的陈旧命中。

## 依赖与版本

| 依赖 | 版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| JEI | >= 15.21.0.148 |
| Forge | 47.2.30（FML 版本范围 `[47,)`） |
| Fabric | Fabric API >= 0.92.11，Fabric Loader >= 0.16.10，Forge Config API Port >= 8.0.3 |

## 构建

```bash
./gradlew build
```

产物分别位于 `fabric/build/libs` 与 `forge/build/libs`。运行客户端：`fabric:runClient` / `forge:runClient`。

## 安装

将对应加载器版本的 jar 放入 `mods` 目录，与 JEI 一同使用。

## 配置

客户端配置（Forge 的 `forge-client.toml`，Fabric 经 Forge Config API Port 写入）：

- `hideJeiBlockTagRecipes`（默认 `true`）：隐藏方块标签配方分类，设为 `false` 恢复。

## 使用与书签文件

- 进游戏后 JEI 左侧分类栏会出现 tag 分类（`tag_recipes/item`）。
- 普通配方中点击 tag 输入槽 → 跳转到对应标签配方页面。
- 在标签配方页面对聚焦物品按 `U` 进入后，悬停配方卡片右上角点星标即可收藏配方书签。
- 已收藏其它物品的 tag 书签后，使用同一 tag 的配方输入槽会自动收窄为该书签物品，悬停仍显示 tag 名称与完整成员。

配方书签文件按世界保存于：

```
config/jei/world/local/<世界名>/bookmarks.ini
```

（多人模式为 `config/jei/world/server/<名>_<hash>/`；世界名中的空格会替换为下划线，例如 `New World` → `New_World`。）

## 许可证

[LGPLv3](LICENSE)
