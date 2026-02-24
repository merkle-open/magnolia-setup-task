package com.merkle.oss.magnolia.setup.task.common.security.util;

import info.magnolia.cms.security.MgnlRole;
import info.magnolia.cms.security.MgnlRoleManager;
import info.magnolia.cms.security.Role;
import info.magnolia.jcr.util.NodeNameHelper;
import info.magnolia.jcr.util.NodeTypes;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.machinezoo.noexception.Exceptions;
import com.merkle.oss.magnolia.powernode.PowerNode;
import com.merkle.oss.magnolia.powernode.PowerNodeService;
import com.merkle.oss.magnolia.powernode.ValueConverter;
import com.merkle.oss.magnolia.powernode.predicate.IsNamePrefix;
import com.merkle.oss.magnolia.powernode.predicate.IsPrimaryNodeType;

import jakarta.inject.Inject;

public class BulkMgnlRoleManager extends MgnlRoleManager {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String WEB_ACCESS_WORKSPACE = "uri";
    private final PowerNodeService powerNodeService;
    private final Function<String, Optional<Session>> sessionProvider;
    private final boolean saveSession;

    public BulkMgnlRoleManager(
            final NodeNameHelper nodeNameHelper,
            final PowerNodeService powerNodeService,
            final Function<String, Optional<Session>> sessionProvider,
            final boolean saveSession
    ) {
        super(nodeNameHelper);
        this.powerNodeService = powerNodeService;
        this.sessionProvider = sessionProvider;
        this.saveSession = saveSession;
    }

    @Override
    public Role getRole(final String name) {
        return getRoles(Set.of(name)).stream().findFirst().orElseGet(() -> {
            LOG.debug("can't find role [{}]", name);
            return null;
        });
    }
    public Set<Role> getRoles(final Set<String> names) {
        return streamRoleNodesByNames(names)
                .map(roleNode -> Exceptions.wrap().get(() -> newRoleInstance(roleNode)))
                .collect(Collectors.toSet());
    }

    @Override
    public void addPermission(final Role role, final String workspace, final String path, final long permission) {
        final PowerNode roleNode = getRoleNodeOrThrow(role);
        addPermissions(roleNode, workspace, Set.of(path), permission);
        save(roleNode);
    }
    public void addPermissions(final Role role, final Map<String, Set<Permissions>> workspacePermissionsSetMapping) {
        final PowerNode roleNode = getRoleNodeOrThrow(role);
        workspacePermissionsSetMapping.forEach((workspace, permissionsSet) ->
                permissionsSet.forEach(permissions ->
                        addPermissions(roleNode, workspace, permissions.paths(), permissions.permission())
                )
        );
        save(roleNode);
    }
    public void addPermissions(final Role role, final String workspace, final Set<String> paths, final long permission) {
        final PowerNode roleNode = getRoleNodeOrThrow(role);
        addPermissions(roleNode, workspace, paths, permission);
        save(roleNode);
    }

    @Override
    public void removePermission(final Role role, final String workspace, final String path, final long permission) {
        final PowerNode roleNode = getRoleNodeOrThrow(role);
        removePermissions(roleNode, workspace, Set.of(path), permission);
        save(roleNode);
    }
    public void removeAllPermissions(final Role role) {
        final PowerNode roleNode = getRoleNodeOrThrow(role);
        roleNode
                .streamChildren(new IsNamePrefix<>("acl_"))
                .filter(aclNode -> !Objects.equals(getAclNodeName(WEB_ACCESS_WORKSPACE), aclNode.getName()))
                .forEach(PowerNode::remove);
        save(roleNode);
    }
    public void removeAllPermissions(final Role role, final String workspace) {
        final PowerNode roleNode = getRoleNodeOrThrow(role);
        roleNode.getChild(getAclNodeName(workspace)).ifPresent(PowerNode::remove);
        save(roleNode);
    }
    public void removePermissions(final Role role, final Map<String, Permissions> workspacePermissionMapping) {
        final PowerNode roleNode = getRoleNodeOrThrow(role);
        workspacePermissionMapping.forEach((workspace, permissions) ->
                removePermissions(roleNode, workspace, permissions.paths(), permissions.permission())
        );
        save(roleNode);
    }
    public void removePermissions(final Role role, final String workspace, final Set<String> paths, final long permission) {
        final PowerNode roleNode = getRoleNodeOrThrow(role);
        removePermissions(roleNode, workspace, paths, permission);
        save(roleNode);
    }

    private void addPermissions(final PowerNode roleNode, final String workspace, final Set<String> paths, final long permission) {
        final PowerNode aclNode = getOrAddAclNode(roleNode, workspace);
        paths.forEach(path -> addPermission(aclNode, path, permission));
    }
    private void addPermission(final PowerNode aclNode, final String path, final long permission) {
        if(getPermissionNode(aclNode, path).isEmpty()) {
            final String nodeName = Exceptions.wrap().get(() -> nodeNameHelper.getUniqueName(aclNode.getSession(), aclNode.getPath(), "0"));
            final PowerNode permissionNode = aclNode.addNode(nodeName, NodeTypes.ContentNode.NAME);
            permissionNode.setProperty("path", path);
            permissionNode.setProperty("permissions", permission);
        }
    }

    private void removePermissions(final PowerNode roleNode, final String workspace, final Set<String> paths, final Long permission) {
        final PowerNode aclNode = getOrAddAclNode(roleNode, workspace);
        paths.forEach(path -> removePermission(aclNode, path, permission));
    }
    private void removePermission(final PowerNode aclNode, final String path, final Long permission) {
        getPermissionNode(aclNode, path)
                .filter(permissionNode ->
                    permission == MgnlRole.PERMISSION_ANY || permissionNode.getProperty("permissions", ValueConverter::getLong).map(permission::equals).orElse(false)
                )
                .ifPresent(PowerNode::remove);
    }

    private void save(final PowerNode roleNode) {
        if (saveSession) {
            roleNode.run(node -> node.getSession().save());
        }
    }
    private Optional<PowerNode> getRoleNode(final Role role) {
        return sessionProvider.apply(getRepositoryName()).flatMap(session ->
                powerNodeService.getByIdentifier(session, role.getId())
        );
    }
    private PowerNode getRoleNodeOrThrow(final Role role) {
        return getRoleNode(role).orElseThrow(() ->
                new NullPointerException("role node not present!")
        );
    }
    private Stream<PowerNode> streamRoleNodesByNames(final Set<String> names) {
        return sessionProvider.apply(getRepositoryName())
                .map(powerNodeService::getRootNode)
                .stream()
                .flatMap(root -> root.streamChildren(new IsPrimaryNodeType<>(NodeTypes.Role.NAME)))
                .filter(roleNode -> names.contains(roleNode.getName()));
    }
    private PowerNode getOrAddAclNode(final PowerNode roleNode, final String workspace) {
        return roleNode.getOrAddChild(getAclNodeName(workspace), NodeTypes.ContentNode.NAME);
    }
    private Optional<PowerNode> getPermissionNode(final PowerNode aclNode, final String path) {
        return aclNode
                .streamChildren(new IsPrimaryNodeType<>(NodeTypes.ContentNode.NAME))
                .filter(permissionNode ->
                    permissionNode.getProperty("path", ValueConverter::getString).map(path::equals).orElse(false)
                )
                .findFirst();
    }
    private String getAclNodeName(final String workspace) {
        return "acl_" + workspace;
    }

    public record Permissions(long permission, Set<String> paths){}

    public static class Factory {
        private final NodeNameHelper nodeNameHelper;
        private final PowerNodeService powerNodeService;

        @Inject
        public Factory(
                final NodeNameHelper nodeNameHelper,
                final PowerNodeService powerNodeService
        ) {
            this.nodeNameHelper = nodeNameHelper;
            this.powerNodeService = powerNodeService;
        }

        public BulkMgnlRoleManager create() {
            return create(powerNodeService::getSystemSession);
        }
        public BulkMgnlRoleManager create(final Function<String, Optional<Session>> sessionProvider) {
            return create(sessionProvider, true);
        }
        public BulkMgnlRoleManager create(final Function<String, Optional<Session>> sessionProvider, final boolean saveSession) {
            return new BulkMgnlRoleManager(nodeNameHelper, powerNodeService, new CachedSessionProvider(sessionProvider), saveSession);
        }
    }
}
