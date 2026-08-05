package com.justenoughtaglib.transfer;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class TransferLayoutPolicy {
	private TransferLayoutPolicy() {
	}

	public static <L> Optional<L> selectPreferredTransferLayout(
		L preferred,
		Supplier<Optional<L>> fallback,
		Predicate<L> allowed
	) {
		if (allowed.test(preferred)) {
			return Optional.of(preferred);
		}
		return fallback.get().filter(allowed);
	}
}
