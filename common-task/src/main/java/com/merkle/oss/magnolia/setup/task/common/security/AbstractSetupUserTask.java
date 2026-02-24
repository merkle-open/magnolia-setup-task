package com.merkle.oss.magnolia.setup.task.common.security;

import info.magnolia.cms.security.Group;
import info.magnolia.cms.security.Realm;
import info.magnolia.cms.security.Role;
import info.magnolia.init.MagnoliaConfigurationProperties;
import info.magnolia.module.InstallContext;
import info.magnolia.module.delta.AbstractTask;
import info.magnolia.module.delta.TaskExecutionException;

import java.util.Optional;
import java.util.Set;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.merkle.oss.magnolia.setup.task.common.security.util.GroupManagerUtil;
import com.merkle.oss.magnolia.setup.task.common.security.util.RoleManagerUtil;
import com.merkle.oss.magnolia.setup.task.common.security.util.UserManagerUtil;

/**
 * Creates a user in the provided realm with username &amp; password loaded from properties using
 * <ul>
 *     <li>users.NAME.username</li>
 *     <li>users.NAME.password</li>
 * </ul>
 */
public abstract class AbstractSetupUserTask extends AbstractTask {
    private final MagnoliaConfigurationProperties properties;
    private final RoleManagerUtil.Factory roleManagerUtilFactory;
    private final GroupManagerUtil.Factory groupManagerUtilFactory;
    private final String name;
    private final UserManagerUtil userManagerUtil;

	protected AbstractSetupUserTask(
            final MagnoliaConfigurationProperties properties,
            final RoleManagerUtil.Factory roleManagerUtilFactory,
            final GroupManagerUtil.Factory groupManagerUtilFactory,
            final UserManagerUtil.Factory userManagerUtilFactory,
            final Realm realm,
			final String name
	) {
		super("Setup " + name, "sets up the " + name + " user");
        this.properties = properties;
        this.roleManagerUtilFactory = roleManagerUtilFactory;
        this.groupManagerUtilFactory = groupManagerUtilFactory;
        this.name = name;
        this.userManagerUtil = userManagerUtilFactory.create(realm);
	}

	@Override
	public void execute(final InstallContext installContext) throws TaskExecutionException {
		try {
			final Set<Group> groups = groups(groupManagerUtilFactory.create(installContext));
			final Set<Role> roles = roles(roleManagerUtilFactory.create(installContext));

			getCredentials()
					.flatMap(credentials ->
							userManagerUtil.getOrCreateUserAndSetPassword(credentials, groups, roles)
					)
					.ifPresent(userManagerUtil::enable);
		} catch (Exception e) {
			throw new TaskExecutionException("Failed to setup " + name + " user", e);
		}
	}

	protected abstract Set<Role> roles(RoleManagerUtil roleManagerUtil) throws Exception;
	protected abstract Set<Group> groups(GroupManagerUtil groupManagerUtil) throws Exception;

	public Optional<Credentials> getCredentials() {
		return Optional.ofNullable(properties.getProperty("users." + name + ".username")).flatMap(username ->
				Optional.ofNullable(properties.getProperty("users." + name + ".password")).map(password ->
						new UsernamePasswordCredentials(username, password)
				)
		);
	}
}
