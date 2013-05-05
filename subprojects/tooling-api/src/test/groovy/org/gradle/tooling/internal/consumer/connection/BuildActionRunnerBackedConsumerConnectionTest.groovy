/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.tooling.internal.consumer.connection

import org.gradle.tooling.internal.adapter.ProtocolToModelAdapter
import org.gradle.tooling.internal.consumer.parameters.ConsumerConnectionParameters
import org.gradle.tooling.internal.consumer.parameters.ConsumerOperationParameters
import org.gradle.tooling.internal.consumer.versioning.CustomModel
import org.gradle.tooling.internal.consumer.versioning.ModelMapping
import org.gradle.tooling.internal.protocol.BuildActionRunner
import org.gradle.tooling.internal.protocol.BuildResult
import org.gradle.tooling.internal.protocol.ConfigurableConnection
import org.gradle.tooling.internal.protocol.ConnectionMetaDataVersion1
import org.gradle.tooling.internal.protocol.ConnectionVersion4
import org.gradle.tooling.model.GradleProject
import org.gradle.tooling.model.build.BuildEnvironment
import org.gradle.tooling.model.eclipse.EclipseProject
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject
import org.gradle.tooling.model.idea.BasicIdeaProject
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.internal.outcomes.ProjectOutcomes
import spock.lang.Specification

class BuildActionRunnerBackedConsumerConnectionTest extends Specification {
    final TestBuildActionRunner target = Mock() {
        getMetaData() >> Mock(ConnectionMetaDataVersion1)
    }
    final ConsumerOperationParameters parameters = Stub()
    final ModelMapping modelMapping = Stub()
    final ProtocolToModelAdapter adapter = Mock()
    final BuildActionRunnerBackedConsumerConnection connection = new BuildActionRunnerBackedConsumerConnection(target, modelMapping, adapter)

    def "describes capabilities of the provider"() {
        given:
        def details = connection.versionDetails

        expect:
        details.supportsConfiguringJavaHome()
        details.supportsConfiguringJvmArguments()
        details.supportsConfiguringStandardInput()
        details.supportsGradleProjectModel()
        details.supportsRunningTasksWhenBuildingModel()

        and:
        details.isModelSupported(HierarchicalEclipseProject)
        details.isModelSupported(EclipseProject)
        details.isModelSupported(IdeaProject)
        details.isModelSupported(BasicIdeaProject)
        details.isModelSupported(GradleProject)
        details.isModelSupported(BuildEnvironment)
        details.isModelSupported(ProjectOutcomes)
        details.isModelSupported(Void)

        and:
        !details.isModelSupported(CustomModel)
    }

    def "configures connection"() {
        def parameters = new ConsumerConnectionParameters(false)

        when:
        connection.configure(parameters)

        then:
        1 * target.configure(parameters)
        0 * target._
    }

    def "builds model using connection's run() method"() {
        BuildResult<String> result = Mock()

        given:
        result.model >> 12

        when:
        def model = connection.run(String.class, parameters)

        then:
        model == 'ok'

        and:
        _ * modelMapping.getProtocolType(String.class) >> Integer.class
        1 * target.run(Integer.class, parameters) >> result
        1 * adapter.adapt(String.class, 12, _) >> 'ok'
        0 * target._
    }

    interface TestBuildActionRunner extends ConnectionVersion4, BuildActionRunner, ConfigurableConnection {
    }
}
