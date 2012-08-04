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

package org.gradle.integtests.samples

import org.gradle.integtests.fixtures.AbstractAutoTestedSamplesTest
import org.junit.Test

/**
 * @author Szczepan Faber, created at: 3/28/11
 */
class AutoTestedSamplesPluginsIntegrationTest extends AbstractAutoTestedSamplesTest {

    @Test
    void runSamples() {
        //for debugging purposes you can samples for a single class
//        includeOnly '**/Test.java'
        runSamplesFrom("subprojects/plugins/src/main")
    }
}