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
package org.apache.geode.internal.memcached;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import org.apache.geode.distributed.internal.DistributedSystemService;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.internal.InternalDataSerializer;
import org.apache.geode.internal.services.classloader.impl.ClassLoaderServiceInstance;
import org.apache.geode.services.result.ServiceResult;

public class MemcachedDistributedSystemService implements DistributedSystemService {
  @Override
  public void init(InternalDistributedSystem internalDistributedSystem) {

  }

  @Override
  public Class getInterface() {
    return getClass();
  }

  @Override
  public Collection<String> getSerializationAcceptlist() throws IOException {
    ServiceResult<URL> serviceResult =
        ClassLoaderServiceInstance.getInstance().getResource(getClass(),
            "sanctioned-geode-memcached-serializables.txt");
    URL sanctionedSerializables = null;
    if (serviceResult.isSuccessful()) {
      sanctionedSerializables = serviceResult.getMessage();
    }
    return InternalDataSerializer.loadClassNames(sanctionedSerializables);
  }
}
