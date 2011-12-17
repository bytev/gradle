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
package org.gradle.cache.internal;

import org.gradle.api.internal.Factory;
import org.gradle.util.UncheckedException;

import java.util.concurrent.Callable;

public abstract class AbstractFileAccess implements FileAccess {
    public <T> T readFromFile(final Callable<? extends T> action) throws LockTimeoutException {
        return readFromFile(new Factory<T>() {
            public T create() {
                try {
                    return action.call();
                } catch (Exception e) {
                    throw UncheckedException.asUncheckedException(e);
                }
            }
        });
    }
}
