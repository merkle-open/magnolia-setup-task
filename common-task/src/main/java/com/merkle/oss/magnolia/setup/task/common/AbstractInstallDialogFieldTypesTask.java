package com.merkle.oss.magnolia.setup.task.common;

import info.magnolia.jcr.nodebuilder.NodeOperation;
import info.magnolia.jcr.nodebuilder.task.ErrorHandling;
import info.magnolia.module.InstallContext;
import info.magnolia.repository.RepositoryConstants;
import info.magnolia.ui.field.ConfiguredFieldDefinition;
import info.magnolia.ui.field.factory.AbstractFieldFactory;

import java.lang.invoke.MethodHandles;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merkle.oss.magnolia.powernode.NodeOperationFactory;
import com.merkle.oss.magnolia.powernode.ValueConverter;
import com.merkle.oss.magnolia.setup.task.nodebuilder.AbstractPathNodeBuilderTask;

/**
 * Base class for dialog field type install tasks.
 */
public abstract class AbstractInstallDialogFieldTypesTask extends AbstractPathNodeBuilderTask {
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private static final String TASK_NAME = "Install Dialog Field Types";
	private static final String TASK_DESCRIPTION = "Install Dialog Field Types";

	public static final String MODULE_PATH = "modules/{0}";

	private final NodeOperationFactory ops;
	private final String fieldTypeName;
	private final Class<? extends ConfiguredFieldDefinition> definitionClass;
	private final Class<? extends AbstractFieldFactory> factoryClass;

	protected AbstractInstallDialogFieldTypesTask(
			final NodeOperationFactory nodeOperationFactory,
			final String fieldTypeName,
			final Class<? extends ConfiguredFieldDefinition> definitionClass,
			final Class<? extends AbstractFieldFactory> factoryClass
	) {
		super(TASK_NAME, TASK_DESCRIPTION, ErrorHandling.strict, RepositoryConstants.CONFIG);
		this.ops = nodeOperationFactory;
		this.fieldTypeName = fieldTypeName;
		this.definitionClass = definitionClass;
		this.factoryClass = factoryClass;
	}

	@Override
	protected NodeOperation[] getNodeOperations(final InstallContext ctx) {
		final String moduleName = ctx.getCurrentModuleDefinition().getName();
		final String modulePath = MessageFormat.format(MODULE_PATH, moduleName);
		LOG.info("installing dialogFieldType '{}' for module {}", fieldTypeName, modulePath);
		return new NodeOperation[]{
				ops.getOrAddContentNode(modulePath).then(
						ops.getOrAddContentNode("fieldTypes").then(
								ops.getOrAddContentNode(fieldTypeName).then(
										ops.setProperty("definitionClass", definitionClass.getName(), ValueConverter::toValue),
										ops.setProperty("factoryClass", factoryClass.getName(), ValueConverter::toValue)
								)
						)
				)
		};
	}
}
