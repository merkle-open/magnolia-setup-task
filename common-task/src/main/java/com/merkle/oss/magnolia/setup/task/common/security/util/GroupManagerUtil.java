package com.merkle.oss.magnolia.setup.task.common.security.util;

import info.magnolia.cms.security.Group;
import info.magnolia.module.InstallContext;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class GroupManagerUtil {
	private final Provider<BulkMgnlGroupManager> groupManager;

	@Inject
	public GroupManagerUtil(final BulkMgnlGroupManager.Factory groupManagerFactory) {
		this(groupManagerFactory::create);
	}
	public GroupManagerUtil(final Provider<BulkMgnlGroupManager> groupManager) {
		this.groupManager = groupManager;
	}

	public Set<Group> getGroups(final Set<String> groupNames) {
		return groupManager.get().getGroups(groupNames);
	}
	public Optional<Group> getGroup(final String groupName) {
		return Optional.ofNullable(groupManager.get().getGroup(groupName));
	}

	public Group getOrCreateGroup(final String name) throws Exception {
		@Nullable final Group group = getGroup(name).orElse(null);
		if (group == null) {
			return groupManager.get().createGroup(name);
		}
		return group;
	}

	public void setRoles(final Group group, final Set<String> roleNames) {
		removeAllRoles(group);
		addRoles(group, roleNames);
	}
	public void addRoles(final Group group, final Set<String> roleNames) {
		groupManager.get().addRoles(group, roleNames);
	}
	public void addRole(final Group group, final String roleName) {
		groupManager.get().addRole(group, roleName);
	}

	public void removeAllRoles(final Group group) {
		groupManager.get().removeAllRoles(group);
	}
	public void removeRoles(final Group group, final Set<String> roleNames) {
		groupManager.get().removeRoles(group, roleNames);
	}
	public void removeRole(final Group group, final String roleName) {
		groupManager.get().removeRole(group, roleName);
	}

	public void setGroups(final Group group, final Set<String> groupNames) {
		removeAllGroups(group);
		addGroups(group, groupNames);
	}
	public void addGroups(final Group group, final Set<String> groupNames) {
		groupManager.get().addGroups(group, groupNames);
	}
	public void addGroup(final Group group, final String groupName) {
		groupManager.get().addGroup(group, groupName);
	}

	public void removeAllGroups(final Group group) {
		groupManager.get().removeAllGroups(group);
	}
	public void removeGroups(final Group group, final Set<String> groupNames) {
		groupManager.get().removeGroups(group, groupNames);
	}
	public void removeGroup(final Group group, final String groupName) {
		groupManager.get().removeGroup(group, groupName);
	}

	public static class Factory {
		private final BulkMgnlGroupManager.Factory groupManagerFactory;

		@Inject
		public Factory(final BulkMgnlGroupManager.Factory groupManagerFactory) {
			this.groupManagerFactory = groupManagerFactory;
		}

		public GroupManagerUtil create(final InstallContext installContext) {
			return create(workspace -> {
				try {
					return Optional.of(installContext.getJCRSession(workspace));
				} catch (RepositoryException e) {
					return Optional.empty();
				}
			}, false);
		}
		public GroupManagerUtil create(final Function<String, Optional<Session>> sessionProvider) {
			return create(sessionProvider, true);
		}
		public GroupManagerUtil create(final Function<String, Optional<Session>> sessionProvider, final boolean saveSession) {
			return new GroupManagerUtil(() -> groupManagerFactory.create(sessionProvider, saveSession));
		}
	}
}
