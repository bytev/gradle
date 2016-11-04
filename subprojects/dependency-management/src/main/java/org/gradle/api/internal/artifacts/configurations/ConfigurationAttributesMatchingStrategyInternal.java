/*
 * Copyright 2016 the original author or authors.
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
package org.gradle.api.internal.artifacts.configurations;

import org.gradle.api.artifacts.ConfigurationAttributeMatcher;
import org.gradle.api.artifacts.ConfigurationAttributesMatchingStrategy;

public interface ConfigurationAttributesMatchingStrategyInternal extends ConfigurationAttributesMatchingStrategy {
    /**
     * Returns the attribute matcher for a specific attribute name.
     * @param attributeName the name of the attribute
     * @return the matcher for this attribute. Must never be null.
     */
    ConfigurationAttributeMatcher getAttributeMatcher(String attributeName);

    /**
     * Sets the attribute matcher for a particular attribute.
     * @param attributeName the name of the attribute.
     * @param matcher the matcher for this attribute
     */
    void setAttributeMatcher(String attributeName, ConfigurationAttributeMatcher matcher);

    ConfigurationAttributesMatchingStrategyInternal copy();

    interface ConfigurationAttributeMatcherBuilderInternal extends ConfigurationAttributesMatchingStrategy.ConfigurationAttributeMatcherBuilder {
        /**
         * Builds the matcher.
         * @return implementations are required to return an immutable matcher.
         */
        ConfigurationAttributeMatcher build();
    }
}
