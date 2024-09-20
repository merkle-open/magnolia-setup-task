package com.merkle.oss.magnolia.setup.task.common.security.util;

import info.magnolia.cms.security.Permission;
import info.magnolia.cms.security.Role;
import info.magnolia.cms.security.RoleManager;
import info.magnolia.cms.security.SecuritySupport;
import info.magnolia.cms.security.SilentSessionOp;
import info.magnolia.cms.security.auth.ACL;
import info.magnolia.context.MgnlContext;
import info.magnolia.jcr.util.NodeTypes;
import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.repository.RepositoryConstants;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;

public class RoleManagerUtil {
	private static final String WEB_ACCESS_WORKSPACE = "uri";
	private final Supplier<RoleManager> roleManager;

	@Inject
	public RoleManagerUtil(final SecuritySupport securitySupport) {
		roleManager = securitySupport::getRoleManager;
	}

	public Optional<Role> getRole(final String name) {
		return Optional.ofNullable(roleManager.get().getRole(name));
	}

	public Role getOrCreateRole(final String name) throws Exception {
		return getOrCreateRole(null, name);
	}

	public Role getOrCreateRole(@Nullable final String path, final String name) throws Exception {
		@Nullable final Role role = getRole(name).orElse(null);
		if (role == null) {
			final Node parent = getOrCreateNode(path);
			return roleManager.get().createRole(parent.getPath(), name);
		}
		return role;
	}

	private Node getOrCreateNode(@Nullable final String path) {
		return MgnlContext.doInSystemContext(new SilentSessionOp<>(RepositoryConstants.USER_ROLES) {
			@Override
			public Node doExec(final Session session) throws RepositoryException {
				if(path != null) {
					return NodeUtil.createPath(session.getRootNode(), StringUtils.removeStart(path, "/"), NodeTypes.Folder.NAME);
				}
				return session.getRootNode();
			}
		});
	}

	public void addWebAccess(final Role role, final long permission, final String... paths) {
		addPermission(role, WEB_ACCESS_WORKSPACE, permission, paths);
	}

	public void removeWebAccess(final Role role, final long permission, final String... paths) {
		removePermission(role, WEB_ACCESS_WORKSPACE, permission, paths);
	}

	public void removeAllWebAccess(final Role role) {
		removePermissions(role, WEB_ACCESS_WORKSPACE, permission -> true);
	}

	public void setPermission(final Role role, final String workspace, final long permission, final String... paths) {
		removePermissions(role, workspace, paths);
		addPermission(role, workspace, permission, paths);
	}

	public void addPermission(final Role role, final String workspace, final long permission, final String... paths) {
		Arrays.stream(paths).forEach(path ->
				roleManager.get().addPermission(role, workspace, path, permission)
		);
	}

	public void removePermission(final Role role, final String workspace, final long permission, final String... paths) {
		Arrays.stream(paths).forEach(path ->
				roleManager.get().removePermission(role, workspace, path, permission)
		);
	}

	public void removePermissions(final Role role, final String workspace, final String... paths) {
		removePermissions(role, workspace, permission -> Set.of(paths).contains(permission.getPattern().getPatternString()));
	}

	public void removeAllPermissions(final Role role) {
		roleManager.get().getACLs(role.getName()).entrySet().stream()
				.filter(entry -> !WEB_ACCESS_WORKSPACE.equals(entry.getKey()))
				.forEach(entry ->
						removePermissions(role, entry.getKey(), entry.getValue(), permission -> true)
				);
	}

	private void removePermissions(final Role role, final String workspace, final Predicate<Permission> filter) {
		Optional.ofNullable(roleManager.get().getACLs(role.getName()).get(workspace)).ifPresent(acls ->
				removePermissions(role, workspace, acls, filter)
		);
	}

	private void removePermissions(final Role role, final String workspace, final ACL acl, final Predicate<Permission> filter) {
		acl.getList().stream().filter(filter).forEach(permission ->
				removePermission(
						role,
						workspace,
						permission.getPermissions(),
						permission.getPattern().getPatternString()
				)
		);
	}
}
