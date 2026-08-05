package com.justenoughtaglib;

import mezz.jei.api.JeiPlugin;

/**
 * Forge-side entry point: only allows JEI to discover the plugin through the
 * {@code @JeiPlugin} annotation; all logic is implemented in
 * {@link JustEnoughTagLibJeiPlugin}.
 */
@JeiPlugin
public final class ForgeJeiPlugin extends JustEnoughTagLibJeiPlugin {
}
