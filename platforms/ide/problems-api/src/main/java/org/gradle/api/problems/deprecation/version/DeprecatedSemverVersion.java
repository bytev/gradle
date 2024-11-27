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

package org.gradle.api.problems.deprecation.version;

import org.gradle.api.Incubating;

import javax.annotation.Nullable;

/**
 * Semantic version of a deprecated feature.
 * <p>
 * One significant difference between this and {@link OpaqueDeprecatedVersion} is that this version is sortable.
 * With ordering, we can determine a minimum version when the code using deprecations will break.
 */
@Incubating
public interface DeprecatedSemverVersion extends DeprecatedVersion {

    String getMajor();

    @Nullable
    String getMinor();

    @Nullable
    String getPatch();

}
