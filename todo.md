# JEI TagLib 改造待办

> 完成度参考最新代码（`git log` 仅一条 initial commit，分析依据为仓库现状）。
> 自动化验证按 `AGENTS.md` 限制：仅 Gradle 构建 / 编译期 mixin 校验 / Java 单元测试；JEI 行为验证由用户按手册人工执行。

## 1. u 键 / 右键操作收藏的配方时，打开对应物品的用途界面 — ✅ 已完成

- 实现：`common/.../mixin/MixinRecipeBookmarkElement.java:83` `justenoughtaglib$showFocusedTagRecipe`
  - 拦截 `RecipeBookmarkElement.show()`；`roles` 含 `INPUT`（U / 右键查用途）→ `recipesGui.show(focuses)`，列出该物品的用途配方；否则 `showRecipes(分类, 配方, focuses)`，固定焦点。
- 输入路径补充：
  - `MixinRecipeBookmark.java:34` 让 `RecipeBookmark.create` 用 focus 物品而非 tag 首成员写书签。
  - `MixinBookmarkList.java:35` 把 `onElementBookmarked` 上的输出槽点击转成书签。
  - `MixinRecipeGuiLayouts.java:39` 通过 `RecipeContextElement` 把布局上下文挂回点击事件。
- 验证：单元测试 `TagBookmarkFocusTest`（聚焦/非聚焦/角色不一致/陈旧值）；人工按 `AGENTS.md` 手册跑 `fabric:runClient` + `jungle_planks` U 操作。

## 2. 按住 Shift 悬停相关显示还需完善 — ⏳ 未完成

- 现状：`MixinRecipeBookmarkElement.java:117-155` 显式保留 JEI 原生 tooltip 流程（Shift 预览/转移提示按 additive 处理），并用书签标题/分类替换两条 JEI 调用。
- 缺口：未实现 Shift 触发的扩展内容（如显示 tag 全名 / tag 成员列表 / 配方 UID 等 mod 特有信息）。当前实现只"让原版 Shift 行为继续工作"，没有"mod 自己的 Shift 增强"。
- 后续：定位 `RecipeBookmarkElement.getTooltip` 中 JEI 的 Shift 分支（持有 `getItemStacks` / tag 信息），在保留 JEI 文案基础上追加 mod 行。
- 验证：纯客户端渲染，自动化仅能做到 mixin 注入不崩；具体视觉需用户进游戏按 Shift 悬停截图核对。

## 3. Tooltip 中通过 `TagUtil.getTagEquivalent()` 反查"是否恰好对应某个 tag" — ❓ 设计讨论 / 待决

- 现状：仓库内**无** `TagUtil.getTagEquivalent()` 引用（`grep` 无结果）。当前实现走的是"构建期捕获"路径：
  - `TagSlotTracker`（`common/.../tag/TagSlotTracker.java`）在 `Ingredient.getItems()` 展开后、布局构建窗口内用 `Ingredient.toJson()` 恢复 tag。
  - 关联到 `RecipeLayout`（`MixinRecipeLayoutBuilder`）后，点击时 `MixinRecipeGuiLayouts` 通过 `TagSlotTracker.findTag(layout, slotStacks)` 精确查询。
  - **不是反查**，是构建期正向捕获。
- 待决问题（来自截图）：用户问"为什么 JEI 这么做，能不能改成直接读源配方内容，而不是反查"。在当前实现里其实已经是正向捕获，但截图里描述的"反查"思路属于 JEI 上游的设计——是否要继续走 JEI 的反查接口（API 兼容更好）还是坚持自建捕获（精确，但依赖私有字段），需要在 1.20.1 + JEI 19.x 兼容性窗口里再评估。
- 后续动作：开会 / 写 design note 决定方向后，再决定是否重构 `TagSlotTracker` 走 `getTagEquivalent()` 路径或保持现状。

## 4. 已有对应 tag recipe bookmark 后 — ⏳ 未完成（含两个子项）

### 4.1 不再在初始化对应 tag 的 slot 时放入所有 tag 物品（不再让 slot 一查网）

- 现状：`MixinTagInfoRecipeCategory.java:25-56` 仅在**有 focus 且 `TagBookmarkFocus.find` 命中**时把布局压缩成"单 input + 单 output，都是 focus 成员"，其余情况放行原 `setRecipe`（仍会铺所有 tag 成员）。
- 缺口：从 tag 书签进入（`MixinRecipeBookmarkElement.show` 用 `showRecipes(...focuses...)`）时已经携带 focus，理论上能命中压缩路径——但**直接浏览 tag 分类页**（无 focus）依然铺全员。
- 后续：
  - 把"无 focus 也压缩为单槽"挪到 layout 构建期之外是不可能的（`TagInfoRecipeCategory` 拿不到书签上下文）；
  - 可行做法：在 `MixinRecipeBookmarkElement.show` / `MixinRecipeGuiLayouts` 处提前判断"展示来源是书签 → 该 tag → 已存在"时，直接 `show` 自定义布局（仅 bookmark 焦点物品），绕过原 `setRecipe`。
- 验证：人工跑书签 → tag 页面 → 检查 slot 数（应为 2，而非 1 + 1 + N 成员循环）；自动化只能 mixin 注入不崩。

### 4.2 转移配方时优先使用 bookmark 的显示物品转移

- 现状：`MixinRecipeBookmarkElement.java:177-241` `justenoughtaglib$tryTransferRecipe` / `justenoughtaglib$selectTransferLayout`：
  - 非 tag：直接用 `focusedLayout`。
  - tag：调 `TransferLayoutPolicy.selectPreferredTransferLayout`——`preferred` 是当前聚焦布局（`focusedLayout`），fallback 是无 focus 的同 recipe 布局。
  - 没有"用 bookmark 显示物品"作为额外优先项；只是"focused 优先，unfocused 兜底"。
- 缺口：当用户点的是"我之前书签里收藏的那一份"（recipe output 已被 mixin 改成 `recipeBookmark.getRecipeOutput()`），但当前窗口里看到的 focus 是另一个 tag 成员时，转移的还是 focus 对应的物品，而不是书签"当初收藏的那个"。
- 后续：在 `justenoughtaglib$selectTransferLayout` 里把 `recipeBookmark.getRecipeOutput()` 包装成第三个候选：若 preferred（focused）/ fallback（unfocused）都不被 transfer 接受，再尝试"用 bookmark output 重建的 layout"；仍然 `TransferLayoutPolicy` 串联判定。
- 验证：人工按 `AGENTS.md` 手册在 `crafting` UI 里点 tag 书签左键——预期填充的是书签里的物品（修复前会填当前 focus 的物品）。

## 5. 下一步（占位）— ⏳ 待拆解

- 待 4.x 完成后再开新清单。建议方向：
  - 把 `TagRecipeJumpElement` / `RecipeContextElement` / `MixinRecipeGuiLayouts` 三处的跳转路径收口到单一工具类（避免重复的 `getRecipeCategory().getRecipeType().getUid().getPath().startsWith("tag_recipes/")` 判定）。
  - 为 `TagSlotTracker` 增加对 `custom.Ingredient` 子类（如 JEI 自定义序列化）的兜底 `toJson()` 失败处理已经存在，但需要单测覆盖。
  - 探索 JEI 上游 `ITagInfoRecipe` 是否有更稳定的 API（如未来的 `getTagKey()`），把 `tag_recipes/...` 路径前缀硬编码换成 uid 注入。

---

## 跟踪矩阵

| 编号 | 功能 | 状态 | 关键文件 | 自动化验证 | 人工验证 |
|---|---|---|---|---|---|
| 1 | U/右键开用途 | ✅ | `MixinRecipeBookmarkElement#show` | `TagBookmarkFocusTest` | AGENTS.md 手册 |
| 2 | Shift 悬停增强 | ⏳ | `MixinRecipeBookmarkElement#getTooltip` | 仅 mixin 注入 | 需进游戏 |
| 3 | 反查 vs 正向捕获 | ❓ | `TagSlotTracker` / `MixinTagInfoRecipeCategory` | 单测已覆盖正向路径 | N/A（设计讨论）|
| 4.1 | slot 不再放全员 | ⏳ | `MixinTagInfoRecipeCategory#setFocusedRecipe` | mixin 注入 | 需进游戏 |
| 4.2 | 转移用 bookmark 物品 | ⏳ | `MixinRecipeBookmarkElement#selectTransferLayout` | mixin 注入 | AGENTS.md 手册（需扩） |
| 5 | 下一步 | ⏳ | — | — | — |
