package com.merkle.oss.magnolia.setup.task.common;

import com.merkle.oss.magnolia.powernode.NodeOperationFactory; 
import com.merkle.oss.magnolia.powernode.ValueConverter;
import com.merkle.oss.magnolia.setup.task.nodebuilder.AbstractPathNodeBuilderTask;
import com.merkle.oss.magnolia.setup.task.type.InstallAndUpdateTask;

import info.magnolia.jcr.nodebuilder.NodeOperation;
import info.magnolia.jcr.nodebuilder.task.ErrorHandling;
import info.magnolia.module.InstallContext;
import info.magnolia.repository.RepositoryConstants;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

public class SetEmptyDefaultExtensionTask extends AbstractPathNodeBuilderTask implements InstallAndUpdateTask {
	private static final String TASK_NAME = "Set default Extension";
	private static final String TASK_DESCRIPTION = "Set default Extension";
	private static final String ACTIONS_PATH = "/server";
	private final NodeOperationFactory ops;

	@Inject
	public SetEmptyDefaultExtensionTask(final NodeOperationFactory nodeOperationFactory) {
		super(TASK_NAME, TASK_DESCRIPTION, ErrorHandling.strict, RepositoryConstants.CONFIG, ACTIONS_PATH);
		ops = nodeOperationFactory;
	}

	@Override
	protected NodeOperation[] getNodeOperations(final InstallContext ctx) {
		return new NodeOperation[]{
				ops.setProperty("defaultExtension", StringUtils.EMPTY, ValueConverter::toValue)
		};
	}
}
