/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.tooling.events.task.internal;

import org.gradle.tooling.Problem;
import org.gradle.tooling.events.task.TaskSkippedResult;

import java.util.List;

/**
 * Implementation of the {@code TaskSkippedResult} interface.
 */
public final class DefaultTaskSkippedResult implements TaskSkippedResult {

    private final long startTime;
    private final long endTime;
    private final String skipMessage;
    private final List<Problem> problems;

    public DefaultTaskSkippedResult(long startTime, long endTime, String skipMessage, List<Problem> problems) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.skipMessage = skipMessage;
        this.problems = problems;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public String getSkipMessage() {
        return this.skipMessage;
    }

    @Override
    public List<Problem> getProblems() {
        return problems;
    }
}
