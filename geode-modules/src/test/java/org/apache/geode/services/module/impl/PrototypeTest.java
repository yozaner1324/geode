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

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.geode.internal.GemFireVersion;
import org.apache.geode.services.module.ModuleDescriptor;
import org.apache.geode.services.module.ModuleService;
import org.apache.geode.services.result.ModuleServiceResult;

public class PrototypeTest {

  private static String rootPath =
      "/Users/patrickjohnson/Documents/GitHub/geode/geode-assembly/build/install/apache-geode/lib/";
  private final String gemFireVersion = GemFireVersion.getGemFireVersion();
  private ModuleService moduleService;

  @Before
  public void setup() {
    moduleService = new JBossModuleServiceImpl(LogManager.getLogger());
  }

  @After
  public void teardown() {
    moduleService = null;
  }

  @Test
  public void bootstrapGeode() {
    String[] projects =
        new String[] {"geode-common", "geode-common-services", "geode-connectors", "geode-core",
            "geode-cq", "geode-gfsh",
            "geode-http-service", "geode-log4j", "geode-logging", "geode-lucene",
            "geode-management", "geode-membership", "geode-memcached",
            "geode-old-client-support", "geode-protobuf",
            "geode-protobuf-messages", "geode-rebalancer", "geode-redis",
            "geode-serialization", "geode-tcp-server", "geode-unsafe", "geode-wan",
            "geode-modules"};
    Arrays.stream(projects).forEach(project -> {
      ModuleServiceResult<Boolean> booleanModuleServiceResult =
          moduleService.registerModule(new ModuleDescriptor.Builder(project, gemFireVersion)
              .fromResourcePaths(rootPath + project + "-" + gemFireVersion + ".jar").build());
      if (!booleanModuleServiceResult.isSuccessful()) {
        System.out.println(booleanModuleServiceResult.getErrorMessage());
      }
    });
    Arrays.stream(projects).forEach(project -> {
      ModuleServiceResult<Boolean> booleanModuleServiceResult =
          moduleService.loadModule(new ModuleDescriptor.Builder(project, gemFireVersion).build());
      if (!booleanModuleServiceResult.isSuccessful()) {
        System.out.println(booleanModuleServiceResult.getErrorMessage());
      }
    });

    System.out.println("projects = " + projects);
  }
}
