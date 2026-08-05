package com.justenoughtaglib;

import mezz.jei.api.JeiPlugin;

/**
 * Forge 端触发类：仅负责让 JEI 通过 @JeiPlugin 注解发现插件，
 * 逻辑全部在 {@link JustEnoughTagLibJeiPlugin} 里。
 */
@JeiPlugin
public final class ForgeJeiPlugin extends JustEnoughTagLibJeiPlugin {
}
