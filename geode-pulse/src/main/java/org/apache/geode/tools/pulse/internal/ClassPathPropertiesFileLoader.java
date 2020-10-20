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

package org.apache.geode.tools.pulse.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.geode.internal.services.registry.ServiceRegistryInstance;
import org.apache.geode.services.classloader.ClassLoaderService;
import org.apache.geode.services.result.ServiceResult;

@Component
public class ClassPathPropertiesFileLoader implements PropertiesFileLoader {
  private static final Logger logger = LogManager.getLogger();

  @Override
  public Properties loadProperties(String propertyFile, ResourceBundle resourceBundle) {
    final Properties properties = new Properties();

    try {
      ServiceResult<List<InputStream>> result =
          ClassLoaderService.getClassLoaderService().getResourceAsStream(propertyFile);
      if (result.isSuccessful()) {
        logger.info(propertyFile + " " + resourceBundle.getString("LOG_MSG_FILE_FOUND"));
        properties.load(result.getMessage().get(0));
      } else {
        throw new IOException("Could not load property file: " + propertyFile);
      }
    } catch (IOException e) {
      logger.error(resourceBundle.getString("LOG_MSG_EXCEPTION_LOADING_PROPERTIES_FILE"), e);
    }

    return properties;
  }
}
