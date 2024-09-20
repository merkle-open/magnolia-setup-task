package com.merkle.oss.magnolia.setup.task;

import info.magnolia.module.InstallContextImpl;
import info.magnolia.module.InstallStatus;
import info.magnolia.module.ModuleRegistry;
import info.magnolia.module.delta.Delta;
import info.magnolia.module.delta.Task;
import info.magnolia.module.delta.TaskExecutionException;
import info.magnolia.module.model.ModuleDefinition;
import info.magnolia.module.model.Version;
import info.magnolia.objectfactory.ComponentProvider;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Can be used to execute tasks from magnolia groovy console:
 * <pre>{@code
 * import com.namics.common.setup.task.TaskExecutor;
 * import info.magnolia.objectfactory.Components;
 *
 * final TaskExecutor executor = Components.newInstance(TaskExecutor.class);
 * executor.execute(com.namics.snb.web.setup.task.migration.VisionTeaserTargetNodeNameMigrationTask.class)
 * }</pre>
 */
public class TaskExecutor {
	private final ModuleRegistry moduleRegistry;
	private final ComponentProvider componentProvider;
	private final Set<Session> sessions = new HashSet<>();

	@Inject
	public TaskExecutor(
			final ModuleRegistry moduleRegistry,
			final ComponentProvider componentProvider
	) {
		this.moduleRegistry = moduleRegistry;
		this.componentProvider = componentProvider;
	}

	public void execute(final Class<? extends Task> taskClazz) throws TaskExecutionException {
		execute(componentProvider.newInstance(taskClazz));
	}

	public void execute(final Task task) throws TaskExecutionException {
		execute(task, getModuleDefinition(task.getClass()).orElse(null));
	}

	public void execute(final Class<? extends Task> taskClazz, @Nullable final ModuleDefinition module) throws TaskExecutionException {
		execute(componentProvider.newInstance(taskClazz), module);
	}

	public void execute(final Task task, @Nullable final ModuleDefinition module) throws TaskExecutionException {
		execute(task, module, true);
	}

	public void execute(final Task task, @Nullable final ModuleDefinition module, final boolean saveSession) throws TaskExecutionException {
		final InstallContextImpl installContext = new InstallContextImpl(moduleRegistry) {
			@Override
			public int getTotalTaskCount() {
				return 1;
			}
			@Override
			public InstallStatus getStatus() {
				return InstallStatus.inProgress;
			}
			@Override
			public Session getJCRSession(String workspaceName) throws RepositoryException {
				final Session session = super.getJCRSession(workspaceName);
				sessions.add(session);
				return session;
			}
		};
		if(module != null) {
			installContext.setCurrentModule(module);
		}
		task.execute(installContext);
		if(saveSession) {
			for (Session session : sessions) {
				try {
					session.save();
				} catch (Exception e) {
					throw new TaskExecutionException("Failed to save session", e);
				}
			}
		}
	}

	private Optional<ModuleDefinition> getModuleDefinition(final Class<? extends Task> taskClass) {
		return moduleRegistry.getModuleNames().stream()
				.map(moduleRegistry::getDefinition)
				.filter(moduleDefinition -> contains(moduleDefinition, taskClass))
				.findFirst();
	}

	private boolean contains(final ModuleDefinition moduleDefinition, final Class<? extends Task> taskClass) {
		final InstallContextImpl installContext = new InstallContextImpl(moduleRegistry);
		installContext.setCurrentModule(new ModuleDefinition(
				moduleDefinition.getName(),
				Version.parseVersion(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE),
				moduleDefinition.getClassName(),
				moduleDefinition.getVersionHandler()
		));
		return moduleRegistry
				.getVersionHandler(moduleDefinition.getName())
				.getDeltas(installContext, null)
				.stream()
				.map(Delta::getTasks)
				.flatMap(Collection::stream)
				.map(Object::getClass)
				.anyMatch(taskClass::equals);
	}
}
