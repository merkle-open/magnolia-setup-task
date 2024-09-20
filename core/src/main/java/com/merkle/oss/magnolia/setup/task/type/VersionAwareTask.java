package com.merkle.oss.magnolia.setup.task.type;

import info.magnolia.module.delta.Task;
import info.magnolia.module.model.Version;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.annotation.Nullable;

public interface VersionAwareTask extends Task, BiPredicate<Version, Version> {

	@Override
	default boolean test(final Version forVersion, @Nullable final Version fromVersion) {
		return true;
	}

	default Optional<VersionAwareTask> dependsOn() {
		return Optional.empty();
	}
}
