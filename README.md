# JustEnoughTagLib

A **Minecraft 1.20.1 + JEI** client-side item tag recipe enhancement mod, supporting both **Forge** and **Fabric**.

[简体中文](./README_zhs.md)

## Features

- **Force-enable tag recipe pages**: JEI's item tag recipe category (recipe type like `minecraft:tag_recipes/item`, e.g. viewing which items `#minecraft:planks` contains) is only enabled during development by default. This mod overrides `ClientConfig#isShowTagRecipesEnabled()` to keep it always enabled in production. (`MixinClientConfig`)
- **Hide block tag recipes by default**: The less useful "block tag recipe" category (`minecraft:tag_recipes/block`) is hidden by default and can be restored by setting the client config `hideJeiBlockTagRecipes` to `false`. (`JustEnoughTagLibJeiPlugin`)
- **Click un-bookmarked tag input to jump**: Clicking a tag-constructed input slot that has no bookmarked preference for that tag jumps directly to the corresponding tag recipe page; once a tag is bookmarked, its inputs fall back to JEI's normal item recipes/usages. (`TagRecipeJumpElement` / `MixinRecipeGuiLayouts`)
- **Bookmark tag-recipe output slots**: The output slot of a tag recipe can be bookmarked as a JEI **recipe bookmark** directly by clicking it, and it appears in the bookmark bar with JEI's native bookmark styling. (`RecipeContextElement` / `MixinBookmarkList`)
- **Fix wrong item in tag recipe bookmarks**: Bookmarking a tag recipe when entering from a specific item records that **focused item**, not the first member of the tag list. (`MixinRecipeBookmark`)
- **Narrow display after bookmark reload**: In recipes whose input slot resolves to a bookmarked tag, that slot displays the bookmarked item as a display-only override (the slot's underlying tag member list is untouched), and the override is re-applied whenever JEI cycles ingredients. (`TagBookmarkPreferences` / `TagSlotTracker` / `MixinRecipeLayoutBuilder` / `MixinRecipeLayout`)
- **Full bookmark interaction**: Tag recipe bookmarks support normal recipe/usages queries (R/U and right-click), as well as JEI recipe transfer directly from the bookmark; when the focused member cannot be transferred it automatically falls back to the full tag recipe. (`MixinRecipeBookmarkElement` / `TransferLayoutPolicy`)
- **Clearer bookmark tooltips**: The tag-recipe bookmark tooltip shows the stored item's name and the recipe category line instead of a generic ingredient tooltip. (`MixinRecipeBookmarkElement`)

## How It Works

Tag identity is lost when JEI expands a tag ingredient into its member item list. The mod resolves the tag of each input slot after the layout has been built (`MixinRecipeLayoutBuilder`): for tag-recipe categories it takes the tag directly from the recipe (`ITagInfoRecipe#getTag`), otherwise it recovers it from the slot's member list via `IIngredientHelper#getTagKeyEquivalent`. The resulting slot → tag mapping is cached per `RecipeLayout` in a `WeakHashMap` (`TagSlotTracker`), so clicks, tooltips, bookmark narrowing, and transfer all use the **current layout's own data** and never go stale across pages or recipes.

## Installation

Place the jar for the corresponding loader into the `mods` directory, and use it alongside JEI.

## Configuration

Client config (Forge's `config/justenoughtaglib-client.toml`, Fabric via `Forge Config API Port`1`):

- `hideJeiBlockTagRecipes` (default `true`): hides the block tag recipe category; set to `false` to restore it.

## Usage

- In-game, the tag category (`tag_recipes/item`) appears in JEI's left category bar.
- Clicking a tag input slot that has no bookmarked preference in a normal recipe jumps to the corresponding tag recipe page.
- To bookmark a single tag recipe, click its **output slot** to add it as a recipe bookmark, or use JEI's standard recipe-bookmark button.
- Once a specific item is bookmarked for a tag, recipe input slots using that tag display the bookmarked item.
- Clicking `U` / right-clicking a tag-recipe bookmark opens the item's usages; `R`/left-click opens the recipe preview; Shift-click transfers it directly.

## License

[LGPLv3](LICENSE)
