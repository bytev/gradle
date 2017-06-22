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

package org.gradle.language.swift

import org.gradle.nativeplatform.fixtures.AbstractInstalledToolChainIntegrationSpec
import org.gradle.nativeplatform.fixtures.NativeLanguageRequirement
import org.gradle.nativeplatform.fixtures.RequiresSupportedLanguage
import org.gradle.nativeplatform.fixtures.app.ExeWithLibraryUsingSwiftLibraryHelloWorldApp
import org.gradle.nativeplatform.fixtures.app.SwiftHelloWorldApp

import static org.gradle.util.Matchers.containsText

@RequiresSupportedLanguage(NativeLanguageRequirement.SWIFT)
class SwiftLibraryIntegrationTest extends AbstractInstalledToolChainIntegrationSpec {
    def helloWorldApp = new SwiftHelloWorldApp()

    def "build fails when compilation fails"() {
        given:
        buildFile << """
            apply plugin: 'swift-library'
         """

        and:
        helloWorldApp.brokenFile.writeToDir(file("src/main"))

        expect:
        fails "assemble"
        failure.assertHasDescription("Execution failed for task ':compileSwift'.");
        failure.assertHasCause("A build operation failed.")
        failure.assertThatCause(containsText("Swift compiler failed while compiling swift file(s)"))
    }

    def "sources are compiled with Swift compiler"() {
        settingsFile << "rootProject.name = 'hello'"

        given:
        helloWorldApp.librarySources*.writeToDir(file('src/main'))

        and:
        buildFile << """
            apply plugin: 'swift-library'
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(":compileSwift", ":assemble")
        sharedLibrary("build/lib/hello").assertExists()
    }

    def "can define public module"() {
        settingsFile << "rootProject.name = 'hello'"
        given:
        def app = new SwiftHelloWorldApp()
        app.library.sourceFiles.each { it.writeToFile(file("src/main/swift/$it.name")) }

        and:
        buildFile << """
            apply plugin: 'swift-library'
         """

        expect:
        succeeds "assemble"
        result.assertTasksExecuted(":compileSwift", ":assemble")
        sharedLibrary("build/lib/hello").assertExists()
        file("build/lib/hello.swiftmodule").assertExists()
    }

    def "can compile and link against another library"() {
        settingsFile << "include 'Hello', 'Greeting'"
        def app = new ExeWithLibraryUsingSwiftLibraryHelloWorldApp()

        given:
        buildFile << """
            project(':Hello') {
                apply plugin: 'swift-library'
                dependencies {
                    implementation project(':Greeting')
                }
            }
            project(':Greeting') {
                apply plugin: 'swift-library'
            }
"""
        app.library.sourceFiles.each { it.writeToFile(file("Hello/src/main/swift/$it.name")) }
        app.greetingsLibrary.sourceFiles.each { it.writeToFile(file("Greeting/src/main/swift/$it.name")) }

        expect:
        succeeds ":Hello:assemble"
        result.assertTasksExecuted(":Greeting:compileSwift", ":Hello:compileSwift", ":Hello:assemble")
        sharedLibrary("Hello/build/lib/Hello").assertExists()
        sharedLibrary("Greeting/build/lib/Greeting").assertExists()
    }

}
