package com.merkle.oss.magnolia.setup.task.common.security;

import info.magnolia.cms.security.Permission;
import info.magnolia.cms.security.Realm;
import info.magnolia.cms.security.Role;
import info.magnolia.cms.security.UserManager;
import info.magnolia.module.InstallContext;
import info.magnolia.module.delta.AbstractTask;
import info.magnolia.module.delta.TaskExecutionException;
import info.magnolia.templating.functions.TemplatingFunctions;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.merkle.oss.magnolia.setup.task.common.security.util.RoleManagerUtil;
import com.merkle.oss.magnolia.setup.task.common.security.util.UserManagerUtil;

public abstract class AbstractSetupAnonymousTask extends AbstractTask {
	private static final String TASK_NAME = "Anonymous ACL";
	private static final String TASK_DESCRIPTION = "Sets ACLs for anonymous user";
	private final TemplatingFunctions templatingFunctions;
	private final RoleManagerUtil.Factory roleManagerUtilFactory;
	private final UserManagerUtil userManagerUtil;

	protected AbstractSetupAnonymousTask(
			final TemplatingFunctions templatingFunctions,
			final RoleManagerUtil.Factory roleManagerUtilFactory,
			final UserManagerUtil.Factory userManagerUtilFactory
	) {
		super(TASK_NAME, TASK_DESCRIPTION);
		this.templatingFunctions = templatingFunctions;
		this.roleManagerUtilFactory = roleManagerUtilFactory;
		this.userManagerUtil = userManagerUtilFactory.create(Realm.REALM_SYSTEM);
	}

	@Override
	public void execute(final InstallContext installContext) throws TaskExecutionException{
		try {
			final RoleManagerUtil roleManagerUtil = roleManagerUtilFactory.create(installContext);
			final Role role = roleManagerUtil.getOrCreateRole("anonymous");
			final Set<Role> roles = Stream.concat(
					Stream.of(role),
					roleManagerUtil.getRoles(Set.of("categorization-base", "contact-base", "imaging-base", "rest-anonymous", "stories-base")).stream()
			).collect(Collectors.toSet());
			configureRole(roleManagerUtil, role);
			userManagerUtil.getOrCreateUserAndSetPassword(
					new UsernamePasswordCredentials(UserManager.ANONYMOUS_USER, new String(Base64.encodeBase64(UserManager.ANONYMOUS_USER.getBytes()))),
					Collections.emptySet(),
					roles
			);
		} catch (Exception e) {
			throw new TaskExecutionException("Failed to set anonymous user", e);
		}
	}

	private void configureRole(final RoleManagerUtil roleManagerUtil, final Role role) {
		roleManagerUtil.removeAllWebAccess(role);
		roleManagerUtil.addWebAccess(role, Permission.NONE, "/.magnolia", "/.magnolia*");
		roleManagerUtil.addWebAccess(role, Permission.READ, "/VAADIN/*");

		/*
		 * On most systems, the rights and permissions of the anonymous role differ between author and public instances:
		 * allow read access to all on the public instance, while deny the same on the author instance.
		 * That is why you should not activate that role.
		 */
		if (templatingFunctions.isPublicInstance()) {
			configureRolePublic(roleManagerUtil, role);
		} else {
			roleManagerUtil.addWebAccess(role, Permission.NONE, "*");
			roleManagerUtil.removeAllPermissions(role);
		}
	}

	protected abstract void configureRolePublic(RoleManagerUtil roleManagerUtil, Role role);
}
