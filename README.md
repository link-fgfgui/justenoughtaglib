# JustEnoughTagLib

A **Minecraft 1.20.1 + JEI** client-side tag-recipe enhancement, for **Forge** and **Fabric**.

[简体中文](./README_zhs.md)

JEI only enables item/fluid tag recipe pages in a development environment. This mod keeps those pages available on a normal client, lets you bookmark one member of a tag, and pins ordinary recipe inputs that resolve to that tag to the bookmarked member.

Requires [JEI](https://github.com/mezz/JustEnoughItems) **15.21.0.148 or newer**. Fabric also needs [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port). Client-side only.

## Features

- **Force-enable tag recipe pages**: JEI's tag recipe category (`minecraft:tag_recipes/item`, e.g. which items `#minecraft:planks` contains) stays enabled outside of development. (`MixinClientConfig`)
- **Hide block tag recipes by default**: `minecraft:tag_recipes/block` is hidden; set `hideJeiBlockTagRecipes` to `false` to restore it. (`JustEnoughTagLibJeiPlugin`)
- **Jump from unbound tag inputs**: In an ordinary recipe, clicking a tag-equivalent input that has no bookmark for that tag opens the matching tag page. After the tag is bookmarked, **newly built** layouts treat that slot as a normal item for R/U. (`TagRecipeJumpElement` / `MixinRecipeGuiLayouts`)
- **Bookmark a tag member from the tag page**: Click a tag recipe **output slot** (a member) to add a JEI **recipe bookmark**, shown with JEI's native recipe-bookmark styling. (`RecipeContextElement` / `MixinBookmarkList`)
- **Bookmark the focused output**: Bookmarking a recipe opened from a specific output — a tag member, or any multi-output recipe — stores that **focused item**, not the first output slot. (`MixinRecipeBookmark`)
- **Pin the bookmarked member in ordinary recipes**: When an ordinary recipe input is tag-equivalent to a bookmarked tag, the slot **displays** the bookmarked member as a display-only override (the underlying member list is untouched). The pin is re-applied every JEI ingredient cycle. (`TagBookmarkPreferences` / `TagSlotTracker` / `MixinRecipeLayoutBuilder` / `MixinRecipeLayout`)
- **Tag-bookmark clicks from the bookmark bar**: `R` / left-click opens the tag recipe focused on the stored member; `U` / right-click opens that member's usages. (`MixinRecipeBookmarkElement`)
- **Shorter recipe-bookmark tooltips**: The tooltip shows the stored item name as the title and the recipe category line at the bottom, instead of the full ingredient tooltip. (`MixinRecipeBookmarkElement`)

## Usage

1. In-game, the item tag category (`tag_recipes/item`) appears in JEI's left category bar.
2. In a normal recipe, click an unbound tag input → jump to that tag's page.
3. On the tag page, click a **member output** (or JEI's recipe-bookmark button) to bookmark that member for the tag.
4. Once a tag has a bookmarked member, ordinary recipe inputs for that tag show that member.
5. From the **bookmark bar**: `R` / left-click → tag recipe preview; `U` / right-click → the stored item's usages.

### Notes

- JEI identifies recipe bookmarks by recipe id, and a tag recipe's id **is the tag**. There is **one recipe bookmark per tag**. Clicking another member of an already-bookmarked tag does not replace the bookmark; remove it first (bookmark bar, or the recipe-bookmark button on the tag page), then bookmark the member you want.
- Display pinning follows the live bookmark list on the next ingredient cycle. Click handling is decided when the layout is **built**, so a recipe page that is already open keeps its old click behavior until JEI rebuilds it (change page, or close and reopen).
- On the tag page itself, `U` / right-click on a **member output** stays on that tag recipe (focused on the member). Look up that item's usages from the bookmark bar or the ingredient list.
- Shift-click transfer from a tag bookmark is attempted, and falls back from the focused-member layout to the unfocused tag layout when the focused one cannot transfer. Tag info recipes usually have no transfer handler, so this often does nothing.

## Installation

Place the jar for your loader in `mods`, next to JEI (and Forge Config API Port on Fabric).

## Configuration

Client config: Forge writes `config/justenoughtaglib-client.toml`; Fabric writes the same file through Forge Config API Port.

- `hideJeiBlockTagRecipes` (default `true`): hide the block tag recipe category; set to `false` to restore it.

## How It Works

JEI expands a tag ingredient into its member list, so the tag identity is gone by the time a slot is clickable. After an **ordinary** recipe layout is built, this mod asks `IIngredientHelper#getTagKeyEquivalent` for each input slot and caches slot → tag on that `RecipeLayout` (`TagSlotTracker`). Tag-recipe pages (`ITagInfoRecipe`) are not tracked this way: their clicks go through the bookmark mixins, and their whole-tag input slot keeps JEI's native cycling.

Two decisions are kept apart:

- **Clicks** use a snapshot taken when the layout was built. Adding or removing a bookmark does not rewire clicks on an already-open page.
- **Display** is re-pinned every JEI ingredient cycle from the live bookmark map, so a newly added bookmark appears on the next cycle without rebuilding the page.

Both flows go through one decision point (`TagSlotTracker.decideTagBehavior`): pin the bookmarked member, jump to the tag listing, or leave the slot to JEI.

## License

[LGPLv3](LICENSE)
