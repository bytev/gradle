/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.internal.buildoption;

import javax.annotation.Nullable;

public class StringInternalOption implements InternalOption<String> {
    private final String systemPropertyName;
    private final String defaultValue;

    public StringInternalOption(String systemPropertyName, @Nullable String defaultValue) {
        this.systemPropertyName = systemPropertyName;
        this.defaultValue = defaultValue;
    }

    @Nullable
    @Override
    public String getSystemPropertyName() {
        return systemPropertyName;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String convert(String value) {
        return value;
    }
}
