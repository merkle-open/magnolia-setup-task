package com.merkle.oss.magnolia.setup.task.common.security.util;

import info.magnolia.cms.security.Group;
import info.magnolia.cms.security.GroupManager;
import info.magnolia.cms.security.SecuritySupport;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import com.machinezoo.noexception.Exceptions;

public class GroupManagerUtil {
	private final Supplier<GroupManager> groupManager;

	@Inject
	public GroupManagerUtil(final SecuritySupport securitySupport) {
		groupManager = securitySupport::getGroupManager;
	}

	public Set<Group> getGroups(final String... groupNames) {
		return Arrays.stream(groupNames)
				.map(this::getGroup)
				.flatMap(Optional::stream)
				.collect(Collectors.toSet());
	}

	public Optional<Group> getGroup(final String groupName) {
		return Optional.ofNullable(Exceptions.wrap().get(() -> groupManager.get().getGroup(groupName)));
	}

	public Group getOrCreateGroup(final String name) throws Exception {
		@Nullable final Group group = getGroup(name).orElse(null);
		if (group == null) {
			return groupManager.get().createGroup(name);
		}
		return group;
	}

	public void setRoles(final Group group, final Set<String> roleNames) {
		removeRoles(group, group.getRoles());
		roleNames.forEach(roleName ->
			addRole(group, roleName)
		);
	}
	public void addRole(final Group group, final String roleName) {
		Exceptions.wrap().run(() -> groupManager.get().addRole(group, roleName));
	}

	public void removeRoles(final Group group, final Collection<String> roleNames) {
		roleNames.forEach(role ->
				removeRole(group, role)
		);
	}
	public void removeRole(final Group group, final String roleName) {
		Exceptions.wrap().run(() -> groupManager.get().removeRole(group, roleName));
	}

	public void setGroups(final Group group, final Set<String> groupNames) {
		removeRoles(group, group.getRoles());
		groupNames.forEach(groupName ->
				addGroup(group, groupName)
		);
	}
	public void addGroup(final Group group, final String groupName) {
		Exceptions.wrap().run(() -> groupManager.get().addGroup(group, groupName));
	}

	public void removeGroups(final Group group, final Collection<String> groupNames) {
		groupNames.forEach(groupName ->
				removeGroup(group, groupName)
		);
	}
	public void removeGroup(final Group group, final String groupName) {
		Exceptions.wrap().run(() -> groupManager.get().removeGroup(group, groupName));
	}
}
