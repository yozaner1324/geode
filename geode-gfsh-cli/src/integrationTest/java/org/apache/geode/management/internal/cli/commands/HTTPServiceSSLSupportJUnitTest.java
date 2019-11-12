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
package org.apache.geode.management.internal.cli.commands;

import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_ENABLED;
import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_KEYSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_KEYSTORE_PASSWORD;
import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_KEYSTORE_TYPE;
import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_PROTOCOLS;
import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_REQUIRE_AUTHENTICATION;
import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_TRUSTSTORE;
import static org.apache.geode.distributed.ConfigurationProperties.CLUSTER_SSL_TRUSTSTORE_PASSWORD;
import static org.apache.geode.distributed.ConfigurationProperties.MCAST_PORT;
import static org.apache.geode.test.util.ResourceUtils.createTempFileFromResource;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.apache.geode.distributed.internal.DistributionConfig;
import org.apache.geode.distributed.internal.DistributionConfigImpl;
import org.apache.geode.internal.net.SocketCreatorFactory;

/**
 * @since GemFire 8.1
 */
public class HTTPServiceSSLSupportJUnitTest {

  private File jks;


  @Before
  public void setUp() throws Exception {
    jks = findTrustedJKS();
  }

  @After
  public void tearDown() throws Exception {
    System.clearProperty(DistributionConfig.GEMFIRE_PREFIX + "javax.net.ssl.keyStore");
    System.clearProperty(DistributionConfig.GEMFIRE_PREFIX + "javax.net.ssl.keyStorePassword");
    System.clearProperty(DistributionConfig.GEMFIRE_PREFIX + "javax.net.ssl.trustStore");
    System.clearProperty(DistributionConfig.GEMFIRE_PREFIX + "javax.net.ssl.trustStorePassword");
    System.clearProperty("gemfireSecurityPropertyFile");
    SocketCreatorFactory.close();
  }

  private static File findTrustedJKS() {
    return new File(
        ResourceUtils.createTempFileFromResource(HTTPServiceSSLSupportJUnitTest.class,
            "/ssl/trusted.keystore").getAbsolutePath());
  }

  public static String makePath(String[] strings) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < strings.length; i++) {
      sb.append(strings[i]);
      sb.append(File.separator);
    }
    return sb.toString();
  }

  // @Ignore("disabled for unknown reason")
  @Test
  public void testSSLWithClusterSSL() throws Exception {

    Properties localProps = new Properties();
    localProps.setProperty(MCAST_PORT, "0");
    localProps.setProperty(CLUSTER_SSL_ENABLED, "true");
    localProps.setProperty(CLUSTER_SSL_KEYSTORE, jks.getCanonicalPath());
    localProps.setProperty(CLUSTER_SSL_KEYSTORE_PASSWORD, "password");
    localProps.setProperty(CLUSTER_SSL_KEYSTORE_TYPE, "JKS");
    localProps.setProperty(CLUSTER_SSL_PROTOCOLS, "SSL");
    localProps.setProperty(CLUSTER_SSL_REQUIRE_AUTHENTICATION, "true");
    localProps.setProperty(CLUSTER_SSL_TRUSTSTORE, jks.getCanonicalPath());
    localProps.setProperty(CLUSTER_SSL_TRUSTSTORE_PASSWORD, "password");

    DistributionConfigImpl config = new DistributionConfigImpl(localProps);

    Assert.assertEquals(config.getHttpServiceSSLEnabled(), true);
    Assert.assertEquals(config.getHttpServiceSSLKeyStore(), jks.getCanonicalPath());
    Assert.assertEquals(config.getHttpServiceSSLKeyStorePassword(), "password");
    Assert.assertEquals(config.getHttpServiceSSLKeyStoreType(), "JKS");
    Assert.assertEquals(config.getHttpServiceSSLProtocols(), "SSL");
    Assert.assertEquals(config.getHttpServiceSSLRequireAuthentication(), true);
    Assert.assertEquals(config.getHttpServiceSSLTrustStore(), jks.getCanonicalPath());
    Assert.assertEquals(config.getHttpServiceSSLTrustStorePassword(), "password");
  }

  @Test
  public void testSSLWithDeprecatedClusterSSL_HTTPService() throws Exception {

    Properties localProps = new Properties();
    localProps.setProperty(MCAST_PORT, "0");
    localProps.setProperty(CLUSTER_SSL_ENABLED, "true");
    System.setProperty(DistributionConfig.GEMFIRE_PREFIX + "javax.net.ssl.keyStore",
        jks.getCanonicalPath());
    System.setProperty(DistributionConfig.GEMFIRE_PREFIX + "javax.net.ssl.keyStorePassword",
        "password");

    localProps.setProperty(CLUSTER_SSL_PROTOCOLS, "SSL");
    localProps.setProperty(CLUSTER_SSL_REQUIRE_AUTHENTICATION, "true");
    System.setProperty(DistributionConfig.GEMFIRE_PREFIX + "javax.net.ssl.trustStore",
        jks.getCanonicalPath());
    System.setProperty(DistributionConfig.GEMFIRE_PREFIX + "javax.net.ssl.trustStorePassword",
        "password");

    DistributionConfigImpl config = new DistributionConfigImpl(localProps);

    Assert.assertEquals(true, config.getHttpServiceSSLEnabled());
    Assert.assertEquals("SSL", config.getHttpServiceSSLProtocols());
    Assert.assertEquals(true, config.getHttpServiceSSLRequireAuthentication());

    Assert.assertEquals(jks.getCanonicalPath(),
        config.getHttpServiceSSLProperties().get("javax.net.ssl.keyStore"));
    Assert.assertEquals("password",
        config.getHttpServiceSSLProperties().get("javax.net.ssl.keyStorePassword"));
    // assertIndexDetailsEquals(system.getConfig().getHttpServiceSSLKeyStoreType(),"JKS");
    Assert.assertEquals(jks.getCanonicalPath(),
        config.getHttpServiceSSLProperties().get("javax.net.ssl.trustStore"));
    Assert.assertEquals("password",
        config.getHttpServiceSSLProperties().get("javax.net.ssl.trustStorePassword"));

  }

  @Test
  public void testSSLWithDeprecatedClusterSSL_HTTPService_WithSSL_Properties() throws Exception {

    Properties localProps = new Properties();
    localProps.setProperty(MCAST_PORT, "0");
    localProps.setProperty(CLUSTER_SSL_ENABLED, "true");

    localProps.setProperty(CLUSTER_SSL_PROTOCOLS, "SSL");
    localProps.setProperty(CLUSTER_SSL_REQUIRE_AUTHENTICATION, "true");

    Properties sslProps = new Properties();
    sslProps.setProperty("javax.net.ssl.keyStore", jks.getCanonicalPath());
    sslProps.setProperty("javax.net.ssl.keyStorePassword", "password");
    sslProps.setProperty("javax.net.ssl.trustStore", jks.getCanonicalPath());
    sslProps.setProperty("javax.net.ssl.trustStorePassword", "password");

    localProps.putAll(sslProps);

    DistributionConfigImpl config = new DistributionConfigImpl(localProps);

    Assert.assertEquals(config.getHttpServiceSSLEnabled(), true);
    Assert.assertEquals(config.getHttpServiceSSLProtocols(), "SSL");
    Assert.assertEquals(config.getHttpServiceSSLRequireAuthentication(), true);

    Assert.assertEquals(jks.getCanonicalPath(),
        config.getHttpServiceSSLProperties().get("javax.net.ssl.keyStore"));
    Assert.assertEquals("password",
        config.getHttpServiceSSLProperties().get("javax.net.ssl.keyStorePassword"));
    Assert.assertEquals(jks.getCanonicalPath(),
        config.getHttpServiceSSLProperties().get("javax.net.ssl.trustStore"));
    Assert.assertEquals("password",
        config.getHttpServiceSSLProperties().get("javax.net.ssl.trustStorePassword"));

  }


}
