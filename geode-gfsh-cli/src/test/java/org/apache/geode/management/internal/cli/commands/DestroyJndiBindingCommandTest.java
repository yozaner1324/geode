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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.configuration.CacheConfig;
import org.apache.geode.cache.configuration.JndiBindingsType;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.distributed.internal.InternalConfigurationPersistenceService;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.management.internal.cli.functions.CliFunctionResult;
import org.apache.geode.management.internal.cli.functions.DestroyJndiBindingFunction;
import org.apache.geode.management.internal.configuration.domain.Configuration;
import org.apache.geode.test.junit.rules.GfshParserRule;

public class DestroyJndiBindingCommandTest {

  @ClassRule
  public static GfshParserRule gfsh = new GfshParserRule();

  private DestroyJndiBindingCommand command;
  private InternalCache cache;
  private CacheConfig cacheConfig;
  private InternalConfigurationPersistenceService ccService;

  private static String COMMAND = "destroy jndi-binding ";

  @Before
  public void setUp() throws Exception {
    cache = Mockito.mock(InternalCache.class);
    command = Mockito.spy(DestroyJndiBindingCommand.class);
    Mockito.doReturn(cache).when(command).getCache();
    cacheConfig = Mockito.mock(CacheConfig.class);
    ccService = Mockito.mock(InternalConfigurationPersistenceService.class);

    Mockito.doReturn(Collections.emptySet()).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());
    Mockito.doReturn(ccService).when(command).getConfigurationPersistenceService();
    Mockito.when(ccService.getCacheConfig(ArgumentMatchers.any())).thenReturn(cacheConfig);
    Mockito.doAnswer(invocation -> {
      UnaryOperator<CacheConfig> mutator = invocation.getArgument(1);
      mutator.apply(cacheConfig);
      return null;
    }).when(ccService).updateCacheConfig(ArgumentMatchers.any(), ArgumentMatchers.any());

    Mockito.when(ccService.getConfigurationRegion()).thenReturn(Mockito.mock(Region.class));
    Mockito.when(ccService.getConfiguration(ArgumentMatchers.any())).thenReturn(Mockito.mock(Configuration.class));
  }

  @Test
  public void missingMandatory() {
    gfsh.executeAndAssertThat(command, COMMAND).statusIsError().containsOutput("Invalid command");
  }

  @Test
  public void returnsErrorIfBindingDoesNotExistAndIfExistsUnspecified() {
    gfsh.executeAndAssertThat(command, COMMAND + " --name=name").statusIsError()
        .containsOutput("does not exist.");
  }

  @Test
  public void skipsIfBindingDoesNotExistAndIfExistsSpecified() {
    gfsh.executeAndAssertThat(command, COMMAND + " --name=name --if-exists").statusIsSuccess()
        .containsOutput("does not exist.");
  }

  @Test
  public void skipsIfBindingDoesNotExistAndIfExistsSpecifiedTrue() {
    gfsh.executeAndAssertThat(command, COMMAND + " --name=name --if-exists=true").statusIsSuccess()
        .containsOutput("does not exist.");
  }

  @Test
  public void returnsErrorIfBindingDoesNotExistAndIfExistsSpecifiedFalse() {
    gfsh.executeAndAssertThat(command, COMMAND + " --name=name --if-exists=false").statusIsError()
        .containsOutput("does not exist.");
  }

  @Test
  public void whenNoMembersFoundAndNoClusterConfigServiceRunningThenError() {
    Mockito.doReturn(Collections.emptySet()).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());
    Mockito.doReturn(null).when(command).getConfigurationPersistenceService();

    gfsh.executeAndAssertThat(command, COMMAND + " --name=name").statusIsSuccess()
        .containsOutput("No members found").containsOutput(
            "Cluster configuration service is not running. Configuration change is not persisted.");
  }

  @Test
  public void whenNoMembersFoundAndClusterConfigRunningThenUpdateClusterConfig() {
    List<JndiBindingsType.JndiBinding> bindings = new ArrayList<>();
    JndiBindingsType.JndiBinding jndiBinding = new JndiBindingsType.JndiBinding();
    jndiBinding.setJndiName("name");
    bindings.add(jndiBinding);
    Mockito.doReturn(bindings).when(cacheConfig).getJndiBindings();

    gfsh.executeAndAssertThat(command, COMMAND + " --name=name").statusIsSuccess()
        .containsOutput("No members found.")
        .containsOutput("Cluster configuration for group 'cluster' is updated");

    Mockito.verify(ccService).updateCacheConfig(ArgumentMatchers.any(), ArgumentMatchers.any());
    Mockito.verify(command).updateConfigForGroup(ArgumentMatchers.eq("cluster"), ArgumentMatchers.eq(cacheConfig), ArgumentMatchers.any());
  }

  @Test
  public void whenMembersFoundAndNoClusterConfigRunningThenOnlyInvokeFunction() {
    Set<DistributedMember> members = new HashSet<>();
    members.add(Mockito.mock(DistributedMember.class));

    CliFunctionResult result =
        new CliFunctionResult("server1", true, "Jndi binding \"name\" destroyed on \"server1\"");
    List<CliFunctionResult> results = new ArrayList<>();
    results.add(result);

    Mockito.doReturn(members).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());
    Mockito.doReturn(null).when(command).getConfigurationPersistenceService();
    Mockito.doReturn(results).when(command).executeAndGetFunctionResult(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

    gfsh.executeAndAssertThat(command, COMMAND + " --name=name").statusIsSuccess()
        .tableHasColumnOnlyWithValues("Member", "server1")
        .tableHasColumnOnlyWithValues("Status", "OK")
        .tableHasColumnOnlyWithValues("Message", "Jndi binding \"name\" destroyed on \"server1\"");

    Mockito.verify(ccService, Mockito.times(0)).updateCacheConfig(ArgumentMatchers.any(), ArgumentMatchers.any());

    ArgumentCaptor<DestroyJndiBindingFunction> function =
        ArgumentCaptor.forClass(DestroyJndiBindingFunction.class);
    ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);

    ArgumentCaptor<Set<DistributedMember>> targetMembers = ArgumentCaptor.forClass(Set.class);
    Mockito.verify(command, Mockito.times(1)).executeAndGetFunctionResult(function.capture(), arguments.capture(),
        targetMembers.capture());

    String jndiName = (String) arguments.getValue()[0];
    boolean destroyingDataSource = (boolean) arguments.getValue()[1];

    assertThat(function.getValue()).isInstanceOf(DestroyJndiBindingFunction.class);
    Assertions.assertThat(jndiName).isEqualTo("name");
    Assertions.assertThat(destroyingDataSource).isEqualTo(false);
    Assertions.assertThat(targetMembers.getValue()).isEqualTo(members);
  }

  @Test
  public void whenMembersFoundAndClusterConfigRunningThenUpdateClusterConfigAndInvokeFunction() {
    List<JndiBindingsType.JndiBinding> bindings = new ArrayList<>();
    JndiBindingsType.JndiBinding jndiBinding = new JndiBindingsType.JndiBinding();
    jndiBinding.setJndiName("name");
    bindings.add(jndiBinding);
    Mockito.doReturn(bindings).when(cacheConfig).getJndiBindings();

    Set<DistributedMember> members = new HashSet<>();
    members.add(Mockito.mock(DistributedMember.class));

    CliFunctionResult result =
        new CliFunctionResult("server1", true, "Jndi binding \"name\" destroyed on \"server1\"");
    List<CliFunctionResult> results = new ArrayList<>();
    results.add(result);

    Mockito.doReturn(members).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());
    Mockito.doReturn(results).when(command).executeAndGetFunctionResult(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());

    gfsh.executeAndAssertThat(command, COMMAND + " --name=name").statusIsSuccess()
        .tableHasColumnOnlyWithValues("Member", "server1")
        .tableHasColumnOnlyWithValues("Status", "OK")
        .tableHasColumnOnlyWithValues("Message", "Jndi binding \"name\" destroyed on \"server1\"");

    Assertions.assertThat(cacheConfig.getJndiBindings().isEmpty()).isTrue();
    Mockito.verify(command).updateConfigForGroup(ArgumentMatchers.eq("cluster"), ArgumentMatchers.eq(cacheConfig), ArgumentMatchers.any());

    ArgumentCaptor<DestroyJndiBindingFunction> function =
        ArgumentCaptor.forClass(DestroyJndiBindingFunction.class);
    ArgumentCaptor<Object[]> arguments = ArgumentCaptor.forClass(Object[].class);

    ArgumentCaptor<Set<DistributedMember>> targetMembers = ArgumentCaptor.forClass(Set.class);
    Mockito.verify(command, Mockito.times(1)).executeAndGetFunctionResult(function.capture(), arguments.capture(),
        targetMembers.capture());

    String jndiName = (String) arguments.getValue()[0];
    boolean destroyingDataSource = (boolean) arguments.getValue()[1];

    assertThat(function.getValue()).isInstanceOf(DestroyJndiBindingFunction.class);
    Assertions.assertThat(jndiName).isEqualTo("name");
    Assertions.assertThat(destroyingDataSource).isEqualTo(false);
    Assertions.assertThat(targetMembers.getValue()).isEqualTo(members);
  }
}
