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
package org.gradle.api.internal.tasks.testing.report;

import org.apache.commons.lang.StringUtils;
import org.gradle.api.internal.tasks.testing.junit.result.TestResultsProvider;

import java.util.*;

/**
 * The model for the test report.
 */
public class AllTestResults extends CompositeTestResults {
    private final Map<String, PackageTestResults> packages = new TreeMap<String, PackageTestResults>();

    public AllTestResults() {
        super(null);
    }

    @Override
    public String getTitle() {
        return "Test Summary";
    }

    @Override
    public String getBaseUrl() {
        return "index.html";
    }

    public Collection<PackageTestResults> getPackages() {
        return packages.values();
    }

    public TestResult addTest(TestResultsProvider classProvider, String testName, long duration) {
        return addTest(classProvider, testName, testName, duration);
    }

    public TestResult addTest(TestResultsProvider classProvider, String testName, String testDisplayName, long duration) {
        PackageTestResults packageResults = addPackageForClass(classProvider.getResult().getName());
        return addTest(packageResults.addTest(classProvider, testName, testDisplayName, duration));
    }

    public ClassTestResults addTestClass(TestResultsProvider classProvider) {
        return addPackageForClass(classProvider.getResult().getName()).addClass(classProvider);
    }

    private PackageTestResults addPackageForClass(String className) {
        String packageName = StringUtils.substringBeforeLast(className, ".");
        if (packageName.equals(className)) {
            packageName = "";
        }
        return addPackage(packageName);
    }

    private PackageTestResults addPackage(String packageName) {
        PackageTestResults packageResults = packages.get(packageName);
        if (packageResults == null) {
            packageResults = new PackageTestResults(packageName, this);
            packages.put(packageName, packageResults);
        }
        return packageResults;
    }
}
