/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.execution.taskgraph;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.NonNullApi;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.execution.TaskExecutionAdapter;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.execution.TaskExecutionGraphListener;
import org.gradle.api.execution.TaskExecutionListener;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.TaskState;
import org.gradle.configuration.internal.ListenerBuildOperationDecorator;
import org.gradle.execution.TaskExecutionGraphInternal;
import org.gradle.internal.Cast;
import org.gradle.internal.event.ListenerBroadcast;
import org.gradle.internal.operations.BuildOperationContext;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.operations.BuildOperationRef;
import org.gradle.internal.operations.CurrentBuildOperationRef;
import org.gradle.internal.operations.RunnableBuildOperation;
import org.gradle.internal.resources.ResourceLockCoordinationService;
import org.gradle.internal.resources.ResourceLockState;
import org.gradle.internal.time.Time;
import org.gradle.internal.time.Timer;
import org.gradle.internal.work.WorkerLeaseService;
import org.gradle.listener.ClosureBackedMethodInvocationDispatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@NonNullApi
public class DefaultTaskExecutionGraph implements TaskExecutionGraphInternal {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTaskExecutionGraph.class);

    private enum TaskGraphState {
        EMPTY, DIRTY, POPULATED
    }

    private final TaskPlanExecutor taskPlanExecutor;
    private final ResourceLockCoordinationService coordinationService;
    private final List<WorkInfoExecutor> workInfoExecutors;
    private final GradleInternal gradleInternal;
    private final ListenerBroadcast<TaskExecutionGraphListener> graphListeners;
    private final ListenerBroadcast<TaskExecutionListener> taskListeners;
    private final DefaultTaskExecutionPlan taskExecutionPlan;
    private final BuildOperationExecutor buildOperationExecutor;
    private final ListenerBuildOperationDecorator listenerBuildOperationDecorator;
    private TaskGraphState taskGraphState = TaskGraphState.EMPTY;
    private List<Task> allTasks;
    private boolean hasFiredWhenReady;

    private final Set<Task> requestedTasks = Sets.newTreeSet();

    public DefaultTaskExecutionGraph(
        TaskPlanExecutor taskPlanExecutor,
        List<WorkInfoExecutor> workInfoExecutors,
        BuildOperationExecutor buildOperationExecutor,
        ListenerBuildOperationDecorator listenerBuildOperationDecorator,
        WorkerLeaseService workerLeaseService,
        ResourceLockCoordinationService coordinationService,
        GradleInternal gradleInternal,
        TaskInfoFactory taskInfoFactory,
        TaskDependencyResolver dependencyResolver,
        ListenerBroadcast<TaskExecutionGraphListener> graphListeners,
        ListenerBroadcast<TaskExecutionListener> taskListeners
    ) {
        this.taskPlanExecutor = taskPlanExecutor;
        this.workInfoExecutors = workInfoExecutors;
        this.buildOperationExecutor = buildOperationExecutor;
        this.listenerBuildOperationDecorator = listenerBuildOperationDecorator;
        this.coordinationService = coordinationService;
        this.gradleInternal = gradleInternal;
        this.graphListeners = graphListeners;
        this.taskListeners = taskListeners;
        this.taskExecutionPlan = new DefaultTaskExecutionPlan(workerLeaseService, gradleInternal, taskInfoFactory, dependencyResolver);
    }

    @Override
    public void setContinueOnFailure(boolean continueOnFailure) {
        taskExecutionPlan.setContinueOnFailure(continueOnFailure);
    }

    @Override
    public void useFilter(Spec<? super Task> filter) {
        Spec<? super Task> castFilter = Cast.uncheckedCast(filter != null ? filter : Specs.SATISFIES_ALL);
        taskExecutionPlan.useFilter(castFilter);
        taskGraphState = TaskGraphState.DIRTY;
    }

    @Override
    public void addTasks(Iterable<? extends Task> tasks) {
        assert tasks != null;

        final Timer clock = Time.startTimer();

        Set<Task> taskSet = new LinkedHashSet<Task>();
        for (Task task : tasks) {
            taskSet.add(task);
            requestedTasks.add(task);
        }

        taskExecutionPlan.addToTaskGraph(taskSet);
        taskGraphState = TaskGraphState.DIRTY;

        LOGGER.debug("Timing: Creating the DAG took " + clock.getElapsed());
    }

    @Override
    public void populate() {
        ensurePopulated();
    }

    @Override
    public void execute(Collection<? super Throwable> failures) {
        Timer clock = Time.startTimer();
        ensurePopulated();
        if (!hasFiredWhenReady) {
            buildOperationExecutor.run(new NotifyTaskGraphWhenReady(this, graphListeners.getSource(), gradleInternal));
            hasFiredWhenReady = true;
        } else if (!graphListeners.isEmpty()) {
            LOGGER.warn("Ignoring listeners of task graph ready event, as this build (" + gradleInternal.getIdentityPath() + ") has already run tasks.");
        }
        try {
            taskPlanExecutor.process(taskExecutionPlan, failures, new BuildOperationAwareWorkItemExecutor(workInfoExecutors, buildOperationExecutor.getCurrentOperation()));
            LOGGER.debug("Timing: Executing the DAG took " + clock.getElapsed());
        } finally {
            coordinationService.withStateLock(new Transformer<ResourceLockState.Disposition, ResourceLockState>() {
                @Override
                public ResourceLockState.Disposition transform(ResourceLockState resourceLockState) {
                    taskExecutionPlan.clear();
                    return ResourceLockState.Disposition.FINISHED;
                }
            });
        }
    }

    @Override
    public void addTaskExecutionGraphListener(TaskExecutionGraphListener listener) {
        graphListeners.add(listenerBuildOperationDecorator.decorate("TaskExecutionGraph.addTaskExecutionGraphListener", TaskExecutionGraphListener.class, listener));
    }

    @Override
    public void removeTaskExecutionGraphListener(TaskExecutionGraphListener listener) {
        graphListeners.remove(listener);
    }

    @Override
    public void whenReady(final Closure closure) {
        graphListeners.add(new ClosureBackedMethodInvocationDispatch("graphPopulated", listenerBuildOperationDecorator.decorate("TaskExecutionGraph.whenReady", closure)));
    }

    @Override
    public void whenReady(final Action<TaskExecutionGraph> action) {
        graphListeners.add(listenerBuildOperationDecorator.decorate("TaskExecutionGraph.whenReady", TaskExecutionGraphListener.class, new TaskExecutionGraphListener() {
            @Override
            public void graphPopulated(TaskExecutionGraph graph) {
                action.execute(graph);
            }
        }));
    }

    @Override
    public void addTaskExecutionListener(TaskExecutionListener listener) {
        taskListeners.add(listener);
    }

    @Override
    public void removeTaskExecutionListener(TaskExecutionListener listener) {
        taskListeners.remove(listener);
    }

    @Override
    public void beforeTask(final Closure closure) {
        taskListeners.add(new ClosureBackedMethodInvocationDispatch("beforeExecute", closure));
    }

    @Override
    public void beforeTask(final Action<Task> action) {
        taskListeners.add(new TaskExecutionAdapter() {
            @Override
            public void beforeExecute(Task task) {
                action.execute(task);
            }
        });
    }

    @Override
    public void afterTask(final Closure closure) {
        taskListeners.add(new ClosureBackedMethodInvocationDispatch("afterExecute", closure));
    }

    @Override
    public void afterTask(final Action<Task> action) {
        taskListeners.add(new TaskExecutionAdapter() {
            @Override
            public void afterExecute(Task task, TaskState state) {
                action.execute(task);
            }
        });
    }

    @Override
    public boolean hasTask(Task task) {
        ensurePopulated();
        return taskExecutionPlan.getTasks().contains(task);
    }

    @Override
    public boolean hasTask(String path) {
        ensurePopulated();
        for (Task task : taskExecutionPlan.getTasks()) {
            if (task.getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        return taskExecutionPlan.size();
    }

    @Override
    public List<Task> getAllTasks() {
        ensurePopulated();
        if (allTasks == null) {
            allTasks = ImmutableList.copyOf(taskExecutionPlan.getTasks());
        }
        return allTasks;
    }

    @Override
    public Set<Task> getDependencies(Task task) {
        ensurePopulated();
        return taskExecutionPlan.getDependencies(task);
    }

    private void ensurePopulated() {
        switch (taskGraphState) {
            case EMPTY:
                throw new IllegalStateException(
                    "Task information is not available, as this task execution graph has not been populated.");
            case DIRTY:
                taskExecutionPlan.determineExecutionPlan();
                allTasks = null;
                taskGraphState = TaskGraphState.POPULATED;
                return;
            case POPULATED:
        }
    }

    /**
     * This action executes a task via the task executer wrapping everything into a build operation.
     */
    private class BuildOperationAwareWorkItemExecutor implements Action<WorkInfo> {
        private final BuildOperationRef parentOperation;
        private final List<WorkInfoExecutor> workInfoExecutors;

        BuildOperationAwareWorkItemExecutor(List<WorkInfoExecutor> workInfoExecutors, BuildOperationRef parentOperation) {
            this.workInfoExecutors = workInfoExecutors;
            this.parentOperation = parentOperation;
        }

        @Override
        public void execute(WorkInfo work) {
            BuildOperationRef previous = CurrentBuildOperationRef.instance().get();
            CurrentBuildOperationRef.instance().set(parentOperation);
            try {
                for (WorkInfoExecutor workInfoExecutor : workInfoExecutors) {
                    if (workInfoExecutor.execute(work)) {
                        return;
                    }
                }
                throw new IllegalStateException("Unknown type of work: " + work);
            } finally {
                CurrentBuildOperationRef.instance().set(previous);
            }
        }
    }

    @Override
    public Set<Task> getRequestedTasks() {
        return requestedTasks;
    }

    @Override
    public Set<Task> getFilteredTasks() {
        /*
            Note: we currently extract this information from the execution plan because it's
            buried under functions in #filter. This could be detangled/simplified by introducing
            excludeTasks(Iterable<Task>) as an analog to addTasks(Iterable<Task>).

            This is too drastic a change for the stage in the release cycle were exposing this information
            was necessary, therefore the minimal change solution was implemented.
         */
        return taskExecutionPlan.getFilteredTasks();
    }

    private static class NotifyTaskGraphWhenReady implements RunnableBuildOperation {

        private final TaskExecutionGraph taskExecutionGraph;
        private final TaskExecutionGraphListener graphListener;
        private final GradleInternal gradleInternal;

        private NotifyTaskGraphWhenReady(TaskExecutionGraph taskExecutionGraph, TaskExecutionGraphListener graphListener, GradleInternal gradleInternal) {
            this.taskExecutionGraph = taskExecutionGraph;
            this.graphListener = graphListener;
            this.gradleInternal = gradleInternal;
        }

        @Override
        public void run(BuildOperationContext context) {
            graphListener.graphPopulated(taskExecutionGraph);
            context.setResult(NotifyTaskGraphWhenReadyBuildOperationType.RESULT);
        }

        @Override
        public BuildOperationDescriptor.Builder description() {
            return BuildOperationDescriptor.displayName(gradleInternal.contextualize("Notify task graph whenReady listeners"))
                .details(new NotifyTaskGraphWhenReadyBuildOperationType.DetailsImpl(
                    gradleInternal.getIdentityPath()
                ));
        }
    }
}
