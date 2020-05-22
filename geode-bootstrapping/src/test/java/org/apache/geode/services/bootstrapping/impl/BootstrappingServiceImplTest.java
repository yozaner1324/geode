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

package org.apache.geode.services.bootstrapping.impl;

import org.junit.Before;

import org.apache.geode.services.bootstrapping.BootstrappingService;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BootstrappingServiceImplTest {
  private BootstrappingService bootstrappingService;

  @Before
  public void setup() {
    bootstrappingService = new BootstrappingServiceImpl();
  }

  @Test
  public void noPropertiesSet() throws Exception {
    bootstrappingService.init(new Properties());
    bootstrappingService.shutdown();
  }

  @Test
  public void initWithInvalidProperties() {
    Properties properties = new Properties();
    properties.setProperty("inValiDD_prOpeRTy", "value");
    assertThatThrownBy(() -> {
      bootstrappingService.init(properties);
    });
  }
}
