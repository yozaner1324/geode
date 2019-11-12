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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.RegionShortcut;
import org.apache.geode.cache.configuration.CacheConfig;
import org.apache.geode.cache.configuration.DeclarableType;
import org.apache.geode.cache.configuration.EnumActionDestroyOverflow;
import org.apache.geode.cache.configuration.RegionAttributesType;
import org.apache.geode.cache.configuration.RegionConfig;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.distributed.internal.InternalConfigurationPersistenceService;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.security.SecurityService;
import org.apache.geode.management.configuration.ClassName;
import org.apache.geode.management.internal.cli.GfshParseResult;
import org.apache.geode.management.internal.cli.functions.CliFunctionResult;
import org.apache.geode.test.junit.rules.GfshParserRule;


public class AlterRegionCommandTest {

  @Rule
  public GfshParserRule parser = new GfshParserRule();

  private AlterRegionCommand command;
  private CacheConfig cacheConfig;
  private RegionConfig existingRegionConfig;

  @Before
  public void before() {
    command = Mockito.spy(AlterRegionCommand.class);
    InternalCache cache = Mockito.mock(InternalCache.class);
    command.setCache(cache);
    Mockito.when(cache.getSecurityService()).thenReturn(Mockito.mock(SecurityService.class));
    InternalConfigurationPersistenceService ccService =
        Mockito.mock(InternalConfigurationPersistenceService.class);
    Mockito.doReturn(ccService).when(command).getConfigurationPersistenceService();
    Set<DistributedMember> members =
        Stream.of(Mockito.mock(DistributedMember.class)).collect(Collectors.toSet());
    Mockito.doReturn(members).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());
    CliFunctionResult result =
        new CliFunctionResult("member", CliFunctionResult.StatusState.OK, "regionA altered");
    Mockito.doReturn(Collections.singletonList(result)).when(command).executeAndGetFunctionResult(ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any());

    cacheConfig = new CacheConfig();
    existingRegionConfig = new RegionConfig();
    existingRegionConfig.setName("/regionA");
    existingRegionConfig.setType(RegionShortcut.REPLICATE.name());
    cacheConfig.getRegions().add(existingRegionConfig);
    Mockito.when(ccService.getCacheConfig("cluster")).thenReturn(cacheConfig);
  }

  @Test
  public void alterWithCacheWriter() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --cache-writer=CommandWriter");
    RegionAttributesType deltaAttributes = deltaConfig.getRegionAttributes();
    Assertions.assertThat(deltaAttributes.getCacheLoader()).isNull();
    Assertions.assertThat(deltaAttributes.getCacheListeners()).isNotNull().isEmpty();
    Assertions.assertThat(deltaAttributes.getCacheWriter().getClassName()).isEqualTo("CommandWriter");

    RegionAttributesType existingAttributes = new RegionAttributesType();
    existingAttributes.setCacheLoader(new DeclarableType("CacheLoader"));
    existingAttributes.setCacheWriter(new DeclarableType("CacheWriter"));
    existingAttributes.getCacheListeners().add(new DeclarableType("CacheListener"));

    existingRegionConfig.setRegionAttributes(existingAttributes);
    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);

    // CacheListeners and CacheLoader are unchanged
    Assertions.assertThat(existingAttributes.getCacheListeners()).hasSize(1);
    Assertions.assertThat(existingAttributes.getCacheLoader().getClassName()).isEqualTo("CacheLoader");
    Assertions.assertThat(existingAttributes.getCacheListeners().get(0).getClassName())
        .isEqualTo("CacheListener");

    // CacheWriter is changed
    Assertions.assertThat(existingAttributes.getCacheWriter().getClassName()).isEqualTo("CommandWriter");
  }

  @Test
  public void alterWithNoCacheWriter() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --cache-writer=' '");
    RegionAttributesType deltaAttributes = deltaConfig.getRegionAttributes();
    Assertions.assertThat(deltaAttributes.getCacheWriter()).isEqualTo(DeclarableType.EMPTY);

    RegionAttributesType existingAttributes = new RegionAttributesType();
    existingAttributes.setCacheWriter(new DeclarableType("CacheWriter"));

    existingRegionConfig.setRegionAttributes(existingAttributes);
    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);

    // CacheWriter is changed
    Assertions.assertThat(existingAttributes.getCacheWriter()).isNull();
  }

  @Test
  public void alterWithInvalidCacheWriter() {
    String command = "alter region --name=/Person --cache-writer='1abc'";
    GfshParseResult result = parser.parse(command);
    assertThat(result).isNull();
  }

  @Test
  public void emptyCustomExpiryAndNoCloning() {
    String command = "alter region --name=/Person --entry-idle-time-custom-expiry=''";
    GfshParseResult result = parser.parse(command);
    ClassName paramValue = (ClassName) result.getParamValue("entry-idle-time-custom-expiry");
    Assertions.assertThat(paramValue).isEqualTo(ClassName.EMPTY);
    Assertions.assertThat(paramValue.getClassName()).isEqualTo("");

    // when enable-cloning is not specified, the value should be null
    Object enableCloning = result.getParamValue("enable-cloning");
    Assertions.assertThat(enableCloning).isNull();
  }

  @Test
  public void regionNameIsConverted() {
    String command = "alter region --name=Person";
    GfshParseResult result = parser.parse(command);
    assertThat(result.getParamValue("name")).isEqualTo("/Person");
  }

  @Test
  public void groupNotExist() {
    Mockito.doReturn(Collections.emptySet()).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());
    parser.executeAndAssertThat(command,
        "alter region --group=group0 --name=regionA --cache-loader=abc")
        .statusIsError()
        .hasInfoSection().hasLines().contains("No Members Found");
  }

  @Test
  public void regionNotExistOnGroup() {
    parser.executeAndAssertThat(command,
        "alter region --name=regionB --cache-loader=abc")
        .statusIsError()
        .hasInfoSection().hasLines().contains("/regionB does not exist in group cluster");

    parser.executeAndAssertThat(command,
        "alter region --group=group1 --name=regionA --cache-loader=abc")
        .statusIsError()
        .hasInfoSection().hasLines().contains("/regionA does not exist in group group1");
  }

  @Test
  public void ccServiceNotAvailable() {
    Mockito.doReturn(null).when(command).getConfigurationPersistenceService();
    parser.executeAndAssertThat(command,
        "alter region --name=regionB --cache-loader=abc")
        .statusIsSuccess()
        .hasInfoSection().hasOutput().contains(
            "Cluster configuration service is not running. Configuration change is not persisted");
  }

  @Test
  public void alterWithCloningEnabled() {
    RegionAttributesType regionAttributes =
        getDeltaRegionConfig("alter region --name=regionA --enable-cloning=false")
            .getRegionAttributes();
    Assertions.assertThat(regionAttributes.isCloningEnabled()).isFalse();
    Assertions.assertThat(regionAttributes.getAsyncEventQueueIds()).isNull();
    Assertions.assertThat(regionAttributes.getDataPolicy()).isNull();
    Assertions.assertThat(regionAttributes.getGatewaySenderIds()).isNull();
    Assertions.assertThat(regionAttributes.getCacheLoader()).isNull();
    Assertions.assertThat(regionAttributes.getCacheWriter()).isNull();
    Assertions.assertThat(regionAttributes.getCacheListeners()).isNotNull().isEmpty();
    Assertions.assertThat(regionAttributes.getEvictionAttributes()).isNull();
    Assertions.assertThat(regionAttributes.getEntryIdleTime()).isNull();
    Assertions.assertThat(regionAttributes.getEntryTimeToLive()).isNull();
    Assertions.assertThat(regionAttributes.getRegionIdleTime()).isNull();
    Assertions.assertThat(regionAttributes.getRegionTimeToLive()).isNull();
  }

  @Test
  public void alterWithEntryIdleTimeOut() {
    // check that the deltaConfig is created as expected
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --entry-idle-time-expiration=7");
    RegionAttributesType.ExpirationAttributesType entryIdleTime =
        deltaConfig.getRegionAttributes().getEntryIdleTime();
    Assertions.assertThat(entryIdleTime).isNotNull();
    Assertions.assertThat(entryIdleTime.getTimeout()).isEqualTo("7");
    Assertions.assertThat(entryIdleTime.getCustomExpiry()).isNull();
    Assertions.assertThat(entryIdleTime.getAction()).isNull();

    // check that the combined the configuration is created as expected
    RegionAttributesType existingAttributes = new RegionAttributesType();
    RegionAttributesType.ExpirationAttributesType expirationAttributesType =
        new RegionAttributesType.ExpirationAttributesType(10,
            ExpirationAction.DESTROY.toXmlString(), null, null);
    existingAttributes.setEntryIdleTime(expirationAttributesType);
    existingRegionConfig.setRegionAttributes(existingAttributes);

    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);
    RegionAttributesType.ExpirationAttributesType combinedExpirationAttributes =
        existingRegionConfig.getRegionAttributes().getEntryIdleTime();
    Assertions.assertThat(combinedExpirationAttributes.getTimeout()).isEqualTo("7");
    Assertions.assertThat(combinedExpirationAttributes.getAction()).isEqualTo("destroy");
    Assertions.assertThat(combinedExpirationAttributes.getCustomExpiry()).isNull();
  }

  @Test
  public void alterWithEntryIdleTimeOutAction() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig(
            "alter region --name=regionA --entry-idle-time-expiration-action=destroy");
    RegionAttributesType.ExpirationAttributesType entryIdleTime =
        deltaConfig.getRegionAttributes().getEntryIdleTime();
    Assertions.assertThat(entryIdleTime).isNotNull();
    Assertions.assertThat(entryIdleTime.getTimeout()).isNull();
    Assertions.assertThat(entryIdleTime.getCustomExpiry()).isNull();
    Assertions.assertThat(entryIdleTime.getAction()).isEqualTo("destroy");

    // check that the combined the configuration is created as expected
    RegionAttributesType existingAttributes = new RegionAttributesType();
    RegionAttributesType.ExpirationAttributesType expirationAttributesType =
        new RegionAttributesType.ExpirationAttributesType(10,
            ExpirationAction.INVALIDATE.toXmlString(), null,
            null);
    existingAttributes.setEntryIdleTime(expirationAttributesType);
    existingRegionConfig.setRegionAttributes(existingAttributes);

    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);
    RegionAttributesType.ExpirationAttributesType combinedExpirationAttributes =
        existingRegionConfig.getRegionAttributes().getEntryIdleTime();
    Assertions.assertThat(combinedExpirationAttributes.getTimeout()).isEqualTo("10");
    Assertions.assertThat(combinedExpirationAttributes.getAction()).isEqualTo("destroy");
    Assertions.assertThat(combinedExpirationAttributes.getCustomExpiry()).isNull();
  }

  @Test
  public void alterWithEntryIdleTimeOutCustomExpiry() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --entry-idle-time-custom-expiry=abc");
    RegionAttributesType.ExpirationAttributesType entryIdleTime =
        deltaConfig.getRegionAttributes().getEntryIdleTime();
    Assertions.assertThat(entryIdleTime).isNotNull();
    Assertions.assertThat(entryIdleTime.getTimeout()).isNull();
    Assertions.assertThat(entryIdleTime.getCustomExpiry().getClassName()).isEqualTo("abc");
    Assertions.assertThat(entryIdleTime.getAction()).isNull();

    // check that the combined the configuration is created as expected
    RegionAttributesType existingAttributes = new RegionAttributesType();
    RegionAttributesType.ExpirationAttributesType expirationAttributesType =
        new RegionAttributesType.ExpirationAttributesType(10,
            ExpirationAction.INVALIDATE.toXmlString(), null,
            null);
    existingAttributes.setEntryIdleTime(expirationAttributesType);
    existingRegionConfig.setRegionAttributes(existingAttributes);

    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);
    RegionAttributesType.ExpirationAttributesType combinedExpirationAttributes =
        existingRegionConfig.getRegionAttributes().getEntryIdleTime();
    Assertions.assertThat(combinedExpirationAttributes.getTimeout()).isEqualTo("10");
    Assertions.assertThat(combinedExpirationAttributes.getAction()).isEqualTo("invalidate");
    Assertions.assertThat(combinedExpirationAttributes.getCustomExpiry().getClassName()).isEqualTo("abc");
  }

  @Test
  public void alterWithEmptyEntryIdleTimeOutCustomExpiry() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --entry-idle-time-custom-expiry=''");
    RegionAttributesType.ExpirationAttributesType entryIdleTime =
        deltaConfig.getRegionAttributes().getEntryIdleTime();
    Assertions.assertThat(entryIdleTime).isNotNull();
    Assertions.assertThat(entryIdleTime.getTimeout()).isNull();
    Assertions.assertThat(entryIdleTime.getCustomExpiry()).isEqualTo(DeclarableType.EMPTY);
    Assertions.assertThat(entryIdleTime.getAction()).isNull();

    // check that the combined the configuration is created as expected
    RegionAttributesType existingAttributes = new RegionAttributesType();
    RegionAttributesType.ExpirationAttributesType expirationAttributesType =
        new RegionAttributesType.ExpirationAttributesType(10,
            ExpirationAction.INVALIDATE.toXmlString(), null,
            null);
    existingAttributes.setEntryIdleTime(expirationAttributesType);
    existingRegionConfig.setRegionAttributes(existingAttributes);

    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);
    RegionAttributesType.ExpirationAttributesType combinedExpirationAttributes =
        existingRegionConfig.getRegionAttributes().getEntryIdleTime();
    Assertions.assertThat(combinedExpirationAttributes.getTimeout()).isEqualTo("10");
    Assertions.assertThat(combinedExpirationAttributes.getAction()).isEqualTo("invalidate");
    Assertions.assertThat(combinedExpirationAttributes.getCustomExpiry()).isNull();
  }

  @Test
  public void alterWithCacheListener() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --cache-listener=abc,def");
    List<DeclarableType> cacheListeners = deltaConfig.getRegionAttributes().getCacheListeners();
    Assertions.assertThat(cacheListeners).hasSize(2);
    Assertions.assertThat(cacheListeners.get(0).getClassName()).isEqualTo("abc");
    Assertions.assertThat(cacheListeners.get(1).getClassName()).isEqualTo("def");

    // check that the combined the configuration is created as expected
    RegionAttributesType existingAttributes = new RegionAttributesType();
    existingAttributes.getCacheListeners().add(new DeclarableType("ghi"));
    existingRegionConfig.setRegionAttributes(existingAttributes);
    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);

    List<DeclarableType> updatedCacheListeners =
        existingRegionConfig.getRegionAttributes().getCacheListeners();
    Assertions.assertThat(updatedCacheListeners).hasSize(2);
    Assertions.assertThat(updatedCacheListeners.get(0).getClassName()).isEqualTo("abc");
    Assertions.assertThat(updatedCacheListeners.get(1).getClassName()).isEqualTo("def");

    Assertions.assertThat(existingRegionConfig.getRegionAttributes().getEntryIdleTime()).isNull();
  }

  @Test
  public void alterWithNoCacheListener() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --cache-listener=''");
    List<DeclarableType> cacheListeners = deltaConfig.getRegionAttributes().getCacheListeners();
    Assertions.assertThat(cacheListeners).hasSize(1);
    Assertions.assertThat(cacheListeners.get(0)).isEqualTo(DeclarableType.EMPTY);

    RegionAttributesType existingAttributes = new RegionAttributesType();
    existingAttributes.getCacheListeners().add(new DeclarableType("ghi"));
    existingRegionConfig.setRegionAttributes(existingAttributes);
    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);

    List<DeclarableType> updatedCacheListeners =
        existingRegionConfig.getRegionAttributes().getCacheListeners();
    Assertions.assertThat(updatedCacheListeners).hasSize(0);
  }

  @Test
  public void alterWithCacheLoader() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --cache-loader=abc");
    RegionAttributesType deltaAttributes = deltaConfig.getRegionAttributes();
    Assertions.assertThat(deltaAttributes.getCacheWriter()).isNull();
    Assertions.assertThat(deltaAttributes.getCacheLoader().getClassName()).isEqualTo("abc");
    Assertions.assertThat(deltaAttributes.getCacheListeners()).isNotNull().isEmpty();

    RegionAttributesType existingAttributes = new RegionAttributesType();
    existingAttributes.getCacheListeners().add(new DeclarableType("def"));
    existingAttributes.setCacheLoader(new DeclarableType("def"));
    existingAttributes.setCacheWriter(new DeclarableType("def"));

    existingRegionConfig.setRegionAttributes(existingAttributes);
    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);

    // after update, the cache listeners remains the same
    Assertions.assertThat(existingAttributes.getCacheListeners()).hasSize(1);
    Assertions.assertThat(existingAttributes.getCacheListeners().get(0).getClassName()).isEqualTo("def");

    // after update the cache writer remains the same
    Assertions.assertThat(existingAttributes.getCacheWriter().getClassName()).isEqualTo("def");

    // after update the cache loader is changed
    Assertions.assertThat(existingAttributes.getCacheLoader().getClassName()).isEqualTo("abc");
  }

  @Test
  public void alterWithNoCacheLoader() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --cache-loader=''");
    RegionAttributesType deltaAttributes = deltaConfig.getRegionAttributes();
    Assertions.assertThat(deltaAttributes.getCacheLoader()).isEqualTo(DeclarableType.EMPTY);

    RegionAttributesType existingAttributes = new RegionAttributesType();
    existingAttributes.setCacheLoader(new DeclarableType("def"));

    existingRegionConfig.setRegionAttributes(existingAttributes);
    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);

    // after the update the cache loader is null
    Assertions.assertThat(existingAttributes.getCacheLoader()).isNull();
  }

  @Test
  public void alterWithAsyncEventQueueIds() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --async-event-queue-id=abc,def");
    Assertions.assertThat(deltaConfig.getRegionAttributes().getAsyncEventQueueIds()).isEqualTo("abc,def");
    Assertions.assertThat(deltaConfig.getRegionAttributes().getGatewaySenderIds()).isNull();

    RegionAttributesType existingAttributes = new RegionAttributesType();
    existingRegionConfig.setRegionAttributes(existingAttributes);
    existingAttributes.setAsyncEventQueueIds("xyz");
    existingAttributes.setGatewaySenderIds("xyz");

    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);
    Assertions.assertThat(existingAttributes.getGatewaySenderIds()).isEqualTo("xyz");
    Assertions.assertThat(existingAttributes.getAsyncEventQueueIds()).isEqualTo("abc,def");
  }

  @Test
  public void alterWithNoAsyncEventQueueIds() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --async-event-queue-id=''");
    Assertions.assertThat(deltaConfig.getRegionAttributes().getAsyncEventQueueIds()).isEqualTo("");
    Assertions.assertThat(deltaConfig.getRegionAttributes().getGatewaySenderIds()).isNull();

    RegionAttributesType existingAttributes = new RegionAttributesType();
    existingRegionConfig.setRegionAttributes(existingAttributes);
    existingAttributes.setAsyncEventQueueIds("xyz");
    existingAttributes.setGatewaySenderIds("xyz");

    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);
    Assertions.assertThat(existingAttributes.getGatewaySenderIds()).isEqualTo("xyz");
    Assertions.assertThat(existingAttributes.getAsyncEventQueueIds()).isEqualTo("");
    Assertions.assertThat(existingAttributes.getAsyncEventQueueIdsAsSet()).isNotNull().isEmpty();
  }

  @Test
  public void alterWithEvictionMaxWithExistingLruHeapPercentage() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --eviction-max=20");

    // we are saving the eviction-max as a lruEntryCount's maximum value
    RegionAttributesType.EvictionAttributes.LruEntryCount lruEntryCount =
        deltaConfig.getRegionAttributes().getEvictionAttributes().getLruEntryCount();
    Assertions.assertThat(lruEntryCount.getMaximum()).isEqualTo("20");

    // when there is no eviction attributes at all
    RegionAttributesType existingAttributes = new RegionAttributesType();
    existingRegionConfig.setRegionAttributes(existingAttributes);
    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);
    Assertions.assertThat(existingAttributes.getEvictionAttributes()).isNull();

    // when there is lruHeapPercentage eviction
    RegionAttributesType.EvictionAttributes evictionAttributes =
        new RegionAttributesType.EvictionAttributes();
    evictionAttributes
        .setLruHeapPercentage(new RegionAttributesType.EvictionAttributes.LruHeapPercentage());
    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);
    Assertions.assertThat(evictionAttributes.getLruEntryCount()).isNull();
    Assertions.assertThat(evictionAttributes.getLruMemorySize()).isNull();
  }

  @Test
  public void alterWithEvictionMaxWithExistingLruEntryCount() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --eviction-max=20");

    // we are saving the eviction-max as a lruEntryCount's maximum value
    RegionAttributesType.EvictionAttributes.LruEntryCount lruEntryCount =
        deltaConfig.getRegionAttributes().getEvictionAttributes().getLruEntryCount();
    Assertions.assertThat(lruEntryCount.getMaximum()).isEqualTo("20");

    // when there is no eviction attributes at all
    RegionAttributesType existingAttributes = new RegionAttributesType();
    existingRegionConfig.setRegionAttributes(existingAttributes);
    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);
    Assertions.assertThat(existingAttributes.getEvictionAttributes()).isNull();

    // when there is lruHeapPercentage eviction
    RegionAttributesType.EvictionAttributes evictionAttributes =
        new RegionAttributesType.EvictionAttributes();
    existingAttributes.setEvictionAttributes(evictionAttributes);
    RegionAttributesType.EvictionAttributes.LruEntryCount existingEntryCount =
        new RegionAttributesType.EvictionAttributes.LruEntryCount();
    existingEntryCount.setMaximum("100");
    existingEntryCount.setAction(EnumActionDestroyOverflow.LOCAL_DESTROY);
    evictionAttributes.setLruEntryCount(existingEntryCount);

    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);
    Assertions.assertThat(evictionAttributes.getLruEntryCount().getMaximum()).isEqualTo("20");
    Assertions.assertThat(evictionAttributes.getLruEntryCount().getAction())
        .isEqualTo(EnumActionDestroyOverflow.LOCAL_DESTROY);
  }

  @Test
  public void alterWithEvictionMaxWithExistingLruMemory() {
    RegionConfig deltaConfig =
        getDeltaRegionConfig("alter region --name=regionA --eviction-max=20");

    // we are saving the eviction-max as a lruEntryCount's maximum value
    RegionAttributesType.EvictionAttributes.LruEntryCount lruEntryCount =
        deltaConfig.getRegionAttributes().getEvictionAttributes().getLruEntryCount();
    Assertions.assertThat(lruEntryCount.getMaximum()).isEqualTo("20");

    // when there is no eviction attributes at all
    RegionAttributesType existingAttributes = new RegionAttributesType();
    existingRegionConfig.setRegionAttributes(existingAttributes);
    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);
    Assertions.assertThat(existingAttributes.getEvictionAttributes()).isNull();

    // when there is lruHeapPercentage eviction
    RegionAttributesType.EvictionAttributes evictionAttributes =
        new RegionAttributesType.EvictionAttributes();
    existingAttributes.setEvictionAttributes(evictionAttributes);
    RegionAttributesType.EvictionAttributes.LruMemorySize existingMemorySize =
        new RegionAttributesType.EvictionAttributes.LruMemorySize();
    existingMemorySize.setMaximum("100");
    existingMemorySize.setAction(EnumActionDestroyOverflow.LOCAL_DESTROY);
    evictionAttributes.setLruMemorySize(existingMemorySize);

    command.updateConfigForGroup("cluster", cacheConfig, deltaConfig);
    Assertions.assertThat(evictionAttributes.getLruMemorySize().getMaximum()).isEqualTo("20");
    Assertions.assertThat(evictionAttributes.getLruMemorySize().getAction())
        .isEqualTo(EnumActionDestroyOverflow.LOCAL_DESTROY);
  }

  private RegionConfig getDeltaRegionConfig(String commandString) {
    return (RegionConfig) parser.executeAndAssertThat(command, commandString).getResultModel()
        .getConfigObject();
  }
}
