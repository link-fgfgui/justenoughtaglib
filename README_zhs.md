# JustEnoughTagLib

一个面向 **Minecraft 1.20.1 + JEI** 的客户端物品标签配方（Tag Recipe）增强 Mod，同时支持 **Forge** 和 **Fabric**。

## 功能特性

- **强制启用标签配方页面**：JEI 的物品标签配方分类（recipe type 形如 `minecraft:tag_recipes/item`，例如查看 `#minecraft:planks` 包含哪些物品）默认只在开发环境开放。本 Mod 通过改写 `ClientConfig#isShowTagRecipesEnabled()`，在生产环境下也始终启用。（`MixinClientConfig`）
- **默认隐藏方块标签配方**：实用性较低的"方块标签配方"分类（`minecraft:tag_recipes/block`）默认隐藏，可通过客户端配置 `hideJeiBlockTagRecipes` 设为 `false` 恢复。（`JustEnoughTagLibJeiPlugin`）
- **未收藏的标签输入槽点击跳转**：在普通配方中点击由 tag 构造、且该 tag 尚未被书签收藏的输入槽时，直接跳到对应的标签配方页面；一旦该 tag 已有书签，其输入槽则回退为 JEI 正常的物品配方/用途查询。（`TagRecipeJumpElement` / `MixinRecipeGuiLayouts`）
- **标签配方输出槽收藏为书签**：标签配方页面的**输出槽**可以直接点击收藏为 JEI **配方书签**，并以 JEI 原生书签样式显示在书签栏。（`RecipeContextElement` / `MixinBookmarkList`）
- **修复标签配方书签记录物品错误**：从某个具体物品进入标签页面后收藏，书签记录的是**该聚焦物品**，而不是 tag 成员列表的第一个物品。（`MixinRecipeBookmark`）
- **书签重载后收窄展示**：在输入槽解析到已收藏 tag 的配方中，该输入槽以"仅显示覆盖"的方式显示书签选定的物品（不会改动槽位底层的 tag 成员列表），并在 JEI 循环展示物品时重新应用该覆盖。（`TagBookmarkPreferences` / `TagSlotTracker` / `MixinRecipeLayoutBuilder` / `MixinRecipeLayout`）
- **配方书签完整交互**：标签配方书签支持常规的配方/用途查询（R/U 与右键），以及从书签直接执行 JEI 配方转移；当聚焦成员无法转移时会自动回退使用完整标签配方进行转移。（`MixinRecipeBookmarkElement` / `TransferLayoutPolicy`）
- **更明确的提示**：标签配方书签的 tooltip 显示所存物品名称与配方分类行，而非通用的原料 tooltip。（`MixinRecipeBookmarkElement`）

## 工作原理

标签身份在 JEI 把 tag 原料展开为其成员物品列表时丢失。Mod 在布局构建完成后解析每个输入槽对应的 tag（`MixinRecipeLayoutBuilder`）：对 tag 配方分类直接从配方取得 tag（`ITagInfoRecipe#getTag`），对其它配方则用 `IIngredientHelper#getTagKeyEquivalent` 从槽位成员列表恢复 tag。得到的"槽 → tag"映射按 `RecipeLayout` 缓存于 `WeakHashMap`（`TagSlotTracker`），因此点击、tooltip、书签收窄与转移都基于**当前布局自己的数据**精确判定，不会跨页面、跨配方产生陈旧命中。

## 安装

将对应加载器版本的 jar 放入 `mods` 目录，与 JEI 一同使用。

## 配置

客户端配置（Forge 的 `config/justenoughtaglib-client.toml`，Fabric 经 Forge Config API Port 写入）：

- `hideJeiBlockTagRecipes`（默认 `true`）：隐藏方块标签配方分类，设为 `false` 恢复。

## 使用

- 进游戏后 JEI 左侧分类栏会出现 tag 分类（`tag_recipes/item`）。
- 普通配方中点击未收藏的 tag 输入槽 → 跳转到对应标签配方页面。
- 收藏单个标签配方：直接点击其**输出槽**加入为配方书签，或使用 JEI 标准的配方收藏按钮。
- 为某个 tag 收藏了具体物品后，使用该 tag 的配方输入槽会显示书签选定的物品。
- 对标签配方书签按 `U`/右键 → 打开该物品的用途；按 `R`/左键 → 打开配方预览；Shift+点击 → 直接转移。

## 许可证

[LGPLv3](LICENSE)
