/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.architecture.library.freeze;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.GradleViolationStoreFactory;
import com.tngtech.archunit.library.freeze.ViolationStore;

import java.util.List;
import java.util.Properties;

import static java.util.stream.Collectors.toList;

public class LineNumberIgnoringViolationStore implements ViolationStore {

    private final ViolationStore delegate;

    public LineNumberIgnoringViolationStore() {
        // We can instantiate TextFileBasedViolationStore directly once ArchUnit 1.0.2 is released,
        // since TextFileBasedViolationStore became public type in https://github.com/TNG/ArchUnit/pull/1046.
        delegate = GradleViolationStoreFactory.create();
    }

    @Override
    public void initialize(Properties properties) {
        delegate.initialize(properties);
    }

    @Override
    public boolean contains(ArchRule rule) {
        return delegate.contains(rule);
    }

    @Override
    public void save(ArchRule rule, List<String> violations) {
        List<String> violationsWithoutLineNumbers = violations.stream()
            .map(it -> it.replaceAll("\\.(java|kt):\\d+", ".$1"))
            .collect(toList());
        delegate.save(rule, violationsWithoutLineNumbers);
    }

    @Override
    public List<String> getViolations(ArchRule rule) {
        return delegate.getViolations(rule);
    }
}
