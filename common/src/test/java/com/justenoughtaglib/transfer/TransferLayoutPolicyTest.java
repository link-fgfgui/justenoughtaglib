package com.justenoughtaglib.transfer;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferLayoutPolicyTest {
	@Test
	void preferredTransferLayoutWinsWhenAllowed() {
		Object preferred = new Object();
		boolean[] fallbackCalled = {false};
		Supplier<Optional<Object>> fallback = () -> {
			fallbackCalled[0] = true;
			return Optional.of(new Object());
		};
		Predicate<Object> allowed = value -> value == preferred;

		Optional<Object> selected = TransferLayoutPolicy.selectPreferredTransferLayout(
			preferred,
			fallback,
			allowed
		);

		assertTrue(selected.isPresent());
		assertSame(preferred, selected.orElseThrow());
		assertFalse(fallbackCalled[0]);
	}

	@Test
	void disallowedPreferredLayoutUsesAllowedFallback() {
		Object preferred = new Object();
		Object fallbackLayout = new Object();
		boolean[] fallbackCalled = {false};
		Supplier<Optional<Object>> fallback = () -> {
			fallbackCalled[0] = true;
			return Optional.of(fallbackLayout);
		};
		Predicate<Object> allowed = value -> value == fallbackLayout;

		Optional<Object> selected = TransferLayoutPolicy.selectPreferredTransferLayout(
			preferred,
			fallback,
			allowed
		);

		assertTrue(selected.isPresent());
		assertSame(fallbackLayout, selected.orElseThrow());
		assertTrue(fallbackCalled[0]);
	}

	@Test
	void disallowedPreferredAndFallbackLayoutsProduceEmptySelection() {
		Object preferred = new Object();
		Object fallbackLayout = new Object();
		boolean[] fallbackCalled = {false};
		Supplier<Optional<Object>> fallback = () -> {
			fallbackCalled[0] = true;
			return Optional.of(fallbackLayout);
		};
		Predicate<Object> allowed = value -> false;

		Optional<Object> selected = TransferLayoutPolicy.selectPreferredTransferLayout(
			preferred,
			fallback,
			allowed
		);

		assertTrue(selected.isEmpty());
		assertTrue(fallbackCalled[0]);
	}
}
