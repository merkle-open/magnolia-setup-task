package com.merkle.oss.magnolia.setup.task.common.security.util;

import static info.magnolia.cms.security.SecurityConstants.*;

import info.magnolia.cms.security.Group;
import info.magnolia.cms.security.MgnlGroupManager;
import info.magnolia.cms.security.Role;
import info.magnolia.jcr.util.NodeNameHelper;
import info.magnolia.jcr.util.NodeTypes;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.jcr.Property;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.machinezoo.noexception.Exceptions;
import com.merkle.oss.magnolia.powernode.PowerNode;
import com.merkle.oss.magnolia.powernode.PowerNodeService;
import com.merkle.oss.magnolia.powernode.ValueConverter;
import com.merkle.oss.magnolia.powernode.predicate.IsPrimaryNodeType;

import jakarta.inject.Inject;

public class BulkMgnlGroupManager extends MgnlGroupManager {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Predicate<Property> IS_SYSTEM_PROPERTY = property -> Set.of(NodeTypes.JCR_PREFIX, NodeTypes.MGNL_PREFIX).stream().anyMatch(prefix ->
            Exceptions.wrap().get(property::getName).startsWith(prefix)
    );
    private final PowerNodeService powerNodeService;
    private final Function<String, Optional<Session>> sessionProvider;
    private final boolean saveSession;
    private final BulkMgnlRoleManager roleManager;

    public BulkMgnlGroupManager(
            final NodeNameHelper nodeNameHelper,
            final PowerNodeService powerNodeService,
            final BulkMgnlRoleManager.Factory bulkMgnlRoleManagerFactory,
            final Function<String, Optional<Session>> sessionProvider,
            final boolean saveSession
    ) {
        super(nodeNameHelper);
        this.powerNodeService = powerNodeService;
        this.sessionProvider = sessionProvider;
        this.saveSession = saveSession;
        this.roleManager = bulkMgnlRoleManagerFactory.create(sessionProvider, saveSession);
    }

    @Override
    public Group getGroup(final String name) {
        return getGroups(Set.of(name)).stream().findFirst().orElseGet(() -> {
            LOG.debug("can't find group [{}]", name);
            return null;
        });
    }
    public Set<Group> getGroups(final Set<String> names) {
        return streamGroupNodesByNames(names)
                .map(groupNode -> Exceptions.wrap().get(() -> newGroupInstance(groupNode)))
                .collect(Collectors.toSet());
    }
    private Group reload(final Group group) {
        return getGroupNode(group).map(node -> Exceptions.wrap().get(() -> newGroupInstance(node))).orElse(null);
    }

    @Override
    public Group addRole(final Group group, final String roleName) {
        addRoles(group, Set.of(roleName));
        return reload(group);
    }
    public void addRoles(final Group group, final Set<String> roleNames) {
        final PowerNode rolesNode = getOrAddRolesNode(getGroupNodeOrThrow(group));
        addProperties(rolesNode, roleManager.getRoles(roleNames).stream().map(Role::getId).collect(Collectors.toSet()));
        save(rolesNode);
    }

    @Override
    public Group removeRole(final Group group, final String roleName) {
        removeRoles(group, Set.of(roleName));
        return reload(group);
    }
    public void removeRoles(final Group group, final Set<String> roleNames) {
        final PowerNode rolesNode = getOrAddRolesNode(getGroupNodeOrThrow(group));
        removeProperties(rolesNode, roleManager.getRoles(roleNames).stream().map(Role::getId).collect(Collectors.toSet()));
        save(rolesNode);
    }
    public void removeAllRoles(final Group group) {
        final PowerNode rolesNode = getOrAddRolesNode(getGroupNodeOrThrow(group));
        removeProperties(rolesNode, ignored -> true);
        save(rolesNode);
    }

    @Override
    public Group addGroup(final Group group, final String groupName) {
        addGroups(group, Set.of(groupName));
        return reload(group);
    }
    public Group addGroups(final Group group, final Set<String> groupNames) {
        final PowerNode groupsNode = getOrAddGroupsNode(getGroupNodeOrThrow(group));
        addProperties(groupsNode, getGroups(groupNames).stream().map(Group::getId).collect(Collectors.toSet()));
        save(groupsNode);
        return reload(group);
    }

    @Override
    public Group removeGroup(final Group group, final String groupName) {
        removeGroups(group, Set.of(groupName));
        return reload(group);
    }
    public void removeGroups(final Group group, final Set<String> groupNames) {
        final PowerNode groupsNode = getOrAddGroupsNode(getGroupNodeOrThrow(group));
        removeProperties(groupsNode, getGroups(groupNames).stream().map(Group::getId).collect(Collectors.toSet()));
        save(groupsNode);
    }
    public void removeAllGroups(final Group group) {
        final PowerNode groupsNode = getOrAddGroupsNode(getGroupNodeOrThrow(group));
        removeProperties(groupsNode, ignored -> true);
        save(groupsNode);
    }

    private void save(final PowerNode roleNode) {
        if (saveSession) {
            roleNode.run(node -> node.getSession().save());
        }
    }
    private Optional<PowerNode> getGroupNode(final Group group) {
        return sessionProvider.apply(getRepositoryName()).flatMap(session ->
                powerNodeService.getByIdentifier(session, group.getId())
        );
    }
    private PowerNode getGroupNodeOrThrow(final Group group) {
        return getGroupNode(group).orElseThrow(() ->
                new NullPointerException("group node not present!")
        );
    }
    private PowerNode getOrAddRolesNode(final PowerNode groupNode) {
        return groupNode.getOrAddChild(NODE_ROLES, NodeTypes.ContentNode.NAME);
    }
    private PowerNode getOrAddGroupsNode(final PowerNode groupNode) {
        return groupNode.getOrAddChild(NODE_GROUPS, NodeTypes.ContentNode.NAME);
    }
    private Stream<PowerNode> streamGroupNodesByNames(final Set<String> names) {
        return sessionProvider.apply(getRepositoryName())
                .map(powerNodeService::getRootNode)
                .stream()
                .flatMap(root -> root.streamChildren(new IsPrimaryNodeType<>(NodeTypes.Group.NAME)))
                .filter(groupNode -> names.contains(groupNode.getName()));
    }
    private void removeProperties(final PowerNode node, final Set<String> values) {
        removeProperties(node, values::contains);
    }
    private void removeProperties(final PowerNode node, final Predicate<String> predicate) {
        final Iterator<Property> iterator = node.getProperties();
        StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false)
                .filter(Predicate.not(IS_SYSTEM_PROPERTY))
                .filter(property -> predicate.test(Exceptions.wrap().get(property::getString)))
                .forEach(property ->
                        Exceptions.wrap().run(property::remove)
                );
    }
    private void addProperties(final PowerNode node, final Set<String> values) {
        values.forEach(value -> addProperty(node, value));
    }
    private void addProperty(final PowerNode node, final String value) {
        final String propertyName = Exceptions.wrap().get(() -> nodeNameHelper.getUniqueName(node, "0"));
        node.setProperty(propertyName, value, ValueConverter::toValue);
    }

    public static class Factory {
        private final NodeNameHelper nodeNameHelper;
        private final PowerNodeService powerNodeService;
        private final BulkMgnlRoleManager.Factory bulkMgnlRoleManagerFactory;

        @Inject
        public Factory(
                final NodeNameHelper nodeNameHelper,
                final PowerNodeService powerNodeService,
                final BulkMgnlRoleManager.Factory bulkMgnlRoleManagerFactory
        ) {
            this.nodeNameHelper = nodeNameHelper;
            this.powerNodeService = powerNodeService;
            this.bulkMgnlRoleManagerFactory = bulkMgnlRoleManagerFactory;
        }

        public BulkMgnlGroupManager create() {
            return create(powerNodeService::getSystemSession);
        }
        public BulkMgnlGroupManager create(final Function<String, Optional<Session>> sessionProvider) {
            return create(sessionProvider, true);
        }
        public BulkMgnlGroupManager create(final Function<String, Optional<Session>> sessionProvider, final boolean saveSession) {
            return new BulkMgnlGroupManager(nodeNameHelper, powerNodeService, bulkMgnlRoleManagerFactory, new CachedSessionProvider(sessionProvider), saveSession);
        }
    }
}
