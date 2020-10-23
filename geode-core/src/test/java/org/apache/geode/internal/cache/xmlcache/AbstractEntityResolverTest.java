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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

import org.apache.geode.internal.deployment.jar.ClassPathLoader;
import org.apache.geode.internal.services.classloader.impl.DefaultClassLoaderServiceImpl;
import org.apache.geode.logging.internal.log4j.api.LogService;
import org.apache.geode.services.classloader.ClassLoaderService;
import org.apache.geode.services.result.ServiceResult;

/**
 * Unit test for {@link PivotalEntityResolver} and {@link DefaultEntityResolver2}.
 */
public abstract class AbstractEntityResolverTest {

  protected abstract EntityResolver getEntityResolver();

  protected abstract String getSystemId();

  /**
   * Assert that {@link PivotalEntityResolver} extends {@link DefaultEntityResolver2}.
   *
   * @since GemFire 8.1
   */
  @Test
  public void testInstanceOfDefaultEntityResolver2() {
    assertTrue(DefaultEntityResolver2.class.isAssignableFrom(getEntityResolver().getClass()));
  }

  /**
   * Find the {@link PivotalEntityResolver} in the {@link ClassPathLoader}. Verifies that the
   * META-INF/services file is correctly found and the the implementation class is loadable.
   *
   * @since GemFire 8.1
   */
  @Test
  public void testDiscovery() {
    boolean found = false;
    ClassLoaderService classLoaderService =
        new DefaultClassLoaderServiceImpl(LogService.getLogger());
    ServiceResult<List<EntityResolver2>> serviceLoadResult =
        classLoaderService.loadService(EntityResolver2.class);
    if (serviceLoadResult.isSuccessful()) {
      for (EntityResolver2 entityResolver : serviceLoadResult.getMessage()) {
        if (getEntityResolver().getClass().isAssignableFrom(entityResolver.getClass())) {
          found = true;
          break;
        }
      }
    }
    assertTrue("Resolver not found.", found);
  }

  /**
   * Resolve the cache.xml XSD using the {@link PivotalEntityResolver}. Verifies that the
   * META-INF/schemas files are correctly found.
   *
   * @since GemFire 8.1
   */
  @Test
  public void testResolveEntity() throws Exception {
    final InputSource inputSource = getEntityResolver().resolveEntity(null, getSystemId());
    assertNotNull(inputSource);
    assertEquals(getSystemId(), inputSource.getSystemId());
  }

  /**
   * Test {@link PivotalEntityResolver#resolveEntity(String, String)} with <code>null</code>
   * <code>systemId</code>. Asserts that returns to <code>null<code>.
   *
   * @since GemFire 8.1
   */
  @Test
  public void testResolveEntityNullSystemId() throws SAXException, Exception {
    final String systemId = null;
    final InputSource inputSource = getEntityResolver().resolveEntity(null, systemId);
    assertNull(inputSource);
  }

  /**
   * Test {@link PivotalEntityResolver#resolveEntity(String, String)} with
   * <code>"--not-a-valid-system-id--"</code> <code>systemId</code>, which is not in the Pivotal
   * namespace.. Asserts that returns to <code>null<code>.
   *
   * @since GemFire 8.1
   */
  @Test
  public void testResolveEntityUnkownSystemId() throws Exception {
    final String systemId = "--not-a-valid-system-id--";
    final InputSource inputSource = getEntityResolver().resolveEntity(null, systemId);
    assertNull(inputSource);
  }

  /**
   * Test {@link PivotalEntityResolver#resolveEntity(String, String)} with
   * <code>"http://schema.pivotal.io/this/should/be/not/found.xsd"</code> <code>systemId</code>,
   * which should not be found. Asserts that returns to <code>null<code>.
   *
   * @since GemFire 8.1
   */
  @Test
  public void testResolveEntityNotFoundSystemId() throws Exception {
    final String systemId = "http://schema.pivotal.io/this/should/be/not/found.xsd";
    final InputSource inputSource = getEntityResolver().resolveEntity(null, systemId);
    assertNull(inputSource);
  }

}
