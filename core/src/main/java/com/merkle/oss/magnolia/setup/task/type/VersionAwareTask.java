package com.merkle.oss.magnolia.setup.task.type;

import info.magnolia.module.InstallContext;
import info.magnolia.module.delta.Task;
import info.magnolia.module.model.Version;

import java.util.Optional;

import javax.annotation.Nullable;

public interface VersionAwareTask extends Task {

	default boolean test(final InstallContext installContext, final Version forVersion, @Nullable final Version fromVersion) {
		return true;
	}

	default Optional<VersionAwareTask> dependsOn() {
		return Optional.empty();
	}
}
