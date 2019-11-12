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
package org.apache.geode.management.internal.cli.functions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.geode.cache.AttributesMutator;
import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.CustomExpiry;
import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.EvictionAttributesMutator;
import org.apache.geode.cache.ExpirationAction;
import org.apache.geode.cache.ExpirationAttributes;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.configuration.DeclarableType;
import org.apache.geode.cache.configuration.RegionAttributesType;
import org.apache.geode.cache.configuration.RegionConfig;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.util.CacheListenerAdapter;
import org.apache.geode.cache.util.CacheWriterAdapter;
import org.apache.geode.internal.cache.AbstractRegion;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.InternalCacheForClientAccess;

public class RegionAlterFunctionTest {
  private RegionAlterFunction function;
  private RegionConfig config;
  private RegionAttributesType regionAttributes;
  private InternalCacheForClientAccess cache;
  private FunctionContext<RegionConfig> context;
  private AttributesMutator mutator;
  private EvictionAttributesMutator evictionMutator;
  private AbstractRegion region;

  public static class MyCustomExpiry implements CustomExpiry, Declarable {
    @Override
    public ExpirationAttributes getExpiry(Region.Entry entry) {
      return null;
    }
  }

  public static class MyCacheListener extends CacheListenerAdapter {
  }

  public static class MyCacheWriter extends CacheWriterAdapter {
  }

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() throws Exception {
    function = Mockito.spy(RegionAlterFunction.class);
    config = new RegionConfig();
    regionAttributes = new RegionAttributesType();
    config.setRegionAttributes(regionAttributes);

    InternalCache internalCache = Mockito.mock(InternalCache.class);
    cache = Mockito.mock(InternalCacheForClientAccess.class);
    mutator = Mockito.mock(AttributesMutator.class);
    evictionMutator = Mockito.mock(EvictionAttributesMutator.class);
    Mockito.when(mutator.getEvictionAttributesMutator()).thenReturn(evictionMutator);
    region = Mockito.mock(AbstractRegion.class);

    context = Mockito.mock(FunctionContext.class);
    Mockito.when(context.getCache()).thenReturn(internalCache);
    Mockito.when(internalCache.getCacheForProcessingClientRequests()).thenReturn(cache);
    Mockito.when(context.getArguments()).thenReturn(config);
    Mockito.when(context.getMemberName()).thenReturn("member");
    Mockito.when(cache.getRegion(ArgumentMatchers.any())).thenReturn(region);
    Mockito.when(region.getAttributesMutator()).thenReturn(mutator);
  }

  @Test
  public void executeFunctionHappyPathReturnsStatusOK() {
    Mockito.doNothing().when(function).alterRegion(ArgumentMatchers.any(), ArgumentMatchers.any());
    config.setName("regionA");
    CliFunctionResult result = function.executeFunction(context);
    assertThat(result.getMemberIdOrName()).isEqualTo("member");
    assertThat(result.getStatus()).isEqualTo("OK");
    assertThat(result.getStatusMessage()).isEqualTo("Region regionA altered");
  }

  @Test
  public void alterRegionWithNullRegionThrowsIllegalArgumentException() {
    Mockito.when(cache.getRegion(ArgumentMatchers.anyString())).thenReturn(null);
    config.setName("regionA");
    Assertions.assertThatThrownBy(() -> function.alterRegion(cache, config))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Region does not exist: regionA");
  }

  @Test
  public void updateWithEmptyRegionAttributes() {
    // the regionAttributes starts with no values inside
    function.alterRegion(cache, config);
    Mockito.verifyZeroInteractions(mutator);
  }

  @Test
  public void updateWithCloningEnabled() {
    regionAttributes.setCloningEnabled(false);
    function.alterRegion(cache, config);
    Mockito.verify(mutator).setCloningEnabled(false);
  }

  @Test
  public void updateWithEvictionAttributes() {
    RegionAttributesType.EvictionAttributes evictionAttributes =
        new RegionAttributesType.EvictionAttributes();
    RegionAttributesType.EvictionAttributes.LruEntryCount lruEntryCount =
        new RegionAttributesType.EvictionAttributes.LruEntryCount();
    lruEntryCount.setMaximum("10");
    evictionAttributes.setLruEntryCount(lruEntryCount);
    regionAttributes.setEvictionAttributes(evictionAttributes);

    function.alterRegion(cache, config);
    Mockito.verify(mutator).getEvictionAttributesMutator();
    Mockito.verify(evictionMutator).setMaximum(10);
  }

  @Test
  public void updateWithEntryIdleTime_timeoutAndAction() {
    RegionAttributesType.ExpirationAttributesType expiration =
        new RegionAttributesType.ExpirationAttributesType();
    regionAttributes.setEntryIdleTime(expiration);
    expiration.setTimeout("10");
    expiration.setAction("invalidate");

    ExpirationAttributes existing = new ExpirationAttributes();
    Mockito.when(region.getEntryIdleTimeout()).thenReturn(existing);

    function.alterRegion(cache, config);

    ArgumentCaptor<ExpirationAttributes> updatedCaptor =
        ArgumentCaptor.forClass(ExpirationAttributes.class);
    Mockito.verify(mutator).setEntryIdleTimeout(updatedCaptor.capture());
    Assertions.assertThat(updatedCaptor.getValue().getTimeout()).isEqualTo(10);
    Assertions.assertThat(updatedCaptor.getValue().getAction()).isEqualTo(ExpirationAction.INVALIDATE);
    Mockito.verify(mutator, Mockito.times(0)).setCustomEntryIdleTimeout(ArgumentMatchers.any());
  }

  @Test
  public void updateWithEntryIdleTime_TimeoutOnly() {
    RegionAttributesType.ExpirationAttributesType expiration =
        new RegionAttributesType.ExpirationAttributesType();
    regionAttributes.setEntryIdleTime(expiration);
    expiration.setTimeout("10");

    ExpirationAttributes existing = new ExpirationAttributes(20, ExpirationAction.DESTROY);
    Mockito.when(region.getEntryIdleTimeout()).thenReturn(existing);

    function.alterRegion(cache, config);

    ArgumentCaptor<ExpirationAttributes> updatedCaptor =
        ArgumentCaptor.forClass(ExpirationAttributes.class);
    Mockito.verify(mutator).setEntryIdleTimeout(updatedCaptor.capture());
    Assertions.assertThat(updatedCaptor.getValue().getTimeout()).isEqualTo(10);
    Assertions.assertThat(updatedCaptor.getValue().getAction()).isEqualTo(ExpirationAction.DESTROY);
    Mockito.verify(mutator, Mockito.times(0)).setCustomEntryIdleTimeout(ArgumentMatchers.any());
  }

  @Test
  public void updateWithCustomExpiry() {
    RegionAttributesType.ExpirationAttributesType expiration =
        new RegionAttributesType.ExpirationAttributesType();
    regionAttributes.setEntryIdleTime(expiration);
    DeclarableType mockExpiry = Mockito.mock(DeclarableType.class);
    Mockito.when(mockExpiry.getClassName()).thenReturn(MyCustomExpiry.class.getName());
    expiration.setCustomExpiry(mockExpiry);

    function.alterRegion(cache, config);

    Mockito.verify(mutator, Mockito.times(0)).setEntryIdleTimeout(ArgumentMatchers.any());
    Mockito.verify(mutator).setCustomEntryIdleTimeout(ArgumentMatchers.notNull());
  }

  @Test
  public void deleteCustomExpiry() {
    RegionAttributesType.ExpirationAttributesType expiration =
        new RegionAttributesType.ExpirationAttributesType();
    regionAttributes.setEntryIdleTime(expiration);
    expiration.setCustomExpiry(DeclarableType.EMPTY);

    function.alterRegion(cache, config);

    Mockito.verify(mutator, Mockito.times(0)).setEntryIdleTimeout(ArgumentMatchers.any());
    Mockito.verify(mutator).setCustomEntryIdleTimeout(null);
  }

  @Test
  public void updateWithGatewaySenders() {
    regionAttributes.setGatewaySenderIds("2,3");
    Mockito.when(region.getGatewaySenderIds()).thenReturn(new HashSet<>(Arrays.asList("1", "2")));

    function.alterRegion(cache, config);

    Mockito.verify(mutator).removeGatewaySenderId("1");
    Mockito.verify(mutator, Mockito.times(0)).removeGatewaySenderId("2");
    Mockito.verify(mutator).addGatewaySenderId("3");

    // asyncEventQueue is left intact
    Mockito.verify(mutator, Mockito.times(0)).addAsyncEventQueueId(ArgumentMatchers.any());
    Mockito.verify(mutator, Mockito.times(0)).removeAsyncEventQueueId(ArgumentMatchers.any());
  }

  @Test
  public void updateWithEmptyGatewaySenders() {
    regionAttributes.setGatewaySenderIds("");
    Mockito.when(region.getGatewaySenderIds()).thenReturn(new HashSet<>(Arrays.asList("1", "2")));

    function.alterRegion(cache, config);

    Mockito.verify(mutator).removeGatewaySenderId("1");
    Mockito.verify(mutator).removeGatewaySenderId("2");
  }

  @Test
  public void updateWithAsynchronousEventQueues() {
    regionAttributes.setAsyncEventQueueIds("queue2,queue3");
    Mockito.when(region.getAsyncEventQueueIds())
        .thenReturn(new HashSet<>(Arrays.asList("queue1", "queue2")));
    function.alterRegion(cache, config);

    Mockito.verify(mutator).removeAsyncEventQueueId("queue1");
    Mockito.verify(mutator, Mockito.times(0)).removeAsyncEventQueueId("queue2");
    Mockito.verify(mutator).addAsyncEventQueueId("queue3");

    // gatewaySender is left intact
    Mockito.verify(mutator, Mockito.times(0)).addGatewaySenderId(ArgumentMatchers.any());
    Mockito.verify(mutator, Mockito.times(0)).removeGatewaySenderId(ArgumentMatchers.any());
  }

  @Test
  public void updateWithEmptyAsynchronousEventQueues() {
    regionAttributes.setAsyncEventQueueIds("");
    Mockito.when(region.getAsyncEventQueueIds())
        .thenReturn(new HashSet<>(Arrays.asList("queue1", "queue2")));
    function.alterRegion(cache, config);

    Mockito.verify(mutator).removeAsyncEventQueueId("queue1");
    Mockito.verify(mutator).removeAsyncEventQueueId("queue2");
  }

  @Test
  public void updateWithCacheListeners() {
    // suppose region has one cacheListener, and we want to replace the oldOne one with the new one
    CacheListener oldOne = Mockito.mock(CacheListener.class);
    Mockito.when(region.getCacheListeners()).thenReturn(new CacheListener[] {oldOne});

    DeclarableType newCacheListenerType = Mockito.mock(DeclarableType.class);
    Mockito.when(newCacheListenerType.getClassName()).thenReturn(MyCacheListener.class.getName());
    regionAttributes.getCacheListeners().add(newCacheListenerType);

    ArgumentCaptor<CacheListener> argument = ArgumentCaptor.forClass(CacheListener.class);

    function.alterRegion(cache, config);
    Mockito.verify(mutator).removeCacheListener(oldOne);
    Mockito.verify(mutator).addCacheListener(argument.capture());
    Assertions.assertThat(argument.getValue()).isInstanceOf(MyCacheListener.class);
  }

  @Test
  public void updateWithEmptyCacheListeners() {
    // suppose region has on listener, and we want to delete that one
    CacheListener oldOne = Mockito.mock(CacheListener.class);
    Mockito.when(region.getCacheListeners()).thenReturn(new CacheListener[] {oldOne});
    regionAttributes.getCacheListeners().add(DeclarableType.EMPTY);

    function.alterRegion(cache, config);
    Mockito.verify(mutator).removeCacheListener(oldOne);
    Mockito.verify(mutator, Mockito.times(0)).addCacheListener(ArgumentMatchers.any());
  }

  @Test
  public void updateWithCacheWriter() {
    DeclarableType newCacheWriterDeclarable = Mockito.mock(DeclarableType.class);
    Mockito.when(newCacheWriterDeclarable.getClassName()).thenReturn(MyCacheWriter.class.getName());
    regionAttributes.setCacheWriter(newCacheWriterDeclarable);

    function.alterRegion(cache, config);
    Mockito.verify(mutator).setCacheWriter(ArgumentMatchers.notNull());
    Mockito.verify(mutator, Mockito.times(0)).setCacheLoader(ArgumentMatchers.any());
  }

  @Test
  public void updateWithNoCacheWriter() {
    regionAttributes.setCacheWriter(DeclarableType.EMPTY);

    function.alterRegion(cache, config);
    Mockito.verify(mutator).setCacheWriter(null);
    Mockito.verify(mutator, Mockito.times(0)).setCacheLoader(ArgumentMatchers.any());
  }
}
