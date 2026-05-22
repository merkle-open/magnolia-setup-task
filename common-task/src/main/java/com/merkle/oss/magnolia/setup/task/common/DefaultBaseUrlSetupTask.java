package com.merkle.oss.magnolia.setup.task.common;

import info.magnolia.init.MagnoliaConfigurationProperties;
import info.magnolia.jcr.nodebuilder.NodeOperation;
import info.magnolia.jcr.nodebuilder.task.ErrorHandling;
import info.magnolia.module.InstallContext;
import info.magnolia.repository.RepositoryConstants;

import java.util.Optional;

import com.merkle.oss.magnolia.powernode.NodeOperationFactory;
import com.merkle.oss.magnolia.powernode.ValueConverter;
import com.merkle.oss.magnolia.setup.task.nodebuilder.AbstractPathNodeBuilderTask;
import com.merkle.oss.magnolia.setup.task.type.InstallAndUpdateTask;

import jakarta.inject.Inject;

/**
 * Configure defaultBaseUrl
 * <p>
 * Use the following properties in magnolia.properties:
 * <p>
 * magnolia.defaultBaseUrl=
 */
public class DefaultBaseUrlSetupTask extends AbstractPathNodeBuilderTask implements InstallAndUpdateTask {
    private static final String TASK_NAME = "DefaultBaseUrl Setup";
    private static final String TASK_DESCRIPTION = "This task configures the default base url.";
    private static final String PATH = "/server";
    private final NodeOperationFactory ops;
    private final MagnoliaConfigurationProperties properties;

    @Inject
    public DefaultBaseUrlSetupTask(
            final NodeOperationFactory nodeOperationFactory,
            final MagnoliaConfigurationProperties properties
    ) {
        super(TASK_NAME, TASK_DESCRIPTION, ErrorHandling.strict, RepositoryConstants.CONFIG, PATH);
        this.ops = nodeOperationFactory;
        this.properties = properties;
    }

    @Override
    protected NodeOperation[] getNodeOperations(final InstallContext ctx) {
        return getDefaultBaseUrl().map(defaultBaseUrl ->
                ops.setProperty("defaultBaseUrl", defaultBaseUrl, ValueConverter::toValue)
        ).stream().toArray(NodeOperation[]::new);
    }

    private Optional<String> getDefaultBaseUrl() {
        return getProperty("magnolia.defaultBaseUrl");
    }

    private Optional<String> getProperty(final String key) {
        return Optional.ofNullable(properties.getProperty(key));
    }
}
