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
package org.gradle.api.internal.artifacts.ivyservice;

import org.gradle.api.artifacts.*;
import org.gradle.api.internal.CachingDirectedGraphWalker;
import org.gradle.api.internal.DirectedGraphWithEdgeValues;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ArtifactNotFoundException;
import org.gradle.api.specs.Spec;

import java.io.File;
import java.util.*;

public class DefaultLenientConfiguration implements ResolvedConfigurationBuilder, LenientConfiguration {
    private final ResolvedDependency root;
    private final Configuration configuration;
    private final Map<ModuleDependency, ResolvedDependency> firstLevelDependencies = new LinkedHashMap<ModuleDependency, ResolvedDependency>();
    private final Set<ResolvedArtifact> artifacts = new LinkedHashSet<ResolvedArtifact>();
    private final Set<UnresolvedDependency> unresolvedDependencies = new LinkedHashSet<UnresolvedDependency>();
    private final CachingDirectedGraphWalker<ResolvedDependency, ResolvedArtifact> walker
            = new CachingDirectedGraphWalker<ResolvedDependency, ResolvedArtifact>(new ResolvedDependencyArtifactsGraph());

    public DefaultLenientConfiguration(Configuration configuration, ResolvedDependency root) {
        this.configuration = configuration;
        this.root = root;
    }

    public boolean hasError() {
        return !unresolvedDependencies.isEmpty();
    }

    public void rethrowFailure() throws ResolveException {
        if (!unresolvedDependencies.isEmpty()) {
            List<Throwable> failures = new ArrayList<Throwable>();
            for (UnresolvedDependency unresolvedDependency : unresolvedDependencies) {
                failures.add(unresolvedDependency.getProblem());
            }
            throw new ResolveException(configuration, failures);
        }
    }

    public Set<UnresolvedDependency> getUnresolvedModuleDependencies() {
        return unresolvedDependencies;
    }

    public Set<ResolvedArtifact> getResolvedArtifacts() throws ResolveException {
        return artifacts;
    }

    public ResolvedDependency getRoot() {
        return root;
    }

    public void addFirstLevelDependency(ModuleDependency moduleDependency, ResolvedDependency refersTo) {
        firstLevelDependencies.put(moduleDependency, refersTo);
    }

    public void addArtifact(ResolvedArtifact artifact) {
        artifacts.add(artifact);
    }

    public void addUnresolvedDependency(UnresolvedDependency unresolvedDependency) {
        unresolvedDependencies.add(unresolvedDependency);
    }

    public Set<ResolvedDependency> getFirstLevelModuleDependencies() {
        return root.getChildren();
    }

    public Set<ResolvedDependency> getFirstLevelModuleDependencies(Spec<? super Dependency> dependencySpec) {
        Set<ResolvedDependency> matches = new LinkedHashSet<ResolvedDependency>();
        for (Map.Entry<ModuleDependency, ResolvedDependency> entry : firstLevelDependencies.entrySet()) {
            if (dependencySpec.isSatisfiedBy(entry.getKey())) {
                matches.add(entry.getValue());
            }
        }
        return matches;
    }

    public Set<File> getFiles(Spec<? super Dependency> dependencySpec) {
        return getFiles(dependencySpec, new LenientArtifactToFileResolver());
    }
    
    public Set<File> getFilesStrict(Spec<? super Dependency> dependencySpec) {
        return getFiles(dependencySpec, new ArtifactFileResolver());
    }

    private Set<File> getFiles(Spec<? super Dependency> dependencySpec, ArtifactFileResolver artifactFileResolver) {
        Set<ResolvedDependency> firstLevelModuleDependencies = getFirstLevelModuleDependencies(dependencySpec);

        Set<ResolvedArtifact> artifacts = new LinkedHashSet<ResolvedArtifact>();

        for (ResolvedDependency resolvedDependency : firstLevelModuleDependencies) {
            artifacts.addAll(resolvedDependency.getParentArtifacts(root));
            walker.add(resolvedDependency);
        }

        artifacts.addAll(walker.findValues());

        Set<File> files = new LinkedHashSet<File>();
        for (ResolvedArtifact artifact : artifacts) {
            File depFile = artifactFileResolver.getFile(artifact);
            if (depFile != null) {
                files.add(depFile);
            }
        }
        return files;
    }

    private static class ResolvedDependencyArtifactsGraph implements DirectedGraphWithEdgeValues<ResolvedDependency, ResolvedArtifact> {
        public void getNodeValues(ResolvedDependency node, Collection<ResolvedArtifact> values,
                                  Collection<ResolvedDependency> connectedNodes) {
            connectedNodes.addAll(node.getChildren());
        }

        public void getEdgeValues(ResolvedDependency from, ResolvedDependency to,
                                  Collection<ResolvedArtifact> values) {
            values.addAll(to.getParentArtifacts(from));
        }
    }

    private static class ArtifactFileResolver {
        public File getFile(ResolvedArtifact artifact) {
            return artifact.getFile();
        }
    }
    
    private static class LenientArtifactToFileResolver extends ArtifactFileResolver {
        public File getFile(ResolvedArtifact artifact) {
            try {
                return super.getFile(artifact);
            } catch (ArtifactNotFoundException e) {
                return null;
            }
        }
    }    
}
