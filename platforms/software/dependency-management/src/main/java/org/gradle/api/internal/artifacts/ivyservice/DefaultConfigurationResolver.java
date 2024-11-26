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

package org.gradle.api.internal.artifacts.ivyservice;

import com.google.common.collect.ImmutableList;
import org.gradle.api.artifacts.DependencyConstraint;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.internal.artifacts.ComponentModuleMetadataHandlerInternal;
import org.gradle.api.internal.artifacts.ConfigurationResolver;
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier;
import org.gradle.api.internal.artifacts.DefaultResolverResults;
import org.gradle.api.internal.artifacts.RepositoriesSupplier;
import org.gradle.api.internal.artifacts.ResolverResults;
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal;
import org.gradle.api.internal.artifacts.configurations.ResolutionStrategyInternal;
import org.gradle.api.internal.artifacts.dsl.ImmutableModuleReplacements;
import org.gradle.api.internal.artifacts.ivyservice.moduleconverter.RootComponentMetadataBuilder;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.conflicts.Conflict;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.results.DefaultVisitedGraphResults;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.graph.results.VisitedGraphResults;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.ResolutionResultGraphBuilder;
import org.gradle.api.internal.artifacts.repositories.ContentFilteringRepository;
import org.gradle.api.internal.artifacts.repositories.ResolutionAwareRepository;
import org.gradle.api.internal.artifacts.result.MinimalResolutionResult;
import org.gradle.api.internal.artifacts.type.ArtifactTypeRegistry;
import org.gradle.api.internal.attributes.AttributeContainerInternal;
import org.gradle.api.internal.attributes.AttributeDesugaring;
import org.gradle.api.internal.attributes.AttributeSchemaServices;
import org.gradle.api.internal.attributes.immutable.artifact.ImmutableArtifactTypeRegistry;
import org.gradle.api.internal.project.ProjectIdentity;
import org.gradle.internal.component.external.model.DefaultModuleComponentSelector;
import org.gradle.internal.component.external.model.ImmutableCapabilities;
import org.gradle.internal.component.local.model.LocalComponentGraphResolveState;
import org.gradle.internal.component.local.model.LocalVariantGraphResolveState;
import org.gradle.internal.component.model.DependencyMetadata;
import org.gradle.internal.component.model.LocalComponentDependencyMetadata;
import org.gradle.internal.deprecation.DeprecationLogger;
import org.gradle.internal.model.CalculatedValue;
import org.gradle.util.Path;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Responsible for resolving a configuration. Delegates to a {@link ShortCircuitingResolutionExecutor} to perform
 * the actual resolution.
 */
public class DefaultConfigurationResolver implements ConfigurationResolver {
    private final RepositoriesSupplier repositoriesSupplier;
    private final ShortCircuitingResolutionExecutor resolutionExecutor;
    private final AttributeDesugaring attributeDesugaring;
    private final ArtifactTypeRegistry artifactTypeRegistry;
    private final ComponentModuleMetadataHandlerInternal componentModuleMetadataHandler;
    private final AttributeSchemaServices attributeSchemaServices;

    public DefaultConfigurationResolver(
        RepositoriesSupplier repositoriesSupplier,
        ShortCircuitingResolutionExecutor resolutionExecutor,
        AttributeDesugaring attributeDesugaring,
        ArtifactTypeRegistry artifactTypeRegistry,
        ComponentModuleMetadataHandlerInternal componentModuleMetadataHandler,
        AttributeSchemaServices attributeSchemaServices
    ) {
        this.repositoriesSupplier = repositoriesSupplier;
        this.resolutionExecutor = resolutionExecutor;
        this.attributeDesugaring = attributeDesugaring;
        this.artifactTypeRegistry = artifactTypeRegistry;
        this.componentModuleMetadataHandler = componentModuleMetadataHandler;
        this.attributeSchemaServices = attributeSchemaServices;
    }

    @Override
    public ResolverResults resolveBuildDependencies(ConfigurationInternal configuration, CalculatedValue<ResolverResults> futureCompleteResults) {
        VisitedGraphResults missingConfigurationResults = maybeGetEmptyGraphForInvalidMissingConfigurationWithNoDependencies(configuration);
        if (missingConfigurationResults != null) {
            return DefaultResolverResults.buildDependenciesResolved(missingConfigurationResults, ShortCircuitingResolutionExecutor.EmptyResults.INSTANCE,
                DefaultResolverResults.DefaultLegacyResolverResults.buildDependenciesResolved(ShortCircuitingResolutionExecutor.EmptyResults.INSTANCE)
            );
        }

        ResolutionParameters params = getResolutionParameters(configuration, false);
        return resolutionExecutor.resolveBuildDependencies(configuration, params, futureCompleteResults);
    }

    @Override
    public ResolverResults resolveGraph(ConfigurationInternal configuration) {
        VisitedGraphResults missingConfigurationResults = maybeGetEmptyGraphForInvalidMissingConfigurationWithNoDependencies(configuration);
        if (missingConfigurationResults != null) {
            ResolvedConfiguration resolvedConfiguration = new DefaultResolvedConfiguration(
                missingConfigurationResults, configuration.getResolutionHost(), ShortCircuitingResolutionExecutor.EmptyResults.INSTANCE, new ShortCircuitingResolutionExecutor.EmptyLenientConfiguration()
            );
            return DefaultResolverResults.graphResolved(missingConfigurationResults, ShortCircuitingResolutionExecutor.EmptyResults.INSTANCE,
                DefaultResolverResults.DefaultLegacyResolverResults.graphResolved(
                    ShortCircuitingResolutionExecutor.EmptyResults.INSTANCE, resolvedConfiguration
                )
            );
        }

        AttributeContainerInternal attributes = configuration.toRootComponent().getRootVariant().getAttributes();
        List<ResolutionAwareRepository> filteredRepositories = repositoriesSupplier.get().stream()
            .filter(repository -> !shouldSkipRepository(repository, configuration.getName(), attributes))
            .collect(Collectors.toList());

        ResolutionParameters params = getResolutionParameters(configuration, true);
        return resolutionExecutor.resolveGraph(configuration, params, filteredRepositories);
    }

    @Override
    public List<ResolutionAwareRepository> getAllRepositories() {
        return repositoriesSupplier.get();
    }

    private ResolutionParameters getResolutionParameters(ConfigurationInternal configuration, boolean includeSyntheticDependencies) {
        ResolutionStrategyInternal resolutionStrategy = configuration.getResolutionStrategy();

        RootComponentMetadataBuilder.RootComponentState rootComponent = configuration.toRootComponent();
        ImmutableList<? extends DependencyMetadata> syntheticDependencies = includeSyntheticDependencies ? getConsistentResolutionConstraints(configuration) : ImmutableList.of();
        ImmutableArtifactTypeRegistry immutableArtifactTypeRegistry = attributeSchemaServices.getArtifactTypeRegistryFactory().create(artifactTypeRegistry);
        ImmutableModuleReplacements moduleReplacements = componentModuleMetadataHandler.getModuleReplacements();
        ConfigurationFailureResolutions failureResolutions = new ConfigurationFailureResolutions(configuration.getDomainObjectContext().getProjectIdentity(), configuration.getName());

        return new ResolutionParameters(
            configuration.getResolutionHost(),
            rootComponent.getRootComponent(),
            rootComponent.getRootVariant(),
            syntheticDependencies,
            resolutionStrategy.getSortOrder(),
            configuration.getConfigurationIdentity(),
            immutableArtifactTypeRegistry,
            moduleReplacements,
            resolutionStrategy.getConflictResolution(),
            configuration.getDependencyLockingId(),
            resolutionStrategy.isDependencyLockingEnabled(),
            resolutionStrategy.getIncludeAllSelectableVariantResults(),
            resolutionStrategy.isDependencyVerificationEnabled(),
            resolutionStrategy.isFailingOnDynamicVersions(),
            resolutionStrategy.isFailingOnChangingVersions(),
            failureResolutions
        );
    }

    private static ImmutableList<? extends DependencyMetadata> getConsistentResolutionConstraints(ConfigurationInternal configuration) {
        ImmutableList<DependencyConstraint> consistentResolutionConstraints = configuration.getConsistentResolutionConstraints();
        ImmutableList.Builder<DependencyMetadata> builder = ImmutableList.builderWithExpectedSize(consistentResolutionConstraints.size());
        for (DependencyConstraint dc : consistentResolutionConstraints) {
            ModuleComponentSelector selector = DefaultModuleComponentSelector.newSelector(
                DefaultModuleIdentifier.newId(dc.getGroup(), dc.getName()), dc.getVersionConstraint()
            );
            builder.add(new LocalComponentDependencyMetadata(
                selector, null, Collections.emptyList(), Collections.emptyList(),
                false, false, false, true, false, true, dc.getReason()
            ));
        }
        return builder.build();
    }

    private static class ConfigurationFailureResolutions implements ResolutionParameters.FailureResolutions {

        private final @Nullable ProjectIdentity owningProject;
        private final String configurationName;

        public ConfigurationFailureResolutions(
            @Nullable ProjectIdentity owningProject,
            String configurationName
        ) {
            this.owningProject = owningProject;
            this.configurationName = configurationName;
        }

        @Override
        public List<String> forVersionConflict(Set<Conflict> conflicts) {
            if (owningProject == null) {
                // owningProject is null for settings execution
                return Collections.emptyList();
            }

            String taskPath = owningProject.getBuildTreePath().append(Path.path("dependencyInsight")).getPath();

            ModuleVersionIdentifier identifier = conflicts.iterator().next().getVersions().get(0);
            String dependencyNotation = identifier.getGroup() + ":" + identifier.getName();

            return Collections.singletonList(String.format(
                "Run with %s --configuration %s --dependency %s to get more insight on how to solve the conflict.",
                taskPath, configurationName, dependencyNotation
            ));
        }
    }

    /**
     * Determines if the repository should not be used to resolve this configuration.
     */
    private static boolean shouldSkipRepository(
        ResolutionAwareRepository repository,
        String resolveContextName,
        AttributeContainer consumerAttributes
    ) {
        if (!(repository instanceof ContentFilteringRepository)) {
            return false;
        }

        ContentFilteringRepository cfr = (ContentFilteringRepository) repository;

        Set<String> includedConfigurations = cfr.getIncludedConfigurations();
        Set<String> excludedConfigurations = cfr.getExcludedConfigurations();

        if ((includedConfigurations != null && !includedConfigurations.contains(resolveContextName)) ||
            (excludedConfigurations != null && excludedConfigurations.contains(resolveContextName))
        ) {
            return true;
        }

        Map<Attribute<Object>, Set<Object>> requiredAttributes = cfr.getRequiredAttributes();
        return hasNonRequiredAttribute(requiredAttributes, consumerAttributes);
    }

    /**
     * Accepts a map of attribute types to the set of values that are allowed for that attribute type.
     * If the request attributes of the resolve context being resolved do not match the allowed values,
     * then the repository is skipped.
     */
    private static boolean hasNonRequiredAttribute(
        @Nullable Map<Attribute<Object>, Set<Object>> requiredAttributes,
        AttributeContainer consumerAttributes
    ) {
        if (requiredAttributes == null) {
            return false;
        }

        for (Map.Entry<Attribute<Object>, Set<Object>> entry : requiredAttributes.entrySet()) {
            Attribute<Object> key = entry.getKey();
            Set<Object> allowedValues = entry.getValue();
            Object value = consumerAttributes.getAttribute(key);
            if (!allowedValues.contains(value)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifies if the configuration has been removed from the container before it was resolved.
     * This fails and has failed in the past, but only when the configuration has dependencies.
     * The short-circuiting done in {@link ShortCircuitingResolutionExecutor} made us not fail
     * when we should have. So, detect that case and deprecate it. This should be removed in 9.0,
     * and we will fail later on without any extra logic when calling
     * {@link RootComponentMetadataBuilder.RootComponentState#getRootVariant()}
     */
    @Nullable
    private VisitedGraphResults maybeGetEmptyGraphForInvalidMissingConfigurationWithNoDependencies(ConfigurationInternal configuration) {
        LocalComponentGraphResolveState rootComponent = configuration.toRootComponent().getRootComponent();

        // This variant can be null if the configuration was removed from the container before resolution.
        @SuppressWarnings("deprecation") LocalVariantGraphResolveState rootVariant =
            rootComponent.getConfigurationLegacy(configuration.getName());

        if (rootVariant == null) {
            configuration.runDependencyActions();
            if (configuration.getAllDependencies().isEmpty()) {
                DeprecationLogger.deprecateBehaviour("Removing a configuration from the container before resolution")
                    .withAdvice("Do not remove configurations from the container and resolve them after.")
                    .willBecomeAnErrorInGradle9()
                    .undocumented()
                    .nagUser();

                MinimalResolutionResult emptyResult = ResolutionResultGraphBuilder.empty(
                    rootComponent.getModuleVersionId(),
                    rootComponent.getId(),
                    configuration.getAttributes().asImmutable(),
                    ImmutableCapabilities.of(rootComponent.getDefaultCapability()),
                    configuration.getName(),
                    attributeDesugaring
                );

                return new DefaultVisitedGraphResults(emptyResult, Collections.emptySet(), null);
            }
        }

        return null;
    }

}
