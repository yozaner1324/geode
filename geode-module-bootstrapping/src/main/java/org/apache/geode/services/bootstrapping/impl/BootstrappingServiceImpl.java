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
package org.apache.geode.services.bootstrapping.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.geode.services.bootstrapping.BootstrappingService;
import org.apache.geode.services.management.ManagementService;
import org.apache.geode.services.module.ModuleDescriptor;
import org.apache.geode.services.module.ModuleService;
import org.apache.geode.services.result.ModuleServiceResult;

public class BootstrappingServiceImpl implements BootstrappingService {

  private static String rootPath =
      System.getProperty("user.dir") + "/geode-assembly/build/install/apache-geode/lib/";
  private final String gemFireVersion = "1.14.0-build.0";

  private final String[] projects = new String[] {"geode-common", "geode-connectors", "geode-core",
      "geode-cq", "geode-gfsh",
      "geode-http-service", "geode-log4j", "geode-logging", "geode-lucene",
      "geode-management", "geode-membership", "geode-memcached",
      "geode-old-client-support", "geode-protobuf",
      "geode-protobuf-messages", "geode-rebalancer", "geode-redis",
      "geode-serialization", "geode-tcp-server", "geode-unsafe", "geode-wan",
      "geode-module-management"};

  private ManagementService managementService;

  private ModuleService moduleService;

  @Override
  public void init(ModuleService moduleService, Properties properties) {
    this.moduleService = moduleService;

    registerModules(moduleService);

    ModuleServiceResult<Map<String, Set<ManagementService>>> mapModuleServiceResult =
        moduleService.loadService(ManagementService.class);

    if (mapModuleServiceResult.isSuccessful()) {
      Map<String, Set<ManagementService>> message = mapModuleServiceResult.getMessage();

      for (Set<ManagementService> managementServices : message.values()) {

        for (ManagementService managementService : managementServices) {
          ModuleServiceResult<Boolean> cacheResult = managementService.createCache(properties);

          if (!cacheResult.isSuccessful()) {
            System.err.println(cacheResult.getErrorMessage());
          }
        }
      }
    }
  }

  private void registerModules(ModuleService moduleService) {

    List<String> registeredModules = new ArrayList<>();

    Arrays.stream(projects).forEach(project -> {
      ModuleDescriptor moduleDescriptor = new ModuleDescriptor.Builder(project, gemFireVersion)
          .fromResourcePaths(rootPath + project + "-" + gemFireVersion + ".jar")
          .dependsOnModules("geode").build();
      ModuleServiceResult<Boolean> registerModule =
          moduleService.registerModule(moduleDescriptor);
      if (!registerModule.isSuccessful()) {
        System.err.println(registerModule.getErrorMessage());
      } else {
        registeredModules.add(moduleDescriptor.getName());
      }
    });

    ModuleDescriptor geodeDescriptor =
        new ModuleDescriptor.Builder("geode")
            .dependsOnModules(registeredModules)
            .build();

    ModuleServiceResult<Boolean> registerModule = moduleService.registerModule(geodeDescriptor);
    if (registerModule.isSuccessful()) {
      ModuleServiceResult<Boolean> loadModuleResult =
          moduleService.loadModule(geodeDescriptor);
      if (!loadModuleResult.isSuccessful()) {
        System.err.println(loadModuleResult.getErrorMessage());
      }
    } else {
      System.err.println(registerModule.getErrorMessage());
    }

    Arrays.stream(projects).forEach(project -> {
      ModuleServiceResult<Boolean> loadModuleResult =
          moduleService.loadModule(new ModuleDescriptor.Builder(project, gemFireVersion).build());
      if (!loadModuleResult.isSuccessful()) {
        System.out.println(loadModuleResult.getErrorMessage());
      }
    });
  }

  @Override
  public void shutdown() {

  }
}