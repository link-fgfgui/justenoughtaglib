# AGENTS.md

## 测试范围限制（强制）

**自动化测试仅限 Java 层。** 允许的自动化验证手段：

- Gradle 构建
- 编译期 mixin AP 校验、refmap 检查、jar 内 class/资源清单检查
- `javap` 字节码/常量池静态检查
- Java 单元测试

**禁止**作为测试手段：

- 操作系统级 UI 自动化：模拟鼠标/键盘注入（SendInput/keybd_event/mouse_event）、
  截图像素定位、PowerShell 控制游戏窗口等，一律不得用于验证。
- 任何未与用户确认的自动化游戏客户端启动（含 `runClient` 冒烟）。

**高级测试必须人工介入。** 需要启动游戏客户端并在游戏内操作的验证
（例如 JEI tag recipe 书签行为），由用户手动执行，助手只负责：
给出精确的操作步骤与预期结果 → 用户操作完成 → 助手检查产物文件。

## 本项目高级测试操作手册（人工执行）

验证"tag recipe 书签写入聚焦物品"：

1. 启动客户端：`fabric:runClient`
2. 进世界 → 点 JEI 底部搜索框输入 `jungle_planks` → 点结果 → 按 `U`
3. 左侧分类栏点 tag 分类（`tag_recipes/item`）→ 悬停配方卡片右上角点星标收藏
4. 退出游戏，检查 `fabric/run/config/jei/world/local/<世界>/bookmarks.ini`
5. 预期出现 `R:minecraft:tag_recipes/item#minecraft:planks#item_stack&minecraft:jungle_planks`
   （修复前会写 tag 首成员 `minecraft:oak_planks`）

已知事实（供核对）：
- 书签文件为每世界路径：`config/jei/world/local/<世界名>/bookmarks.ini`（多人为 `world/server/<名>_<hash>/`）
- 世界名会做路径净化：空格 → 下划线（如 `New World` → `New_World`）
- JEI 15.21.0.148 自身 fabric.mod.json 要求 `fabric-api >= 0.92.2+1.20.1`、`fabricloader >= 0.16.3`；本仓库 gradle.properties 构建用 `jei_version=15.21.0.148`、`fabric_api_version=0.92.11`、`fabric_loader_version=0.16.10`
- fabric.mod.json 依赖在内联用 `>=` 写法，无独立 range 属性（如 `"fabricloader": ">=0.14"`、`"jei": ">=${jei_version}"`，相应 file 位于 `fabric/src/main/resources/fabric.mod.json`）；forge 的 `mods.toml` 才用 `[${jei_version},)` 区间
