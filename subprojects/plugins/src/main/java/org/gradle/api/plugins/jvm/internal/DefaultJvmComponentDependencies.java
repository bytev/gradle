/*
 * Copyright 2021 the original author or authors.
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

package org.gradle.api.plugins.jvm.internal;

import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.dsl.DependencyAdder;
import org.gradle.api.attributes.Category;
import org.gradle.api.internal.artifacts.dsl.dependencies.ProjectFinder;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.jvm.JvmComponentDependencies;
import org.gradle.internal.deprecation.DeprecationLogger;

import javax.inject.Inject;

public abstract class DefaultJvmComponentDependencies implements JvmComponentDependencies {
    private final DependencyAdder implementation;
    private final DependencyAdder compileOnly;
    private final DependencyAdder runtimeOnly;
    private final DependencyAdder annotationProcessor;

    @Inject
    public DefaultJvmComponentDependencies(DependencyAdder implementation, DependencyAdder compileOnly, DependencyAdder runtimeOnly, DependencyAdder annotationProcessor) {
        this.implementation = implementation;
        this.compileOnly = compileOnly;
        this.runtimeOnly = runtimeOnly;
        this.annotationProcessor = annotationProcessor;
    }

    @Override
    public DependencyAdder getImplementation() {
        return this.implementation;
    }

    @Override
    public DependencyAdder getCompileOnly() {
        return this.compileOnly;
    }

    @Override
    public DependencyAdder getRuntimeOnly() {
        return this.runtimeOnly;
    }

    @Override
    public DependencyAdder getAnnotationProcessor() {
        return this.annotationProcessor;
    }

    @Inject
    protected abstract ProjectFinder getProjectFinder();

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    @Override
    public ProjectDependency project(String projectPath) {
        return getDependencyFactory().create(getProjectFinder().getProject(projectPath));
    }

    @Override
    public ProjectDependency project() {
        return getDependencyFactory().create(getProjectFinder().getBaseProject());
    }

    public <D extends ModuleDependency> D platform(D dependency) {
        dependency.endorseStrictVersions();
        dependency.attributes(attributeContainer -> attributeContainer.attribute(Category.CATEGORY_ATTRIBUTE, getObjectFactory().named(Category.class, Category.REGULAR_PLATFORM)));
        return dependency;
    }

    public <D extends ModuleDependency> D enforcedPlatform(D dependency) {
        dependency.attributes(attributeContainer -> attributeContainer.attribute(Category.CATEGORY_ATTRIBUTE, getObjectFactory().named(Category.class, Category.ENFORCED_PLATFORM)));
        return dependency;
    }

    public <D extends ExternalDependency> D enforcedPlatform(D dependency) {
        DeprecationLogger.whileDisabled(() -> dependency.setForce(true));
        dependency.attributes(attributeContainer -> attributeContainer.attribute(Category.CATEGORY_ATTRIBUTE, getObjectFactory().named(Category.class, Category.ENFORCED_PLATFORM)));
        return dependency;
    }
}
