/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.core.api.extension.RuntimeExtensionModelProvider;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.LoaderDescriber;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.xml.ArtifactXmlExtensionModelLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;

import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discover the {@link ExtensionModel} based on the {@link ExtensionModelLoader} type.
 *
 * @since 4.0
 */
public class ExtensionModelDiscoverer {

  private final static Logger LOGGER = LoggerFactory.getLogger(ExtensionModelDiscoverer.class);

  /**
   * For each artifactPlugin discovers the {@link ExtensionModel}.
   *
   * @param loaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders.
   * @param artifactPlugins {@link Pair} of {@link ArtifactPluginDescriptor} and {@link ArtifactClassLoader} for artifact plugins
   *        deployed inside the artifact. Non null.
   * @return {@link Set} of {@link Pair} carrying the {@link ArtifactPluginDescriptor} and it's corresponding
   *         {@link ExtensionModel}.
   */
  public Set<Pair<ArtifactDescriptor, ExtensionModel>> discoverExtensionModels(Optional<Pair<ClassLoader, ArtifactDescriptor>> artifact,
                                                                               ExtensionModelLoaderRepository loaderRepository,
                                                                               List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> artifactPlugins) {
    return discoverExtensionModels(artifact, loaderRepository, artifactPlugins, new HashSet<>());
  }

  /**
   * For each artifactPlugin discovers the {@link ExtensionModel}.
   *
   * @param loaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders.
   * @param artifactPlugins {@link Pair} of {@link ArtifactPluginDescriptor} and {@link ArtifactClassLoader} for artifact plugins
   *        deployed inside the artifact. Non null.
   * @param parentArtifactExtensions {@link Set} of {@link ExtensionModel} to also take into account when parsing extensions
   * @return {@link Set} of {@link Pair} carrying the {@link ArtifactPluginDescriptor} and it's corresponding
   *         {@link ExtensionModel}.
   */
  public Set<Pair<ArtifactDescriptor, ExtensionModel>> discoverExtensionModels(Optional<Pair<ClassLoader, ArtifactDescriptor>> artifact,
                                                                               ExtensionModelLoaderRepository loaderRepository,
                                                                               List<Pair<ArtifactPluginDescriptor, ArtifactClassLoader>> artifactPlugins,
                                                                               Set<ExtensionModel> parentArtifactExtensions) {


    final Set<Pair<ArtifactDescriptor, ExtensionModel>> descriptorsWithExtensions = new HashSet<>();
    artifactPlugins.forEach(artifactPlugin -> {
      Set<ExtensionModel> extensions = descriptorsWithExtensions.stream().map(Pair::getSecond).collect(toSet());
      extensions.addAll(parentArtifactExtensions);
      final ArtifactPluginDescriptor artifactPluginDescriptor = artifactPlugin.getFirst();
      Optional<LoaderDescriber> loaderDescriber = artifactPluginDescriptor.getExtensionModelDescriptorProperty();
      ClassLoader artifactClassloader = artifactPlugin.getSecond().getClassLoader();
      String artifactName = artifactPluginDescriptor.getName();
      ExtensionModel extension = loaderDescriber
          .map(describer -> discoverExtensionThroughJsonDescriber(loaderRepository, describer,
                                                                  extensions, artifactClassloader,
                                                                  artifactName))
          .orElse(null);
      if (extension != null) {
        descriptorsWithExtensions.add(new Pair<>(artifactPluginDescriptor, extension));
      }
    });

    // the descriptor may be null in the case of the default domian
    if (artifact.isPresent() && artifact.get().getSecond() != null) {
      Set<String> configResources = artifact.get().getSecond().getConfigResources();
      if (configResources == null) {
        configResources = Collections.emptySet();
      }
      String[] configurationFiles = configResources.toArray(new String[0]);
      BundleDescriptor bundleDescriptor = artifact.get().getSecond().getBundleDescriptor();
      ClassLoader executionClassLoader = artifact.get().getFirst();

      LoaderDescriber applicationLoaderDescriber = new LoaderDescriber(ArtifactXmlExtensionModelLoader.DESCRIBER_ID);
      HashMap<String, Object> attributes = new HashMap<>();

      //TODO PLG fix since there could be others

      attributes.put(ArtifactXmlExtensionModelLoader.TYPE, artifact.get().getSecond().getBundleDescriptor().getClassifier()
          .map(classifier -> classifier.equals("mule-application") ? ArtifactType.APP : ArtifactType.DOMAIN).get());
      attributes.put(ArtifactXmlExtensionModelLoader.RESOURCE_XMLS, configurationFiles);
      attributes.put(ArtifactXmlExtensionModelLoader.NAME, bundleDescriptor.getGroupId()
          + "/" + bundleDescriptor.getArtifactId());
      applicationLoaderDescriber.addAttributes(attributes);

      ExtensionModelLoader applicationModelLoader = loaderRepository.getExtensionModelLoader(applicationLoaderDescriber)
          .orElseThrow(() -> new IllegalArgumentException(format("The identifier '%s' does not match with the describers available "
              + "to generate an ExtensionModel (working with the plugin '%s')", applicationLoaderDescriber.getId(),
                                                                 artifact.get().getSecond().getName())));

      try {
        ExtensionModel extensionModel = applicationModelLoader
            .loadExtensionModel(executionClassLoader,
                                getDefault(descriptorsWithExtensions.stream().map(pair -> pair.getSecond()).collect(toSet())),
                                applicationLoaderDescriber.getAttributes());
        descriptorsWithExtensions.add(new Pair(artifact.get().getSecond(), extensionModel));
      } catch (Exception e) {
        //TODO PLG for now we don't care for the POC
        LOGGER.error("####### fail retrieving artifact extension model", e);
      }
    }


    return descriptorsWithExtensions;
  }

  /**
   * Discover the extension models provided by the runtime.
   *
   * @return {@link Set} of the runtime provided {@link ExtensionModel}s.
   */
  public Set<ExtensionModel> discoverRuntimeExtensionModels() {
    final Set<ExtensionModel> extensionModels = new HashSet<>();

    Collection<RuntimeExtensionModelProvider> runtimeExtensionModelProviders = new SpiServiceRegistry()
        .lookupProviders(RuntimeExtensionModelProvider.class, Thread.currentThread().getContextClassLoader());
    for (RuntimeExtensionModelProvider runtimeExtensionModelProvider : runtimeExtensionModelProviders) {
      extensionModels.add(runtimeExtensionModelProvider.createExtensionModel());
    }
    return extensionModels;
  }

  /**
   * Looks for an extension using the mule-artifact.json file, where if available it will parse it using the
   * {@link ExtensionModelLoader} which {@link ExtensionModelLoader#getId() ID} matches the plugin's descriptor ID.
   *
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders.
   * @param loaderDescriber a descriptor that contains parametrization to construct an {@link ExtensionModel}
   * @param extensions with the previously generated {@link ExtensionModel}s that will be used to generate the current
   *        {@link ExtensionModel} and store it in {@code extensions} once generated.
   * @param artifactClassloader the loaded artifact {@link ClassLoader} to find the required resources.
   * @param artifactName the name of the artifact being loaded.
   * @throws IllegalArgumentException there is no {@link ExtensionModelLoader} for the ID in the {@link MulePluginModel}.
   */
  private ExtensionModel discoverExtensionThroughJsonDescriber(ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                                               LoaderDescriber loaderDescriber, Set<ExtensionModel> extensions,
                                                               ClassLoader artifactClassloader, String artifactName) {
    ExtensionModelLoader loader = extensionModelLoaderRepository.getExtensionModelLoader(loaderDescriber)
        .orElseThrow(() -> new IllegalArgumentException(format("The identifier '%s' does not match with the describers available "
            + "to generate an ExtensionModel (working with the plugin '%s')", loaderDescriber.getId(), artifactName)));
    ExtensionModel coreModel = MuleExtensionModelProvider.getExtensionModel();
    if (!extensions.contains(coreModel)) {
      extensions = ImmutableSet.<ExtensionModel>builder().addAll(extensions).add(coreModel).build();
    }
    return loader.loadExtensionModel(artifactClassloader, getDefault(extensions), loaderDescriber.getAttributes());
  }
}
