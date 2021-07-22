/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geode.gradle.jboss.modules.plugins.services;

import static org.apache.geode.gradle.jboss.modules.plugins.utils.ProjectUtils.getProjectDependenciesForConfiguration;
import static org.apache.geode.gradle.jboss.modules.plugins.utils.ProjectUtils.getTargetConfigurations;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.geode.gradle.jboss.modules.plugins.config.ModulesGeneratorConfig;
import org.apache.geode.gradle.jboss.modules.plugins.domain.DependencyWrapper;
import org.apache.geode.gradle.jboss.modules.plugins.generator.ModuleDescriptorGenerator;
import org.apache.geode.gradle.jboss.modules.plugins.generator.domain.ModuleDependency;
import org.apache.geode.gradle.jboss.modules.plugins.generator.xml.JBossModuleDescriptorGenerator;
import org.apache.geode.gradle.jboss.modules.plugins.utils.ProjectUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.SelfResolvingDependency;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GeodeJBossModuleDescriptorService extends GeodeCommonModuleDescriptorService {

    private static final String API = "api";
    private static final String IMPLEMENTATION = "implementation";
    private static final String RUNTIME_ONLY = "runtimeOnly";
    private static final String RUNTIME_CLASSPATH = "runtimeClasspath";
    private static final String JBOSS_MODULAR = "jbossModular";
    private static final String EXTERNAL_LIBRARY_DEPENDENCIES_MODULE_NAME = "external-library-dependencies";
    private static final String LIB_PATH_PREFIX = "../../../../lib/";
    private static final String PLUGIN_ID = "geode.jboss-modules-plugin";
    private final ModuleDescriptorGenerator moduleDescriptorGenerator = new JBossModuleDescriptorGenerator();
    private final Map<String, List<ResolvedArtifact>> resolvedProjectRuntimeArtifacts = new HashMap<>();
    private final Map<String, List<DependencyWrapper>> apiProjectDependencies = new HashMap<>();
    private final Map<String, List<DependencyWrapper>> runtimeProjectDependencies = new HashMap<>();
    private final Map<String, List<DependencyWrapper>> jbossProjectDependencies = new HashMap<>();
    private final Map<String, List<String>> embeddedArtifactNames = new HashMap<>();
    private List<Project> allProjects;

    @Override
    public void createModuleDescriptor(Project project, ModulesGeneratorConfig config,
                                       File projectJarFile) {

        List<ModuleDependency> moduleDependencies = generateModuleDependencies(project, config);

        List<String> resourceRoots = generateResourceRoots(project, projectJarFile, config);

        String moduleVersion = project.getVersion().toString();

        generateNewModuleDescriptor(project, config, moduleDependencies, resourceRoots, moduleVersion);
    }


    private void generateNewModuleDescriptor(Project project, ModulesGeneratorConfig config, List<ModuleDependency> moduleDependencies, List<String> resourceRoots, String moduleVersion) {
        moduleDescriptorGenerator.generate(config.outputRoot.resolve(config.name), project.getName(),
                moduleVersion, resourceRoots, moduleDependencies, config.mainClass);

        moduleDescriptorGenerator
                .generateAlias(config.outputRoot.resolve(config.name), project.getName(), moduleVersion);
    }

    private List<ModuleDependency> generateModuleDependencies(Project project,
                                                              ModulesGeneratorConfig config) {
        List<String> projectNames =
                getAllProjects(project).stream().filter(proj -> proj.getPluginManager().hasPlugin(
                        PLUGIN_ID)).map(Project::getName).collect(Collectors.toList());

        List<ModuleDependency> moduleDependencies = getModuleDependencies(projectNames,
                getApiProjectDependencies(project, config),
                getRuntimeProjectDependencies(project, config),
                getJBossProjectDependencies(project, config));

        moduleDependencies.add(new ModuleDependency(EXTERNAL_LIBRARY_DEPENDENCIES_MODULE_NAME,
                false, false));

        moduleDependencies.addAll(getJBossJDKModuleDependencies(config));
        return moduleDependencies;
    }

    private synchronized List<DependencyWrapper> getRuntimeProjectDependencies(Project project,
                                                                               ModulesGeneratorConfig config) {
        return getOrPopulateCachedDependencies(runtimeProjectDependencies, project, config, IMPLEMENTATION, RUNTIME_ONLY);
    }

    private synchronized List<DependencyWrapper> getJBossProjectDependencies(Project project,
                                                                             ModulesGeneratorConfig config) {
        return getOrPopulateCachedDependencies(jbossProjectDependencies, project, config, JBOSS_MODULAR);
    }

    private synchronized List<DependencyWrapper> getApiProjectDependencies(Project project,
                                                                           ModulesGeneratorConfig config) {

        return getOrPopulateCachedDependencies(apiProjectDependencies, project, config, API);
    }

    private List<DependencyWrapper> getOrPopulateCachedDependencies(
            Map<String, List<DependencyWrapper>> dependenciesMap, Project project,
            ModulesGeneratorConfig config, String... gradleConfigurations) {
        String key = config.name + project.getName();
        if (dependenciesMap.get(key) == null) {
            dependenciesMap.put(key, Collections.unmodifiableList(
                    getProjectDependenciesForConfiguration(project, getTargetConfigurations(
                            config.name, gradleConfigurations))));
        }
        return dependenciesMap.get(key);
    }

    @Override
    public void createExternalLibraryDependenciesModuleDescriptor(Project project,
                                                                  ModulesGeneratorConfig config) {
        Set<String> embeddedProjectArtifacts =
                new TreeSet<>(getEmbeddedArtifactsNames(project, config));
        List<ResolvedArtifact> resolvedArtifacts = getResolvedProjectRuntimeArtifacts(project, config);
        for (ResolvedArtifact resolvedArtifact : resolvedArtifacts) {
            for (Project innerProject : getAllProjects(project)) {
                if (innerProject.getName().equals(resolvedArtifact.getName())) {
                    embeddedProjectArtifacts.addAll(getEmbeddedArtifactsNames(innerProject, config));
                }
            }
        }

        Set<String> projectNames = getAllProjects(project).stream().map(Project::getName).collect(
                Collectors.toSet());

        List<String> artifactNames = resolvedArtifacts.stream()
                .filter(artifact -> !projectNames.contains(artifact.getName()))
                .filter(artifact -> !embeddedProjectArtifacts.contains(artifact.getName()))
                .map(artifact -> LIB_PATH_PREFIX + artifact.getFile().getName())
                .collect(Collectors.toList());

        List<ModuleDependency> moduleDependencies = getJBossJDKModuleDependencies(config);

        moduleDescriptorGenerator
                .generate(config.outputRoot.resolve(config.name), EXTERNAL_LIBRARY_DEPENDENCIES_MODULE_NAME,
                        project.getVersion().toString(), artifactNames, moduleDependencies);
    }

    private List<ModuleDependency> getJBossJDKModuleDependencies(
            ModulesGeneratorConfig config) {
        return config.jbossJdkModules == null ?
                Collections.emptyList() :
                config.jbossJdkModules.stream().map(jbossJdkModule ->
                        new ModuleDependency(jbossJdkModule, true, false)).collect(Collectors.toList());
    }

    @Override
    public void combineModuleDescriptors(Project project, ModulesGeneratorConfig config,
                                         List<File> externalLibraryModuleDescriptors) {
        Set<String> externalLibraries = new TreeSet<>();
        for (File inputFile : externalLibraryModuleDescriptors) {
            externalLibraries.addAll(getResourceRootsFromModuleXml(inputFile));
        }

        moduleDescriptorGenerator
                .generate(config.outputRoot.resolve(config.name), EXTERNAL_LIBRARY_DEPENDENCIES_MODULE_NAME,
                        project.getVersion().toString(), new ArrayList<>(externalLibraries),
                        getJBossJDKModuleDependencies(config));

        moduleDescriptorGenerator.generateAlias(config.outputRoot.resolve(config.name),
                EXTERNAL_LIBRARY_DEPENDENCIES_MODULE_NAME,
                project.getVersion().toString());
    }

    private synchronized List<ResolvedArtifact> getResolvedProjectRuntimeArtifacts(Project project,
                                                                                   ModulesGeneratorConfig config) {
        String key = config.name + project.getName();
        if (resolvedProjectRuntimeArtifacts.get(key) == null) {
            resolvedProjectRuntimeArtifacts.put(key,
                    ProjectUtils.getResolvedProjectRuntimeArtifacts(project,
                            getTargetConfigurations(config.name, RUNTIME_CLASSPATH)));
        }
        return resolvedProjectRuntimeArtifacts.get(key);
    }

    private List<String> getAdditionalSourceSets(Project project,
                                                 ModulesGeneratorConfig config) {
        List<DependencyWrapper> wrappers = new LinkedList<>();
        wrappers.addAll(getApiProjectDependencies(project, config));
        wrappers.addAll(getRuntimeProjectDependencies(project, config));
        List<String> resourcePaths = new LinkedList<>();
        for (DependencyWrapper wrapper : wrappers) {
            if (wrapper.getDependency() instanceof SelfResolvingDependency) {
                resourcePaths.addAll((((SelfResolvingDependency) wrapper.getDependency()).resolve().stream()
                        .map(File::getAbsolutePath)
                        .filter(path -> new File(path).exists())
                        .collect(Collectors.toList())));
            }
        }
        return resourcePaths;
    }

    private List<String> generateResourceRoots(Project project, File resourceFile,
                                               ModulesGeneratorConfig config) {

        List<String> embeddedArtifactNames = getEmbeddedArtifactsNames(project, config);

        List<String> resourceRoots = new ArrayList<>();

        getResolvedProjectRuntimeArtifacts(project, config).stream()
                .filter(artifact -> embeddedArtifactNames.contains(artifact.getName()))
                .forEach(artifact -> resourceRoots.add(LIB_PATH_PREFIX + artifact.getFile().getName()));

        if (config.shouldAssembleFromSource()) {
            resourceRoots.addAll(getAdditionalSourceSets(project, config));

            if (!config.name.equals("main")) {
                validateAndAddResourceRoot(resourceRoots,
                        project.getBuildDir().toPath().resolve("classes").resolve("java").resolve(config.name)
                                .toString());
                validateAndAddResourceRoot(resourceRoots,
                        project.getBuildDir().toPath().resolve("resources").resolve(config.name).toString());
            }
            validateAndAddResourceRoot(resourceRoots,
                    project.getBuildDir().toPath().resolve("classes").resolve("java").resolve("main")
                            .toString());
            validateAndAddResourceRoot(resourceRoots,
                    project.getBuildDir().toPath().resolve("resources").resolve("main").toString());
        } else if (resourceFile != null) {
            resourceRoots.add(LIB_PATH_PREFIX + resourceFile.getName());
        }

        return resourceRoots;
    }

    private void validateAndAddResourceRoot(List<String> resourceRoots,
                                            String resourceRootToValidateAndAdd) {
        if (new File(resourceRootToValidateAndAdd).exists()) {
            resourceRoots.add(resourceRootToValidateAndAdd);
        }
    }

    private List<String> getEmbeddedArtifactsNames(Project project,
                                                   ModulesGeneratorConfig config) {
        String key = config.name + project.getName();
        if (embeddedArtifactNames.get(key) == null) {
            TreeSet<String> artifacts = new TreeSet<>();
            artifacts.addAll(
                    getModuleNameForEmbeddedDependency(getApiProjectDependencies(project, config)));
            artifacts
                    .addAll(
                            getModuleNameForEmbeddedDependency(getRuntimeProjectDependencies(project, config)));
            embeddedArtifactNames
                    .put(key, Collections.unmodifiableList(new LinkedList<>(artifacts)));
        }
        return embeddedArtifactNames.get(key);
    }

    private List<String> getModuleNameForEmbeddedDependency(
            List<DependencyWrapper> projectDependencies) {
        return projectDependencies.stream().filter(DependencyWrapper::isEmbedded)
                .map(dependencyWrapper -> dependencyWrapper.getDependency().getName())
                .collect(Collectors.toList());
    }

    private List<String> getResourceRootsFromModuleXml(File moduleXml) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        List<String> resourceRoots = new LinkedList<>();
        try {
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(moduleXml);

            NodeList resourceRootNodes = document.getElementsByTagName("resource-root");
            for (int i = 0; i < resourceRootNodes.getLength(); i++) {
                Element resourceElement = (Element) resourceRootNodes.item(i);
                resourceRoots.add(resourceElement.getAttribute("path"));
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
        return resourceRoots;
    }

    private List<ModuleDependency> getModuleDependencies(List<String> allProjectNames,
                                                         List<DependencyWrapper> apiProjectDependencies,
                                                         List<DependencyWrapper> runtimeProjectDependencies,
                                                         List<DependencyWrapper> jbossProjectDependencies) {

        Set<ModuleDependency> moduleDependencies = new HashSet<>(
                getModuleDependencies(apiProjectDependencies, allProjectNames, true));

        moduleDependencies.addAll(
                getModuleDependencies(runtimeProjectDependencies, allProjectNames, false));

        moduleDependencies.addAll(
                getModuleDependencies(jbossProjectDependencies, allProjectNames, false));

        return new LinkedList<>(moduleDependencies);
    }

    private List<ModuleDependency> getModuleDependencies(List<DependencyWrapper> dependencyWrappers,
                                                         List<String> allProjectNames,
                                                         boolean export) {
        return dependencyWrappers.stream()
                .filter(dependencyWrapper -> allProjectNames
                        .contains(dependencyWrapper.getDependency().getName()))
                .map(dependencyWrapper -> {
                    Dependency dependency = dependencyWrapper.getDependency();
                    boolean optional = ProjectUtils.invokeGroovyCode(
                            "hasExtension", "optional", dependency);
                    return new ModuleDependency(dependency.getName(), export, optional);
                }).collect(Collectors.toList());
    }

    private synchronized List<Project> getAllProjects(Project project) {
        if (allProjects == null) {
            allProjects =
                    Collections.unmodifiableList(new LinkedList<>(project.getRootProject().getSubprojects()));
        }
        return allProjects;
    }
}
