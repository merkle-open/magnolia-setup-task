package com.merkle.oss.magnolia.setup.task.common;

import info.magnolia.jcr.nodebuilder.NodeOperation;
import info.magnolia.jcr.nodebuilder.task.ErrorHandling;
import info.magnolia.jcr.util.PropertyUtil;
import info.magnolia.module.InstallContext;
import info.magnolia.module.scheduler.JobDefinition;
import info.magnolia.repository.RepositoryConstants;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.value.ValueFactoryImpl;

import com.merkle.oss.magnolia.powernode.NodeOperationFactory;
import com.merkle.oss.magnolia.powernode.ValueConverter;
import com.merkle.oss.magnolia.setup.task.nodebuilder.AbstractPathNodeBuilderTask;
import com.merkle.oss.magnolia.setup.task.type.InstallAndUpdateTask;

public abstract class AbstractConfigureSchedulerJobTask extends AbstractPathNodeBuilderTask implements InstallAndUpdateTask {
	private static final String PATH = "/modules/scheduler/config/jobs";

	private final NodeOperationFactory ops;

	protected AbstractConfigureSchedulerJobTask(
			final NodeOperationFactory nodeOperationFactory,
			final String taskName,
			final String description,
			final ErrorHandling errorHandling
	) {
		super(taskName, description, errorHandling, RepositoryConstants.CONFIG, PATH);
		this.ops = nodeOperationFactory;
	}

	@Override
	protected NodeOperation[] getNodeOperations(InstallContext ctx) {
		return getJobs().map(this::configureJob).toArray(NodeOperation[]::new);
	}

	protected abstract Stream<JobDefinition> getJobs();

	private NodeOperation configureJob(final JobDefinition jobDefinition) {
		return ops.getOrAddContentNode(jobDefinition.getName()).then(
				ops.setProperty("catalog", jobDefinition.getCatalog(), ValueConverter::toValue),
				ops.setProperty("command", jobDefinition.getCommand(), ValueConverter::toValue),
				ops.setProperty("cron", jobDefinition.getCron(), ValueConverter::toValue),
				ops.setProperty("description", Optional.ofNullable(jobDefinition.getDescription()).orElse(StringUtils.EMPTY), ValueConverter::toValue),
				ops.setProperty("concurrent", jobDefinition.isConcurrent(), ValueConverter::toValue),
				ops.getOrAddContentNode("params").then(
						((Map<String, Object>) jobDefinition.getParams()).entrySet().stream().map(entry ->
							ops.setProperty(entry.getKey(), entry.getValue(), (valueConverter, property) -> Optional.of(PropertyUtil.createValue(property, ValueFactoryImpl.getInstance())))
						).toArray(NodeOperation[]::new)
				),
				ops.setEnabledProperty(jobDefinition.isEnabled())
		);
	}
}
