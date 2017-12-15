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
package org.gradle.testing.junit5;

import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.tasks.InputFiles;

import java.io.Serializable;

/**
 * JUnit Platform specific test options.
 */
public class JUnitPlatformOptions implements Serializable {
    @InputFiles
    public final ConfigurableFileCollection classpathRoots;

    public JUnitPlatformOptions(Project project) {
        this.classpathRoots = project.files();
    }

    // TODO should be able to easily add the other selector options here
}
