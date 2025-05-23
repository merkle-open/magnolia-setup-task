package com.merkle.oss.magnolia.setup.task.common;

import info.magnolia.jcr.util.NodeNameHelper;
import info.magnolia.module.InstallContext;
import info.magnolia.module.delta.ArrayDelegateTask;
import info.magnolia.module.delta.RegisterServletTask;
import info.magnolia.module.delta.TaskExecutionException;
import info.magnolia.module.model.ModuleDefinition;
import info.magnolia.module.model.ServletDefinition;

import java.util.Optional;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.machinezoo.noexception.Exceptions;
import com.merkle.oss.magnolia.setup.task.type.InstallAndUpdateTask;

/**
 * Reregistering servlets. Normally they only get registered on install, but not on update (info.magnolia.module.delta.RegisterModuleServletsTask)
 */
public class ReregisterServletsTask extends ArrayDelegateTask implements InstallAndUpdateTask {
	private static final String DEFAULT_SERVLET_FILTER_PATH = "server/filters/servlets";
	private final NodeNameHelper nodeNameHelper;

	@Inject
	public ReregisterServletsTask(final NodeNameHelper nodeNameHelper) {
		super("Reregister module servlets", "Reregisters servlets for this module.");
		this.nodeNameHelper = nodeNameHelper;
	}

	@Override
	public void execute(final InstallContext installContext) throws TaskExecutionException {
		final ModuleDefinition moduleDefinition = installContext.getCurrentModuleDefinition();
		for (ServletDefinition servletDefinition : moduleDefinition.getServlets()) {
			addTask(new ReregisterServletTask(servletDefinition, nodeNameHelper));
		}
		super.execute(installContext);
	}

	private static class ReregisterServletTask extends RegisterServletTask {
		public ReregisterServletTask(ServletDefinition servletDefinition, NodeNameHelper nodeNameHelper) {
			super(servletDefinition, nodeNameHelper);
		}

		@Override
		public void execute(final InstallContext installContext) throws TaskExecutionException {
			try {
				getServletNode(installContext).ifPresent(servletNode -> Exceptions.wrap().run(servletNode::remove));
				super.execute(installContext);
			} catch (Exception e) {
				throw new TaskExecutionException("Failed to reregister servlet "+getServletDefinition().getName(), e);
			}
		}

		private Optional<Node> getServletNode(final InstallContext installContext) throws RepositoryException {
			try {
				final Session session = installContext.getConfigJCRSession();
				return Optional.of(session.getRootNode().getNode(DEFAULT_SERVLET_FILTER_PATH + "/" + getServletDefinition().getName()));
			} catch (PathNotFoundException e) {
				return Optional.empty();
			}
		}
	}
}
