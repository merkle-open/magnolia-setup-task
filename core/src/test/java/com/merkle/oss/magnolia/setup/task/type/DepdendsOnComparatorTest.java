package com.merkle.oss.magnolia.setup.task.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import info.magnolia.module.InstallContext;
import info.magnolia.module.delta.Task;
import info.magnolia.module.delta.TaskExecutionException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class DepdendsOnComparatorTest {

    @Test
    void sort() {
        final VersionAwareTask task2 = new Task2();
        final VersionAwareTask task1 = new Task1();
        final VersionAwareTask task3 = new Task3();
        final VersionAwareTask task4 = new Task4();
        final VersionAwareTask task5 = new Task5();
        final Task task6 = new MockTask();
        assertEquals(
                List.of(task6, task1, task2, task5, task4, task3),
                Stream.of(task2, task4, task5, task6, task1, task3).sorted(new DepdendsOnComparator()).toList()
        );

        assertEquals(
                List.of(task6, task1, task5, task2, task3, task4),
                Stream.of(task3, task6, task1, task5, task2, task4).sorted(new DepdendsOnComparator()).toList()
        );
    }

    private static class Task1 extends MockVersionAwareTask {}
    private static class Task2 extends MockVersionAwareTask {
        @Override
        public Optional<VersionAwareTask> dependsOn() {
            return Optional.of(new Task1());
        }
    }
    private static class Task3 extends MockVersionAwareTask {
        @Override
        public Optional<VersionAwareTask> dependsOn() {
            return Optional.of(new Task2());
        }
    }
    private static class Task4 extends MockVersionAwareTask {
        @Override
        public Optional<VersionAwareTask> dependsOn() {
            return Optional.of(new Task2());
        }
    }
    private static class Task5 extends MockVersionAwareTask {
        @Override
        public Optional<VersionAwareTask> dependsOn() {
            return Optional.of(new Task1());
        }
    }

    private static abstract class MockVersionAwareTask extends MockTask implements VersionAwareTask {}

    private static class MockTask implements Task {
        @Override
        public String getName() {
            return getClass().getSimpleName();
        }
        @Override
        public String getDescription() {
            return getClass().getSimpleName()+"_description";
        }
        @Override
        public void execute(final InstallContext installContext) {}
        @Override
        public String toString() {
            return getName();
        }
    }
}