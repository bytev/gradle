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

package org.gradle.api.internal.attributes;

import com.google.common.collect.ImmutableList;
import org.gradle.api.Action;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeMatchingStrategy;
import org.gradle.internal.component.AbstractVariantSelectionException;
import org.gradle.internal.component.resolution.failure.ResolutionFailure2;
import org.gradle.internal.component.resolution.failure.describer.ResolutionFailureDescriber;
import org.gradle.internal.component.model.AttributeMatcher;
import org.gradle.internal.component.resolution.failure.describer.ResolutionFailureDescriber2;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EmptySchema implements AttributesSchemaInternal {
    public static final EmptySchema INSTANCE = new EmptySchema();

    private final DoNothingCompatibilityRule compatibilityRule = new DoNothingCompatibilityRule();
    private final DoNothingDisambiguationRule disambiguationRule = new DoNothingDisambiguationRule();

    protected EmptySchema() {
    }

    @Override
    public CompatibilityRule<Object> compatibilityRules(Attribute<?> attribute) {
        return compatibilityRule;
    }

    @Override
    public DisambiguationRule<Object> disambiguationRules(Attribute<?> attribute) {
        return disambiguationRule;
    }

    @Override
    public List<AttributeDescriber> getConsumerDescribers() {
        return Collections.emptyList();
    }

    @Override
    public void addConsumerDescriber(AttributeDescriber describer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> AttributeMatchingStrategy<T> getMatchingStrategy(Attribute<T> attribute) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AttributeMatcher matcher() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AttributeMatcher withProducer(AttributesSchemaInternal producerSchema) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> AttributeMatchingStrategy<T> attribute(Attribute<T> attribute) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> AttributeMatchingStrategy<T> attribute(Attribute<T> attribute, Action<? super AttributeMatchingStrategy<T>> configureAction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Attribute<?>> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    public boolean hasAttribute(Attribute<?> key) {
        return false;
    }

    @Override
    public void attributeDisambiguationPrecedence(Attribute<?>... attribute) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAttributeDisambiguationPrecedence(List<Attribute<?>> attributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Attribute<?>> getAttributeDisambiguationPrecedence() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Attribute<?> getAttributeByName(String name) {
        return null;
    }

    @Override
    public void addFailureDescriber(Class<? extends ResolutionFailureDescriber<? extends AbstractVariantSelectionException>> describerClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <FAILURE extends ResolutionFailure2> List<ResolutionFailureDescriber2<?, FAILURE>> getFailureDescribers2(FAILURE failure) {
        return Collections.emptyList();
    }

    @Override
    public void addFailureDescriber2(Class<? extends ResolutionFailureDescriber2<?, ?>> describerClass) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ImmutableList<ResolutionFailureDescriber<? extends AbstractVariantSelectionException>> getFailureDescribers() {
        return ImmutableList.of();
    }

    private static class DoNothingCompatibilityRule implements CompatibilityRule<Object> {
        @Override
        public void execute(CompatibilityCheckResult<Object> result) {
        }

        @Override
        public boolean doesSomething() {
            return false;
        }
    }

    private static class DoNothingDisambiguationRule implements DisambiguationRule<Object> {
        @Override
        public void execute(MultipleCandidatesResult<Object> objectMultipleCandidatesResult) {
        }

        @Override
        public boolean doesSomething() {
            return false;
        }
    }
}
