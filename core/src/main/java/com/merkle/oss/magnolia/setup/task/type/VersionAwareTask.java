package com.merkle.oss.magnolia.setup.task.type;

import info.magnolia.module.InstallContext;
import info.magnolia.module.delta.Task;
import info.magnolia.module.model.Version;

import java.util.Optional;

import jakarta.annotation.Nullable;

public interface VersionAwareTask extends Task {

	default boolean test(final InstallContext installContext, final Version forVersion, @Nullable final Version fromVersion) {
		return true;
	}

    /**
     * Defines task that will be executed before this one.
     * <br>
     * <b>Be aware:</b><br>
     * JCR queries run on persisted content. Unsaved modifications in the current session are not considered! (e.g. unsaved modifications in earlier executed setup tasks)
     */
	default Optional<VersionAwareTask> dependsOn() {
		return Optional.empty();
	}
}
