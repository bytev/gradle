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

package org.gradle.api.internal.tasks.properties;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import org.gradle.api.GradleException;
import org.gradle.api.NonNullApi;
import org.gradle.api.internal.tasks.DefaultTaskInputPropertySpec;
import org.gradle.api.internal.tasks.PropertySpecFactory;
import org.gradle.api.internal.tasks.TaskValidationContext;
import org.gradle.api.internal.tasks.ValidatingValue;
import org.gradle.api.internal.tasks.ValidationAction;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.internal.Factory;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.hash.HashCode;
import org.gradle.util.DeferredUtil;
import org.gradle.util.DeprecationLogger;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import static org.gradle.api.internal.tasks.TaskValidationContext.Severity.ERROR;

@NonNullApi
public class DefaultPropertyWalker implements PropertyWalker {

    private final PropertyMetadataStore propertyMetadataStore;
    private final ClassImplementationHasher classImplementationHasher;

    public DefaultPropertyWalker(PropertyMetadataStore propertyMetadataStore, ClassImplementationHasher classImplementationHasher) {
        this.propertyMetadataStore = propertyMetadataStore;
        this.classImplementationHasher = classImplementationHasher;
    }

    @Override
    public void visitProperties(PropertySpecFactory specFactory, PropertyVisitor visitor, Object bean) {
        Queue<PropertyNode> queue = new ArrayDeque<PropertyNode>();
        queue.add(new PropertyNode(null, bean));
        while (!queue.isEmpty()) {
            PropertyNode node = queue.remove();
            Object nested = node.getBean();
            TypeMetadata nestedTypeMetadata = propertyMetadataStore.getTypeMetadata(nested.getClass());
            if (node.isIterable(nestedTypeMetadata)) {
                Iterable<?> nestedBeans = (Iterable<?>) nested;
                int count = 0;
                for (Object nestedBean : nestedBeans) {
                    String nestedPropertyName = node.getQualifiedPropertyName("$" + ++count);
                    queue.add(new PropertyNode(nestedPropertyName, nestedBean));
                }
            } else {
                visitProperties(node, nestedTypeMetadata, queue, visitor, specFactory, classImplementationHasher);
            }
        }
    }

    private static void visitProperties(PropertyNode node, TypeMetadata typeMetadata, Queue<PropertyNode> queue, PropertyVisitor visitor, PropertySpecFactory specFactory, ClassImplementationHasher classImplementationHasher) {
        visitImplementation(node, visitor, specFactory, classImplementationHasher);
        for (PropertyMetadata propertyMetadata : typeMetadata.getPropertiesMetadata()) {
            PropertyValueVisitor propertyValueVisitor = propertyMetadata.getPropertyValueVisitor();
            if (propertyValueVisitor == null) {
                continue;
            }
            String propertyName = node.getQualifiedPropertyName(propertyMetadata.getFieldName());
            Object bean = node.getBean();
            PropertyValue propertyValue = new DefaultPropertyValue(propertyName, propertyMetadata.getAnnotations(), bean, propertyMetadata.getMethod());
            propertyValueVisitor.visitPropertyValue(propertyValue, visitor, specFactory);
            if (propertyValue.isAnnotationPresent(Nested.class)) {
                try {
                    Object nested = propertyValue.getValue();
                    if (nested != null) {
                        queue.add(new PropertyNode(propertyName, nested));
                    }
                } catch (Exception e) {
                    // No nested bean
                }
            }
        }
    }

    private static void visitImplementation(PropertyNode node, PropertyVisitor visitor, PropertySpecFactory specFactory, ClassImplementationHasher hasher) {
        // The root bean (Task) implementation is currently tracked separately
        if (!node.isRoot()) {
            DefaultTaskInputPropertySpec implementation = specFactory.createInputPropertySpec(node.getQualifiedPropertyName("$$implementation$$"), new ImplementationPropertyValue(hasher.getImplementationHash(node.getBeanClass())));
            implementation.optional(false);
            visitor.visitInputProperty(implementation);
        }
    }

    private static class PropertyNode extends AbstractBeanNode {
        private final Object bean;

        public PropertyNode(@Nullable String parentPropertyName, Object bean) {
            super(parentPropertyName, Preconditions.checkNotNull(bean, "Null is not allowed as nested property '" + parentPropertyName + "'").getClass());
            this.bean = bean;
        }

        public Object getBean() {
            return bean;
        }
    }

    private static class DefaultPropertyValue implements PropertyValue {
        private final String propertyName;
        private final List<Annotation> annotations;
        private final Object bean;
        private final Method method;
        private final Supplier<Object> valueSupplier = Suppliers.memoize(new Supplier<Object>() {
            @Override
            @Nullable
            public Object get() {
                Object value = DeprecationLogger.whileDisabled(new Factory<Object>() {
                    public Object create() {
                        try {
                            return method.invoke(bean);
                        } catch (InvocationTargetException e) {
                            throw UncheckedException.throwAsUncheckedException(e.getCause());
                        } catch (Exception e) {
                            throw new GradleException(String.format("Could not call %s.%s() on %s", method.getDeclaringClass().getSimpleName(), method.getName(), bean), e);
                        }
                    }
                });
                return value instanceof Provider ? ((Provider<?>) value).getOrNull() : value;
            }
        });

        public DefaultPropertyValue(String propertyName, List<Annotation> annotations, Object bean, Method method) {
            this.propertyName = propertyName;
            this.annotations = ImmutableList.copyOf(annotations);
            this.bean = bean;
            this.method = method;
            method.setAccessible(true);
        }

        @Override
        public String getPropertyName() {
            return propertyName;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            return getAnnotation(annotationType) != null;
        }

        @Nullable
        @Override
        public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
            for (Annotation annotation : annotations) {
                if (annotationType.equals(annotation.annotationType())) {
                    return annotationType.cast(annotation);
                }
            }
            return null;
        }

        @Override
        public boolean isOptional() {
            return isAnnotationPresent(Optional.class);
        }

        @Nullable
        @Override
        public Object getValue() {
            return valueSupplier.get();
        }

        @Nullable
        @Override
        public Object call() {
            return getValue();
        }

        @Override
        public void validate(String propertyName, boolean optional, ValidationAction valueValidator, TaskValidationContext context) {
            Object unpacked = DeferredUtil.unpack(getValue());
            if (unpacked == null) {
                if (!optional) {
                    context.recordValidationMessage(ERROR, String.format("No value has been specified for property '%s'.", propertyName));
                }
            } else {
                valueValidator.validate(propertyName, unpacked, context, ERROR);
            }
        }
    }

    private static class ImplementationPropertyValue implements ValidatingValue {

        private final HashCode hash;

        public ImplementationPropertyValue(@Nullable HashCode hash) {
            this.hash = hash;
        }

        @Override
        public Object call() {
            return hash;
        }

        @Override
        public void validate(String propertyName, boolean optional, ValidationAction valueValidator, TaskValidationContext context) {
        }
    }
}
