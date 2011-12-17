/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.api.internal.artifacts.ivyservice.ivyresolve;

import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;

/**
 * A repository of module versions.
 *
 * <p>Current contains a subset of methods from {@link org.apache.ivy.plugins.resolver.DependencyResolver}, while we transition away from it.
 * The plan is to sync with (or replace with) {@link org.gradle.api.internal.artifacts.ivyservice.ModuleVersionResolver}.
 */
public interface ModuleVersionRepository {
    String getId();

    ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data);

    DownloadReport download(Artifact[] artifacts, DownloadOptions options);

    // TODO - should be part of the meta-data returned by getDependency()
    boolean isChanging(ResolvedModuleRevision revision, ResolveData resolveData);

    // TODO - should be internal to the implementation of this
    boolean isLocal();
}
