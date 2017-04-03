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

package org.gradle.api.internal.changedetection.rules

import groovy.transform.NotYetImplemented
import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.LocalBuildCacheFixture
import spock.lang.Unroll

class OverlappingOutputsIntegrationTest extends AbstractIntegrationSpec implements LocalBuildCacheFixture {
    def setup() {
        buildFile << """
            @CacheableTask
            class OutputDirectoryTask extends DefaultTask {
                @OutputDirectory
                File outputDir = project.buildDir
                
                @Input
                String message = "Generated by " + path
                
                @Input
                String outputFileName = name + ".txt"
                
                @TaskAction
                void generate() {
                    new File(outputDir, outputFileName).text = message
                }
            }
            
            @CacheableTask
            class OutputFileTask extends DefaultTask {
                @Input
                String message = "Generated by " + path
                
                @Input
                String outputFileName = name + ".txt"
                
                @OutputFile
                File getOutputFile() {
                    new File(project.buildDir, outputFileName)
                } 
                
                @TaskAction
                void generate() {
                    outputFile.text = message
                }
            }
        """
    }

    private void useOverlappingOutputDirectories() {
        buildFile << """
            task A(type: OutputDirectoryTask)
            task B(type: OutputDirectoryTask)
            task cleanB(type: Delete) {
                delete B
            }
        """
    }

    def "overlapping output directory with A B then A B"() {
        useOverlappingOutputDirectories()

        when:
        withBuildCache().succeeds("A", "B")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // Only one task can be cached
        listCacheFiles().size() == 1

        when:
        cleanBuildDir()
        withBuildCache().succeeds("A", "B")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // A can be from the cache
        result.assertTaskSkipped(":A")
        // B cannot be from the cache
        result.assertTaskNotSkipped(":B")
    }

    def "overlapping output directory with A B then B A"() {
        useOverlappingOutputDirectories()

        when:
        withBuildCache().succeeds("A", "B")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // Only one task can be cached
        listCacheFiles().size() == 1

        when:
        cleanBuildDir()
        withBuildCache().succeeds("B", "A")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // A and B will not be from the cache
        result.assertTasksNotSkipped(":B", ":A")
    }

    def "overlapping output directory with A B then B"() {
        useOverlappingOutputDirectories()

        when:
        withBuildCache().succeeds("A", "B")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // Only one task can be cached
        listCacheFiles().size() == 1

        when:
        cleanBuildDir()
        withBuildCache().succeeds("B")
        then:
        file("build/A.txt").assertDoesNotExist()
        file("build/B.txt").assertExists()
        // B will not be from the cache
        result.assertTasksNotSkipped(":B")
        // B can be cached now
        listCacheFiles().size() == 2
    }

    def "overlapping output directory with A cleanB B then A B"() {
        useOverlappingOutputDirectories()

        when:
        withBuildCache().succeeds("A", "cleanB", "B")
        then:
        // Both tasks can be cached
        listCacheFiles().size() == 2

        when:
        cleanBuildDir()
        withBuildCache().succeeds("A", "B")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // A can be from the cache
        result.assertTaskSkipped(":A")
        // B cannot be from the cache
        result.assertTaskNotSkipped(":B")
    }

    private void useOverlappingOutputFileAndDirectory() {
        buildFile << """
            task A(type: OutputFileTask)
            task B(type: OutputDirectoryTask)
            task cleanB(type: Delete) {
                delete B
            }
        """
    }

    def "overlapping output directory and output file with A B then A B"() {
        useOverlappingOutputFileAndDirectory()

        when:
        withBuildCache().succeeds("A", "B")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // Only one task can be cached
        listCacheFiles().size() == 1

        when:
        cleanBuildDir()
        withBuildCache().succeeds("A", "B")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // A can be from the cache
        result.assertTaskSkipped(":A")
        // B cannot be from the cache
        result.assertTaskNotSkipped(":B")
    }

    def "overlapping output directory and output file with A B then B A"() {
        useOverlappingOutputFileAndDirectory()

        when:
        withBuildCache().succeeds("A", "B")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // Only one task can be cached
        listCacheFiles().size() == 1

        when:
        cleanBuildDir()
        withBuildCache().succeeds("B", "A")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // A can be from the cache and B will not be in the cache
        result.assertTaskSkipped(":A")
        result.assertTaskNotSkipped(":B")

        when:
        cleanBuildDir()
        withBuildCache().succeeds("B", "A")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // Both can be from the cache
        result.assertTasksSkipped(":B", ":A")
    }

    def "overlapping output directory and output file with A B then B"() {
        useOverlappingOutputFileAndDirectory()

        when:
        withBuildCache().succeeds("A", "B")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // Only one task can be cached
        listCacheFiles().size() == 1

        when:
        cleanBuildDir()
        withBuildCache().succeeds("B")
        then:
        file("build/A.txt").assertDoesNotExist()
        file("build/B.txt").assertExists()
        // B will not be from the cache
        result.assertTasksNotSkipped(":B")
        // B can be cached now
        listCacheFiles().size() == 2
    }

    // This fails because cleanB will remove A's outputs.
    // So, unless we change this to only clean the *real* outputs of B, this won't work.
    @NotYetImplemented
    def "overlapping output directory and output file with A cleanB B then A B"() {
        useOverlappingOutputFileAndDirectory()

        when:
        withBuildCache().succeeds("A", "cleanB", "B")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // Both tasks can be cached
        listCacheFiles().size() == 2

        when:
        cleanBuildDir()
        withBuildCache().succeeds("A", "B")
        then:
        file("build/A.txt").assertExists()
        file("build/B.txt").assertExists()
        // A can be from the cache
        result.assertTaskSkipped(":A")
        // B cannot be from the cache
        result.assertTaskNotSkipped(":B")
    }

    private void useOverlappingOutputFiles() {
        buildFile << """
            task A(type: OutputFileTask)
            task B(type: OutputFileTask) {
                // B's message needs to be different so we don't detect the file has unchanged
                message = "Generated by task " + path
            }
            task cleanB(type: Delete) {
                delete B
            }
            tasks.withType(OutputFileTask) {
                outputFileName = "AB.txt"
            }
        """
    }

    def "overlapping output files with A B then A B"() {
        useOverlappingOutputFiles()

        when:
        withBuildCache().succeeds("A", "B")
        then:
        file("build/AB.txt").text == "Generated by task :B"
        // Only A task can be cached
        listCacheFiles().size() == 1

        when:
        cleanBuildDir()
        withBuildCache().succeeds("A", "B")
        then:
        file("build/AB.txt").text == "Generated by task :B"
        // A can be from the cache
        result.assertTaskSkipped(":A")
        // B cannot be from the cache
        result.assertTaskNotSkipped(":B")

        when:
        withBuildCache().succeeds("A")
        then:
        // A overwrites B's output
        file("build/AB.txt").text == "Generated by :A"
        result.assertTaskSkipped(":A")
    }

    def "overlapping output files with A B then B A"() {
        useOverlappingOutputFiles()

        when:
        withBuildCache().succeeds("A", "B")
        then:
        file("build/AB.txt").text == "Generated by task :B"
        // Only one task can be cached
        listCacheFiles().size() == 1

        when:
        cleanBuildDir()
        withBuildCache().succeeds("B", "A")
        then:
        file("build/AB.txt").text == "Generated by :A"
        // A and B will not be from the cache
        result.assertTaskNotSkipped(":B")
        result.assertTaskSkipped(":A")
    }

    def "overlapping output files with A B then B"() {
        useOverlappingOutputFiles()

        when:
        withBuildCache().succeeds("A", "B")
        then:
        file("build/AB.txt").text == "Generated by task :B"
        // Only one task can be cached
        listCacheFiles().size() == 1

        when:
        cleanBuildDir()
        withBuildCache().succeeds("B")
        then:
        file("build/AB.txt").text == "Generated by task :B"
        // B will not be from the cache
        result.assertTasksNotSkipped(":B")
        // B can be cached now
        listCacheFiles().size() == 2
    }

    def "overlapping output files with A cleanB B then A B"() {
        useOverlappingOutputFiles()

        when:
        withBuildCache().succeeds("A", "cleanB", "B")
        then:
        file("build/AB.txt").text == "Generated by task :B"
        // Both tasks can be cached
        listCacheFiles().size() == 2

        when:
        cleanBuildDir()
        withBuildCache().succeeds("A", "B")
        then:
        file("build/AB.txt").text == "Generated by task :B"
        // A can be from the cache
        result.assertTaskSkipped(":A")
        // B cannot be from the cache
        result.assertTaskSkipped(":B")
    }

    private void cleanBuildDir() {
        file("build").deleteDir()
    }

    // We ignore external.txt as an input because the file doesn't change after executing A
    // So when external.txt changes, we don't count that as a change in outputs.
    // @NotYetImplemented
    def "overlapping directory with external process and a pre-existing file"() {
        buildFile << """
            task A(type: OutputDirectoryTask)
        """
        def externalFile = file("build/external.txt")
        externalFile.text = "Created by something else"
        when:
        withBuildCache().succeeds("A")
        then:
        // A cannot be cached.
        listCacheFiles().size() == 0
        externalFile.assertExists()
        file("build/A.txt").assertExists()

        when:
        externalFile.text = "changed"
        file("build/A.txt").delete()
        withBuildCache().succeeds("A")
        then:
        result.assertTaskNotSkipped(":A")
        externalFile.text == "changed"
        file("build/A.txt").assertExists()

        when:
        cleanBuildDir()
        withBuildCache().succeeds("A")
        then:
        result.assertTaskNotSkipped(":A")
        externalFile.assertDoesNotExist()
        file("build/A.txt").assertExists()
        // A can be cached now
        listCacheFiles().size() == 1
    }

    def "overlapping file with external process and a pre-existing file"() {
        buildFile << """
            task A(type: OutputFileTask)
        """
        file("build/A.txt").text = "Created by something else"
        when:
        withBuildCache().succeeds("A")
        then:
        // A cannot be cached.
        listCacheFiles().size() == 0
        file("build/A.txt").text == "Generated by :A"
    }

    @Unroll
    def "overlapping #taskType with external process and a build-generated file"() {
        buildFile << """
            task A(type: $taskType)
        """
        def generatedFile = file("build/A.txt")

        when:
        withBuildCache().succeeds("A")
        then:
        // A can be cached.
        listCacheFiles().size() == 1
        generatedFile.assertExists()

        when:
        generatedFile.text = "changed"
        withBuildCache().succeeds("A")
        then:
        result.assertTaskSkipped(":A")
        generatedFile.text == "Generated by :A"

        when:
        // Looks the same as clean
        generatedFile.delete()
        withBuildCache().succeeds("A")
        then:
        result.assertTaskSkipped(":A")
        generatedFile.text == "Generated by :A"

        where:
        taskType << [ "OutputDirectoryTask", "OutputFileTask" ]
    }

    // We don't consider just empty directories as being an "overlapping" output
    @NotYetImplemented
    def "overlapping directory with external process that creates a directory"() {
        buildFile << """
            task A(type: OutputDirectoryTask)
        """

        when:
        withBuildCache().succeeds("A")
        then:
        // A can be cached.
        listCacheFiles().size() == 1
        file("build/A.txt").assertExists()

        when:
        cleanBuildDir()
        file("build/emptyDir").createDir()
        withBuildCache().succeeds("A")
        then:
        result.assertTaskNotSkipped(":A")
        file("build/A.txt").assertExists()
    }
}
