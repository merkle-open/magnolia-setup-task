package com.merkle.oss.magnolia.setup.task.common;

import com.merkle.oss.magnolia.powernode.NodeOperationFactory;
import com.merkle.oss.magnolia.powernode.PowerNode;
import com.merkle.oss.magnolia.powernode.PowerNodeService;
import com.merkle.oss.magnolia.powernode.ValueConverter;
import com.merkle.oss.magnolia.setup.task.nodebuilder.AbstractPathNodeBuilderTask;
import com.merkle.oss.magnolia.setup.task.type.InstallAndUpdateTask;

import info.magnolia.init.MagnoliaConfigurationProperties;
import info.magnolia.jcr.nodebuilder.NodeOperation;
import info.magnolia.jcr.nodebuilder.Ops;
import info.magnolia.jcr.nodebuilder.task.ErrorHandling;
import info.magnolia.module.InstallContext;
import info.magnolia.objectfactory.Components;
import info.magnolia.repository.RepositoryConstants;

import javax.inject.Inject;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * Configure SMTP server.
 * <p>
 * Use the following properties in magnolia.properties:
 * <p>
 * magnolia.smtp.security=none|ssl|tls
 * magnolia.smtp.auth=null|userPassword
 * magnolia.smtp.user=user
 * magnolia.smtp.keystorePath=/folder/smtp # get path from the password app
 * magnolia.smtp.server=mailgateway.sg.ch.namics.com
 * magnolia.smtp.port=25
 * <p>
 * - Add to according 'ModuleVersionHandler' in a project
 * - Execute as getInstallAndUpdateTask
 */
public class SetupSmtpTask extends AbstractPathNodeBuilderTask implements InstallAndUpdateTask {
	private static final String TASK_NAME = "SetupSmtpTask";
	private static final String TASK_DESCRIPTION = "Set SMTP configuration from magnolia.properties";

	private static final String MAIL_MODULE_CONFIG_PATH = "/modules/mail/config";

	private final MagnoliaConfigurationProperties properties;
	private final PowerNodeService powerNodeService;
	private final NodeOperationFactory ops;

	@Inject
	public SetupSmtpTask(
			final PowerNodeService powerNodeService,
			final NodeOperationFactory nodeOperationFactory
	) {
		super(TASK_NAME, TASK_DESCRIPTION, ErrorHandling.strict, RepositoryConstants.CONFIG, MAIL_MODULE_CONFIG_PATH);
		this.powerNodeService = powerNodeService;
		this.ops = nodeOperationFactory;
		this.properties = Components.getComponent(MagnoliaConfigurationProperties.class);
	}

	@Override
	protected NodeOperation[] getNodeOperations(final InstallContext ctx) {
		final String security = getProperty("magnolia.smtp.security", "none");
		final String auth = getProperty("magnolia.smtp.auth", "null");
		final String server = getProperty("magnolia.smtp.server", "localhost");
		final String port = getProperty("magnolia.smtp.port", "25");
		final String user = getProperty("magnolia.smtp.user", StringUtils.EMPTY);
		final String keystorePath = getProperty("magnolia.smtp.keystorePath", StringUtils.EMPTY);

		return new NodeOperation[]{
				ops.getOrAddContentNode("smtpConfiguration").then(
						ops.getOrAddNode("authentication").then(ops.clearProperties().then(
								getAuth(auth, user, keystorePath)
						)),
						ops.setProperty("server", server, ValueConverter::toValue),
						ops.setProperty("port", port, ValueConverter::toValue),
						ops.setProperty("security", security, ValueConverter::toValue)
				)
		};
	}

	private NodeOperation[] getAuth(final String auth, final String user, final String keystorePath) {
		if ("userPassword".equals(auth)) {
			return getUserPasswordAuth(user, getKeystoreId(keystorePath).orElse(null));
		}
		return getNullAuth();
	}

	private Optional<String> getKeystoreId(final String keystorePath) {
		return powerNodeService.getByPath("keystore", keystorePath).map(PowerNode::getIdentifier);
	}

	private NodeOperation[] getUserPasswordAuth(final String user, final String passwordKeyStoreId) {
		return new NodeOperation[]{
				ops.setProperty("class", "info.magnolia.module.mail.smtp.authentication.UsernamePasswordSmtpAuthentication", ValueConverter::toValue),
				ops.setProperty("user", user, ValueConverter::toValue),
				passwordKeyStoreId != null ? ops.setProperty("passwordKeyStoreId", passwordKeyStoreId, ValueConverter::toValue) : Ops.noop()
		};
	}

	private NodeOperation[] getNullAuth() {
		return new NodeOperation[]{
				ops.setProperty("class", "info.magnolia.module.mail.smtp.authentication.NullSmtpAuthentication", ValueConverter::toValue)
		};
	}

	private String getProperty(final String name, final String fallback) {
		if (properties.hasProperty(name)) {
			return properties.getProperty(name);
		}
		return fallback;
	}
}
