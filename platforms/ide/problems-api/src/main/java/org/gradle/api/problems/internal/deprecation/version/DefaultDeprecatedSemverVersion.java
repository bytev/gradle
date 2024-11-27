/*
 * Copyright 2024 the original author or authors.
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

package org.gradle.api.problems.internal.deprecation.version;

import org.gradle.api.problems.deprecation.version.DeprecatedSemverVersion;

import javax.annotation.Nullable;

public class DefaultDeprecatedSemverVersion implements DeprecatedSemverVersion {

    private final String major;
    private final String minor;
    private final String patch;

    public DefaultDeprecatedSemverVersion(String major, String minor, String patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    @Override
    public String getMajor() {
        return major;
    }

    @Nullable
    @Override
    public String getMinor() {
        return minor;
    }

    @Nullable
    @Override
    public String getPatch() {
        return patch;
    }
}
