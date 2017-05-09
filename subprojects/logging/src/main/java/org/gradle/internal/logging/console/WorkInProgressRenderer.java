/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.internal.logging.console;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.gradle.internal.logging.events.BatchOutputEventListener;
import org.gradle.internal.logging.events.EndOutputEvent;
import org.gradle.internal.logging.events.MaxWorkerCountChangeEvent;
import org.gradle.internal.logging.events.OperationIdentifier;
import org.gradle.internal.logging.events.OutputEvent;
import org.gradle.internal.logging.events.OutputEventListener;
import org.gradle.internal.logging.events.ProgressCompleteEvent;
import org.gradle.internal.logging.events.ProgressEvent;
import org.gradle.internal.logging.events.ProgressStartEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WorkInProgressRenderer extends BatchOutputEventListener {
    private final OutputEventListener listener;
    private final ProgressOperations operations = new ProgressOperations();
    private final BuildProgressArea progressArea;
    private final DefaultWorkInProgressFormatter labelFormatter;
    private final ConsoleLayoutCalculator consoleLayoutCalculator;

    // Track all unused labels to display future progress operation
    private final Deque<StyledLabel> unusedProgressLabels;

    // Track currently associated label with its progress operation
    private final Map<OperationIdentifier, AssociationLabel> operationIdToAssignedLabels = new HashMap<OperationIdentifier, AssociationLabel>();

    // Track any progress operation that either can't be display due to label shortage or child progress operation is already been displayed
    private final Deque<ProgressOperation> unassignedProgressOperations = new ArrayDeque<ProgressOperation>();

    // Track the parent-children relation between progress operation to avoid displaying a parent when children are been displayed
    private final Map<OperationIdentifier, Set<OperationIdentifier>> parentIdToChildrenIds = new HashMap<OperationIdentifier, Set<OperationIdentifier>>();

    public WorkInProgressRenderer(OutputEventListener listener, BuildProgressArea progressArea, DefaultWorkInProgressFormatter labelFormatter, ConsoleLayoutCalculator consoleLayoutCalculator) {
        this.listener = listener;
        this.progressArea = progressArea;
        this.labelFormatter = labelFormatter;
        this.consoleLayoutCalculator = consoleLayoutCalculator;
        this.unusedProgressLabels = new ArrayDeque<StyledLabel>(progressArea.getBuildProgressLabels());
    }

    @Override
    public void onOutput(OutputEvent event) {
        if (event instanceof ProgressStartEvent) {
            progressArea.setVisible(true);
            ProgressStartEvent startEvent = (ProgressStartEvent) event;
            ProgressOperation op = operations.start(startEvent.getShortDescription(), startEvent.getStatus(), startEvent.getCategory(), startEvent.getProgressOperationId(), startEvent.getParentProgressOperationId());
            attach(op);
        } else if (event instanceof ProgressCompleteEvent) {
            ProgressCompleteEvent completeEvent = (ProgressCompleteEvent) event;
            detach(operations.complete(completeEvent.getProgressOperationId()));
        } else if (event instanceof ProgressEvent) {
            ProgressEvent progressEvent = (ProgressEvent) event;
            operations.progress(progressEvent.getStatus(), progressEvent.getProgressOperationId());
        } else if (event instanceof EndOutputEvent) {
            progressArea.setVisible(false);
        } else if (event instanceof MaxWorkerCountChangeEvent) {
            int newCount = consoleLayoutCalculator.calculateNumWorkersForConsoleDisplay(
                ((MaxWorkerCountChangeEvent) event).getNewMaxWorkerCount());
            resizeTo(newCount);
        }

        listener.onOutput(event);
    }

    @Override
    public void onOutput(Iterable<OutputEvent> events) {
        Set<OperationIdentifier> completeEventOperationIds = toOperationIdSet(toProgressCompleteEvents(events));
        Set<OperationIdentifier> startEventOperationIdsToSkip = new HashSet<OperationIdentifier>();

        for (OutputEvent event : events) {
            if (event instanceof ProgressStartEvent && completeEventOperationIds.contains(((ProgressStartEvent) event).getProgressOperationId())) {
                startEventOperationIdsToSkip.add(((ProgressStartEvent) event).getProgressOperationId());
                listener.onOutput(event);
            } else if (event instanceof ProgressCompleteEvent && startEventOperationIdsToSkip.contains(((ProgressCompleteEvent) event).getProgressOperationId())) {
                listener.onOutput(event);
            } else {
                onOutput(event);
            }
        }
        renderNow();
    }

    // Filters the events for ProgressCompleteEvent only.
    private Iterable<ProgressCompleteEvent> toProgressCompleteEvents(Iterable<OutputEvent> events) {
        return Iterables.filter(events, ProgressCompleteEvent.class);
    }

    // Transform ProgressCompleteEvent into their corresponding progress OperationIdentifier.
    private Set<OperationIdentifier> toOperationIdSet(Iterable<ProgressCompleteEvent> events) {
        return Sets.newHashSet(Iterables.transform(events, new Function<ProgressCompleteEvent, OperationIdentifier>() {
            @Override
            public OperationIdentifier apply(ProgressCompleteEvent event) {
                return event.getProgressOperationId();
            }
        }));
    }

    private void resizeTo(int newBuildProgressLabelCount) {
        int previousBuildProgressLabelCount = progressArea.getBuildProgressLabels().size();
        if (previousBuildProgressLabelCount >= newBuildProgressLabelCount) {
            // We don't support shrinking at the moment
            return;
        }

        progressArea.resizeBuildProgressTo(newBuildProgressLabelCount);

        // Add new labels to the unused queue
        for (int i = newBuildProgressLabelCount - 1; i >= previousBuildProgressLabelCount; --i) {
            unusedProgressLabels.push(progressArea.getBuildProgressLabels().get(i));
        }

        // Try to empty the unassigned progress operations
        while (!unusedProgressLabels.isEmpty() && !unassignedProgressOperations.isEmpty()) {
            attach(unassignedProgressOperations.pop());
        }
    }

    private void attach(ProgressOperation operation) {
        // Skip attach if a child is already present or no progress message to display
        if (isChildAssociationAlreadyExists(operation.getOperationId())) {
            return;
        }

        AssociationLabel association = null;

        // Reuse parent label if possible
        if (operation.getParent() != null) {
            addDirectChildOperationId(operation.getParent().getOperationId(), operation.getOperationId());
            association = operationIdToAssignedLabels.remove(operation.getParent().getOperationId());
            if (association != null) {
                unusedProgressLabels.push(association.label);
                association = null;
            }
        }

        if (!isRenderable(operation)) {
            return;
        }

        // No parent? Try to use a new label
        if (!unusedProgressLabels.isEmpty()) {
            association = new AssociationLabel(operation, unusedProgressLabels.pop());
        }

        if (association == null) {
            unassignedProgressOperations.addLast(operation);
        } else {
            operationIdToAssignedLabels.put(operation.getOperationId(), association);
        }
    }

    private void detach(ProgressOperation operation) {
        if (operation.getParent() != null) {
            removeDirectChildOperationId(operation.getParent().getOperationId(), operation.getOperationId());
        }

        if (!isRenderable(operation)) {
            return;
        }

        AssociationLabel association = operationIdToAssignedLabels.remove(operation.getOperationId());
        if (association != null) {
            unusedProgressLabels.push(association.label);
            if (operation.getParent() != null) {
                attach(operation.getParent());
            } else if (!unassignedProgressOperations.isEmpty()){
                attach(unassignedProgressOperations.pop());
            }
        } else {
            unassignedProgressOperations.remove(operation);
        }
    }

    private void addDirectChildOperationId(OperationIdentifier parentId, OperationIdentifier childId) {
        Set<OperationIdentifier> children = parentIdToChildrenIds.get(parentId);
        if (children == null) {
            children = new HashSet<OperationIdentifier>();
            parentIdToChildrenIds.put(parentId, children);
        }
        children.add(childId);
    }

    private void removeDirectChildOperationId(OperationIdentifier parentId, OperationIdentifier childId) {
        Set<OperationIdentifier> children = parentIdToChildrenIds.get(parentId);
        if (children == null) {
            return;
        }
        children.remove(childId);
        if (children.isEmpty()) {
            parentIdToChildrenIds.remove(parentId);
        }
    }

    private boolean isChildAssociationAlreadyExists(OperationIdentifier parentId) {
        Set<OperationIdentifier> children = parentIdToChildrenIds.get(parentId);
        if (children != null && !children.isEmpty()) {
            return true;
        }

        return false;
    }

    // Any ProgressOperation in the parent chain has a message, the operation is considered renderable.
    private boolean isRenderable(ProgressOperation operation) {
        for (ProgressOperation current = operation;
             current != null && !"org.gradle.internal.progress.BuildProgressLogger".equals(current.getCategory());
             current = current.getParent()) {
            if (current.getMessage() != null) {
                return true;
            }
        }

        return false;
    }

    private void renderNow() {
        for (AssociationLabel associatedLabel : operationIdToAssignedLabels.values()) {
            associatedLabel.renderNow();
        }
        for (StyledLabel emptyLabel : unusedProgressLabels) {
            emptyLabel.setText(labelFormatter.format());
        }
    }

    private class AssociationLabel {
        final ProgressOperation operation;
        final StyledLabel label;

        AssociationLabel(ProgressOperation operation, StyledLabel label) {
            this.operation = operation;
            this.label = label;
        }

        void renderNow() {
            label.setText(labelFormatter.format(operation));
        }
    }
}
