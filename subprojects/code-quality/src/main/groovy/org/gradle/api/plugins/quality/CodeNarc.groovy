/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.api.plugins.quality


import org.gradle.api.GradleException
import org.gradle.api.Incubating
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.AntBuilderDelegate
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.quality.internal.CodeNarcReportsImpl
import org.gradle.api.plugins.quality.internal.forking.AntExecutionSpec
import org.gradle.api.plugins.quality.internal.forking.AntProcessBuilder
import org.gradle.api.plugins.quality.internal.forking.AntSourceBuilder
import org.gradle.api.plugins.quality.internal.forking.RootAntSourceBuilder
import org.gradle.api.reporting.Report
import org.gradle.api.reporting.Reporting
import org.gradle.api.resources.TextResource
import org.gradle.api.tasks.*
import org.gradle.internal.Factory
import org.gradle.internal.reflect.Instantiator
import org.gradle.logging.ConsoleRenderer
import org.gradle.process.internal.WorkerProcessBuilder

import javax.inject.Inject


/**
 * Runs CodeNarc against some source files.
 */
class CodeNarc extends SourceTask implements VerificationTask, Reporting<CodeNarcReports> {
    /**
     * The class path containing the CodeNarc library to be used.
     */
    @InputFiles
    FileCollection codenarcClasspath

    /**
     * The CodeNarc configuration to use. Replaces the {@code configFile} property.
     *
     * @since 2.2
     */
    @Incubating
    @Nested
    TextResource config

    /**
     * The maximum number of priority 1 violations allowed before failing the build.
     */
    @Input
    int maxPriority1Violations

    /**
     * The maximum number of priority 2 violations allowed before failing the build.
     */
    @Input
    int maxPriority2Violations

    /**
     * The maximum number of priority 3 violations allowed before failing the build.
     */
    @Input
    int maxPriority3Violations

    @Nested
    private final CodeNarcReportsImpl reports

    /**
     * Whether or not the build should break when the verifications performed by this task fail.
     */
    boolean ignoreFailures

    CodeNarc() {
        reports = instantiator.newInstance(CodeNarcReportsImpl, this)
    }

    /**
     * The CodeNarc configuration file to use.
     */
    File getConfigFile() {
        getConfig()?.asFile()
    }

    /**
     * The CodeNarc configuration file to use.
     */
    void setConfigFile(File configFile) {
        setConfig(project.resources.text.fromFile(configFile))
    }

    @Inject
    Instantiator getInstantiator() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected Factory<WorkerProcessBuilder> getWorkerProcessBuilderFactory() {
        throw new UnsupportedOperationException();
    }

    @TaskAction
    void run() {
        logging.captureStandardOutput(LogLevel.INFO)
        def antResult = new AntProcessBuilder(project).withAntExecutionSpec(new CodenarcAntAction(this))
            .withClasspath(getCodenarcClasspath())
            .withWorkerProcessBuilderFactory(getWorkerProcessBuilderFactory())
            .execute()

        if(antResult.errorCount != 0) {
            if (antResult.throwable.message.matches('Exceeded maximum number of priority \\d* violations.*')) {
                def message = "CodeNarc rule violations were found."
                def report = reports.firstEnabled
                if (report) {
                    def reportUrl = new ConsoleRenderer().asClickableFileUrl(report.destination)
                    message += " See the report at: $reportUrl"
                }
                if (getIgnoreFailures()) {
                    logger.warn(message)
                    return
                }
                throw new GradleException(message, antResult.throwable)
            }
            throw antResult.throwable
        }
    }

    /**
     * Returns the reports to be generated by this task.
     */
    CodeNarcReports getReports() {
        return reports
    }

    /**
     * Configures the reports to be generated by this task.
     */
    CodeNarcReports reports(Closure closure) {
        reports.configure(closure)
    }

    private static class CodenarcAntAction implements AntExecutionSpec {

        private final int maxPriority1Violations
        private final int maxPriority2Violations
        private final int maxPriority3Violations
        private final String pathToConfigFile;
        private final Map<String, String> reportMapping = [:]
        private final AntSourceBuilder sources;


        CodenarcAntAction(CodeNarc task) {
            this.maxPriority1Violations = task.getMaxPriority1Violations()
            this.maxPriority2Violations = task.getMaxPriority2Violations()
            this.maxPriority3Violations = task.getMaxPriority3Violations()
            this.pathToConfigFile = task.getConfigFile().getAbsolutePath()
            task.reports.enabled.each { Report r ->
                reportMapping.put(r.name, r.destination.getAbsolutePath())
            }
            sources = new RootAntSourceBuilder(task.getSource(), 'fileset', FileCollection.AntType.FileSet)
        }

        @Override
        void execute(AntBuilderDelegate antBuilderDelegate) {
            antBuilderDelegate.taskdef(name: 'codenarc', classname: 'org.codenarc.ant.CodeNarcTask')
            antBuilderDelegate.codenarc(ruleSetFiles: "file:${pathToConfigFile}", maxPriority1Violations: maxPriority1Violations, maxPriority2Violations: maxPriority2Violations, maxPriority3Violations: maxPriority3Violations) {
                reportMapping.each { String name, String destination ->
                    report(type: name) {
                        option(name: 'outputFile', value: destination)
                    }
                }

                sources.apply(antBuilderDelegate)
            }
        }
    }
}
