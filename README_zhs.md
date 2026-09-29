# JustEnoughTagLib

一个面向 **Minecraft 1.20.1 + JEI** 的客户端标签配方（Tag Recipe）增强 Mod，同时支持 **Forge** 和 **Fabric**。

[English](./README.md)

JEI 默认只在开发环境开放物品/流体标签配方页。本 Mod 在正式客户端里也保持这些页面可用，允许为某个 tag 收藏其中一个成员，并把普通配方里解析到该 tag 的输入槽钉在这个成员上。

需要 [JEI](https://github.com/mezz/JustEnoughItems) **15.21.0.148 或更高**。Fabric 还需要 [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port)。仅客户端。

## 功能特性

- **强制启用标签配方页面**：JEI 的标签配方分类（`minecraft:tag_recipes/item`，例如查看 `#minecraft:planks` 包含哪些物品）在生产环境也始终启用。（`MixinClientConfig`）
- **默认隐藏方块标签配方**：`minecraft:tag_recipes/block` 默认隐藏，可通过客户端配置 `hideJeiBlockTagRecipes` 设为 `false` 恢复。（`JustEnoughTagLibJeiPlugin`）
- **未收藏的标签输入槽点击跳转**：普通配方里，点击由 tag 构造、且该 tag 尚未被书签收藏的输入槽，直接跳到对应标签页。该 tag 已有书签后，**之后新建的布局**会把该槽当作普通物品做 R/U。（`TagRecipeJumpElement` / `MixinRecipeGuiLayouts`）
- **标签配方输出槽收藏为书签**：标签页的**输出槽**（某个成员）可以直接点击，收藏为 JEI **配方书签**，并以 JEI 原生配方书签样式显示在书签栏。（`RecipeContextElement` / `MixinBookmarkList`）
- **修复书签记录物品错误**：从某个具体输出进入配方页（tag 成员槽、或多输出配方）后再收藏，书签记录的是**该聚焦物品**，而不是第一个输出槽。（`MixinRecipeBookmark`）
- **收藏后收窄展示**：普通配方的输入槽若解析到已收藏的 tag，该槽以「仅显示覆盖」的方式显示书签选定的成员（不改动底层成员列表），并在 JEI 每次循环展示时重新钉上。（`TagBookmarkPreferences` / `TagSlotTracker` / `MixinRecipeLayoutBuilder` / `MixinRecipeLayout`）
- **书签栏上的标签书签交互**：`R` / 左键打开聚焦到所存成员的标签配方；`U` / 右键打开该成员的用途。（`MixinRecipeBookmarkElement`）
- **更短的书签提示**：标签配方书签的 tooltip 显示所存物品名称与配方分类行，而不是完整的原料 tooltip。（`MixinRecipeBookmarkElement`）

## 使用

1. 进游戏后，JEI 左侧分类栏会出现物品 tag 分类（`tag_recipes/item`）。
2. 普通配方中点击未收藏的 tag 输入槽 → 跳转到对应标签页。
3. 在标签页点击某个**成员输出槽**（或 JEI 标准的配方收藏按钮），为该 tag 收藏这个成员。
4. 某个 tag 有了收藏成员后，使用该 tag 的普通配方输入槽会显示这个成员。
5. 在**书签栏**：`R` / 左键 → 标签配方预览；`U` / 右键 → 所存物品的用途。

### 说明

- JEI 用配方 id 区分配方书签，而标签配方的 id **就是这个 tag**。因此**每个 tag 只能有一条配方书签**。对已经收藏过的 tag 再点另一个成员不会替换书签；需要先在书签栏（或标签页的配方收藏按钮）删掉，再收藏想要的成员。
- 显示钉跟随实时书签列表，下一拍物品循环就会更新。点击行为在布局**构建时**就定下来，所以已经打开的配方页会保持旧的点击逻辑，直到 JEI 重建布局（换页，或关掉再开）。
- 在标签页上对**成员输出槽**按 `U` / 右键，会留在该标签配方（并聚焦到该成员），不会打开用途。查用途请走书签栏或 JEI 物品列表。
- 从标签书签 Shift 点击转移会先试聚焦成员布局，失败再回退到未聚焦的完整标签布局。标签信息配方通常没有转移 handler，所以多数情况下不会发生转移。

## 安装

将对应加载器的 jar 放入 `mods`，与 JEI 一起使用（Fabric 还需 Forge Config API Port）。

## 配置

客户端配置：Forge 写入 `config/justenoughtaglib-client.toml`；Fabric 经 Forge Config API Port 写入同一文件。

- `hideJeiBlockTagRecipes`（默认 `true`）：隐藏方块标签配方分类，设为 `false` 恢复。

## 工作原理

JEI 会把 tag 原料展开成成员列表，槽位可点击时标签身份已经丢失。对**普通配方**，本 Mod 在布局构建完成后对每个输入槽调用 `IIngredientHelper#getTagKeyEquivalent`，并把「槽 → tag」缓存在该 `RecipeLayout` 上（`TagSlotTracker`）。标签配方页（`ITagInfoRecipe`）不走这套跟踪：点击由书签 mixin 处理，整 tag 输入槽保持 JEI 原有循环。

两件事刻意分开：

- **点击**用的是布局构建时的快照。在已经打开的页面上增删书签，不会改写这一页的点击。
- **显示**每个 JEI 物品循环都按实时书签表重新钉一次，所以新收藏会在下一拍循环出现，不必重建页面。

两条路径都经过同一个决策点（`TagSlotTracker.decideTagBehavior`）：钉住所选成员、跳到标签列表，或交给 JEI。

## 许可证

[LGPLv3](LICENSE)
