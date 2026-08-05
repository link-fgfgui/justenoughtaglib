package com.justenoughtaglib.mixin;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class TransferLayoutPolicy {
	private TransferLayoutPolicy() {
	}

	static <L> Optional<L> justenoughtaglib$selectPreferredTransferLayout(
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
