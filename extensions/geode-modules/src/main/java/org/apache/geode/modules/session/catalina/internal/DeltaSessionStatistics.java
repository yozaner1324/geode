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
package org.apache.geode.modules.session.catalina.internal;

import org.apache.geode.stats.common.statistics.StatisticDescriptor;
import org.apache.geode.stats.common.statistics.Statistics;
import org.apache.geode.stats.common.statistics.StatisticsFactory;
import org.apache.geode.stats.common.statistics.StatisticsType;

public class DeltaSessionStatistics {

  public static final String typeName = "SessionStatistics";

  private StatisticsType type;

  private static final String SESSIONS_CREATED = "sessionsCreated";
  private static final String SESSIONS_INVALIDATED = "sessionsInvalidated";
  private static final String SESSIONS_EXPIRED = "sessionsExpired";

  private int sessionsCreatedId;
  private int sessionsInvalidatedId;
  private int sessionsExpiredId;

  private void initializeStats(StatisticsFactory factory) {
    // Initialize type
    type = factory.createType(typeName, typeName,
        new StatisticDescriptor[] {
            factory.createIntCounter(SESSIONS_CREATED, "The number of sessions created",
                "operations"),
            factory.createIntCounter(SESSIONS_INVALIDATED,
                "The number of sessions invalidated by invoking invalidate", "operations"),
            factory.createIntCounter(SESSIONS_EXPIRED,
                "The number of sessions invalidated by timeout",
                "operations"),});

    // Initialize id fields
    sessionsCreatedId = type.nameToId(SESSIONS_CREATED);
    sessionsInvalidatedId = type.nameToId(SESSIONS_INVALIDATED);
    sessionsExpiredId = type.nameToId(SESSIONS_EXPIRED);
  }

  private final Statistics stats;

  public DeltaSessionStatistics(StatisticsFactory factory, String applicationName) {
    initializeStats(factory);
    this.stats = factory.createAtomicStatistics(type, typeName + "_" + applicationName);
  }

  public void close() {
    this.stats.close();
  }

  public int getSessionsCreated() {
    return this.stats.getInt(sessionsCreatedId);
  }

  public void incSessionsCreated() {
    this.stats.incInt(sessionsCreatedId, 1);
  }

  public int getSessionsInvalidated() {
    return this.stats.getInt(sessionsInvalidatedId);
  }

  public void incSessionsInvalidated() {
    this.stats.incInt(sessionsInvalidatedId, 1);
  }

  public int getSessionsExpired() {
    return this.stats.getInt(sessionsExpiredId);
  }

  public void incSessionsExpired() {
    this.stats.incInt(sessionsExpiredId, 1);
  }
}
