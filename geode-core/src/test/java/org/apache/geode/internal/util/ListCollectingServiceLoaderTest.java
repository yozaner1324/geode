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
package org.apache.geode.internal.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class ListCollectingServiceLoaderTest {
  @Test
  public void loadServices_returnsLoadedServices() {
    ListCollectingServiceLoader<TestService> collectingServiceLoader =
        new ListCollectingServiceLoader<>();

    Collection<TestService> actualServices =
        collectingServiceLoader.loadServices(TestService.class);

    assertThat(actualServices.size()).isEqualTo(3);

    List<String> serviceClassNames =
        actualServices.stream().map(service -> service.getClass().getName())
            .collect(Collectors.toList());

    assertThat(serviceClassNames).containsExactlyInAnyOrder(
        "org.apache.geode.internal.util.ListCollectingServiceLoaderTest$TestService1",
        "org.apache.geode.internal.util.ListCollectingServiceLoaderTest$TestService2",
        "org.apache.geode.internal.util.ListCollectingServiceLoaderTest$TestService4");
  }

  public interface TestService {

  }

  public static class TestService1 implements TestService {

  }
  public static class TestService2 implements TestService {

  }
  public class TestService3 implements TestService {

  }
  public static class TestService4 implements TestService {

  }
}
