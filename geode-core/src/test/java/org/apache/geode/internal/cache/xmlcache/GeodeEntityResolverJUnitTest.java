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
package org.apache.geode.internal.cache.xmlcache;

import org.junit.Before;
import org.xml.sax.EntityResolver;

import org.apache.geode.services.module.ModuleService;


/**
 * Unit test for {@link GeodeEntityResolver} and {@link DefaultEntityResolver2}.
 */
public class GeodeEntityResolverJUnitTest extends AbstractEntityResolverTest {

  private GeodeEntityResolver2 entityResolver;

  private final String systemId = "http://geode.apache.org/schema/cache/cache-1.0.xsd";

  @Before
  public void setup() {
    entityResolver = new GeodeEntityResolver();
    entityResolver.init(ModuleService.DEFAULT);
  }

  @Override
  protected EntityResolver getEntityResolver() {
    return entityResolver;
  }

  @Override
  protected String getSystemId() {
    return systemId;
  }

}
