package com.merkle.oss.magnolia.setup.task.common.security.util;

import info.magnolia.cms.security.MgnlRole;
import info.magnolia.cms.security.Role;
import info.magnolia.cms.security.SilentSessionOp;
import info.magnolia.context.MgnlContext;
import info.magnolia.jcr.util.NodeTypes;
import info.magnolia.jcr.util.NodeUtil;
import info.magnolia.module.InstallContext;
import info.magnolia.repository.RepositoryConstants;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class RoleManagerUtil {
	private static final String WEB_ACCESS_WORKSPACE = "uri";
	private final Provider<BulkMgnlRoleManager> roleManager;

	@Inject
	public RoleManagerUtil(final BulkMgnlRoleManager.Factory roleManagerFactory) {
		this(roleManagerFactory::create);
	}
	public RoleManagerUtil(final Provider<BulkMgnlRoleManager> roleManager) {
		this.roleManager = roleManager;
	}

	public Set<Role> getRoles(final Set<String> roleNames) {
		return roleManager.get().getRoles(roleNames);
	}
	public Optional<Role> getRole(final String roleName) {
		return Optional.ofNullable(roleManager.get().getRole(roleName));
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
		roleManager.get().removeAllPermissions(role, WEB_ACCESS_WORKSPACE);
	}

	public void setAllPermissions(final Role role, final Map<String, BulkMgnlRoleManager.Permissions> workspacePermissionMapping) {
		setAllPermissionsSet(role, workspacePermissionMapping.entrySet().stream().collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> Set.of(entry.getValue())
		)));
	}
	public void setAllPermissionsSet(final Role role, final Map<String, Set<BulkMgnlRoleManager.Permissions>> workspacePermissionsSetMapping) {
		removeAllPermissions(role);
		roleManager.get().addPermissions(role, workspacePermissionsSetMapping);
	}

	public void setPermission(final Role role, final String workspace, final long permission, final String... paths) {
		removePermissions(role, workspace, paths);
		addPermission(role, workspace, permission, paths);
	}

	public void addPermission(final Role role, final String workspace, final long permission, final String... paths) {
		if(paths.length > 0) {
			roleManager.get().addPermissions(role, workspace, Set.of(paths), permission);
		}
	}

	public void removePermission(final Role role, final String workspace, final long permission, final String... paths) {
		if(paths.length > 0) {
			roleManager.get().removePermissions(role, workspace, Set.of(paths), permission);
		}
	}
	public void removePermissions(final Role role, final String workspace, final String... paths) {
		removePermission(role, workspace, MgnlRole.PERMISSION_ANY, paths);
	}
	public void removeAllPermissions(final Role role) {
		roleManager.get().removeAllPermissions(role);
	}

	public static class Factory {
        private final BulkMgnlRoleManager.Factory roleManagerFactory;

		@Inject
        public Factory(final BulkMgnlRoleManager.Factory roleManagerFactory) {
            this.roleManagerFactory = roleManagerFactory;
        }

		public RoleManagerUtil create(final InstallContext installContext) {
			return create(workspace -> {
                try {
                    return Optional.of(installContext.getJCRSession(workspace));
                } catch (RepositoryException e) {
					return Optional.empty();
                }
            }, false);
		}
		public RoleManagerUtil create(final Function<String, Optional<Session>> sessionProvider) {
			return create(sessionProvider, true);
		}
		public RoleManagerUtil create(final Function<String, Optional<Session>> sessionProvider, final boolean saveSession) {
			return new RoleManagerUtil(() -> roleManagerFactory.create(sessionProvider, saveSession));
		}
	}
}
