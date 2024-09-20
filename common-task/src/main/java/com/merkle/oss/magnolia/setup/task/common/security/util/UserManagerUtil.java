package com.merkle.oss.magnolia.setup.task.common.security.util;

import info.magnolia.cms.security.Group;
import info.magnolia.cms.security.MgnlUserManager;
import info.magnolia.cms.security.Realm;
import info.magnolia.cms.security.Role;
import info.magnolia.cms.security.SecuritySupport;
import info.magnolia.cms.security.User;
import info.magnolia.cms.security.UserManager;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.http.auth.Credentials;

public class UserManagerUtil {
	private final Supplier<UserManager> userManager;

	public UserManagerUtil(
			final SecuritySupport securitySupport,
			final Realm realm
	) {
		userManager = () -> securitySupport.getUserManager(realm.getName());
	}

	public Optional<User> getUser(final String username) {
		return Optional.ofNullable(userManager.get().getUser(username));
	}

	public Optional<User> getOrCreateUserAndSetPassword(final Credentials credentials, final Set<Group> groups, final Set<Role> roles) {
		final User user = getOrCreateUserAndSetPassword(credentials.getUserPrincipal().getName(), credentials.getPassword());
		for (String group : user.getGroups()) {
			if(groups.stream().map(Group::getName).noneMatch(group::equals)) {
				userManager.get().removeGroup(user, group);
			}
		}
		for (Group group : groups) {
			userManager.get().addGroup(user, group.getName());
		}
		for (String role : user.getRoles()) {
			if(roles.stream().map(Role::getName).noneMatch(role::equals)) {
				userManager.get().removeRole(user, role);
			}
		}
		for (Role role : roles) {
			userManager.get().addRole(user, role.getName());
		}
		return Optional.ofNullable(userManager.get().getUser(user.getName()));
	}

	private User getOrCreateUserAndSetPassword(final String name, final String password) {
		return Optional
				.ofNullable(userManager.get().getUser(name))
				.map(user -> userManager.get().changePassword(user, password))
				.orElseGet(() -> userManager.get().createUser(name, password));
	}

	public void enable(final User user) {
		userManager.get().setProperty(user, MgnlUserManager.PROPERTY_ENABLED, "true");
	}

	public static class Factory {
        private final SecuritySupport securitySupport;

        @Inject
		public Factory(final SecuritySupport securitySupport) {
            this.securitySupport = securitySupport;
        }

		public UserManagerUtil create(final Realm realm) {
			return new UserManagerUtil(securitySupport, realm);
		}
	}
}
