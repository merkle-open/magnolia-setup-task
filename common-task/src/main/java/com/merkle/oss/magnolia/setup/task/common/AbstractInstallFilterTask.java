package com.merkle.oss.magnolia.setup.task.common;

import com.merkle.oss.magnolia.powernode.NodeOperationFactory; 
import com.merkle.oss.magnolia.setup.task.nodebuilder.AbstractPathNodeBuilderTask;
import com.merkle.oss.magnolia.powernode.ValueConverter;
import info.magnolia.cms.filters.FilterManager;
import info.magnolia.cms.filters.MgnlFilter;
import info.magnolia.jcr.nodebuilder.NodeOperation;
import info.magnolia.jcr.nodebuilder.task.ErrorHandling;
import info.magnolia.module.InstallContext;
import info.magnolia.module.delta.FilterOrderingTask;
import info.magnolia.module.delta.TaskExecutionException;
import info.magnolia.repository.RepositoryConstants;

import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.stream.Stream;

public abstract class AbstractInstallFilterTask extends AbstractPathNodeBuilderTask {
	protected final NodeOperationFactory ops;
	private final Class<? extends MgnlFilter> filterClass;
	private final String filterName;
	private final String[] requiredFiltersBefore;

	/**
	 * Installs a Filter into the filter chain (and replaces an existing filter at the same place with the same name)
	 *
	 * @param filterClass class of the magnolia filter
	 * @param filterName name of the node the filter should be created in. Must be a relative path below the root path of the filter chain (/server/filters)
	 * @param requiredFiltersBefore an array of filter names that must appear before the filter specified as filterName.
	 */
	protected AbstractInstallFilterTask(
			final NodeOperationFactory nodeOperationFactory,
			final Class<? extends MgnlFilter> filterClass,
			final String filterName,
			final String... requiredFiltersBefore
	) {
		super(
				"Install Filter " + filterName + "(" + filterClass + ")",
				"",
				ErrorHandling.strict,
				RepositoryConstants.CONFIG,
				FilterManager.SERVER_FILTERS
		);
		this.ops = nodeOperationFactory;
		this.filterClass = filterClass;
		this.filterName = filterName;
		this.requiredFiltersBefore = requiredFiltersBefore;
	}

	@Override
	protected final void doExecute(final InstallContext installContext) throws RepositoryException, TaskExecutionException {
		super.doExecute(installContext);
		new FilterOrderingTask(filterName, requiredFiltersBefore).execute(installContext);
	}

	@Override
	protected final NodeOperation[] getNodeOperations(InstallContext ctx) {
		return new NodeOperation[]{
				ops.getOrAddNode(filterName).then(
						append(
								getFilterNodeOperations(),
								ops.setProperty("class", filterClass.getName(), ValueConverter::toValue),
								ops.setProperty("enabled", true, ValueConverter::toValue)
						)
				)
		};
	}

	private NodeOperation[] append(final NodeOperation[] ops1, final NodeOperation... ops2) {
		return Stream
				.concat(
						Arrays.stream(ops1),
						Arrays.stream(ops2)
				)
				.toArray(NodeOperation[]::new);
	}

	/**
	 * NodeOperations to be executed in the context of the newly created filter node.
	 */
	protected NodeOperation[] getFilterNodeOperations() {
		return new NodeOperation[]{};
	}
}
