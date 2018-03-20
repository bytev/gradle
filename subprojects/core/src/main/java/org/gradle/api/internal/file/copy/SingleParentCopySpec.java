/*
 * Copyright 2013 the original author or authors.
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
package org.gradle.api.internal.file.copy;

import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.internal.reflect.Instantiator;

public class SingleParentCopySpec extends DefaultCopySpec {

    private final DefaultCopySpec parent;

    public SingleParentCopySpec(FileResolver resolver, Instantiator instantiator, DefaultCopySpec parent) {
        super(resolver, instantiator);
        this.parent = parent;
    }

    @Override
    public boolean isCaseSensitive() {
        return withDefault(caseSensitive, parent.isCaseSensitive());
    }

    @Override
    public boolean getIncludeEmptyDirs() {
        return withDefault(includeEmptyDirs, parent.getIncludeEmptyDirs());
    }

    @Override
    public DuplicatesStrategy getDuplicatesStrategy() {
        return withDefault(duplicatesStrategy, parent.getDuplicatesStrategy());
    }

    @Override
    public Integer getDirMode() {
        return withDefault(dirMode, parent.getDirMode());
    }

    @Override
    public Integer getFileMode() {
        return withDefault(fileMode, parent.getFileMode());
    }

    @Override
    public String getFilteringCharset() {
        return withDefault(filteringCharset, parent.getFilteringCharset());
    }
}
