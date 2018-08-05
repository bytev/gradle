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
package org.gradle.api.internal.artifacts.ivyservice;

import org.gradle.api.Transformer;
import org.gradle.api.internal.filestore.ivy.ArtifactIdentifierFileStore;
import org.gradle.cache.CacheBuilder;
import org.gradle.cache.CacheRepository;
import org.gradle.cache.CleanupAction;
import org.gradle.cache.FileLockManager;
import org.gradle.cache.PersistentCache;
import org.gradle.cache.PersistentIndexedCache;
import org.gradle.cache.PersistentIndexedCacheParameters;
import org.gradle.cache.internal.CacheCleanupFileAccessTimeProvider;
import org.gradle.cache.internal.CompositeCleanupAction;
import org.gradle.cache.internal.DelegatingCleanupAction;
import org.gradle.cache.internal.LeastRecentlyUsedCacheCleanup;
import org.gradle.cache.internal.SingleDepthFilesFinder;
import org.gradle.cache.internal.SnapshottingCacheCleanupFileAccessTimeProvider;
import org.gradle.cache.internal.UnusedVersionsCacheCleanup;
import org.gradle.cache.internal.UsedGradleVersions;
import org.gradle.internal.Factory;
import org.gradle.internal.resource.cached.ExternalResourceFileStore;
import org.gradle.internal.resource.local.FileAccessTimeJournal;
import org.gradle.internal.serialize.Serializer;

import javax.annotation.Nullable;
import java.io.Closeable;

import static org.gradle.cache.internal.LeastRecentlyUsedCacheCleanup.DEFAULT_MAX_AGE_IN_DAYS_FOR_EXTERNAL_CACHE_ENTRIES;
import static org.gradle.cache.internal.filelock.LockOptionsBuilder.mode;

public class DefaultArtifactCacheLockingManager implements ArtifactCacheLockingManager, Closeable {
    private final PersistentCache cache;

    public DefaultArtifactCacheLockingManager(CacheRepository cacheRepository, ArtifactCacheMetadata cacheMetaData, FileAccessTimeJournal fileAccessTimeJournal,
                                              UsedGradleVersions usedGradleVersions) {
        cache = cacheRepository
                .cache(cacheMetaData.getCacheDir())
                .withCrossVersionCache(CacheBuilder.LockTarget.CacheDirectory)
                .withDisplayName("artifact cache")
                .withLockOptions(mode(FileLockManager.LockMode.None)) // Don't need to lock anything until we use the caches
                .withCleanup(createCleanupAction(cacheMetaData, fileAccessTimeJournal, usedGradleVersions))
                .open();
    }

    private CleanupAction createCleanupAction(ArtifactCacheMetadata cacheMetaData, FileAccessTimeJournal fileAccessTimeJournal, UsedGradleVersions usedGradleVersions) {
        CacheCleanupFileAccessTimeProvider fileAccessTimeProvider = new SnapshottingCacheCleanupFileAccessTimeProvider(fileAccessTimeJournal);
        long maxAgeInDays = DEFAULT_MAX_AGE_IN_DAYS_FOR_EXTERNAL_CACHE_ENTRIES;
        CleanupAction cleanupAction = CompositeCleanupAction.builder()
            .add(UnusedVersionsCacheCleanup.create(CacheLayout.ROOT.getName(), CacheLayout.ROOT.getVersionMapping(), usedGradleVersions))
            .add(cacheMetaData.getExternalResourcesStoreDirectory(),
                UnusedVersionsCacheCleanup.create(CacheLayout.RESOURCES.getName(), CacheLayout.RESOURCES.getVersionMapping(), usedGradleVersions),
                new LeastRecentlyUsedCacheCleanup(new SingleDepthFilesFinder(ExternalResourceFileStore.FILE_TREE_DEPTH_TO_TRACK_AND_CLEANUP), fileAccessTimeProvider, maxAgeInDays))
            .add(cacheMetaData.getFileStoreDirectory(),
                UnusedVersionsCacheCleanup.create(CacheLayout.FILE_STORE.getName(), CacheLayout.FILE_STORE.getVersionMapping(), usedGradleVersions),
                new LeastRecentlyUsedCacheCleanup(new SingleDepthFilesFinder(ArtifactIdentifierFileStore.FILE_TREE_DEPTH_TO_TRACK_AND_CLEANUP), fileAccessTimeProvider, maxAgeInDays))
            .add(cacheMetaData.getMetaDataStoreDirectory().getParentFile(),
                UnusedVersionsCacheCleanup.create(CacheLayout.META_DATA.getName(), CacheLayout.META_DATA.getVersionMapping(), usedGradleVersions))
            .build();
        return new DelegatingCleanupAction(cleanupAction, fileAccessTimeProvider);
    }

    @Override
    public void close() {
        cache.close();
    }

    @Override
    public <T> T withFileLock(Factory<? extends T> action) {
        return cache.withFileLock(action);
    }

    @Override
    public void withFileLock(Runnable action) {
        cache.withFileLock(action);
    }

    @Override
    public <T> T useCache(Factory<? extends T> action) {
        return cache.useCache(action);
    }

    @Override
    public void useCache(Runnable action) {
        cache.useCache(action);
    }

    @Override
    public <K, V> PersistentIndexedCache<K, V> createCache(String cacheName, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
        String cacheFileInMetaDataStore = CacheLayout.META_DATA.getKey() + "/" + cacheName;
        final PersistentIndexedCache<K, V> persistentCache = cache.createCache(new PersistentIndexedCacheParameters<K, V>(cacheFileInMetaDataStore, keySerializer, valueSerializer));
        return new CacheLockingPersistentCache<K, V>(persistentCache);
    }

    private class CacheLockingPersistentCache<K, V> implements PersistentIndexedCache<K, V> {
        private final PersistentIndexedCache<K, V> persistentCache;

        public CacheLockingPersistentCache(PersistentIndexedCache<K, V> persistentCache) {
            this.persistentCache = persistentCache;
        }

        @Nullable
        @Override
        public V get(final K key) {
            return cache.useCache(new Factory<V>() {
                @Override
                public V create() {
                    return persistentCache.get(key);
                }
            });
        }

        @Override
        public V get(final K key, final Transformer<? extends V, ? super K> producer) {
            return cache.useCache(new Factory<V>() {
                @Override
                public V create() {
                    return persistentCache.get(key, producer);
                }
            });
        }

        @Override
        public void put(final K key, final V value) {
            cache.useCache(new Runnable() {
                @Override
                public void run() {
                    persistentCache.put(key, value);
                }
            });
        }

        @Override
        public void remove(final K key) {
            cache.useCache(new Runnable() {
                @Override
                public void run() {
                    persistentCache.remove(key);
                }
            });
        }

        @Override
        public Snapshot<K, V> createSnapshot() {
            return cache.useCache(new Factory<Snapshot<K, V>>() {
                @Nullable
                @Override
                public Snapshot<K, V> create() {
                    return persistentCache.createSnapshot();
                }
            });
        }
    }
}
