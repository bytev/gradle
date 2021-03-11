/*
 * Copyright 2020 the original author or authors.
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

package gradlebuild.identity.extension

import gradlebuild.identity.tasks.BuildReceipt
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion


abstract class ModuleIdentityExtension(val tasks: TaskContainer, val objects: ObjectFactory) {

    abstract val version: Property<GradleVersion>

    abstract val baseName: Property<String>

    abstract val buildTimestamp: Property<String>
    abstract val snapshot: Property<Boolean>
    abstract val promotionBuild: Property<Boolean>

    /**
     * The actual build branch.
     */
    abstract val gradleBuildBranch: Property<String>

    /**
     * The base branch for pre-tested commit, i.e. the branch is not `master`/`release`,
     * but it's tested in the same way of `master`/`release`.
     *
     * The pre-tested commit branch name looks like "pre-test/master/queue/alice/personal-branch".
     */
    val gradleBuildLogicBranch: Provider<String>
        get() = gradleBuildBranch.map(this::toPreTestedCommitBaseBranch)

    abstract val gradleBuildCommitId: Property<String>

    abstract val releasedVersions: Property<ReleasedVersionsDetails>

    fun createBuildReceipt() {
        val createBuildReceipt by tasks.registering(BuildReceipt::class) {
            this.version.set(this@ModuleIdentityExtension.version.map { it.version })
            this.baseVersion.set(this@ModuleIdentityExtension.version.map { it.baseVersion.version })
            this.snapshot.set(this@ModuleIdentityExtension.snapshot)
            this.promotionBuild.set(this@ModuleIdentityExtension.promotionBuild)
            this.buildTimestampFrom(this@ModuleIdentityExtension.buildTimestamp)
            this.commitId.set(this@ModuleIdentityExtension.gradleBuildCommitId)
            this.receiptFolder.set(project.layout.buildDirectory.dir("generated-resources/build-receipt"))
        }
        tasks.named<Jar>("jar").configure {
            from(createBuildReceipt.map { it.receiptFolder })
        }
    }

    // pre-test/master/queue/alice/feature -> master
    // pre-test/release/current/bob/bugfix -> release
    private
    fun toPreTestedCommitBaseBranch(actualBranch: String): String = when {
        actualBranch.startsWith("pre-test/") -> actualBranch.substringAfter("/").substringBefore("/")
        else -> actualBranch
    }
}
