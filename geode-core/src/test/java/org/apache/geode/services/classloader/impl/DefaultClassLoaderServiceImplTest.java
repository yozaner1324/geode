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
package org.apache.geode.services.classloader.impl;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.junit.BeforeClass;
import org.junit.Test;

import org.apache.geode.internal.services.classloader.impl.DefaultClassLoaderServiceImpl;
import org.apache.geode.services.classloader.ClassLoaderService;
import org.apache.geode.services.result.ServiceResult;

public class DefaultClassLoaderServiceImplTest {

  private static final String resourceFile = "TestResourceFile.txt";
  private static ClassLoaderService classLoaderService;

  @BeforeClass
  public static void setup() {
    classLoaderService = new DefaultClassLoaderServiceImpl(LogManager.getLogger());
  }

  @Test
  public void findResourceAsStreamExists() {
    ServiceResult<InputStream> resourceAsStream =
        classLoaderService.getResourceAsStream(resourceFile);

    assertThat(resourceAsStream.isSuccessful()).isTrue();
  }

  @Test
  public void findResourceAsStreamNotExists() {
    ServiceResult<InputStream> resourceAsStream =
        classLoaderService.getResourceAsStream("invalidReSource.file");

    assertThat(resourceAsStream.isSuccessful()).isFalse();
  }

  @Test
  public void loadClassExists() {
    ServiceResult<Class<?>> loadedClassResult =
        classLoaderService.forName("org.apache.geode.services.classloader.impl.TestServiceImpl");

    assertThat(loadedClassResult.isSuccessful()).isTrue();
  }

  @Test
  public void loadClassExistsMissingPackage() {
    ServiceResult<Class<?>> loadedClassResult = classLoaderService.forName("TestService");

    assertThat(loadedClassResult.isFailure()).isTrue();
  }

  @Test
  public void loadClassNotExists() {
    ServiceResult<Class<?>> loadedClassResult =
        classLoaderService.forName("org.apache.geode.services.moduleFakeClassThatIsNotReal");

    assertThat(loadedClassResult.isSuccessful()).isFalse();
  }

  @Test
  public void loadService() {
    ServiceResult<Set<TestService>> loadServiceResult =
        classLoaderService.loadService(TestService.class);
    assertThat(loadServiceResult.isSuccessful()).isTrue();
    assertThat(loadServiceResult.getMessage().size()).isEqualTo(1);
    assertThat(loadServiceResult.getMessage().iterator().next())
        .isInstanceOf(TestServiceImpl.class);
  }

  @Test
  public void loadInvalidService() {
    ServiceResult<Set<TestServiceImpl>> loadServiceResult =
        classLoaderService.loadService(TestServiceImpl.class);
    assertThat(loadServiceResult.isSuccessful()).isTrue();
  }
}
