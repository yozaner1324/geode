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
package org.apache.geode.management.internal.cli.remote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.configuration.CacheConfig;
import org.apache.geode.distributed.internal.InternalConfigurationPersistenceService;
import org.apache.geode.internal.cache.AbstractRegion;
import org.apache.geode.internal.config.JAXBService;
import org.apache.geode.management.cli.Result;
import org.apache.geode.management.cli.SingleGfshCommand;
import org.apache.geode.management.cli.UpdateAllConfigurationGroupsMarker;
import org.apache.geode.management.internal.cli.GfshParseResult;
import org.apache.geode.UserErrorException;
import org.apache.geode.management.internal.cli.result.model.ResultModel;
import org.apache.geode.management.internal.exceptions.EntityNotFoundException;
import org.apache.geode.security.NotAuthorizedException;

public class CommandExecutorTest {

  private GfshParseResult parseResult;
  private CommandExecutor executor;
  private ResultModel result;
  private SingleGfshCommand testCommand;
  private InternalConfigurationPersistenceService ccService;
  private Region configRegion;

  @Before
  public void setUp() {
    parseResult = Mockito.mock(GfshParseResult.class);
    result = new ResultModel();
    executor = Mockito.spy(CommandExecutor.class);
    testCommand = mock(SingleGfshCommand.class,
        Mockito.withSettings().extraInterfaces(UpdateAllConfigurationGroupsMarker.class));
    ccService =
        Mockito.spy(new InternalConfigurationPersistenceService(JAXBService.create(CacheConfig.class)));
    configRegion = Mockito.mock(AbstractRegion.class);

    Mockito.doReturn(ccService).when(testCommand).getConfigurationPersistenceService();
    Mockito.doCallRealMethod().when(ccService).updateCacheConfig(ArgumentMatchers.any(), ArgumentMatchers.any());
    Mockito.doReturn(true).when(ccService).lockSharedConfiguration();
    Mockito.doNothing().when(ccService).unlockSharedConfiguration();
    Mockito.doReturn(configRegion).when(ccService).getConfigurationRegion();
  }

  @Test
  public void executeWhenGivenDummyParseResult() {
    Object result = executor.execute(parseResult);
    Assertions.assertThat(result).isInstanceOf(ResultModel.class);
    Assertions.assertThat(result.toString()).contains("Error while processing command");
  }

  @Test
  public void returnsResultAsExpected() {
    doReturn(result).when(executor).invokeCommand(ArgumentMatchers.any(), ArgumentMatchers.any());
    Object thisResult = executor.execute(parseResult);
    Assertions.assertThat(thisResult).isSameAs(result);
  }

  @Test
  public void testNullResult() {
    Mockito.doReturn(null).when(executor).invokeCommand(ArgumentMatchers.any(), ArgumentMatchers.any());
    Object thisResult = executor.execute(parseResult);
    Assertions.assertThat(thisResult.toString()).contains("Command returned null");
  }

  @Test
  public void anyRuntimeExceptionGetsCaught() {
    Mockito.doThrow(new RuntimeException("my message here")).when(executor).invokeCommand(ArgumentMatchers.any(), ArgumentMatchers.any());
    Object thisResult = executor.execute(parseResult);
    assertThat(((ResultModel) thisResult).getStatus()).isEqualTo(Result.Status.ERROR);
    Assertions.assertThat(thisResult.toString()).contains("my message here");
  }

  @Test
  public void notAuthorizedExceptionGetsThrown() {
    Mockito.doThrow(new NotAuthorizedException("Not Authorized")).when(executor).invokeCommand(ArgumentMatchers.any(),
        ArgumentMatchers.any());
    Assertions.assertThatThrownBy(() -> executor.execute(parseResult))
        .isInstanceOf(NotAuthorizedException.class);
  }

  @Test
  public void anyIllegalArgumentExceptionGetsCaught() {
    Mockito.doThrow(new IllegalArgumentException("my message here")).when(executor).invokeCommand(ArgumentMatchers.any(),
        ArgumentMatchers.any());
    Object thisResult = executor.execute(parseResult);
    assertThat(((ResultModel) thisResult).getStatus()).isEqualTo(Result.Status.ERROR);
    Assertions.assertThat(thisResult.toString()).contains("my message here");
  }

  @Test
  public void anyIllegalStateExceptionGetsCaught() {
    Mockito.doThrow(new IllegalStateException("my message here")).when(executor).invokeCommand(ArgumentMatchers.any(),
        ArgumentMatchers.any());
    Object thisResult = executor.execute(parseResult);
    assertThat(((ResultModel) thisResult).getStatus()).isEqualTo(Result.Status.ERROR);
    Assertions.assertThat(thisResult.toString()).contains("my message here");
  }

  @Test
  public void anyUserErrorExceptionGetsCaught() {
    doThrow(new UserErrorException("my message here")).when(executor).invokeCommand(ArgumentMatchers.any(), ArgumentMatchers.any());
    Object thisResult = executor.execute(parseResult);
    assertThat(((ResultModel) thisResult).getStatus()).isEqualTo(Result.Status.ERROR);
    Assertions.assertThat(thisResult.toString()).contains("my message here");
  }

  @Test
  public void anyEntityNotFoundException_statusOK() {
    Mockito.doThrow(new EntityNotFoundException("my message here", true)).when(executor)
        .invokeCommand(ArgumentMatchers.any(), ArgumentMatchers.any());
    Object thisResult = executor.execute(parseResult);
    assertThat(((ResultModel) thisResult).getStatus()).isEqualTo(Result.Status.OK);
    Assertions.assertThat(thisResult.toString()).contains("Skipping: my message here");
  }

  @Test
  public void anyEntityNotFoundException_statusERROR() {
    Mockito.doThrow(new EntityNotFoundException("my message here")).when(executor).invokeCommand(ArgumentMatchers.any(),
        ArgumentMatchers.any());
    Object thisResult = executor.execute(parseResult);
    assertThat(((ResultModel) thisResult).getStatus()).isEqualTo(Result.Status.ERROR);
    Assertions.assertThat(thisResult.toString()).contains("my message here");
  }

  @Test
  public void invokeCommandWithUpdateAllConfigsInterface_multipleGroupOptionSpecifiedWhenSingleConfiguredGroups_CallsUpdateConfigForGroupTwice() {
    Set<String> configuredGroups = new HashSet<>();
    configuredGroups.add("group1");
    Mockito.when(parseResult.getParamValueAsString("group")).thenReturn("Group1,Group2");
    doReturn(result).when(executor).callInvokeMethod(ArgumentMatchers.any(), ArgumentMatchers.any());
    Mockito.doReturn(configuredGroups).when(ccService).getGroups();

    Object thisResult = executor.invokeCommand(testCommand, parseResult);

    Mockito.verify(testCommand, Mockito.times(1)).updateConfigForGroup(ArgumentMatchers.eq("Group1"), ArgumentMatchers.any(), ArgumentMatchers.any());
    Mockito.verify(testCommand, Mockito.times(1)).updateConfigForGroup(ArgumentMatchers.eq("Group2"), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void invokeCommandWithUpdateAllConfigsInterface_singleGroupOptionSpecifiedWhenMultipleConfiguredGroups_CallsUpdateConfigForGroup() {
    Set<String> configuredGroups = new HashSet<>();
    configuredGroups.add("group1");
    configuredGroups.add("group2");
    Mockito.when(parseResult.getParamValueAsString("group")).thenReturn("group1");
    doReturn(result).when(executor).callInvokeMethod(ArgumentMatchers.any(), ArgumentMatchers.any());
    Mockito.doReturn(configuredGroups).when(ccService).getGroups();

    Object thisResult = executor.invokeCommand(testCommand, parseResult);

    Mockito.verify(testCommand, Mockito.times(1)).updateConfigForGroup(ArgumentMatchers.eq("group1"), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void invokeCommandWithUpdateAllConfigsInterface_noGroupOptionSpecifiedWhenSingleConfiguredGroups_CallsUpdateConfigForGroup() {
    Set<String> configuredGroups = new HashSet<>();
    configuredGroups.add("group1");
    Mockito.when(parseResult.getParamValueAsString("group")).thenReturn(null);
    doReturn(result).when(executor).callInvokeMethod(ArgumentMatchers.any(), ArgumentMatchers.any());
    Mockito.doReturn(configuredGroups).when(ccService).getGroups();

    Object thisResult = executor.invokeCommand(testCommand, parseResult);

    Mockito.verify(testCommand, Mockito.times(1)).updateConfigForGroup(ArgumentMatchers.eq("group1"), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void invokeCommandWithOutUpdateAllConfigsInterface_noGroupOptionSpecifiedWhenSingleConfiguredGroups_CallsUpdateConfigForCluster() {
    testCommand = Mockito.mock(SingleGfshCommand.class);
    Mockito.doReturn(ccService).when(testCommand).getConfigurationPersistenceService();

    Set<String> configuredGroups = new HashSet<>();
    configuredGroups.add("group1");
    Mockito.when(parseResult.getParamValueAsString("group")).thenReturn(null);
    doReturn(result).when(executor).callInvokeMethod(ArgumentMatchers.any(), ArgumentMatchers.any());
    Mockito.doReturn(configuredGroups).when(ccService).getGroups();

    Object thisResult = executor.invokeCommand(testCommand, parseResult);

    Mockito.verify(testCommand, Mockito.times(1)).updateConfigForGroup(ArgumentMatchers.eq("cluster"), ArgumentMatchers.any(), ArgumentMatchers.any());
  }
}
