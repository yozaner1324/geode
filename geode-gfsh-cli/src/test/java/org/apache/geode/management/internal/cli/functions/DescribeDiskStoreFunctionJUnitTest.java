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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Before;
import org.junit.Test;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.DataPolicy;
import org.apache.geode.cache.DiskStore;
import org.apache.geode.cache.EvictionAction;
import org.apache.geode.cache.EvictionAttributes;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.asyncqueue.AsyncEventQueue;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.ResultSender;
import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.cache.server.ClientSubscriptionConfig;
import org.apache.geode.cache.wan.GatewaySender;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.lang.ObjectUtils;
import org.apache.geode.internal.util.CollectionUtils;
import org.apache.geode.management.internal.cli.domain.DiskStoreDetails;
import org.apache.geode.management.internal.exceptions.EntityNotFoundException;

/**
 * The DescribeDiskStoreFunctionJUnitTest test suite class tests the contract and functionality of
 * the DescribeDiskStoreFunction class.
 *
 * @see org.apache.geode.cache.DiskStore
 * @see org.apache.geode.management.internal.cli.domain.DiskStoreDetails
 * @see org.apache.geode.management.internal.cli.functions.DescribeDiskStoreFunction
 * @see org.junit.Test
 * @since GemFire 7.0
 */
public class DescribeDiskStoreFunctionJUnitTest {
  private InternalCache mockCache;

  @Before
  public void setup() {
    mockCache = Mockito.mock(InternalCache.class, "Cache");
  }

  private void assertAsyncEventQueueDetails(
      final Set<DiskStoreDetails.AsyncEventQueueDetails> expectedAsyncEventQueueDetailsSet,
      final DiskStoreDetails diskStoreDetails) {
    int actualCount = 0;

    for (final DiskStoreDetails.AsyncEventQueueDetails actualAsyncEventQueueDetails : diskStoreDetails
        .iterateAsyncEventQueues()) {
      final DiskStoreDetails.AsyncEventQueueDetails expectedAsyncEventQueueDetails = CollectionUtils
          .findBy(expectedAsyncEventQueueDetailsSet, asyncEventQueueDetails -> ObjectUtils
              .equals(asyncEventQueueDetails.getId(), actualAsyncEventQueueDetails.getId()));

      assertThat(expectedAsyncEventQueueDetails).isNotNull();
      actualCount++;
    }

    Assertions.assertThat(actualCount).isEqualTo(expectedAsyncEventQueueDetailsSet.size());
  }

  private void assertCacheServerDetails(
      final Set<DiskStoreDetails.CacheServerDetails> expectedCacheServerDetailsSet,
      final DiskStoreDetails diskStoreDetails) {
    int actualCount = 0;

    for (final DiskStoreDetails.CacheServerDetails actualCacheServerDetails : diskStoreDetails
        .iterateCacheServers()) {
      final DiskStoreDetails.CacheServerDetails expectedCacheServerDetails =
          CollectionUtils.findBy(expectedCacheServerDetailsSet,
              cacheServerDetails -> ObjectUtils.equals(cacheServerDetails.getBindAddress(),
                  actualCacheServerDetails.getBindAddress())
                  && ObjectUtils.equals(cacheServerDetails.getPort(),
                      actualCacheServerDetails.getPort()));

      assertThat(expectedCacheServerDetails).isNotNull();
      assertThat(actualCacheServerDetails.getHostName())
          .isEqualTo(expectedCacheServerDetails.getHostName());
      actualCount++;
    }

    Assertions.assertThat(actualCount).isEqualTo(expectedCacheServerDetailsSet.size());
  }

  private void assertGatewayDetails(
      final Set<DiskStoreDetails.GatewayDetails> expectedGatewayDetailsSet,
      final DiskStoreDetails diskStoreDetails) {
    int actualCount = 0;

    for (final DiskStoreDetails.GatewayDetails actualGatewayDetails : diskStoreDetails
        .iterateGateways()) {
      DiskStoreDetails.GatewayDetails expectedGatewayDetails =
          CollectionUtils.findBy(expectedGatewayDetailsSet, gatewayDetails -> ObjectUtils
              .equals(gatewayDetails.getId(), actualGatewayDetails.getId()));

      assertThat(expectedGatewayDetails).isNotNull();
      assertThat(actualGatewayDetails.isPersistent())
          .isEqualTo(expectedGatewayDetails.isPersistent());
      actualCount++;
    }

    Assertions.assertThat(actualCount).isEqualTo(expectedGatewayDetailsSet.size());
  }

  private void assertRegionDetails(
      final Set<DiskStoreDetails.RegionDetails> expectedRegionDetailsSet,
      final DiskStoreDetails diskStoreDetails) {
    int actualCount = 0;

    for (final DiskStoreDetails.RegionDetails actualRegionDetails : diskStoreDetails
        .iterateRegions()) {
      final DiskStoreDetails.RegionDetails expectedRegionDetails =
          CollectionUtils.findBy(expectedRegionDetailsSet, regionDetails -> ObjectUtils
              .equals(regionDetails.getFullPath(), actualRegionDetails.getFullPath()));

      assertThat(expectedRegionDetails).isNotNull();
      assertThat(actualRegionDetails.getName()).isEqualTo(expectedRegionDetails.getName());
      assertThat(actualRegionDetails.isPersistent())
          .isEqualTo(expectedRegionDetails.isPersistent());
      assertThat(actualRegionDetails.isOverflowToDisk())
          .isEqualTo(expectedRegionDetails.isOverflowToDisk());
      actualCount++;
    }

    Assertions.assertThat(actualCount).isEqualTo(expectedRegionDetailsSet.size());
  }

  private DiskStoreDetails.AsyncEventQueueDetails createAsyncEventQueueDetails(final String id) {
    return new DiskStoreDetails.AsyncEventQueueDetails(id);
  }

  private DiskStoreDetails.CacheServerDetails createCacheServerDetails(final String bindAddress,
      final int port, final String hostname) {
    final DiskStoreDetails.CacheServerDetails cacheServerDetails =
        new DiskStoreDetails.CacheServerDetails(bindAddress, port);
    cacheServerDetails.setHostName(hostname);

    return cacheServerDetails;
  }

  private File[] createFileArray(final String... locations) {
    assert locations != null : "The locations argument cannot be null!";

    final File[] directories = new File[locations.length];
    int index = 0;

    for (final String location : locations) {
      directories[index++] = new File(location);
    }

    return directories;
  }

  private DiskStoreDetails.GatewayDetails createGatewayDetails(final String id,
      final boolean persistent) {
    DiskStoreDetails.GatewayDetails gatewayDetails = new DiskStoreDetails.GatewayDetails(id);
    gatewayDetails.setPersistent(persistent);
    return gatewayDetails;
  }

  private int[] createIntArray(final int... array) {
    assert array != null : "The array of int values cannot be null!";
    return array;
  }

  private DiskStore createMockDiskStore(final UUID diskStoreId, final String name,
      final boolean allowForceCompaction, final boolean autoCompact, final int compactionThreshold,
      final long maxOplogSize, final int queueSize, final long timeInterval,
      final int writeBufferSize, final File[] diskDirs, final int[] diskDirSizes,
      final float warningPercentage, final float criticalPercentage) {
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, name);
    Mockito.when(mockDiskStore.getAllowForceCompaction()).thenReturn(allowForceCompaction);
    Mockito.when(mockDiskStore.getAutoCompact()).thenReturn(autoCompact);
    Mockito.when(mockDiskStore.getCompactionThreshold()).thenReturn(compactionThreshold);
    Mockito.when(mockDiskStore.getDiskStoreUUID()).thenReturn(diskStoreId);
    Mockito.when(mockDiskStore.getMaxOplogSize()).thenReturn(maxOplogSize);
    Mockito.when(mockDiskStore.getName()).thenReturn(name);
    Mockito.when(mockDiskStore.getQueueSize()).thenReturn(queueSize);
    Mockito.when(mockDiskStore.getTimeInterval()).thenReturn(timeInterval);
    Mockito.when(mockDiskStore.getWriteBufferSize()).thenReturn(writeBufferSize);
    Mockito.when(mockDiskStore.getDiskDirs()).thenReturn(diskDirs);
    Mockito.when(mockDiskStore.getDiskDirSizes()).thenReturn(diskDirSizes);
    Mockito.when(mockDiskStore.getDiskUsageWarningPercentage()).thenReturn(warningPercentage);
    Mockito.when(mockDiskStore.getDiskUsageCriticalPercentage()).thenReturn(criticalPercentage);

    return mockDiskStore;
  }

  private DiskStoreDetails.RegionDetails createRegionDetails(final String fullPath,
      final String name, final boolean persistent, final boolean overflow) {
    final DiskStoreDetails.RegionDetails regionDetails =
        new DiskStoreDetails.RegionDetails(fullPath, name);
    regionDetails.setPersistent(persistent);
    regionDetails.setOverflowToDisk(overflow);

    return regionDetails;
  }

  @SuppressWarnings("unchecked")
  private Set<DiskStoreDetails.RegionDetails> setupRegionsForTestExecute(
      final InternalCache mockCache, final String diskStoreName) {
    final Region mockUserRegion = Mockito.mock(Region.class, "/UserRegion");
    final Region mockGuestRegion = Mockito.mock(Region.class, "/GuestRegion");
    final Region mockSessionRegion = Mockito.mock(Region.class, "/UserRegion/SessionRegion");
    final RegionAttributes mockUserRegionAttributes =
        Mockito.mock(RegionAttributes.class, "UserRegionAttributes");
    final RegionAttributes mockSessionRegionAttributes =
        Mockito.mock(RegionAttributes.class, "SessionRegionAttributes");
    final RegionAttributes mockGuestRegionAttributes =
        Mockito.mock(RegionAttributes.class, "GuestRegionAttributes");
    final EvictionAttributes mockUserEvictionAttributes =
        Mockito.mock(EvictionAttributes.class, "UserEvictionAttributes");
    final EvictionAttributes mockSessionEvictionAttributes =
        Mockito.mock(EvictionAttributes.class, "SessionEvictionAttributes");
    final EvictionAttributes mockGuestEvictionAttributes =
        Mockito.mock(EvictionAttributes.class, "GuestEvictionAttributes");

    Mockito.when(mockCache.rootRegions())
        .thenReturn(CollectionUtils.asSet(mockUserRegion, mockGuestRegion));
    Mockito.when(mockUserRegion.getAttributes()).thenReturn(mockUserRegionAttributes);
    Mockito.when(mockUserRegion.getFullPath()).thenReturn("/UserRegion");
    Mockito.when(mockUserRegion.getName()).thenReturn("UserRegion");
    Mockito.when(mockUserRegion.subregions(false)).thenReturn(CollectionUtils.asSet(mockSessionRegion));
    Mockito.when(mockUserRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.PERSISTENT_PARTITION);
    Mockito.when(mockUserRegionAttributes.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockUserRegionAttributes.getEvictionAttributes()).thenReturn(mockUserEvictionAttributes);
    Mockito.when(mockUserEvictionAttributes.getAction()).thenReturn(EvictionAction.LOCAL_DESTROY);
    Mockito.when(mockSessionRegion.getAttributes()).thenReturn(mockSessionRegionAttributes);
    Mockito.when(mockSessionRegion.getFullPath()).thenReturn("/UserRegion/SessionRegion");
    Mockito.when(mockSessionRegion.getName()).thenReturn("SessionRegion");
    Mockito.when(mockSessionRegion.subregions(false)).thenReturn(Collections.emptySet());
    Mockito.when(mockSessionRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.REPLICATE);
    Mockito.when(mockSessionRegionAttributes.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockSessionRegionAttributes.getEvictionAttributes())
        .thenReturn(mockSessionEvictionAttributes);
    Mockito.when(mockSessionEvictionAttributes.getAction()).thenReturn(EvictionAction.OVERFLOW_TO_DISK);
    Mockito.when(mockGuestRegion.getAttributes()).thenReturn(mockGuestRegionAttributes);
    Mockito.when(mockGuestRegion.subregions(false)).thenReturn(Collections.emptySet());
    Mockito.when(mockGuestRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.REPLICATE);
    Mockito.when(mockGuestRegionAttributes.getDiskStoreName())
        .thenReturn(DiskStoreDetails.DEFAULT_DISK_STORE_NAME);
    Mockito.when(mockGuestRegionAttributes.getEvictionAttributes()).thenReturn(mockGuestEvictionAttributes);
    Mockito.when(mockGuestEvictionAttributes.getAction()).thenReturn(EvictionAction.OVERFLOW_TO_DISK);

    return CollectionUtils.asSet(createRegionDetails("/UserRegion", "UserRegion", true, false),
        createRegionDetails("/UserRegion/SessionRegion", "SessionRegion", false, true));
  }

  private Set<DiskStoreDetails.GatewayDetails> setupGatewaysForTestExecute(
      final InternalCache mockCache, final String diskStoreName) {
    final GatewaySender mockGatewaySender = Mockito.mock(GatewaySender.class, "GatewaySender");
    Mockito.when(mockCache.getGatewaySenders()).thenReturn(CollectionUtils.asSet(mockGatewaySender));
    Mockito.when(mockGatewaySender.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockGatewaySender.getId()).thenReturn("0123456789");
    Mockito.when(mockGatewaySender.isPersistenceEnabled()).thenReturn(true);

    return CollectionUtils.asSet(createGatewayDetails("0123456789", true));
  }

  private Set<DiskStoreDetails.CacheServerDetails> setupCacheServersForTestExecute(
      final InternalCache mockCache, final String diskStoreName) {
    final CacheServer mockCacheServer1 = Mockito.mock(CacheServer.class, "CacheServer1");
    final CacheServer mockCacheServer2 = Mockito.mock(CacheServer.class, "CacheServer2");
    final CacheServer mockCacheServer3 = Mockito.mock(CacheServer.class, "CacheServer3");
    final ClientSubscriptionConfig cacheServer1ClientSubscriptionConfig =
        Mockito.mock(ClientSubscriptionConfig.class, "cacheServer1ClientSubscriptionConfig");
    final ClientSubscriptionConfig cacheServer3ClientSubscriptionConfig =
        Mockito.mock(ClientSubscriptionConfig.class, "cacheServer3ClientSubscriptionConfig");

    Mockito.when(mockCache.getCacheServers())
        .thenReturn(Arrays.asList(mockCacheServer1, mockCacheServer2, mockCacheServer3));
    Mockito.when(mockCacheServer1.getClientSubscriptionConfig())
        .thenReturn(cacheServer1ClientSubscriptionConfig);
    Mockito.when(cacheServer1ClientSubscriptionConfig.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockCacheServer2.getClientSubscriptionConfig()).thenReturn(null);
    Mockito.when(mockCacheServer3.getClientSubscriptionConfig())
        .thenReturn(cacheServer3ClientSubscriptionConfig);
    Mockito.when(cacheServer3ClientSubscriptionConfig.getDiskStoreName()).thenReturn("");
    Mockito.when(mockCacheServer1.getBindAddress()).thenReturn("10.127.0.1");
    Mockito.when(mockCacheServer1.getPort()).thenReturn(10123);
    Mockito.when(mockCacheServer1.getHostnameForClients()).thenReturn("rodan");

    return CollectionUtils.asSet(createCacheServerDetails("10.127.0.1", 10123, "rodan"));
  }

  private Set<DiskStoreDetails.AsyncEventQueueDetails> setupAsyncEventQueuesForTestExecute(
      final InternalCache mockCache, final String diskStoreName) {
    final AsyncEventQueue mockAsyncEventQueue1 = Mockito.mock(AsyncEventQueue.class, "AsyncEventQueue1");
    final AsyncEventQueue mockAsyncEventQueue2 = Mockito.mock(AsyncEventQueue.class, "AsyncEventQueue2");
    final AsyncEventQueue mockAsyncEventQueue3 = Mockito.mock(AsyncEventQueue.class, "AsyncEventQueue3");

    Mockito.when(mockCache.getAsyncEventQueues()).thenReturn(
        CollectionUtils.asSet(mockAsyncEventQueue1, mockAsyncEventQueue2, mockAsyncEventQueue3));
    Mockito.when(mockAsyncEventQueue1.isPersistent()).thenReturn(true);
    Mockito.when(mockAsyncEventQueue1.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockAsyncEventQueue1.getId()).thenReturn("9876543210");
    Mockito.when(mockAsyncEventQueue2.isPersistent()).thenReturn(false);
    Mockito.when(mockAsyncEventQueue3.isPersistent()).thenReturn(true);
    Mockito.when(mockAsyncEventQueue3.getDiskStoreName()).thenReturn("memSto");

    return CollectionUtils.asSet(createAsyncEventQueueDetails("9876543210"));
  }

  @Test
  public void testAssertState() {
    DescribeDiskStoreFunction.assertState(true, "null");
  }

  @Test
  public void testAssertStateThrowsIllegalStateException() {
    Assertions.assertThatThrownBy(
        () -> DescribeDiskStoreFunction.assertState(false, "Expected (%1$s) message!", "test"))
            .isInstanceOf(IllegalStateException.class).hasMessage("Expected (test) message!");
  }

  @Test
  public void testExecute() throws Throwable {
    // Prepare Mocks
    final UUID diskStoreId = UUID.randomUUID();
    final String diskStoreName = "mockDiskStore";
    final String memberId = "mockMemberId";
    final String memberName = "mockMemberName";
    final InternalDistributedMember mockMember =
        Mockito.mock(InternalDistributedMember.class, "DistributedMember");
    final FunctionContext mockFunctionContext =
        Mockito.mock(FunctionContext.class, "testExecute$FunctionContext");
    final DiskStore mockDiskStore =
        createMockDiskStore(diskStoreId, diskStoreName, true, false,
            75, 8192L, 500, 120L, 10240, createFileArray("/export/disk/backup",
                "/export/disk/overflow", "/export/disk/persistence"),
            createIntArray(10240, 204800, 4096000), 50, 75);
    final TestResultSender testResultSender = new TestResultSender();
    Mockito.when(mockCache.getMyId()).thenReturn(mockMember);
    Mockito.when(mockCache.findDiskStore(diskStoreName)).thenReturn(mockDiskStore);
    Mockito.when(mockCache.getPdxPersistent()).thenReturn(true);
    Mockito.when(mockCache.getPdxDiskStore()).thenReturn("memoryStore");
    Mockito.when(mockMember.getId()).thenReturn(memberId);
    Mockito.when(mockMember.getName()).thenReturn(memberName);
    Mockito.when(mockFunctionContext.getCache()).thenReturn(mockCache);
    Mockito.when(mockFunctionContext.getArguments()).thenReturn(diskStoreName);
    Mockito.when(mockFunctionContext.getResultSender()).thenReturn(testResultSender);

    // Expected Results
    final Set<DiskStoreDetails.RegionDetails> expectedRegionDetails =
        setupRegionsForTestExecute(mockCache, diskStoreName);
    final Set<DiskStoreDetails.GatewayDetails> expectedGatewayDetails =
        setupGatewaysForTestExecute(mockCache, diskStoreName);
    final Set<DiskStoreDetails.CacheServerDetails> expectedCacheServerDetails =
        setupCacheServersForTestExecute(mockCache, diskStoreName);
    final Set<DiskStoreDetails.AsyncEventQueueDetails> expectedAsyncEventQueueDetails =
        setupAsyncEventQueuesForTestExecute(mockCache, diskStoreName);

    // Execute Function and assert results
    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    function.execute(mockFunctionContext);

    final List<?> results = testResultSender.getResults();
    Assertions.assertThat(results).isNotNull();
    Assertions.assertThat(results.size()).isEqualTo(1);

    final DiskStoreDetails diskStoreDetails = (DiskStoreDetails) results.get(0);
    AssertionsForClassTypes.assertThat(diskStoreDetails).isNotNull();
    assertThat(diskStoreDetails.getId()).isEqualTo(diskStoreId);
    assertThat(diskStoreDetails.getName()).isEqualTo(diskStoreName);
    assertThat(diskStoreDetails.getMemberId()).isEqualTo(memberId);
    assertThat(diskStoreDetails.getMemberName()).isEqualTo(memberName);
    assertThat(diskStoreDetails.getAllowForceCompaction()).isTrue();
    assertThat(diskStoreDetails.getAutoCompact()).isFalse();
    assertThat(diskStoreDetails.getCompactionThreshold().intValue()).isEqualTo(75);
    assertThat(diskStoreDetails.getMaxOplogSize().longValue()).isEqualTo(8192L);
    assertThat(diskStoreDetails.isPdxSerializationMetaDataStored()).isFalse();
    assertThat(diskStoreDetails.getQueueSize().intValue()).isEqualTo(500);
    assertThat(diskStoreDetails.getTimeInterval().longValue()).isEqualTo(120L);
    assertThat(diskStoreDetails.getWriteBufferSize().intValue()).isEqualTo(10240);
    assertThat(diskStoreDetails.getDiskUsageWarningPercentage()).isEqualTo(50.0f);
    assertThat(diskStoreDetails.getDiskUsageCriticalPercentage()).isEqualTo(75.0f);

    final List<Integer> expectedDiskDirSizes = Arrays.asList(10240, 204800, 4096000);
    final List<String> expectedDiskDirs =
        Arrays.asList(new File("/export/disk/backup").getAbsolutePath(),
            new File("/export/disk/overflow").getAbsolutePath(),
            new File("/export/disk/persistence").getAbsolutePath());
    int count = 0;

    for (final DiskStoreDetails.DiskDirDetails diskDirDetails : diskStoreDetails) {
      Assertions.assertThat(expectedDiskDirSizes.contains(diskDirDetails.getSize())).isTrue();
      Assertions.assertThat(expectedDiskDirs.contains(diskDirDetails.getAbsolutePath())).isTrue();
      count++;
    }

    Mockito.verify(mockDiskStore, Mockito.atLeastOnce()).getName();
    Mockito.verify(mockDiskStore, Mockito.atLeastOnce()).getDiskStoreUUID();
    Assertions.assertThat(count).isEqualTo(expectedDiskDirs.size());
    assertRegionDetails(expectedRegionDetails, diskStoreDetails);
    assertCacheServerDetails(expectedCacheServerDetails, diskStoreDetails);
    assertGatewayDetails(expectedGatewayDetails, diskStoreDetails);
    assertAsyncEventQueueDetails(expectedAsyncEventQueueDetails, diskStoreDetails);
  }

  @Test
  public void testExecuteOnMemberHavingANonGemFireCache() throws Throwable {
    final Cache mockNonGemCache = Mockito.mock(Cache.class, "NonGemCache");
    final FunctionContext mockFunctionContext = Mockito.mock(FunctionContext.class, "FunctionContext");
    final TestResultSender testResultSender = new TestResultSender();
    Mockito.when(mockFunctionContext.getCache()).thenReturn(mockNonGemCache);
    Mockito.when(mockFunctionContext.getResultSender()).thenReturn(testResultSender);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    function.execute(mockFunctionContext);

    final List<?> results = testResultSender.getResults();
    Assertions.assertThat(results).isNotNull();
    Assertions.assertThat(results.isEmpty()).isTrue();
  }

  @Test
  public void testExecuteThrowingEntityNotFoundException() {
    final String memberId = "mockMemberId";
    final String memberName = "mockMemberName";
    final String diskStoreName = "testDiskStore";
    final InternalDistributedMember mockMember =
        Mockito.mock(InternalDistributedMember.class, "DistributedMember");
    final FunctionContext mockFunctionContext = Mockito.mock(FunctionContext.class, "FunctionContext");
    final TestResultSender testResultSender = new TestResultSender();
    Mockito.when(mockCache.getMyId()).thenReturn(mockMember);
    Mockito.when(mockCache.findDiskStore(diskStoreName)).thenReturn(null);
    Mockito.when(mockMember.getId()).thenReturn(memberId);
    Mockito.when(mockMember.getName()).thenReturn(memberName);
    Mockito.when(mockFunctionContext.getCache()).thenReturn(mockCache);
    Mockito.when(mockFunctionContext.getArguments()).thenReturn(diskStoreName);
    Mockito.when(mockFunctionContext.getResultSender()).thenReturn(testResultSender);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    function.execute(mockFunctionContext);
    String expected = String.format("A disk store with name '%1$s' was not found on member '%2$s'.",
        diskStoreName, memberName);
    Assertions.assertThatThrownBy(testResultSender::getResults).isInstanceOf(EntityNotFoundException.class)
        .hasMessage(expected);
  }

  @Test
  public void testExecuteThrowingRuntimeException() {
    final String diskStoreName = "testDiskStore";
    final String memberId = "mockMemberId";
    final String memberName = "mockMemberName";
    final FunctionContext mockFunctionContext = Mockito.mock(FunctionContext.class, "FunctionContext");
    final InternalDistributedMember mockMember =
        Mockito.mock(InternalDistributedMember.class, "DistributedMember");
    final TestResultSender testResultSender = new TestResultSender();
    Mockito.when(mockCache.getMyId()).thenReturn(mockMember);
    Mockito.when(mockCache.findDiskStore(diskStoreName)).thenThrow(new RuntimeException("ExpectedStrings"));
    Mockito.when(mockMember.getId()).thenReturn(memberId);
    Mockito.when(mockMember.getName()).thenReturn(memberName);
    Mockito.when(mockFunctionContext.getCache()).thenReturn(mockCache);
    Mockito.when(mockFunctionContext.getArguments()).thenReturn(diskStoreName);
    Mockito.when(mockFunctionContext.getResultSender()).thenReturn(testResultSender);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    function.execute(mockFunctionContext);
    Assertions.assertThatThrownBy(testResultSender::getResults).isInstanceOf(RuntimeException.class)
        .hasMessage("ExpectedStrings");
  }

  @Test
  public void testExecuteWithDiskDirsAndDiskSizesMismatch() {
    final String diskStoreName = "mockDiskStore";
    final String memberId = "mockMemberId";
    final String memberName = "mockMemberName";
    final UUID diskStoreId = UUID.randomUUID();
    final FunctionContext mockFunctionContext = Mockito.mock(FunctionContext.class, "FunctionContext");
    final InternalDistributedMember mockMember =
        Mockito.mock(InternalDistributedMember.class, "DistributedMember");
    final DiskStore mockDiskStore =
        createMockDiskStore(diskStoreId, diskStoreName, false, true, 70, 8192000L, 1000, 300L, 8192,
            createFileArray("/export/disk0/gemfire/backup"), new int[0], 50, 75);
    final TestResultSender testResultSender = new TestResultSender();
    Mockito.when(mockCache.getMyId()).thenReturn(mockMember);
    Mockito.when(mockCache.findDiskStore(diskStoreName)).thenReturn(mockDiskStore);
    Mockito.when(mockMember.getId()).thenReturn(memberId);
    Mockito.when(mockMember.getName()).thenReturn(memberName);
    Mockito.when(mockFunctionContext.getCache()).thenReturn(mockCache);
    Mockito.when(mockFunctionContext.getArguments()).thenReturn(diskStoreName);
    Mockito.when(mockFunctionContext.getResultSender()).thenReturn(testResultSender);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    function.execute(mockFunctionContext);
    String expected =
        "The number of disk directories with a specified size (0) does not match the number of disk directories (1)!";
    Assertions.assertThatThrownBy(testResultSender::getResults).hasMessage(expected);
    Mockito.verify(mockDiskStore, Mockito.atLeastOnce()).getName();
    Mockito.verify(mockDiskStore, Mockito.atLeastOnce()).getDiskStoreUUID();
  }

  @Test
  public void testGetRegionDiskStoreName() {
    final String expectedDiskStoreName = "testDiskStore";
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getDiskStoreName()).thenReturn(expectedDiskStoreName);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.getDiskStoreName(mockRegion)).isEqualTo(expectedDiskStoreName);
  }

  @Test
  public void testGetRegionDiskStoreNameWhenUnspecified() {
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getDiskStoreName()).thenReturn(null);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.getDiskStoreName(mockRegion))
        .isEqualTo(DiskStoreDetails.DEFAULT_DISK_STORE_NAME);
  }

  @Test
  public void testIsRegionOverflowToDiskWhenEvictionActionIsLocalDestroy() {
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    final EvictionAttributes mockEvictionAttributes =
        Mockito.mock(EvictionAttributes.class, "EvictionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getEvictionAttributes()).thenReturn(mockEvictionAttributes);
    Mockito.when(mockEvictionAttributes.getAction()).thenReturn(EvictionAction.LOCAL_DESTROY);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isOverflowToDisk(mockRegion)).isFalse();
    Mockito.verify(mockRegion, Mockito.times(2)).getAttributes();
    Mockito.verify(mockRegionAttributes, Mockito.times(2)).getEvictionAttributes();
  }

  @Test
  public void testIsRegionOverflowToDiskWhenEvictionActionIsOverflowToDisk() {
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    final EvictionAttributes mockEvictionAttributes =
        Mockito.mock(EvictionAttributes.class, "EvictionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getEvictionAttributes()).thenReturn(mockEvictionAttributes);
    Mockito.when(mockEvictionAttributes.getAction()).thenReturn(EvictionAction.OVERFLOW_TO_DISK);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isOverflowToDisk(mockRegion)).isTrue();
    Mockito.verify(mockRegion, Mockito.times(2)).getAttributes();
    Mockito.verify(mockRegionAttributes, Mockito.times(2)).getEvictionAttributes();
  }

  @Test
  public void testIsRegionOverflowToDiskWithNullEvictionAttributes() {
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getEvictionAttributes()).thenReturn(null);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isOverflowToDisk(mockRegion)).isFalse();
  }

  @Test
  public void testIsRegionPersistentWhenDataPolicyIsPersistentPartition() {
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.PERSISTENT_PARTITION);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isPersistent(mockRegion)).isTrue();
  }

  @Test
  public void testIsRegionPersistentWhenDataPolicyIsPersistentReplicate() {
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.PERSISTENT_REPLICATE);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isPersistent(mockRegion)).isTrue();
  }

  @Test
  public void testIsRegionPersistentWhenDataPolicyIsNormal() {
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.NORMAL);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isPersistent(mockRegion)).isFalse();
  }

  @Test
  public void testIsRegionPersistentWhenDataPolicyIsPartition() {
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.PARTITION);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isPersistent(mockRegion)).isFalse();
  }

  @Test
  public void testIsRegionPersistentWhenDataPolicyIsPreloaded() {
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.PRELOADED);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isPersistent(mockRegion)).isFalse();
  }

  @Test
  public void testIsRegionPersistentWhenDataPolicyIsReplicate() {
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.REPLICATE);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isPersistent(mockRegion)).isFalse();
  }

  @Test
  public void testIsRegionUsingDiskStoreWhenUsingDefaultDiskStore() {
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.PERSISTENT_REPLICATE);
    Mockito.when(mockRegionAttributes.getDiskStoreName()).thenReturn(null);
    Mockito.when(mockDiskStore.getName()).thenReturn(DiskStoreDetails.DEFAULT_DISK_STORE_NAME);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockRegion, mockDiskStore)).isTrue();
    Mockito.verify(mockRegion, Mockito.atLeastOnce()).getAttributes();
  }

  @Test
  public void testIsRegionUsingDiskStoreWhenPersistent() {
    final String diskStoreName = "testDiskStore";
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.PERSISTENT_PARTITION);
    Mockito.when(mockRegionAttributes.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockDiskStore.getName()).thenReturn(diskStoreName);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockRegion, mockDiskStore)).isTrue();
    Mockito.verify(mockRegion, Mockito.atLeastOnce()).getAttributes();
  }

  @Test
  public void testIsRegionUsingDiskStoreWhenOverflowing() {
    final String diskStoreName = "testDiskStore";
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    final EvictionAttributes mockEvictionAttributes =
        Mockito.mock(EvictionAttributes.class, "EvictionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.PARTITION);
    Mockito.when(mockRegionAttributes.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockRegionAttributes.getEvictionAttributes()).thenReturn(mockEvictionAttributes);
    Mockito.when(mockEvictionAttributes.getAction()).thenReturn(EvictionAction.OVERFLOW_TO_DISK);
    Mockito.when(mockDiskStore.getName()).thenReturn(diskStoreName);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockRegion, mockDiskStore)).isTrue();
    Mockito.verify(mockRegion, Mockito.times(4)).getAttributes();
    Mockito.verify(mockRegionAttributes, Mockito.times(2)).getEvictionAttributes();
  }

  @Test
  public void testIsRegionUsingDiskStoreWhenDiskStoresMismatch() {
    final Region mockRegion = Mockito.mock(Region.class, "Region");
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final RegionAttributes mockRegionAttributes = Mockito.mock(RegionAttributes.class, "RegionAttributes");
    Mockito.when(mockRegion.getAttributes()).thenReturn(mockRegionAttributes);
    Mockito.when(mockRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.PERSISTENT_PARTITION);
    Mockito.when(mockRegionAttributes.getDiskStoreName()).thenReturn("mockDiskStore");
    Mockito.when(mockDiskStore.getName()).thenReturn("testDiskStore");

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockRegion, mockDiskStore)).isFalse();
  }

  @Test
  public void testSetRegionDetails() {
    // Prepare Mocks
    final String diskStoreName = "companyDiskStore";

    final Region mockCompanyRegion = Mockito.mock(Region.class, "/CompanyRegion");
    final RegionAttributes mockCompanyRegionAttributes =
        Mockito.mock(RegionAttributes.class, "CompanyRegionAttributes");
    final EvictionAttributes mockCompanyEvictionAttributes =
        Mockito.mock(EvictionAttributes.class, "CompanyEvictionAttributes");
    Mockito.when(mockCompanyRegion.getAttributes()).thenReturn(mockCompanyRegionAttributes);
    Mockito.when(mockCompanyRegion.getFullPath()).thenReturn("/CompanyRegion");
    Mockito.when(mockCompanyRegion.getName()).thenReturn("CompanyRegion");
    Mockito.when(mockCompanyRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.PERSISTENT_PARTITION);
    Mockito.when(mockCompanyRegionAttributes.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockCompanyRegionAttributes.getEvictionAttributes())
        .thenReturn(mockCompanyEvictionAttributes);
    Mockito.when(mockCompanyEvictionAttributes.getAction()).thenReturn(EvictionAction.LOCAL_DESTROY);

    final Region mockEmployeeRegion = Mockito.mock(Region.class, "/CompanyRegion/EmployeeRegion");
    Mockito.when(mockEmployeeRegion.getAttributes()).thenReturn(mockCompanyRegionAttributes);
    Mockito.when(mockEmployeeRegion.getFullPath()).thenReturn("/CompanyRegion/EmployeeRegion");
    Mockito.when(mockEmployeeRegion.getName()).thenReturn("EmployeeRegion");

    final Region mockProductsRegion = Mockito.mock(Region.class, "/CompanyRegion/ProductsRegion");
    final RegionAttributes mockProductsServicesRegionAttributes =
        Mockito.mock(RegionAttributes.class, "ProductsServicesRegionAttributes");
    Mockito.when(mockProductsRegion.getAttributes()).thenReturn(mockProductsServicesRegionAttributes);
    Mockito.when(mockProductsRegion.subregions(false)).thenReturn(Collections.emptySet());
    Mockito.when(mockProductsServicesRegionAttributes.getDataPolicy())
        .thenReturn(DataPolicy.PERSISTENT_REPLICATE);
    Mockito.when(mockProductsServicesRegionAttributes.getDiskStoreName())
        .thenReturn("productsServicesDiskStore");

    final Region mockServicesRegion = Mockito.mock(Region.class, "/CompanyRegion/ServicesRegion");
    Mockito.when(mockServicesRegion.getAttributes()).thenReturn(mockProductsServicesRegionAttributes);
    Mockito.when(mockServicesRegion.subregions(false)).thenReturn(Collections.emptySet());

    final Region mockContractorsRegion = Mockito.mock(Region.class, "/CompanyRegion/ContractorsRegion");
    final RegionAttributes mockContractorsRegionAttributes =
        Mockito.mock(RegionAttributes.class, "ContractorsRegionAttributes");
    final EvictionAttributes mockContractorsEvictionAttributes =
        Mockito.mock(EvictionAttributes.class, "ContractorsEvictionAttributes");
    Mockito.when(mockContractorsRegion.getAttributes()).thenReturn(mockContractorsRegionAttributes);
    Mockito.when(mockContractorsRegion.getFullPath()).thenReturn("/CompanyRegion/ContractorsRegion");
    Mockito.when(mockContractorsRegion.getName()).thenReturn("ContractorsRegion");
    Mockito.when(mockContractorsRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.REPLICATE);
    Mockito.when(mockContractorsRegionAttributes.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockContractorsRegionAttributes.getEvictionAttributes())
        .thenReturn(mockContractorsEvictionAttributes);
    Mockito.when(mockContractorsEvictionAttributes.getAction()).thenReturn(EvictionAction.OVERFLOW_TO_DISK);

    final Region mockRolesRegion = Mockito.mock(Region.class, "/CompanyRegion/EmployeeRegion/RolesRegion");
    Mockito.when(mockRolesRegion.getAttributes()).thenReturn(mockCompanyRegionAttributes);
    Mockito.when(mockRolesRegion.getFullPath()).thenReturn("/CompanyRegion/EmployeeRegion/RolesRegion");
    Mockito.when(mockRolesRegion.getName()).thenReturn("RolesRegion");
    Mockito.when(mockRolesRegion.subregions(false)).thenReturn(Collections.emptySet());

    final Region mockPartnersRegion = Mockito.mock(Region.class, "/PartnersRegion");
    final RegionAttributes mockPartnersRegionAttributes =
        Mockito.mock(RegionAttributes.class, "PartnersRegionAttributes");
    Mockito.when(mockPartnersRegion.getAttributes()).thenReturn(mockPartnersRegionAttributes);
    Mockito.when(mockPartnersRegion.subregions(false)).thenReturn(Collections.emptySet());
    Mockito.when(mockPartnersRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.PERSISTENT_PARTITION);
    Mockito.when(mockPartnersRegionAttributes.getDiskStoreName()).thenReturn("");

    final Region mockCustomersRegion = Mockito.mock(Region.class, "/CustomersRegion");
    final RegionAttributes mockCustomersRegionAttributes =
        Mockito.mock(RegionAttributes.class, "CustomersRegionAttributes");
    final EvictionAttributes mockCustomersEvictionAttributes =
        Mockito.mock(EvictionAttributes.class, "CustomersEvictionAttributes");
    Mockito.when(mockCustomersRegion.getAttributes()).thenReturn(mockCustomersRegionAttributes);
    Mockito.when(mockCustomersRegion.subregions(false)).thenReturn(Collections.emptySet());
    Mockito.when(mockCustomersRegionAttributes.getDataPolicy()).thenReturn(DataPolicy.REPLICATE);
    Mockito.when(mockCustomersRegionAttributes.getDiskStoreName()).thenReturn(null);
    Mockito.when(mockCustomersRegionAttributes.getEvictionAttributes())
        .thenReturn(mockCustomersEvictionAttributes);
    Mockito.when(mockCustomersEvictionAttributes.getAction()).thenReturn(EvictionAction.OVERFLOW_TO_DISK);

    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    Mockito.when(mockDiskStore.getName()).thenReturn(diskStoreName);

    Set<Region<?, ?>> mockRootRegions = new HashSet<>();
    mockRootRegions.add(mockCompanyRegion);
    mockRootRegions.add(mockPartnersRegion);
    mockRootRegions.add(mockCustomersRegion);
    Mockito.when(mockCache.rootRegions()).thenReturn(mockRootRegions);
    Mockito.when(mockCompanyRegion.subregions(false)).thenReturn(CollectionUtils
        .asSet(mockContractorsRegion, mockEmployeeRegion, mockProductsRegion, mockServicesRegion));
    Mockito.when(mockEmployeeRegion.subregions(false)).thenReturn(CollectionUtils.asSet(mockRolesRegion));
    Mockito.when(mockContractorsRegion.subregions(false)).thenReturn(Collections.emptySet());

    // Execute Region and assert results
    final Set<DiskStoreDetails.RegionDetails> expectedRegionDetails = CollectionUtils.asSet(
        createRegionDetails("/CompanyRegion", "CompanyRegion", true, false),
        createRegionDetails("/CompanyRegion/EmployeeRegion", "EmployeeRegion", true, false),
        createRegionDetails("/CompanyRegion/EmployeeRegion/RolesRegion", "RolesRegion", true,
            false),
        createRegionDetails("/CompanyRegion/ContractorsRegion", "ContractorsRegion", false, true));
    final DiskStoreDetails diskStoreDetails = new DiskStoreDetails(diskStoreName, "memberOne");
    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    function.setRegionDetails(mockCache, mockDiskStore, diskStoreDetails);
    assertRegionDetails(expectedRegionDetails, diskStoreDetails);
    Mockito.verify(mockCompanyRegion, Mockito.times(5)).getAttributes();
    Mockito.verify(mockEmployeeRegion, Mockito.times(5)).getAttributes();
    Mockito.verify(mockRolesRegion, Mockito.times(5)).getAttributes();
    Mockito.verify(mockCompanyRegionAttributes, Mockito.times(6)).getDataPolicy();
    Mockito.verify(mockCompanyRegionAttributes, Mockito.times(3)).getDiskStoreName();
    Mockito.verify(mockCompanyRegionAttributes, Mockito.times(6)).getEvictionAttributes();
    Mockito.verify(mockCompanyEvictionAttributes, Mockito.times(3)).getAction();
    Mockito.verify(mockContractorsRegion, Mockito.times(7)).getAttributes();
    Mockito.verify(mockContractorsRegionAttributes, Mockito.times(2)).getDataPolicy();
    Mockito.verify(mockContractorsRegionAttributes, Mockito.times(4)).getEvictionAttributes();
    Mockito.verify(mockContractorsEvictionAttributes, Mockito.times(2)).getAction();
    Mockito.verify(mockProductsRegion, Mockito.times(2)).getAttributes();
    Mockito.verify(mockServicesRegion, Mockito.times(2)).getAttributes();
    Mockito.verify(mockProductsServicesRegionAttributes, Mockito.times(2)).getDataPolicy();
    Mockito.verify(mockProductsServicesRegionAttributes, Mockito.times(2)).getDiskStoreName();
    Mockito.verify(mockPartnersRegion, Mockito.times(2)).getAttributes();
    Mockito.verify(mockCustomersRegion, Mockito.times(4)).getAttributes();
    Mockito.verify(mockCustomersRegionAttributes, Mockito.times(2)).getEvictionAttributes();
    Mockito.verify(mockDiskStore, Mockito.atLeastOnce()).getName();
  }

  @Test
  public void testGetCacheServerDiskStoreName() {
    final String expectedDiskStoreName = "testDiskStore";
    final CacheServer mockCacheServer = Mockito.mock(CacheServer.class, "CacheServer");
    final ClientSubscriptionConfig mockClientSubscriptionConfig =
        Mockito.mock(ClientSubscriptionConfig.class, "ClientSubscriptionConfig");
    Mockito.when(mockCacheServer.getClientSubscriptionConfig()).thenReturn(mockClientSubscriptionConfig);
    Mockito.when(mockClientSubscriptionConfig.getDiskStoreName()).thenReturn(expectedDiskStoreName);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.getDiskStoreName(mockCacheServer)).isEqualTo(expectedDiskStoreName);
    Mockito.verify(mockCacheServer, Mockito.times(2)).getClientSubscriptionConfig();
  }

  @Test
  public void testGetCacheServerDiskStoreNameWhenUnspecified() {
    final CacheServer mockCacheServer = Mockito.mock(CacheServer.class, "CacheServer");
    final ClientSubscriptionConfig mockClientSubscriptionConfig =
        Mockito.mock(ClientSubscriptionConfig.class, "ClientSubscriptionConfig");
    Mockito.when(mockCacheServer.getClientSubscriptionConfig()).thenReturn(mockClientSubscriptionConfig);
    Mockito.when(mockClientSubscriptionConfig.getDiskStoreName()).thenReturn(null);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.getDiskStoreName(mockCacheServer))
        .isEqualTo(DiskStoreDetails.DEFAULT_DISK_STORE_NAME);
    Mockito.verify(mockCacheServer, Mockito.times(2)).getClientSubscriptionConfig();
  }

  @Test
  public void testGetCacheServerDiskStoreNameWithNullClientSubscriptionConfig() {
    final CacheServer mockCacheServer = Mockito.mock(CacheServer.class, "CacheServer");
    Mockito.when(mockCacheServer.getClientSubscriptionConfig()).thenReturn(null);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.getDiskStoreName(mockCacheServer)).isNull();
  }

  @Test
  public void testIsCacheServerUsingDiskStore() {
    final String diskStoreName = "testDiskStore";
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final CacheServer mockCacheServer = Mockito.mock(CacheServer.class, "CacheServer");
    final ClientSubscriptionConfig mockClientSubscriptionConfig =
        Mockito.mock(ClientSubscriptionConfig.class, "ClientSubscriptionConfig");
    Mockito.when(mockCacheServer.getClientSubscriptionConfig()).thenReturn(mockClientSubscriptionConfig);
    Mockito.when(mockClientSubscriptionConfig.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockDiskStore.getName()).thenReturn(diskStoreName);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockCacheServer, mockDiskStore)).isTrue();
    Mockito.verify(mockCacheServer, Mockito.times(2)).getClientSubscriptionConfig();

  }

  @Test
  public void testIsCacheServerUsingDiskStoreWhenDiskStoresMismatch() {
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final CacheServer mockCacheServer = Mockito.mock(CacheServer.class, "CacheServer");
    final ClientSubscriptionConfig mockClientSubscriptionConfig =
        Mockito.mock(ClientSubscriptionConfig.class, "ClientSubscriptionConfig");
    Mockito.when(mockCacheServer.getClientSubscriptionConfig()).thenReturn(mockClientSubscriptionConfig);
    Mockito.when(mockClientSubscriptionConfig.getDiskStoreName()).thenReturn(" ");
    Mockito.when(mockDiskStore.getName()).thenReturn("otherDiskStore");

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockCacheServer, mockDiskStore)).isFalse();
    Mockito.verify(mockCacheServer, Mockito.times(2)).getClientSubscriptionConfig();
  }

  @Test
  public void testIsCacheServerUsingDiskStoreWhenUsingDefaultDiskStore() {
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final CacheServer mockCacheServer = Mockito.mock(CacheServer.class, "CacheServer");
    final ClientSubscriptionConfig mockClientSubscriptionConfig =
        Mockito.mock(ClientSubscriptionConfig.class, "ClientSubscriptionConfig");
    Mockito.when(mockCacheServer.getClientSubscriptionConfig()).thenReturn(mockClientSubscriptionConfig);
    Mockito.when(mockClientSubscriptionConfig.getDiskStoreName()).thenReturn("");
    Mockito.when(mockDiskStore.getName()).thenReturn(DiskStoreDetails.DEFAULT_DISK_STORE_NAME);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockCacheServer, mockDiskStore)).isTrue();
    Mockito.verify(mockCacheServer, Mockito.times(2)).getClientSubscriptionConfig();
  }

  @Test
  public void testSetCacheServerDetails() {
    final String diskStoreName = "testDiskStore";
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final CacheServer mockCacheServer1 = Mockito.mock(CacheServer.class, "CacheServer1");
    final CacheServer mockCacheServer2 = Mockito.mock(CacheServer.class, "CacheServer2");
    final CacheServer mockCacheServer3 = Mockito.mock(CacheServer.class, "CacheServer3");
    final ClientSubscriptionConfig mockCacheServer1ClientSubscriptionConfig =
        Mockito.mock(ClientSubscriptionConfig.class, "cacheServer1ClientSubscriptionConfig");
    final ClientSubscriptionConfig mockCacheServer2ClientSubscriptionConfig =
        Mockito.mock(ClientSubscriptionConfig.class, "cacheServer2ClientSubscriptionConfig");
    Mockito.when(mockCache.getCacheServers())
        .thenReturn(Arrays.asList(mockCacheServer1, mockCacheServer2, mockCacheServer3));
    Mockito.when(mockCacheServer1.getClientSubscriptionConfig())
        .thenReturn(mockCacheServer1ClientSubscriptionConfig);
    Mockito.when(mockCacheServer1ClientSubscriptionConfig.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockCacheServer1.getBindAddress()).thenReturn("10.127.255.1");
    Mockito.when(mockCacheServer1.getPort()).thenReturn(65536);
    Mockito.when(mockCacheServer1.getHostnameForClients()).thenReturn("gemini");
    Mockito.when(mockCacheServer2.getClientSubscriptionConfig())
        .thenReturn(mockCacheServer2ClientSubscriptionConfig);
    Mockito.when(mockCacheServer2ClientSubscriptionConfig.getDiskStoreName()).thenReturn("  ");
    Mockito.when(mockCacheServer3.getClientSubscriptionConfig()).thenReturn(null);
    Mockito.when(mockDiskStore.getName()).thenReturn(diskStoreName);

    final Set<DiskStoreDetails.CacheServerDetails> expectedCacheServerDetails =
        CollectionUtils.asSet(createCacheServerDetails("10.127.255.1", 65536, "gemini"));
    final DiskStoreDetails diskStoreDetails = new DiskStoreDetails(diskStoreName, "memberOne");
    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    function.setCacheServerDetails(mockCache, mockDiskStore, diskStoreDetails);
    assertCacheServerDetails(expectedCacheServerDetails, diskStoreDetails);
    Mockito.verify(mockCacheServer1, Mockito.times(2)).getClientSubscriptionConfig();
    Mockito.verify(mockCacheServer2, Mockito.times(2)).getClientSubscriptionConfig();
    Mockito.verify(mockDiskStore, Mockito.times(3)).getName();
  }

  @Test
  public void testGetGatewaySenderDiskStoreName() {
    final String expectedDiskStoreName = "testDiskStore";
    final GatewaySender mockGatewaySender = Mockito.mock(GatewaySender.class, "GatewaySender");
    Mockito.when(mockGatewaySender.getDiskStoreName()).thenReturn(expectedDiskStoreName);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.getDiskStoreName(mockGatewaySender)).isEqualTo(expectedDiskStoreName);
  }

  @Test
  public void testGetGatewaySenderDiskStoreNameWhenUnspecified() {
    final GatewaySender mockGatewaySender = Mockito.mock(GatewaySender.class, "GatewaySender");
    Mockito.when(mockGatewaySender.getDiskStoreName()).thenReturn(" ");

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.getDiskStoreName(mockGatewaySender))
        .isEqualTo(DiskStoreDetails.DEFAULT_DISK_STORE_NAME);
  }

  @Test
  public void testIsGatewaySenderPersistent() {
    final GatewaySender mockGatewaySender = Mockito.mock(GatewaySender.class, "GatewaySender");
    Mockito.when(mockGatewaySender.isPersistenceEnabled()).thenReturn(true);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isPersistent(mockGatewaySender)).isTrue();
  }

  @Test
  public void testIsGatewaySenderPersistentWhenPersistenceIsNotEnabled() {
    final GatewaySender mockGatewaySender = Mockito.mock(GatewaySender.class, "GatewaySender");
    Mockito.when(mockGatewaySender.isPersistenceEnabled()).thenReturn(true);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isPersistent(mockGatewaySender)).isTrue();
  }

  @Test
  public void testIsGatewaySenderUsingDiskStore() {
    final String diskStoreName = "testDiskStore";
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final GatewaySender mockGatewaySender = Mockito.mock(GatewaySender.class, "GatewaySender");
    Mockito.when(mockGatewaySender.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockDiskStore.getName()).thenReturn(diskStoreName);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockGatewaySender, mockDiskStore)).isTrue();
  }

  @Test
  public void testIsGatewaySenderUsingDiskStoreWhenDiskStoresMismatch() {
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final GatewaySender mockGatewaySender = Mockito.mock(GatewaySender.class, "GatewaySender");
    Mockito.when(mockGatewaySender.getDiskStoreName()).thenReturn("mockDiskStore");
    Mockito.when(mockDiskStore.getName()).thenReturn("testDiskStore");

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockGatewaySender, mockDiskStore)).isFalse();
  }

  @Test
  public void testIsGatewaySenderUsingDiskStoreWhenUsingDefaultDiskStores() {
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final GatewaySender mockGatewaySender = Mockito.mock(GatewaySender.class, "GatewaySender");
    Mockito.when(mockGatewaySender.getDiskStoreName()).thenReturn(" ");
    Mockito.when(mockDiskStore.getName()).thenReturn(DiskStoreDetails.DEFAULT_DISK_STORE_NAME);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockGatewaySender, mockDiskStore)).isTrue();
  }

  @Test
  public void testSetPdxSerializationDetails() {
    final String diskStoreName = "testDiskStore";
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    Mockito.when(mockCache.getPdxPersistent()).thenReturn(true);
    Mockito.when(mockCache.getPdxDiskStore()).thenReturn(diskStoreName);
    Mockito.when(mockDiskStore.getName()).thenReturn(diskStoreName);

    final DiskStoreDetails diskStoreDetails = new DiskStoreDetails(diskStoreName, "memberOne");
    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    function.setPdxSerializationDetails(mockCache, mockDiskStore, diskStoreDetails);
    assertThat(diskStoreDetails.isPdxSerializationMetaDataStored()).isTrue();
  }

  @Test
  public void testSetPdxSerializationDetailsWhenDiskStoreMismatch() {
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    Mockito.when(mockCache.getPdxPersistent()).thenReturn(true);
    Mockito.when(mockCache.getPdxDiskStore()).thenReturn("mockDiskStore");
    Mockito.when(mockDiskStore.getName()).thenReturn("testDiskStore");

    final DiskStoreDetails diskStoreDetails = new DiskStoreDetails("testDiskStore", "memberOne");
    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    function.setPdxSerializationDetails(mockCache, mockDiskStore, diskStoreDetails);
    assertThat(diskStoreDetails.isPdxSerializationMetaDataStored()).isFalse();
  }

  @Test
  public void testSetPdxSerializationDetailsWhenPdxIsNotPersistent() {
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    Mockito.when(mockCache.getPdxPersistent()).thenReturn(false);

    final DiskStoreDetails diskStoreDetails = new DiskStoreDetails("testDiskStore", "memberOne");
    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    function.setPdxSerializationDetails(mockCache, mockDiskStore, diskStoreDetails);
    assertThat(diskStoreDetails.isPdxSerializationMetaDataStored()).isFalse();
  }

  @Test
  public void testGetAsyncEventQueueDiskStoreName() {
    final String expectedDiskStoreName = "testDiskStore";
    final AsyncEventQueue mockQueue = Mockito.mock(AsyncEventQueue.class, "AsyncEventQueue");
    Mockito.when(mockQueue.getDiskStoreName()).thenReturn(expectedDiskStoreName);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.getDiskStoreName(mockQueue)).isEqualTo(expectedDiskStoreName);
  }

  @Test
  public void testGetAsyncEventQueueDiskStoreNameUsingDefaultDiskStore() {
    final AsyncEventQueue mockQueue = Mockito.mock(AsyncEventQueue.class, "AsyncEventQueue");
    Mockito.when(mockQueue.getDiskStoreName()).thenReturn(null);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.getDiskStoreName(mockQueue))
        .isEqualTo(DiskStoreDetails.DEFAULT_DISK_STORE_NAME);
  }

  @Test
  public void testIsAsyncEventQueueUsingDiskStore() {
    final String diskStoreName = "testDiskStore";
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final AsyncEventQueue mockQueue = Mockito.mock(AsyncEventQueue.class, "AsyncEventQueue");
    Mockito.when(mockQueue.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockQueue.isPersistent()).thenReturn(true);
    Mockito.when(mockDiskStore.getName()).thenReturn(diskStoreName);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockQueue, mockDiskStore)).isTrue();
  }

  @Test
  public void testIsAsyncEventQueueUsingDiskStoreWhenDiskStoresMismatch() {
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final AsyncEventQueue mockQueue = Mockito.mock(AsyncEventQueue.class, "AsyncEventQueue");
    Mockito.when(mockQueue.getDiskStoreName()).thenReturn("mockDiskStore");
    Mockito.when(mockQueue.isPersistent()).thenReturn(true);
    Mockito.when(mockDiskStore.getName()).thenReturn(DiskStoreDetails.DEFAULT_DISK_STORE_NAME);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockQueue, mockDiskStore)).isFalse();
  }

  @Test
  public void testIsAsyncEventQueueUsingDiskStoreWhenQueueIsNotPersistent() {
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final AsyncEventQueue mockQueue = Mockito.mock(AsyncEventQueue.class, "AsyncEventQueue");
    Mockito.when(mockQueue.isPersistent()).thenReturn(false);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockQueue, mockDiskStore)).isFalse();
  }

  @Test
  public void testIsAsyncEventQueueUsingDiskStoreWhenUsingDefaultDiskStore() {
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final AsyncEventQueue mockQueue = Mockito.mock(AsyncEventQueue.class, "AsyncEventQueue");
    Mockito.when(mockQueue.getDiskStoreName()).thenReturn(" ");
    Mockito.when(mockQueue.isPersistent()).thenReturn(true);
    Mockito.when(mockDiskStore.getName()).thenReturn(DiskStoreDetails.DEFAULT_DISK_STORE_NAME);

    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    assertThat(function.isUsingDiskStore(mockQueue, mockDiskStore)).isTrue();
  }

  @Test
  public void testSetAsyncEventQueueDetails() {
    final String diskStoreName = "testDiskStore";
    final DiskStore mockDiskStore = Mockito.mock(DiskStore.class, "DiskStore");
    final AsyncEventQueue mockQueue1 = Mockito.mock(AsyncEventQueue.class, "AsyncEvenQueue1");
    final AsyncEventQueue mockQueue2 = Mockito.mock(AsyncEventQueue.class, "AsyncEvenQueue2");
    final AsyncEventQueue mockQueue3 = Mockito.mock(AsyncEventQueue.class, "AsyncEvenQueue3");
    Mockito.when(mockCache.getAsyncEventQueues())
        .thenReturn(CollectionUtils.asSet(mockQueue1, mockQueue2, mockQueue3));
    Mockito.when(mockQueue1.isPersistent()).thenReturn(true);
    Mockito.when(mockQueue1.getDiskStoreName()).thenReturn(diskStoreName);
    Mockito.when(mockQueue1.getId()).thenReturn("q1");
    Mockito.when(mockQueue2.isPersistent()).thenReturn(true);
    Mockito.when(mockQueue2.getDiskStoreName()).thenReturn(null);
    Mockito.when(mockQueue3.isPersistent()).thenReturn(false);
    Mockito.when(mockDiskStore.getName()).thenReturn(diskStoreName);

    final Set<DiskStoreDetails.AsyncEventQueueDetails> expectedAsyncEventQueueDetails =
        CollectionUtils.asSet(createAsyncEventQueueDetails("q1"));
    final DiskStoreDetails diskStoreDetails = new DiskStoreDetails(diskStoreName, "memberOne");
    final DescribeDiskStoreFunction function = new DescribeDiskStoreFunction();
    function.setAsyncEventQueueDetails(mockCache, mockDiskStore, diskStoreDetails);
    assertAsyncEventQueueDetails(expectedAsyncEventQueueDetails, diskStoreDetails);
    Mockito.verify(mockDiskStore, Mockito.atLeastOnce()).getName();
  }

  private static class TestResultSender implements ResultSender {
    private Throwable t;
    private final List<Object> results = new LinkedList<>();

    protected List<Object> getResults() throws Throwable {
      if (t != null) {
        throw t;
      }

      return Collections.unmodifiableList(results);
    }

    @Override
    public void lastResult(final Object lastResult) {
      results.add(lastResult);
    }

    @Override
    public void sendResult(final Object oneResult) {
      results.add(oneResult);
    }

    @Override
    public void sendException(final Throwable t) {
      this.t = t;
    }
  }
}
