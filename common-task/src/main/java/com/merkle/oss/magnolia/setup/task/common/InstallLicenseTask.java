package com.merkle.oss.magnolia.setup.task.common;

import info.magnolia.init.MagnoliaConfigurationProperties;
import info.magnolia.jcr.nodebuilder.NodeOperation;
import info.magnolia.jcr.nodebuilder.task.ErrorHandling;
import info.magnolia.module.InstallContext;
import info.magnolia.repository.RepositoryConstants;

import java.util.Optional;

import javax.inject.Inject;

import com.merkle.oss.magnolia.powernode.NodeOperationFactory;
import com.merkle.oss.magnolia.powernode.ValueConverter;
import com.merkle.oss.magnolia.setup.task.nodebuilder.AbstractPathNodeBuilderTask;
import com.merkle.oss.magnolia.setup.task.type.InstallAndUpdateTask;
import com.merkle.oss.magnolia.setup.task.type.LocalDevelopmentStartupTask;

/**
 * Configure magnolia license.
 * <p>
 * Use the following properties in magnolia.properties:
 * <p>
 * magnolia.license.owner=
 * magnolia.license.key=
 * <p>
 * - Add to according 'ModuleVersionHandler' in a project
 * - Execute as getInstallAndUpdateTask
 */
public class InstallLicenseTask extends AbstractPathNodeBuilderTask implements InstallAndUpdateTask {
	private static final String TASK_NAME = "Install License Task";
	private static final String TASK_DESCRIPTION = "This task installs the Magnolia license.";
	private static final String PATH = "/modules/enterprise";

	private final MagnoliaConfigurationProperties properties;
	private final NodeOperationFactory ops;

	@Inject
	public InstallLicenseTask(
			final NodeOperationFactory nodeOperationFactory,
			final MagnoliaConfigurationProperties properties
	) {
		super(TASK_NAME, TASK_DESCRIPTION, ErrorHandling.strict, RepositoryConstants.CONFIG, PATH);
		this.ops = nodeOperationFactory;
		this.properties = properties;
	}

	@Override
	protected NodeOperation[] getNodeOperations(final InstallContext ctx) {
		return getOwner().flatMap(owner -> getKey().map(key ->
				ops.getOrAddContentNode("license").then(
						ops.setProperty("owner", owner, ValueConverter::toValue),
						ops.setProperty("key", key, ValueConverter::toValue)
				)
		)).stream().toArray(NodeOperation[]::new);
	}

	private Optional<String> getOwner() {
		return getProperty("magnolia.license.owner");
	}

	private Optional<String> getKey() {
		return getProperty("magnolia.license.key");
	}

	private Optional<String> getProperty(final String key) {
		return Optional.ofNullable(properties.getProperty(key));
	}
}
