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

package org.apache.geode.internal.cache.client.protocol;

import java.util.LinkedList;
import java.util.List;

import org.apache.geode.internal.cache.client.protocol.exception.ServiceLoadingFailureException;
import org.apache.geode.services.classloader.ClassLoaderService;

public class ClientProtocolServiceLoader {
  private final List<ClientProtocolService> clientProtocolServices;

  public ClientProtocolServiceLoader(ClassLoaderService classLoaderService) {
    clientProtocolServices = initializeProtocolServices(classLoaderService);
  }

  private List<ClientProtocolService> initializeProtocolServices(
      ClassLoaderService classLoaderService) {
    List<ClientProtocolService> resultList = new LinkedList<>();
    classLoaderService.loadService(ClientProtocolService.class)
        .ifSuccessful(clientProtocolServices -> clientProtocolServices.forEach((service -> {
          service.init(classLoaderService);
          resultList.add(service);
        })));
    return resultList;
  }

  public ClientProtocolService lookupService() {
    if (clientProtocolServices.isEmpty()) {
      throw new ServiceLoadingFailureException(
          "There is no ClientProtocolService implementation found in JVM");
    }

    if (clientProtocolServices.size() > 1) {
      throw new ServiceLoadingFailureException(
          "There is more than one ClientProtocolService implementation found in JVM; aborting");
    }
    return clientProtocolServices.get(0);
  }
}
