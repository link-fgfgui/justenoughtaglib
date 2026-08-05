package com.justenoughtaglib;

import mezz.jei.api.JeiPlugin;

/**
 * Fabric-side entry point: only allows JEI to discover the plugin through the
 * "jei_mod_plugin" entry point in fabric.mod.json; all logic is implemented in
 * {@link JustEnoughTagLibJeiPlugin}.
 */
@JeiPlugin
public final class FabricJeiPlugin extends JustEnoughTagLibJeiPlugin {
}
