# JEI TagLib 改造待办

## 2. tooltip修改

- [x] `MixinRecipeBookmarkElement.java:117-155` 显式保留 JEI 原生 tooltip 流程（Shift 预览/转移提示按 additive 处理），并用书签标题/分类替换两条 JEI 调用。

## 3. Tooltip 中通过 `TagUtil.getTagEquivalent()` 反查"是否恰好对应某个 tag" — ❓ 设计讨论 / 待决

- 现状：仓库内**无** `TagUtil.getTagEquivalent()` 引用（`grep` 无结果）。当前实现走的是"构建期捕获"路径：
  - `TagSlotTracker`（`common/.../tag/TagSlotTracker.java`）在 `Ingredient.getItems()` 展开后、布局构建窗口内用 `Ingredient.toJson()` 恢复 tag。
  - 关联到 `RecipeLayout`（`MixinRecipeLayoutBuilder`）后，点击时 `MixinRecipeGuiLayouts` 通过 `TagSlotTracker.findTag(layout, slotStacks)` 精确查询。
  - **不是反查**，是构建期正向捕获。

## 4. 已有对应 tag recipe bookmark 后 — ⏳ 未完成（含两个子项）

### 4.1 tag recipe bookmark 为 tag 绑定具体物品 — ✅ 已完成

- bookmark 存在时，JEI 布局中的对应 tag Ingredient 实际收窄为 bookmark 指定物品；显示、R/U、缺料判断和转移读取同一份单物品槽数据。
- tag 身份和完整成员由 `TagSlotTracker.TagSlotData` 独立保留，收窄后的槽仍追加原 tag 名称与成员 tooltip。
- tag recipe 页面顶部 input slot 只放绑定物品，底部 output 网格继续完整陈列全部 tag 成员；无 bookmark 时保持 JEI 原始布局。

### 4.2 转移配方时优先使用 bookmark 的显示物品转移
