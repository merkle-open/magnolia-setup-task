package com.merkle.oss.magnolia.setup.task.common.security.util;

import info.magnolia.cms.security.AccessDeniedException;
import info.magnolia.cms.security.Group;
import info.magnolia.cms.security.GroupManager;
import info.magnolia.cms.security.SecuritySupport;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupManagerUtil {
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
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
		try {
			return Optional.ofNullable(groupManager.get().getGroup(groupName));
		} catch (AccessDeniedException e) {
			LOG.error("Access denied to get group " + groupName, e);
			return Optional.empty();
		}
	}
}
