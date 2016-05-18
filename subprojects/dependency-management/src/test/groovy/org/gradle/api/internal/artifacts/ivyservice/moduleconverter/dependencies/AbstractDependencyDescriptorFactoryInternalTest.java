/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.moduleconverter.dependencies;

import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExcludeRule;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.internal.artifacts.dependencies.DefaultDependencyArtifact;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.excludes.PatternMatchers;
import org.gradle.internal.component.external.descriptor.DefaultExclude;
import org.gradle.internal.component.model.DependencyMetaData;
import org.gradle.internal.component.model.Exclude;
import org.gradle.internal.component.model.IvyArtifactName;
import org.gradle.util.TestUtil;
import org.gradle.util.WrapUtil;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(JMock.class)
public abstract class AbstractDependencyDescriptorFactoryInternalTest {
    private JUnit4Mockery context = new JUnit4Mockery();

    protected static final String TEST_CONF = "conf";
    protected static final String TEST_DEP_CONF = "depconf1";

    protected static final ExcludeRule TEST_EXCLUDE_RULE = new org.gradle.api.internal.artifacts.DefaultExcludeRule("testOrg", null);
    protected static final Exclude TEST_IVY_EXCLUDE_RULE = getTestExcludeRule();
    protected ExcludeRuleConverter excludeRuleConverterStub = context.mock(ExcludeRuleConverter.class);
    protected final DefaultModuleDescriptor moduleDescriptor = TestUtil.createModuleDescriptor(WrapUtil.toSet(TEST_CONF));
    private DefaultDependencyArtifact artifact = new DefaultDependencyArtifact("name", "type", null, null, null);
    private DefaultDependencyArtifact artifactWithClassifiers = new DefaultDependencyArtifact("name2", "type2", "ext2", "classifier2", "http://www.url2.com");

    @Before
    public void setUp() {
        expectExcludeRuleConversion(TEST_EXCLUDE_RULE, TEST_IVY_EXCLUDE_RULE);
    }

    protected void expectExcludeRuleConversion(final ExcludeRule excludeRule, final Exclude exclude) {
        context.checking(new Expectations() {{
            allowing(excludeRuleConverterStub).convertExcludeRule(TEST_CONF, excludeRule);
            will(returnValue(exclude));
        }});
    }

    protected Dependency setUpDependency(ModuleDependency dependency) {
        return dependency.addArtifact(artifact).
                addArtifact(artifactWithClassifiers).
                exclude(WrapUtil.toMap("group", TEST_EXCLUDE_RULE.getGroup())).
                setTransitive(true);
    }

    protected void assertDependencyDescriptorHasCommonFixtureValues(DependencyMetaData dependencyMetaData) {
        assertEquals(TEST_IVY_EXCLUDE_RULE, dependencyMetaData.getExcludes(Collections.singleton(TEST_CONF)).get(0));
        assertThat(dependencyMetaData.getDependencyConfigurations(TEST_CONF, TEST_CONF), equalTo(WrapUtil.toArray(TEST_DEP_CONF)));
        assertThat(dependencyMetaData.isTransitive(), equalTo(true));
        assertDependencyDescriptorHasArtifacts(dependencyMetaData);
    }

    private void assertDependencyDescriptorHasArtifacts(DependencyMetaData dependencyMetaData) {
        List<IvyArtifactName> artifactDescriptors = WrapUtil.toList(dependencyMetaData.getArtifacts());
        assertThat(artifactDescriptors.size(), equalTo(2));

        IvyArtifactName artifactDescriptorWithoutClassifier = findDescriptor(artifactDescriptors, artifact);
        assertEquals(null, artifactDescriptorWithoutClassifier.getClassifier());
        compareArtifacts(artifact, artifactDescriptorWithoutClassifier);
        assertEquals(artifact.getType(), artifactDescriptorWithoutClassifier.getExtension());

        IvyArtifactName artifactDescriptorWithClassifierAndConfs = findDescriptor(artifactDescriptors, artifactWithClassifiers);
        assertEquals(artifactWithClassifiers.getClassifier(), artifactDescriptorWithClassifierAndConfs.getClassifier());
        compareArtifacts(artifactWithClassifiers, artifactDescriptorWithClassifierAndConfs);
        assertEquals(artifactWithClassifiers.getExtension(), artifactDescriptorWithClassifierAndConfs.getExtension());
    }

    private IvyArtifactName findDescriptor(List<IvyArtifactName> artifactDescriptors, DefaultDependencyArtifact dependencyArtifact) {
        for (IvyArtifactName artifactDescriptor : artifactDescriptors) {
            if (artifactDescriptor.getName().equals(dependencyArtifact.getName())) {
                return artifactDescriptor;
            }
        }
        throw new RuntimeException("Descriptor could not be found");
    }

    private void compareArtifacts(DependencyArtifact artifact, IvyArtifactName artifactDescriptor) {
        assertEquals(artifact.getName(), artifactDescriptor.getName());
        assertEquals(artifact.getType(), artifactDescriptor.getType());
    }

    private static DefaultExclude getTestExcludeRule() {
        return new DefaultExclude("org", "testOrg", new String[0], PatternMatchers.EXACT);
    }
}

