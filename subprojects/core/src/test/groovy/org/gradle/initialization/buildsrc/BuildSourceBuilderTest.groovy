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
package org.gradle.initialization.buildsrc

import org.gradle.StartParameter
import org.gradle.api.internal.initialization.ClassLoaderScope
import org.gradle.initialization.GradleLauncher
import org.gradle.initialization.NestedBuildFactory
import org.gradle.internal.classpath.CachedClasspathTransformer
import org.gradle.internal.classpath.DefaultClassPath
import org.gradle.internal.cleanup.BuildOutputCleanupRegistry
import org.gradle.internal.progress.TestBuildOperationExecutor
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification

class BuildSourceBuilderTest extends Specification {

    @Rule TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()

    def buildFactory = Mock(NestedBuildFactory)
    def classLoaderScope = Mock(ClassLoaderScope)
    def executor = new TestBuildOperationExecutor()
    def transformer = Mock(CachedClasspathTransformer)
    def buildSrcBuildListenerFactory = Stub(BuildSrcBuildListenerFactory)
    def buildOutputCleanupRegistry = Mock(BuildOutputCleanupRegistry)
    def buildSourceBuilder = Spy(BuildSourceBuilder, constructorArgs: [buildFactory, classLoaderScope, executor, transformer, buildOutputCleanupRegistry])

    def parameter = new StartParameter()

    void "creates classpath when build src does not exist"() {
        when:
        parameter.setCurrentDir(new File('nonexisting'))

        then:
        buildSourceBuilder.createBuildSourceClasspath(parameter).asFiles == []
    }

    void "creates classpath when build src exists"() {
        def classpath = [new File('test')]
        def launcher = Stub(GradleLauncher)
        def listener = Stub(BuildSrcBuildListenerFactory.Listener)
        buildFactory.nestedInstance(_) >> launcher
        buildSourceBuilder.buildSrcBuildListenerFactory = buildSrcBuildListenerFactory
        buildSrcBuildListenerFactory.create() >> listener
        listener.runtimeClasspath >> classpath

        when:
        parameter.setCurrentDir(tmpDir.createDir("someDir"))

        then:
        buildSourceBuilder.createBuildSourceClasspath(parameter) == new DefaultClassPath(classpath)
    }
}
