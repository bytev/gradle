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

import groovy.lang.Closure;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionGraphListener;
import org.gradle.api.execution.TaskExecutionListener;
import org.gradle.api.specs.Spec;
import org.gradle.execution.TaskFailureHandler;
import org.gradle.execution.TaskGraphExecuter;
import org.gradle.listener.ListenerBroadcast;
import org.gradle.listener.ListenerManager;
import org.gradle.util.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Hans Dockter
 */
public class DefaultTaskGraphExecuter implements TaskGraphExecuter {
    private static Logger logger = LoggerFactory.getLogger(DefaultTaskGraphExecuter.class);

    private final TaskExecutor taskExecutor;
    private final ListenerBroadcast<TaskExecutionGraphListener> graphListeners;
    private final ListenerBroadcast<TaskExecutionListener> taskListeners;
    private final DefaultTaskExecutionPlan taskExecutionPlan = new DefaultTaskExecutionPlan();
    private boolean populated;

    public DefaultTaskGraphExecuter(ListenerManager listenerManager, TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
        graphListeners = listenerManager.createAnonymousBroadcaster(TaskExecutionGraphListener.class);
        taskListeners = listenerManager.createAnonymousBroadcaster(TaskExecutionListener.class);
    }

    public void useFilter(Spec<? super Task> filter) {
        taskExecutionPlan.useFilter(filter);
    }

    public void useFailureHandler(TaskFailureHandler handler) {
        taskExecutionPlan.useFailureHandler(handler);
    }

    public void addTasks(Iterable<? extends Task> tasks) {
        assert tasks != null;

        Clock clock = new Clock();

        Set<Task> taskSet = new LinkedHashSet<Task>();
        for (Task task : tasks) {
            taskSet.add(task);
        }
        taskExecutionPlan.addToTaskGraph(taskSet);
        populated = true;

        logger.debug("Timing: Creating the DAG took " + clock.getTime());
    }

    public void execute() {
        assertPopulated();
        Clock clock = new Clock();

        graphListeners.getSource().graphPopulated(this);
        try {
            taskExecutor.process(taskExecutionPlan, taskListeners.getSource());
            logger.debug("Timing: Executing the DAG took " + clock.getTime());
        } finally {
            taskExecutionPlan.clear();
        }
    }

    public void addTaskExecutionGraphListener(TaskExecutionGraphListener listener) {
        graphListeners.add(listener);
    }

    public void removeTaskExecutionGraphListener(TaskExecutionGraphListener listener) {
        graphListeners.remove(listener);
    }

    public void whenReady(final Closure closure) {
        graphListeners.add("graphPopulated", closure);
    }

    public void addTaskExecutionListener(TaskExecutionListener listener) {
        taskListeners.add(listener);
    }

    public void removeTaskExecutionListener(TaskExecutionListener listener) {
        taskListeners.remove(listener);
    }

    public void beforeTask(final Closure closure) {
        taskListeners.add("beforeExecute", closure);
    }

    public void afterTask(final Closure closure) {
        taskListeners.add("afterExecute", closure);
    }

    public boolean hasTask(Task task) {
        assertPopulated();
        return taskExecutionPlan.getTasks().contains(task);
    }

    public boolean hasTask(String path) {
        assertPopulated();
        assert path != null && path.length() > 0;
        for (Task task : taskExecutionPlan.getTasks()) {
            if (task.getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    public List<Task> getAllTasks() {
        assertPopulated();
        return taskExecutionPlan.getTasks();
    }

    private void assertPopulated() {
        if (!populated) {
            throw new IllegalStateException(
                    "Task information is not available, as this task execution graph has not been populated.");
        }
    }
}
