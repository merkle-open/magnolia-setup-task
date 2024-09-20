package com.merkle.oss.magnolia.setup.task.nodebuilder;


import info.magnolia.jcr.nodebuilder.*;
import info.magnolia.jcr.nodebuilder.task.ErrorHandling;
import info.magnolia.jcr.nodebuilder.task.TaskLogErrorHandler;
import info.magnolia.module.InstallContext;
import info.magnolia.module.delta.AbstractRepositoryTask;
import info.magnolia.module.delta.TaskExecutionException;

import java.lang.invoke.MethodHandles;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractContentNodeBuilderTask extends AbstractRepositoryTask {
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final ErrorHandling errorHandling;

	protected AbstractContentNodeBuilderTask(String name, String description, ErrorHandling errorHandling) {
		super(name, description);
		this.errorHandling = errorHandling;
	}

	@Override
	protected void doExecute(final InstallContext ctx) throws RepositoryException, TaskExecutionException {
		final Node root = getRootNode(ctx);
		final NodeOperation[] operations = obtainNodeOperations(ctx);
		final ErrorHandler errorHandler = newErrorHandler(ctx);
		final NodeBuilder nodeBuilder = new NodeBuilder(errorHandler, root, operations);
		try {
			nodeBuilder.exec();
		} catch (NodeOperationException e) {
			LOG.error("Could not execute node builder task", e);
			throw new TaskExecutionException(e.getMessage(), e.getCause());
		}
	}

	/**
	 * This method must be used to set NodeOperations. Use this pattern:
	 * return new NodeOperation[]{  addNode(...).then(     ) };
	 *
	 * @return node operations to be used in this tasks
	 * @param ctx install context
	 */
	protected abstract NodeOperation[] getNodeOperations(final InstallContext ctx);

	protected abstract Node getRootNode(final InstallContext ctx) throws RepositoryException;

	protected ErrorHandler newErrorHandler(final InstallContext ctx) {
		if (errorHandling == ErrorHandling.strict) {
			return new StrictErrorHandler();
		}
		return new TaskLogErrorHandler(ctx);
	}

	private NodeOperation[] obtainNodeOperations(final InstallContext ctx) throws TaskExecutionException {
		final NodeOperation[] operations = getNodeOperations(ctx);
		if (operations == null) {
			if (errorHandling == ErrorHandling.logging) {
				LOG.warn("No NodeOperations have been specified. Doing nothing");
				return new NodeOperation[0];
			}
			throw new TaskExecutionException("Please specify NodeOperations. Can be an empty array if no operations should be done...");
		}
		return operations;
	}
}
