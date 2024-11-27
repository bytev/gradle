/*
 * Copyright 2024 the original author or authors.
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

package org.gradle.api.problems.internal.deprecation;

import org.gradle.api.Action;
import org.gradle.api.problems.deprecation.DeprecationReporter;
import org.gradle.api.problems.deprecation.spec.DeprecateBehaviorGenericSpec;
import org.gradle.api.problems.deprecation.spec.DeprecateGenericSpec;
import org.gradle.api.problems.deprecation.spec.DeprecateMethodGenericSpec;
import org.gradle.api.problems.internal.DefaultProblemBuilder;
import org.gradle.api.problems.internal.DefaultProblemReporter;
import org.gradle.api.problems.internal.GradleCoreProblemGroup;
import org.gradle.api.problems.internal.Problem;

public class DefaultDeprecationReporter implements DeprecationReporter {

    private final DefaultProblemReporter reporter;

    public DefaultDeprecationReporter(DefaultProblemReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public Problem deprecate(Action<DeprecateGenericSpec> spec) {
        DefaultProblemBuilder builder = reporter.createProblemBuilder();
        builder.id("generic", "Generic deprecation", GradleCoreProblemGroup.deprecation());
        spec.execute(builder);
        return builder.build();
    }

    @Override
    public Problem deprecateBehavior(String behavior, Action<DeprecateBehaviorGenericSpec> spec) {
        DefaultProblemBuilder builder = reporter.createProblemBuilder();
        builder.id("behavior", "Behavior deprecation", GradleCoreProblemGroup.deprecation());
        spec.execute(builder);
        return builder.build();
    }

    @Override
    public Problem deprecateMethod(Action<DeprecateMethodGenericSpec> spec) {
        DefaultProblemBuilder builder = reporter.createProblemBuilder();
        builder.id("method", "Method deprecation", GradleCoreProblemGroup.deprecation());
        spec.execute(builder);
        return builder.build();
    }

    @Override
    public Problem deprecateMethod(String method, Action<DeprecateMethodGenericSpec> spec) {
        DefaultProblemBuilder builder = reporter.createProblemBuilder();
        builder.id("method", "Method deprecation", GradleCoreProblemGroup.deprecation());
        spec.execute(builder);
        return builder.build();
    }
}
