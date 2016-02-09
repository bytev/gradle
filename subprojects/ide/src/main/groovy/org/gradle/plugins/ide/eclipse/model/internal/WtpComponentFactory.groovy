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
package org.gradle.plugins.ide.eclipse.model.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.EclipseWtpPlugin
import org.gradle.plugins.ide.eclipse.model.EclipseWtpComponent
import org.gradle.plugins.ide.eclipse.model.WbDependentModule
import org.gradle.plugins.ide.eclipse.model.WbResource
import org.gradle.plugins.ide.eclipse.model.WtpComponent
import org.gradle.plugins.ide.internal.IdeDependenciesExtractor

class WtpComponentFactory {
    void configure(EclipseWtpComponent wtp, WtpComponent component) {
        def entries = []
        entries.addAll(getEntriesFromSourceDirs(wtp))
        entries.addAll(wtp.resources.findAll { wtp.project.file(it.sourcePath).isDirectory() } )
        entries.addAll(wtp.properties)
        // for ear files root deps are NOT transitive; wars don't use root deps so this doesn't hurt them
        // TODO: maybe do this in a more explicit way, via config or something
        entries.addAll(getEntriesFromConfigurations(wtp.rootConfigurations, wtp.minusConfigurations, wtp, '/', false))
        entries.addAll(getEntriesFromConfigurations(wtp.libConfigurations, wtp.minusConfigurations, wtp, wtp.libDeployPath, true))

        component.configure(wtp.deployName, wtp.contextPath, entries)
    }

    private List<WbResource> getEntriesFromSourceDirs(EclipseWtpComponent wtp) {
        wtp.sourceDirs.findAll { it.isDirectory() }.collect { dir ->
            new WbResource(wtp.classesDeployPath, wtp.project.relativePath(dir))
        }
    }

    private List<WbDependentModule> getEntriesFromConfigurations(Set<Configuration> plusConfigurations, Set<Configuration> minusConfigurations, EclipseWtpComponent wtp, String deployPath, boolean transitive) {
        (getEntriesFromProjectDependencies(plusConfigurations, minusConfigurations, deployPath, transitive) as List) +
                (getEntriesFromLibraries(plusConfigurations, minusConfigurations, wtp, deployPath, transitive) as List)
    }

    private Set getEntriesFromProjectDependencies(Set<Configuration> plusConfigurations, Set<Configuration> minusConfigurations, String deployPath, boolean transitive) {
        LinkedHashSet allProjects = getProjectDependencies(plusConfigurations, minusConfigurations, transitive)
        allProjects.collect { project ->
            def moduleName = project.plugins.hasPlugin(EclipsePlugin) ? project.eclipse.project.name : project.name
            new WbDependentModule(deployPath, "module:/resource/" + moduleName + "/" + moduleName)
        }
    }

    private static LinkedHashSet getProjectDependencies(Set<Configuration> plusConfigurations, Set<Configuration> minusConfigurations, boolean transitive) {
        def dependencies = getDependencies(plusConfigurations, minusConfigurations, { it instanceof ProjectDependency })

        def projects = dependencies*.dependencyProject

        def result = [ ] as LinkedHashSet
        result.addAll(projects)
        if (transitive) {
            projects.each{ collectDependedUponProjects(it, result) }
        }
        return result
    }

    // TODO: might have to search all class paths of all source sets for project dependencies, not just runtime configuration
    private static void collectDependedUponProjects(Project project, Set<Project> result) {
        def runtimeConfig = project.configurations.findByName("runtime")
        if (runtimeConfig) {
            def projectDeps = runtimeConfig.allDependencies.withType(ProjectDependency)
            def dependedUponProjects = projectDeps*.dependencyProject
            result.addAll(dependedUponProjects)
            for (dependedUponProject in dependedUponProjects) {
                collectDependedUponProjects(dependedUponProject, result)
            }
        }
    }

    public static Collection resolveDependenciesFor(Set<Configuration> plusConfigurations, Set<Configuration> minusConfigurations, boolean transitive) {
        def extractor = new IdeDependenciesExtractor()

        if (transitive) {
            def projects = getProjectDependencies(plusConfigurations, minusConfigurations, transitive)
            projects.findAll{ it.plugins.hasPlugin(EclipseWtpPlugin) }.each{ project ->
                def wtp = project.eclipse.wtp.component
                plusConfigurations += wtp.plusConfigurations
                minusConfigurations += wtp.minusConfigurations
            }
        }

        //below is not perfect because we're skipping the unresolved dependencies completely
        //however, it should be better anyway. Sometime soon we will hopefully change the wtp component stuff
        def externals = extractor.resolvedExternalDependencies(plusConfigurations, minusConfigurations)
        def locals = extractor.extractLocalFileDependencies(plusConfigurations, minusConfigurations)
        return externals + locals
    }

    private Set getEntriesFromLibraries(Set<Configuration> plusConfigurations, Set<Configuration> minusConfigurations, EclipseWtpComponent wtp, String deployPath, boolean transitive) {
        resolveDependenciesFor(plusConfigurations, minusConfigurations, transitive).collect { dep ->
            createWbDependentModuleEntry(dep.file, wtp.fileReferenceFactory, deployPath)
        }
    }

    private WbDependentModule createWbDependentModuleEntry(File file, FileReferenceFactory fileReferenceFactory, String deployPath) {
        def ref = fileReferenceFactory.fromFile(file)
        def handleSnippet
        if (ref.relativeToPathVariable) {
            handleSnippet = "var/$ref.path"
        } else {
            handleSnippet = "lib/${ref.path}"
        }
        return new WbDependentModule(deployPath, "module:/classpath/$handleSnippet")
    }

    private static LinkedHashSet getDependencies(Set plusConfigurations, Set minusConfigurations, Closure filter) {
        def declaredDependencies = new LinkedHashSet()
        plusConfigurations.each { Configuration configuration ->
            declaredDependencies.addAll(configuration.allDependencies.matching(filter))
        }
        minusConfigurations.each { Configuration configuration ->
            declaredDependencies.removeAll(configuration.allDependencies.matching(filter))
        }
        return declaredDependencies
    }
}
