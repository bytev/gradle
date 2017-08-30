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

package org.gradle.initialization.option;

/**
 * Configuration for a command line option.
 *
 * @since 4.2
 */
public final class CommandLineOptionConfiguration {
    private final String option;
    private final String description;
    private boolean incubating;
    private boolean argument;

    private CommandLineOptionConfiguration(String option, String description) {
        this.option = option;
        this.description = description;
    }

    public static CommandLineOptionConfiguration create(String option, String description) {
        return new CommandLineOptionConfiguration(option, description);
    }

    public CommandLineOptionConfiguration incubating() {
        incubating = true;
        return this;
    }

    public CommandLineOptionConfiguration hasArgument() {
        argument = true;
        return this;
    }

    public String getOption() {
        return option;
    }

    public String getDescription() {
        return description;
    }

    public boolean isIncubating() {
        return incubating;
    }

    public boolean isArgument() {
        return argument;
    }
}
