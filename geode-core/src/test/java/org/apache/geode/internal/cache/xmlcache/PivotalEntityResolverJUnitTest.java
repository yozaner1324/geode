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

import org.apache.geode.logging.internal.log4j.api.LogService;
import org.apache.geode.services.classloader.impl.DefaultClassLoaderServiceImpl;


/**
 * Unit test for {@link PivotalEntityResolver} and {@link DefaultEntityResolver2}.
 *
 * @since GemFire 8.1
 */
public class PivotalEntityResolverJUnitTest extends AbstractEntityResolverTest {

  private EntityResolver entityResolver;

  private final String systemId = "http://schema.pivotal.io/gemfire/cache/cache-8.1.xsd";

  @Before
  public void setup() throws Exception {
    PivotalEntityResolver pivotalEntityResolver = new PivotalEntityResolver();
    pivotalEntityResolver.init(new DefaultClassLoaderServiceImpl(LogService.getLogger()));
    this.entityResolver = pivotalEntityResolver;
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
