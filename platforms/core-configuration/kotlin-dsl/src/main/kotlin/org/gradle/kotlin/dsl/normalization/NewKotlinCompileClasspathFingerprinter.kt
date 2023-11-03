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

@file:OptIn(ExperimentalBuildToolsApi::class)

package org.gradle.kotlin.dsl.normalization

import com.google.common.collect.ImmutableMultimap
import org.gradle.api.file.FileCollection
import org.gradle.internal.execution.FileCollectionFingerprinter
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint
import org.gradle.internal.fingerprint.FileCollectionFingerprint
import org.gradle.internal.fingerprint.FileNormalizer
import org.gradle.internal.fingerprint.FileSystemLocationFingerprint
import org.gradle.internal.fingerprint.FingerprintingStrategy
import org.gradle.internal.fingerprint.FingerprintingStrategy.COMPILE_CLASSPATH_IDENTIFIER
import org.gradle.internal.fingerprint.impl.EmptyCurrentFileCollectionFingerprint
import org.gradle.internal.hash.ChecksumService
import org.gradle.internal.hash.HashCode
import org.gradle.internal.hash.Hashing
import org.gradle.internal.snapshot.FileSystemSnapshot
import org.gradle.kotlin.dsl.support.walkReproducibly
import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jvm.AccessibleClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshot
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import java.io.File


internal
class NewKotlinCompileClasspathFingerprinter(
    val checksumService: ChecksumService,
    val classpathSnapshotHashesCache: KotlinDslCompileAvoidanceClasspathHashCache
) : FileCollectionFingerprinter { // TODO: rename/replace KotlinCompileClasspathFingerprinter

    override fun getNormalizer(): FileNormalizer {
        TODO("Not yet implemented")
    }

    override fun fingerprint(files: FileCollection): CurrentFileCollectionFingerprint {
        val fingerprints: Map<String, HashCode> = files
            .map {
                val hash = getHash(it)
                val path = it.path
                Pair(path, hash)
            }.toMap()
        return when {
            fingerprints.isEmpty() -> EmptyCurrentFileCollectionFingerprint(COMPILE_CLASSPATH_IDENTIFIER)
            else -> CurrentFileCollectionFingerprintImpl(fingerprints)
        }
    }

    private
    fun getHash(file: File): HashCode {
        val checksum = getChecksum(file)
        return checksum.let { classpathSnapshotHashesCache.getHash(it) { computeHashForFile(file) } }
    }

    private
    fun getChecksum(file: File): HashCode {
        return if (file.isFile) {
            getChecksumOfFile(file)
        } else {
            getChecksumOfDirectory(file)
        }
    }

    private
    fun getChecksumOfFile(file: File): HashCode = checksumService.md5(file)

    private
    fun getChecksumOfDirectory(file: File): HashCode {
        val hasher = Hashing.newHasher()
        file.walkReproducibly()
            .filter { it.isFile }
            .forEach {
                hasher.putHash(getChecksumOfFile(it))
            }
        return hasher.hash()
    }

    private
    fun computeHashForFile(file: File): HashCode {
        val snapshots = compilationService.calculateClasspathSnapshot(file, ClassSnapshotGranularity.CLASS_LEVEL).classSnapshots
        return hash(snapshots)
    }

    private
    fun hash(snapshots: Map<String, ClassSnapshot>): HashCode {
        val hasher = Hashing.newHasher()
        snapshots.entries.stream()
            .filter { it.value is AccessibleClassSnapshot }
            .map { (it.value as AccessibleClassSnapshot).classAbiHash }
            .forEach {
                hasher.putLong(it)
            }
        return hasher.hash()
    }

    override fun fingerprint(snapshot: FileSystemSnapshot, previousFingerprint: FileCollectionFingerprint?): CurrentFileCollectionFingerprint {
        TODO("Not yet implemented")
    }

    override fun empty(): CurrentFileCollectionFingerprint {
        TODO("Not yet implemented")
    }
}


private
class CurrentFileCollectionFingerprintImpl(private val fingerprints: Map<String, HashCode>) : CurrentFileCollectionFingerprint {

    private
    val hashCode: HashCode by lazy {
        val hasher = Hashing.newHasher()
        fingerprints.values.forEach {
            hasher.putHash(it)
        }
        hasher.hash()
    }

    override fun getHash(): HashCode = hashCode

    override fun getStrategyIdentifier(): String = COMPILE_CLASSPATH_IDENTIFIER

    override fun getSnapshot(): FileSystemSnapshot {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun archive(factory: CurrentFileCollectionFingerprint.ArchivedFileCollectionFingerprintFactory): FileCollectionFingerprint {
        TODO("Not yet implemented")
    }

    override fun getFingerprints(): Map<String, FileSystemLocationFingerprint> {
        TODO("Not yet implemented")
    }

    override fun getRootHashes(): ImmutableMultimap<String, HashCode> {
        TODO("Not yet implemented")
    }

    override fun wasCreatedWithStrategy(strategy: FingerprintingStrategy): Boolean {
        TODO("Not yet implemented")
    }
}


internal
val compilationService = compilationServiceFor<NewKotlinCompileClasspathFingerprinter>()


internal
inline fun <reified T : Any> compilationServiceFor(): CompilationService =
    CompilationService.loadImplementation(T::class.java.classLoader)
