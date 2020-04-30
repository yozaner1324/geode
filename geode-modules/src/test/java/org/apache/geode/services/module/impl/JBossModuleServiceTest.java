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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Before;
import org.junit.Test;

import org.apache.geode.services.module.ModuleDescriptor;

public class JBossModuleServiceTest {

  private static final String MODULE1_PATH =
      System.getProperty("user.dir") + "/../libs/module1.jar";
  private static final String MODULE2_PATH =
      System.getProperty("user.dir") + "/../libs/module2.jar";

  private JBossModuleService moduleService;

  @Before
  public void createModuleService() {
    moduleService = new JBossModuleService();
  }

  @Test
  public void modulesNotAccessibleFromSystemClassloader() {
    assertThatThrownBy(() -> {
      this.getClass().getClassLoader().loadClass("org.apache.geode.Module1");
    }).isInstanceOf(ClassNotFoundException.class);

    assertThatThrownBy(() -> {
      this.getClass().getClassLoader().loadClass("org.apache.geode.Module2");
    }).isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  public void loadSingleModuleNoDependencies() throws ClassNotFoundException {
    ModuleDescriptor module1Descriptor = new ModuleDescriptor.Builder("module1", "1.0")
        .fromSources(MODULE1_PATH)
        .build();
    assertThat(moduleService.loadModule(module1Descriptor)).isTrue();

    moduleService.getModule(module1Descriptor.getVersionedName()).getClassLoader()
        .loadClass("org.apache.geode.Module1");
  }

  @Test
  public void loadSingleModuleFromMultipleJarsNoDependencies() throws ClassNotFoundException {
    ModuleDescriptor moduleDescriptor = new ModuleDescriptor.Builder("multiJarModule", "1.0")
        .fromSources(MODULE1_PATH, MODULE2_PATH)
        .build();
    assertThat(moduleService.loadModule(moduleDescriptor)).isTrue();

    moduleService.getModule(moduleDescriptor.getVersionedName()).getClassLoader()
        .loadClass("org.apache.geode.Module1");
    moduleService.getModule(moduleDescriptor.getVersionedName()).getClassLoader()
        .loadClass("org.apache.geode.Module2");
  }

  @Test
  public void loadMultipleModulesNoDependencies() throws ClassNotFoundException {
    ModuleDescriptor module1Descriptor = new ModuleDescriptor.Builder("module1", "1.0")
        .fromSources(MODULE1_PATH)
        .build();
    ModuleDescriptor module2Descriptor = new ModuleDescriptor.Builder("module2", "1.0")
        .fromSources(MODULE2_PATH)
        .build();
    assertThat(moduleService.loadModule(module1Descriptor)).isTrue();
    assertThat(moduleService.loadModule(module2Descriptor)).isTrue();

    moduleService.getModule(module1Descriptor.getVersionedName()).getClassLoader()
        .loadClass("org.apache.geode.Module1");
    moduleService.getModule(module2Descriptor.getVersionedName()).getClassLoader()
        .loadClass("org.apache.geode.Module2");

    assertThatThrownBy(() -> {
      moduleService.getModule(module1Descriptor.getVersionedName()).getClassLoader()
          .loadClass("org.apache.geode.Module2");
    }).isInstanceOf(ClassNotFoundException.class);

    assertThatThrownBy(() -> {
      moduleService.getModule(module2Descriptor.getVersionedName()).getClassLoader()
          .loadClass("org.apache.geode.Module1");
    }).isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  public void loadModulesWithDependencies() throws ClassNotFoundException {
    ModuleDescriptor module1Descriptor = new ModuleDescriptor.Builder("module1", "1.0")
        .fromSources(MODULE1_PATH)
        .build();
    ModuleDescriptor module2Descriptor = new ModuleDescriptor.Builder("module2", "1.0")
        .fromSources(MODULE2_PATH)
        .dependsOnModules(module1Descriptor.getVersionedName())
        .build();
    assertThat(moduleService.loadModule(module1Descriptor)).isTrue();
    assertThat(moduleService.loadModule(module2Descriptor)).isTrue();

    moduleService.getModule(module1Descriptor.getVersionedName()).getClassLoader()
        .loadClass("org.apache.geode.Module1");
    moduleService.getModule(module2Descriptor.getVersionedName()).getClassLoader()
        .loadClass("org.apache.geode.Module2");
    moduleService.getModule(module2Descriptor.getVersionedName()).getClassLoader()
        .loadClass("org.apache.geode.Module1");

    assertThatThrownBy(() -> {
      moduleService.getModule(module1Descriptor.getVersionedName()).getClassLoader()
          .loadClass("org.apache.geode.Module2");
    }).isInstanceOf(ClassNotFoundException.class);
  }

  @Test
  public void loadModuleMultipleTimes() throws ClassNotFoundException {
    ModuleDescriptor module1Descriptor = new ModuleDescriptor.Builder("module1", "1.0")
        .fromSources(MODULE1_PATH)
        .build();
    assertThat(moduleService.loadModule(module1Descriptor)).isTrue();
    assertThat(moduleService.loadModule(module1Descriptor)).isFalse();

    moduleService.getModule(module1Descriptor.getVersionedName()).getClassLoader()
        .loadClass("org.apache.geode.Module1");
  }

  @Test
  public void loadModuleFromInvalidSource() {
    ModuleDescriptor module1Descriptor = new ModuleDescriptor.Builder("module1", "1.0")
        .fromSources("/there/is/nothing/here.jar")
        .build();
    assertThat(moduleService.loadModule(module1Descriptor)).isFalse();
    assertThat(moduleService.getModule(module1Descriptor.getVersionedName())).isNull();
  }

  @Test
  public void loadModuleWithInvalidDependencies() {
    ModuleDescriptor module1Descriptor = new ModuleDescriptor.Builder("module1", "1.0")
        .fromSources(MODULE1_PATH)
        .dependsOnModules("this_is_invalid")
        .build();
    assertThat(moduleService.loadModule(module1Descriptor)).isFalse();
    assertThat(moduleService.getModule(module1Descriptor.getVersionedName())).isNull();
  }

  @Test
  public void getModuleNoModulesLoaded() {
    assertThat(moduleService.getModule("module1")).isNull();
  }

  @Test
  public void loadModuleWithModuleXML() {

  }
}
