package com.merkle.oss.magnolia.setup.task.common.security;

import info.magnolia.cms.security.Group;
import info.magnolia.cms.security.Permission;
import info.magnolia.cms.security.Realm;
import info.magnolia.cms.security.Role;
import info.magnolia.init.MagnoliaConfigurationProperties;
import info.magnolia.module.InstallContext;
import info.magnolia.module.delta.AbstractTask;
import info.magnolia.module.delta.TaskExecutionException;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.merkle.oss.magnolia.setup.task.common.security.util.GroupManagerUtil;
import com.merkle.oss.magnolia.setup.task.common.security.util.RoleManagerUtil;
import com.merkle.oss.magnolia.setup.task.common.security.util.UserManagerUtil;

public abstract class AbstractSetupSuperuserTask extends AbstractTask {
	private static final String TASK_NAME = "Superuser ACL";
	private static final String TASK_DESCRIPTION = "Sets ACLs for superuser";
	private final UserManagerUtil userManagerUtil;
	private final RoleManagerUtil.Factory roleManagerUtilFactory;
	private final GroupManagerUtil.Factory groupManagerUtilFactory;
	private final MagnoliaConfigurationProperties properties;

	protected AbstractSetupSuperuserTask(
			final RoleManagerUtil.Factory roleManagerUtilFactory,
			final GroupManagerUtil.Factory groupManagerUtilFactory,
			final UserManagerUtil.Factory userManagerUtil,
			final MagnoliaConfigurationProperties properties
	) {
		super(TASK_NAME, TASK_DESCRIPTION);
		this.userManagerUtil = userManagerUtil.create(Realm.REALM_SYSTEM);
		this.groupManagerUtilFactory = groupManagerUtilFactory;
		this.roleManagerUtilFactory = roleManagerUtilFactory;
		this.properties = properties;
	}

	@Override
	public void execute(final InstallContext installContext) throws TaskExecutionException {
		try {
			final RoleManagerUtil roleManagerUtil = roleManagerUtilFactory.create(installContext);
			final GroupManagerUtil groupManagerUtil = groupManagerUtilFactory.create(installContext);
			final Role role = roleManagerUtil.getOrCreateRole("superuser");
			final Set<Role> roles = Stream.concat(
					Stream.of(role),
					roleManagerUtil.getRoles(Set.of("rest-admin")).stream()
			).collect(Collectors.toSet());
			final Set<Group> groups = groupManagerUtil.getGroups(Set.of("publishers"));

			configureRoleInternal(roleManagerUtil, role);
			getCredentials()
					.flatMap(credentials ->
							userManagerUtil.getOrCreateUserAndSetPassword(credentials, groups, roles)
					)
					.ifPresent(userManagerUtil::enable);
		} catch (Exception e) {
			throw new TaskExecutionException("Failed to set superuser ACLs", e);
		}
	}

	private void configureRoleInternal(final RoleManagerUtil roleManagerUtil, final Role role) {
		roleManagerUtil.addWebAccess(role, Permission.ALL, "*");
		configureRole(roleManagerUtil, role);
	}
	protected abstract void configureRole(RoleManagerUtil roleManagerUtil, Role role);

	public Optional<Credentials> getCredentials() {
		return Optional.ofNullable(properties.getProperty("users.superuser.username")).flatMap(username ->
				Optional.ofNullable(properties.getProperty("users.superuser.password")).map(password ->
						new UsernamePasswordCredentials(username, password)
				)
		);
	}
}
