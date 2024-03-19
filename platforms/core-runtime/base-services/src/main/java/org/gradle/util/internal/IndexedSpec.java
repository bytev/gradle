/*
 * Copyright 2024 original author or authors.
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
package org.gradle.util.internal;

/**
 * Represents some predicate against objects of type T with a position in an iterable collection.
 *
 * @param <T> The target type for this spec
 */
public interface IndexedSpec<T> {
    boolean isSatisfiedBy(int index, T element);
}
