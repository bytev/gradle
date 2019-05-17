/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.api.artifacts.transform;

import org.gradle.api.Incubating;
import org.gradle.api.file.FileCollection;
import org.gradle.api.reflect.InjectionPointQualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attached to a property that should receive the <em>artifact dependencies</em> of the {@link InputArtifact} of an artifact transform.
 *
 * <p>
 *     The order of the files match that of the dependencies in the source artifact view.
 *     The type of the injected dependencies is {@link org.gradle.api.file.FileCollection}.
 *     For example, when a project depends on the guava jar, when the project is transformed (i.e. the project is the input artifact),
 *     the input artifact dependencies is the file collection containing only the guava jar.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>
 * abstract class MyTransform implements TransformAction&lt;TransformParameters.None&gt; {
 *
 *     {@literal @}InputArtifact
 *     abstract Provider&lt;FileSystemLocation&gt; getInputArtifact();
 *
 *     {@literal @}InputArtifactDependencies
 *     abstract FileCollection getDependencies();
 *
 *     {@literal @}Override
 *     void transform(TransformOutputs outputs) {
 *         FileCollection dependencies = getDependencies();
 *         ... // Do something with the dependencies
 *     }
 * }
 * </pre>
 *
 * @since 5.3
 */
@Incubating
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
@InjectionPointQualifier(supportedTypes = FileCollection.class)
public @interface InputArtifactDependencies {
}
