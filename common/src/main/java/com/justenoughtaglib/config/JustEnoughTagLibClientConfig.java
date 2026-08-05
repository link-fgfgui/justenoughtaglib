package com.justenoughtaglib.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class JustEnoughTagLibClientConfig {
	public static final ForgeConfigSpec SPEC;
	public static final ForgeConfigSpec.BooleanValue HIDE_JEI_BLOCK_TAG_RECIPES;

	static {
		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		HIDE_JEI_BLOCK_TAG_RECIPES = builder
			.comment("Hide JEI's block tag recipe category.")
			.define("hideJeiBlockTagRecipes", true);
		SPEC = builder.build();
	}

	private JustEnoughTagLibClientConfig() {
	}
}
