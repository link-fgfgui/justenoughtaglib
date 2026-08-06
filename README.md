# JustEnoughTagLib

A **Minecraft 1.20.1 + JEI** client-side item tag recipe enhancement mod, supporting both **Forge** and **Fabric**.

[简体中文](./README_zhs.md)

## Features

- **Force-enable tag recipe pages**: JEI's item tag recipe category (recipe type like `minecraft:tag_recipes/item`, e.g. viewing which items `#minecraft:planks` contains) is only enabled during development by default. This mod overrides `ClientConfig#isShowTagRecipesEnabled()` to keep it always enabled in production. (`MixinClientConfig`)
- **Hide block tag recipes by default**: The less useful "block tag recipe" category (`minecraft:tag_recipes/block`) is hidden by default and can be restored by setting the client config `hideJeiBlockTagRecipes` to `false`. (`JustEnoughTagLibJeiPlugin`)
- **Click tag input slot to jump**: Clicking a tag-constructed input slot in a normal recipe jumps directly to the corresponding tag recipe page instead of merely looking up recipes by item. (`TagRecipeJumpElement` / `MixinRecipeGuiLayouts`)
- **Bookmark recipe output slots**: Recipe output slots (not just items) can be bookmarked as JEI **bookmarks**. Click an output slot to add it to the JEI bookmark bar, with a star overlay and a "recipe category" tooltip. (`RecipeContextElement` / `MixinBookmarkList` / `MixinRecipeBookmarkElement`)
- **Fix wrong item in tag recipe bookmarks**: Bookmarking after entering a tag page from a specific item records that **focused item**, not the first member of the tag list. (`MixinRecipeBookmark`)
- **Narrow display after bookmark reload**: After reloading bookmarks, recipe input slots using the same tag prefer showing the bookmarked item while keeping the original tag name and full member list in the tooltip. (`TagBookmarkPreferences` / `MixinTagInfoRecipeCategory` / `MixinRecipeSlotBuilder` / `TagTooltipHelper`)
- **Full bookmark interaction**: Tag recipe bookmarks support normal recipe/usages queries (R/U and right-click), as well as JEI recipe transfer directly from the bookmark; when the focused member cannot be transferred it automatically falls back to the full tag recipe. (`MixinRecipeBookmarkElement` / `TransferLayoutPolicy`)
- **Clearer tooltips**: The recipe bookmark tooltip shows the item name and recipe category, and uses JEI's native bookmark icon (half star) as the overlay.

## How It Works

Tag identity is lost when `Ingredient#getItems()` expands (only the item list remains). While layouts are being built, the mod intercepts the return of `getItems()` through each loader module's `MixinIngredient`, recovers the tag source via `Ingredient#toJson()`, and associates the "expansion → tag" mapping with the `RecipeLayout` currently being built (`TagSlotTracker`). This way clicks, tooltips, bookmark narrowing, and transfer can all be determined precisely from the **current layout's own data**, avoiding stale hits across pages and recipes.

## Dependencies & Versions

| Dependency | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| JEI | >= 15.21.0.148 |
| Forge | 47.2.30 (FML version range `[47,)`) |
| Fabric | Fabric API >= 0.92.11, Fabric Loader >= 0.16.10, Forge Config API Port >= 8.0.3 |

## Building

```bash
./gradlew build
```

Artifacts are output to `fabric/build/libs` and `forge/build/libs` respectively. Run the client: `fabric:runClient` / `forge:runClient`.

## Installation

Place the jar for the corresponding loader into the `mods` directory, and use it alongside JEI.

## Configuration

Client config (Forge's `forge-client.toml`, Fabric via Forge Config API Port):

- `hideJeiBlockTagRecipes` (default `true`): hides the block tag recipe category; set to `false` to restore it.

## Usage & Bookmark Files

- In-game, the tag category (`tag_recipes/item`) appears in JEI's left category bar.
- Clicking a tag input slot in a normal recipe jumps to the corresponding tag recipe page.
- After pressing `U` on a focused item in the tag recipe page, hover the top-right corner of the recipe card and click the star to bookmark it.
- After bookmarking a different item for a tag, recipe inputs using the same tag automatically narrow to the bookmarked item, while still showing the tag name and full members on hover.

Recipe bookmark files are saved per-world at:

```
config/jei/world/local/<world name>/bookmarks.ini
```

(In multiplayer, this is `config/jei/world/server/<name>_<hash>/`; spaces in world names are replaced with underscores, e.g. `New World` → `New_World`.)

## License

[LGPLv3](LICENSE)
