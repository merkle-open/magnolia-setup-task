package com.merkle.oss.magnolia.setup.task.common;

import info.magnolia.commands.MgnlCommand;
import info.magnolia.jcr.nodebuilder.NodeOperation;
import info.magnolia.jcr.nodebuilder.task.ErrorHandling;
import info.magnolia.jcr.util.NodeTypes;
import info.magnolia.module.InstallContext;
import info.magnolia.repository.RepositoryConstants;

import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merkle.oss.magnolia.powernode.NodeOperationFactory;
import com.merkle.oss.magnolia.setup.task.nodebuilder.AbstractPathNodeBuilderTask;

/**
 * Base class for command install tasks.
 */
public abstract class AbstractInstallCommandTask extends AbstractPathNodeBuilderTask {
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static final String MODULE_PATH = "modules/{0}";

	private final NodeOperationFactory ops;
	private final String catalog;
	private final String name;
	private final Class<? extends MgnlCommand> clazz;

	protected AbstractInstallCommandTask(
			final NodeOperationFactory nodeOperationFactory,
			final String catalog,
			final String name,
			final Class<? extends MgnlCommand> clazz
	) {
		super("Install command " + name, "Install command " + name + " in catalog " + catalog, ErrorHandling.strict, RepositoryConstants.CONFIG);
		this.ops = nodeOperationFactory;
		this.catalog = catalog;
		this.name = name;
		this.clazz = clazz;
	}

	@Override
	protected NodeOperation[] getNodeOperations(final InstallContext ctx) {
		final String moduleName = ctx.getCurrentModuleDefinition().getName();
		final String modulePath = MessageFormat.format(MODULE_PATH, moduleName);
		LOG.info("installing command '{}' for module {}", name, modulePath);
		return new NodeOperation[]{
				ops.getOrAddNode(modulePath, NodeTypes.Content.NAME).then(
						ops.getOrAddNode("commands", NodeTypes.Content.NAME).then(
								ops.getOrAddNode(catalog, NodeTypes.Content.NAME).then(
										ops.getOrAddContentNode(name).then(
												ops.setClassProperty(clazz)
										)
								)
						)
				)
		};
	}
}
