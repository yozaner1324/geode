/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.geode.services.module.impl;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.geode.services.bootstrapping.BootstrappingService;
import org.apache.geode.services.module.ModuleDescriptor;
import org.apache.geode.services.module.ModuleService;
import org.apache.geode.services.result.ModuleServiceResult;

public class PrototypeTest {

  private static String rootPath =
      System.getProperty("user.dir") + "/geode-assembly/build/install/apache-geode/lib/";
  private final String gemFireVersion = "1.14.0-build.0";
  private ModuleService moduleService;
  private BootstrappingService bootstrappingService;

  private void setup() {
    moduleService = new JBossModuleServiceImpl();
    ModuleDescriptor moduleManagementDescriptor =
        new ModuleDescriptor.Builder("bootStrapping", gemFireVersion)
            .fromResourcePaths(rootPath + "geode-module-bootstrapping-" + gemFireVersion + ".jar")
            .build();

    ModuleServiceResult<Boolean> registerModule =
        moduleService.registerModule(moduleManagementDescriptor);
    if (registerModule.isSuccessful()) {
      ModuleServiceResult<Boolean> loadModule =
          moduleService.loadModule(moduleManagementDescriptor);
      if (loadModule.isSuccessful()) {
        ModuleServiceResult<Map<String, Set<BootstrappingService>>> serviceLoadResult =
            moduleService.loadService(BootstrappingService.class);
        if (serviceLoadResult.isSuccessful()) {
          for (Map.Entry<String, Set<BootstrappingService>> serviceEntrySet : serviceLoadResult
              .getMessage().entrySet()) {
            for (BootstrappingService service : serviceEntrySet.getValue()) {
              bootstrappingService = service;
              break;
            }
            if (bootstrappingService != null) {
              break;
            }
          }
        }
      } else {
        System.err.println(loadModule.getErrorMessage());
      }
    } else {
      System.err.println(registerModule.getErrorMessage());
    }
  }

  public static void main(String[] args) {
    PrototypeTest prototypeTest = new PrototypeTest();
    prototypeTest.setup();
    if (prototypeTest.bootstrappingService != null) {
      prototypeTest.bootstrappingService.init(prototypeTest.moduleService, new Properties());
    }

    System.out.println("rootPath = " + rootPath);
  }
}
