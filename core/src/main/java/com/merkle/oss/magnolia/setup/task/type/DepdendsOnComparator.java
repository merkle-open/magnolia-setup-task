package com.merkle.oss.magnolia.setup.task.type;

import info.magnolia.module.delta.Task;

import java.util.Comparator;
import java.util.Objects;


public class DepdendsOnComparator implements Comparator<Task> {
    @Override
    public int compare(final Task task1, final Task task2) {
        if (!(task1 instanceof VersionAwareTask) && !(task2 instanceof VersionAwareTask)) {
            return 0;
        }
        if (!(task1 instanceof VersionAwareTask versionAwareTask1)) {
            return -1;
        }
        if (!(task2 instanceof VersionAwareTask versionAwareTask2)) {
            return 1;
        }

        if (versionAwareTask1.dependsOn().isEmpty() && versionAwareTask2.dependsOn().isEmpty()) {
            return 0;
        }
        if (versionAwareTask1.dependsOn().isEmpty()) {
            return -1;
        }
        if (versionAwareTask2.dependsOn().isEmpty()) {
            return 1;
        }

        if (Objects.equals(versionAwareTask1.dependsOn().get().getClass(), versionAwareTask2.getClass())) {
            return 1;
        }
        if (Objects.equals(versionAwareTask2.dependsOn().get().getClass(), versionAwareTask1.getClass())) {
            return -1;
        }
        return compare(versionAwareTask1.dependsOn().get(), versionAwareTask2.dependsOn().get());

    }
}
