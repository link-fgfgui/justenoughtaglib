package com.justenoughtaglib.mixin;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.gui.bookmarks.BookmarkList;
import mezz.jei.gui.bookmarks.RecipeBookmark;
import mezz.jei.gui.elements.GuiIconToggleButton;
import mezz.jei.gui.input.IUserInputHandler;
import mezz.jei.gui.input.UserInput;
import mezz.jei.gui.recipes.RecipeBookmarkButton;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

/**
 * Allows pressing the bookmark key ('A') while hovering over the recipe bookmark button
 * in the bottom right corner of a recipe layout to add or remove the recipe bookmark.
 */
@Mixin(RecipeBookmarkButton.class)
public abstract class MixinRecipeBookmarkButton extends GuiIconToggleButton {
	@Shadow(remap = false)
	@Final
	private BookmarkList bookmarks;

	@Shadow(remap = false)
	@Final
	@Nullable
	private RecipeBookmark<?, ?> recipeBookmark;

	protected MixinRecipeBookmarkButton(IDrawable offIcon, IDrawable onIcon) {
		super(offIcon, onIcon);
	}

	@Override
	public IUserInputHandler createInputHandler() {
		IUserInputHandler original = super.createInputHandler();
		return new IUserInputHandler() {
			@Override
			public Optional<IUserInputHandler> handleUserInput(Screen screen, UserInput input, IInternalKeyMappings keyBindings) {
				if (recipeBookmark != null && button.active && button.visible && isMouseOver(input.getMouseX(), input.getMouseY())) {
					if (input.is(keyBindings.getBookmark())) {
						if (!input.isSimulate()) {
							bookmarks.toggleBookmark(recipeBookmark);
							tick();
						}
						return Optional.of(this);
					}
				}
				return original.handleUserInput(screen, input, keyBindings);
			}

			@Override
			public Optional<IUserInputHandler> handleMouseScrolled(double mouseX, double mouseY, double scrollDelta) {
				return original.handleMouseScrolled(mouseX, mouseY, scrollDelta);
			}

			@Override
			public void unfocus() {
				original.unfocus();
			}
		};
	}
}
