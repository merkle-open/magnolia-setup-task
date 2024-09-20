package com.merkle.oss.magnolia.setup;

import info.magnolia.module.DefaultModuleVersionHandler;
import info.magnolia.module.InstallContext;
import info.magnolia.module.delta.Delta;
import info.magnolia.module.delta.DeltaBuilder;
import info.magnolia.module.delta.Task;
import info.magnolia.module.model.Version;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.merkle.oss.magnolia.setup.task.type.DepdendsOnComparator;
import com.merkle.oss.magnolia.setup.task.type.InstallAndUpdateTask;
import com.merkle.oss.magnolia.setup.task.type.InstallTask;
import com.merkle.oss.magnolia.setup.task.type.LocalDevelopmentStartupTask;
import com.merkle.oss.magnolia.setup.task.type.ModuleStartupTask;
import com.merkle.oss.magnolia.setup.task.type.SnapshotStartupTask;
import com.merkle.oss.magnolia.setup.task.type.UpdateTask;
import com.merkle.oss.magnolia.setup.task.type.VersionAwareTask;

public abstract class EnhancedModuleVersionHandler extends DefaultModuleVersionHandler {
    private final Set<InstallTask> installTasks;
    private final Set<UpdateTask> updateTasks;
    private final Set<InstallAndUpdateTask> installAndUpdateTasks;
    private final Set<ModuleStartupTask> moduleStartupTasks;
    private final Set<SnapshotStartupTask> snapshotStartupTasks;
    private final Set<LocalDevelopmentStartupTask> localDevelopmentStartupTasks;

    protected EnhancedModuleVersionHandler(
            final Set<InstallTask> installTasks,
            final Set<UpdateTask> updateTasks,
            final Set<InstallAndUpdateTask> installAndUpdateTasks,
            final Set<ModuleStartupTask> moduleStartupTasks,
            final Set<SnapshotStartupTask> snapshotStartupTasks,
            final Set<LocalDevelopmentStartupTask> localDevelopmentStartupTasks
    ) {
        this.installTasks = installTasks;
        this.updateTasks = updateTasks;
        this.installAndUpdateTasks = installAndUpdateTasks;
        this.moduleStartupTasks = moduleStartupTasks;
        this.snapshotStartupTasks = snapshotStartupTasks;
        this.localDevelopmentStartupTasks = localDevelopmentStartupTasks;
    }

    @Override
    public List<Delta> getDeltas(final InstallContext installContext, @Nullable final Version versionFrom) {
        final Version forVersion = installContext.getCurrentModuleDefinition().getVersion();
        return Stream.concat(
                super.getDeltas(installContext, versionFrom).stream(),
                getDeltas(installContext, forVersion, versionFrom)
        ).toList();
    }

    private Stream<Delta> getDeltas(final InstallContext installContext, final Version forVersion, @Nullable final Version versionFrom) {
        return Stream.of(
                getInstallAndUpdateTasksDelta(installContext, forVersion, versionFrom),
                getStartupTasksDelta(installContext, forVersion, versionFrom)
        );
    }

    private Delta getInstallAndUpdateTasksDelta(final InstallContext installContext, final Version forVersion, @Nullable final Version versionFrom) {
        final boolean isUpdate = forVersion.isStrictlyAfter(versionFrom);
        final boolean isInstall = versionFrom == null;

        return DeltaBuilder.install(forVersion, "setup-task install and update").addTasks(Stream.of(
                isInstall ? getInstallTasks(installContext, forVersion) : Stream.<Task>empty(),
                isUpdate ? getInstallAndUpdateTasks(installContext, forVersion, null) : Stream.<Task>empty(),
                (isInstall || isUpdate)? getUpdateTasks(installContext, forVersion, versionFrom) : Stream.<Task>empty()
        ).flatMap(Function.identity()).sorted(new DepdendsOnComparator()).toList());
    }

    private Delta getStartupTasksDelta(final InstallContext installContext, final Version forVersion, @Nullable final Version versionFrom) {
        return DeltaBuilder.startup(forVersion, "setup-task startup").addTasks( Stream.of(
                getModuleStartupTasks(installContext, forVersion, versionFrom),
                isSnapshot(forVersion) ? getSnapshotStartupTasks(installContext, forVersion, versionFrom) : Stream.<Task>empty(),
                isLocalDevelopmentEnvironment() ? getLocalDevelopmentStartupTasks(installContext, forVersion, versionFrom) : Stream.<Task>empty()
        ).flatMap(Function.identity()).sorted(new DepdendsOnComparator()).toList());
    }

    protected abstract boolean isLocalDevelopmentEnvironment();

    private boolean isSnapshot(final Version version) {
        return "SNAPSHOT".equalsIgnoreCase(version.getClassifier());
    }

    protected Stream<Task> getInstallTasks(final InstallContext installContext, final Version forVersion) {
        return filter(installTasks, forVersion, null);
    }

    protected Stream<Task> getInstallAndUpdateTasks(final InstallContext installContext, final Version forVersion, @Nullable final Version fromVersion) {
        return filter(installAndUpdateTasks, forVersion, fromVersion);
    }

    protected Stream<Task> getUpdateTasks(final InstallContext installContext, final Version forVersion, @Nullable final Version fromVersion) {
        return filter(updateTasks, forVersion, fromVersion);
    }

    protected Stream<Task> getModuleStartupTasks(final InstallContext installContext, final Version forVersion, @Nullable final Version fromVersion) {
        return filter(moduleStartupTasks, forVersion, fromVersion);
    }

    protected Stream<Task> getSnapshotStartupTasks(final InstallContext installContext, final Version forVersion, @Nullable final Version fromVersion) {
        return Stream.of(
                filter(snapshotStartupTasks, forVersion, fromVersion),
                // execute all general install and update tasks on snapshot
                getInstallAndUpdateTasks(installContext, forVersion, fromVersion),
                getUpdateTasks(installContext, forVersion, fromVersion)
        ).flatMap(Function.identity());
    }

    protected Stream<Task> getLocalDevelopmentStartupTasks(final InstallContext installContext, final Version forVersion, @Nullable final Version fromVersion) {
        return filter(localDevelopmentStartupTasks, forVersion, fromVersion);
    }

    protected Stream<Task> filter(final Collection<? extends VersionAwareTask> tasks, final Version forVersion, @Nullable final Version fromVersion) {
        return tasks
                .stream()
                .filter(task -> task.test(forVersion, fromVersion))
                .map(task -> task);
    }
}
