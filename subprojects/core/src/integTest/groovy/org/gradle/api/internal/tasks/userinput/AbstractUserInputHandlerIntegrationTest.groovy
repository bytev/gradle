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

package org.gradle.api.internal.tasks.userinput

import org.gradle.api.logging.configuration.ConsoleOutput
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.executer.GradleHandle

import static org.gradle.util.TextUtil.getPlatformLineSeparator

class AbstractUserInputHandlerIntegrationTest extends AbstractIntegrationSpec {

    protected void interactiveExecution() {
        executer.withStdinPipe().withForceInteractive(true)
    }

    protected void withDaemon(boolean enabled) {
        if (enabled) {
            executer.requireDaemon().requireIsolatedDaemons()
        }
    }

    protected void withRichConsole(boolean enabled) {
        if (enabled) {
            executer.withConsole(ConsoleOutput.Rich)
        }
    }

    static void writeToStdInAndClose(GradleHandle gradleHandle, input) {
        gradleHandle.stdinPipe.write(input)
        writeLineSeparatorToStdInAndClose(gradleHandle)
    }

    static void writeLineSeparatorToStdInAndClose(GradleHandle gradleHandle) {
        gradleHandle.stdinPipe.write(getPlatformLineSeparator().bytes)
        gradleHandle.stdinPipe.close()
    }
}
