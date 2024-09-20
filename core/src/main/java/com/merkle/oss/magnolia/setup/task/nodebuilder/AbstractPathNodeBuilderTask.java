package com.merkle.oss.magnolia.setup.task.nodebuilder;

import info.magnolia.jcr.nodebuilder.task.ErrorHandling;
import info.magnolia.module.InstallContext;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * A task using the NodeBuilder API, applying operations on a given path.
 */
public abstract class AbstractPathNodeBuilderTask extends AbstractContentNodeBuilderTask {
	private final String workspaceName;
	private final String rootPath;

	protected AbstractPathNodeBuilderTask(
			final String taskName,
			final String description,
			final ErrorHandling errorHandling,
			final String workspaceName
	) {
		this(taskName, description, errorHandling, workspaceName, "/");
	}

	protected AbstractPathNodeBuilderTask(
			final String taskName,
			final String description,
			final ErrorHandling errorHandling,
			final String workspaceName,
			final String rootPath
	) {
		super(taskName, description, errorHandling);
		this.workspaceName = workspaceName;
		this.rootPath = rootPath;
	}

	@Override
	protected Node getRootNode(final InstallContext ctx) throws RepositoryException {
		final Session hm = ctx.getJCRSession(workspaceName);
		return hm.getNode(rootPath);
	}
}
