# Magnolia setup task

Setup task to help bootstrap magnolia.

## Implementation

```java
import info.magnolia.module.InstallContext;
import info.magnolia.module.model.Version;

import java.util.Optional;

import javax.annotation.Nullable;

import com.merkle.oss.magnolia.setup.task.type.InstallAndUpdateTask;
import com.merkle.oss.magnolia.setup.task.type.VersionAwareTask;

public class SomeTask implements InstallAndUpdateTask {

    @Override
    public String getName() {
        return "someTask";
    }

    @Override
    public String getDescription() {
        return "someTask description";
    }

    @Override
    public void execute(InstallContext installContext) {
        //do stuff
    }

    //Optional
    @Override
    public boolean test(final Version forVersion, @Nullable final Version fromVersion) {
        return true;
    }

    //Optional
    @Override
    public Optional<VersionAwareTask> dependsOn() {
        return Optional.empty();
    }
}

```


## Setup
### Guice Set-Binding
```java
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

import info.magnolia.objectfactory.guice.AbstractGuiceComponentConfigurer;

public class CustomGuiceComponentConfigurer extends AbstractGuiceComponentConfigurer {
    @Override
    protected void configure() {
        super.configure();
        final Multibinder<InstallTask> installTaskSetBinder = Multibinder.newSetBinder(binder(), InstallTask.class, Names.named("myModule"));
        installTaskSetBinder.addBinding().to(SomeInstallTask.class);
        ...
    }
}
```

### Module version handler
```xml
<module>
    <name>myModule</name>
    <versionHandler>...MyModuleVersionHandler</versionHandler>
    ...
</module>
```
```java
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import com.merkle.oss.magnolia.setup.EnhancedModuleVersionHandler;
import com.merkle.oss.magnolia.setup.task.type.InstallAndUpdateTask;
import com.merkle.oss.magnolia.setup.task.type.InstallTask;
import com.merkle.oss.magnolia.setup.task.type.LocalDevelopmentStartupTask;
import com.merkle.oss.magnolia.setup.task.type.ModuleStartupTask;
import com.merkle.oss.magnolia.setup.task.type.SnapshotStartupTask;
import com.merkle.oss.magnolia.setup.task.type.UpdateTask;

public class MyModuleVersionHandler extends EnhancedModuleVersionHandler {

    // Multibinding configured in SetupTasksGuiceComponentConfigurer
    @Inject
    public MyModuleVersionHandler(
            @Named("myModule") final Set<InstallTask> installTasks,
            @Named("myModule") final Set<UpdateTask> updateTasks,
            @Named("myModule") final Set<InstallAndUpdateTask> installAndUpdateTasks,
            @Named("myModule") final Set<ModuleStartupTask> moduleStartupTasks,
            @Named("myModule") final Set<SnapshotStartupTask> snapshotStartupTasks,
            @Named("myModule") final Set<LocalDevelopmentStartupTask> localDevelopmentStartupTasks
    ) {
        super(installTasks, updateTasks, installAndUpdateTasks, moduleStartupTasks, snapshotStartupTasks, localDevelopmentStartupTasks);
    }

    @Override
    protected boolean isLocalDevelopmentEnvironment() {
        return false; //TODO implement
    }
}
```