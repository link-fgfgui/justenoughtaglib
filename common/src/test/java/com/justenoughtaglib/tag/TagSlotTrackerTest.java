package com.justenoughtaglib.tag;

import mezz.jei.api.ingredients.IIngredientType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.justenoughtaglib.tag.TagSlotTracker.SlotBehavior.INERT;
import static com.justenoughtaglib.tag.TagSlotTracker.SlotBehavior.JUMP;
import static com.justenoughtaglib.tag.TagSlotTracker.SlotBehavior.NARROW;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the single tag-slot decision point. Deliberately no JEI/Minecraft
 * runtime: {@code decideTagBehavior} only looks at booleans and the member count, so a
 * {@link List} of strings stands in for real ingredients. The tag and ingredient-type
 * components of {@link TagSlotTracker.TagSlotData} stay {@code null} — the decision
 * never reads them, and a real {@link net.minecraft.tags.TagKey} cannot be built without
 * a bootstrapped game registry.
 */
class TagSlotTrackerTest {

	private static TagSlotTracker.TagSlotData data(int memberCount, boolean preferred) {
		List<String> members = switch (memberCount) {
			case 0 -> List.of();
			case 1 -> List.of("only");
			default -> List.of("first", "second");
		};
		return new TagSlotTracker.TagSlotData(null, null, members, preferred);
	}

	@Test
	void missingDataIsInert() {
		assertEquals(INERT, TagSlotTracker.decideTagBehavior(null, false, false));
	}

	@Test
	void preferredMemberNarrowsOnlyWhenSlotUnclaimed() {
		assertEquals(NARROW, TagSlotTracker.decideTagBehavior(data(2, false), false, true));
		assertEquals(INERT, TagSlotTracker.decideTagBehavior(data(2, false), true, true));
	}

	@Test
	void unboundMultiMemberTagJumpsRegardlessOfCategory() {
		// Clicks are a pure interaction: a category-owned display does not suppress them.
		assertEquals(JUMP, TagSlotTracker.decideTagBehavior(data(2, false), false, false));
		assertEquals(JUMP, TagSlotTracker.decideTagBehavior(data(2, false), true, false));
	}

	@Test
	void singleMemberTagIsNotAUsefulJump() {
		assertEquals(INERT, TagSlotTracker.decideTagBehavior(data(1, false), false, false));
		assertEquals(INERT, TagSlotTracker.decideTagBehavior(data(0, false), false, false));
	}
}