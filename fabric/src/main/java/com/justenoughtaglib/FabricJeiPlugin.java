package com.justenoughtaglib;

import mezz.jei.api.JeiPlugin;

/**
 * Fabric 端触发类：仅负责让 JEI 通过 fabric.mod.json 的
 * "jei_mod_plugin" 入口点发现插件，逻辑全部在 {@link JustEnoughTagLibJeiPlugin} 里。
 */
@JeiPlugin
public final class FabricJeiPlugin extends JustEnoughTagLibJeiPlugin {
}
