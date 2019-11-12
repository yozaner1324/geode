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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.geode.cache.configuration.RegionConfig;
import org.apache.geode.cache.execute.ResultCollector;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.distributed.internal.InternalConfigurationPersistenceService;
import org.apache.geode.management.api.ClusterManagementListResult;
import org.apache.geode.management.api.ClusterManagementService;
import org.apache.geode.management.configuration.Region;
import org.apache.geode.management.internal.cli.functions.CliFunctionResult;
import org.apache.geode.test.junit.rules.GfshParserRule;

public class CreateIndexCommandTest {
  @Rule
  public GfshParserRule gfshParser = new GfshParserRule();

  private CreateIndexCommand command;
  private ResultCollector rc;
  private InternalConfigurationPersistenceService ccService;
  private ClusterManagementService cms;

  @Before
  public void before() throws Exception {
    command = Mockito.spy(CreateIndexCommand.class);
    rc = Mockito.mock(ResultCollector.class);
    Mockito.when(rc.getResult()).thenReturn(Collections.emptyList());
    Mockito.doReturn(Collections.emptyList()).when(command).executeAndGetFunctionResult(ArgumentMatchers.any(), ArgumentMatchers.any(),
        ArgumentMatchers.any());
    ccService = Mockito.mock(InternalConfigurationPersistenceService.class);
    cms = Mockito.mock(ClusterManagementService.class);
  }

  @Test
  public void missingName() throws Exception {
    gfshParser.executeAndAssertThat(command,
        "create index --expression=abc --region=abc")
        .statusIsError()
        .containsOutput("Invalid command");
  }

  @Test
  public void missingExpression() throws Exception {
    gfshParser.executeAndAssertThat(command, "create index --name=abc --region=abc")
        .statusIsError()
        .containsOutput("Invalid command");
  }

  @Test
  public void missingRegion() throws Exception {
    gfshParser.executeAndAssertThat(command, "create index --name=abc --expression=abc")
        .statusIsError()
        .containsOutput("Invalid command");
  }

  @Test
  public void invalidIndexType() throws Exception {
    gfshParser.executeAndAssertThat(command,
        "create index --name=abc --expression=abc --region=abc --type=abc")
        .statusIsError()
        .containsOutput("Invalid command");
  }

  @Test
  public void validIndexType() throws Exception {
    Mockito.doReturn(Collections.EMPTY_SET).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());
    gfshParser.executeAndAssertThat(command,
        "create index --name=abc --expression=abc --region=abc --type=range")
        .statusIsError()
        .containsOutput("No Members Found");
  }

  @Test
  public void validRegionPath() throws Exception {
    Mockito.doReturn(ccService).when(command).getConfigurationPersistenceService();
    gfshParser.executeAndAssertThat(command,
        "create index --name=abc --expression=abc --region=\"region.entrySet() z\" --type=range")
        .statusIsError();
  }

  @Test
  public void validIndexType2() throws Exception {
    Mockito.doReturn(Collections.EMPTY_SET).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());
    gfshParser.executeAndAssertThat(command,
        "create index --name=abc --expression=abc --region=abc --type=hash")
        .statusIsError()
        .containsOutput("No Members Found");
  }

  @Test
  public void noMemberFound() throws Exception {
    Mockito.doReturn(Collections.EMPTY_SET).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());
    gfshParser.executeAndAssertThat(command,
        "create index --name=abc --expression=abc --region=abc")
        .statusIsError()
        .containsOutput("No Members Found");
  }

  @Test
  public void defaultIndexType() throws Exception {
    DistributedMember member = Mockito.mock(DistributedMember.class);
    Mockito.doReturn(Collections.singleton(member)).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());

    ArgumentCaptor<RegionConfig.Index> indexTypeCaptor =
        ArgumentCaptor.forClass(RegionConfig.Index.class);
    gfshParser.executeAndAssertThat(command,
        "create index --name=abc --expression=abc --region=abc");

    Mockito.verify(command).executeAndGetFunctionResult(ArgumentMatchers.any(), indexTypeCaptor.capture(),
        ArgumentMatchers.eq(Collections.singleton(member)));

    Assertions.assertThat(indexTypeCaptor.getValue().getType()).isEqualTo("range");
  }

  @Test
  public void getValidRegionName() {
    // the existing configuration has a region named /regionA.B
    Mockito.doReturn(Collections.singleton("A")).when(command).getGroupsContainingRegion(cms,
        "/regionA.B");
    Mockito.when(cms.list(ArgumentMatchers.any(Region.class))).thenReturn(new ClusterManagementListResult<>());

    assertThat(command.getValidRegionName("regionB", cms)).isEqualTo("regionB");
    assertThat(command.getValidRegionName("/regionB", cms)).isEqualTo("/regionB");
    assertThat(command.getValidRegionName("/regionB b", cms)).isEqualTo("/regionB");
    assertThat(command.getValidRegionName("/regionB.entrySet()", cms))
        .isEqualTo("/regionB");
    assertThat(command.getValidRegionName("/regionA.B.entrySet() A", cms))
        .isEqualTo("/regionA.B");
    assertThat(command.getValidRegionName("/regionA.fieldName.entrySet() B", cms))
        .isEqualTo("/regionA");
  }

  @Test
  public void commandWithGroup() {
    Mockito.doReturn(ccService).when(command).getConfigurationPersistenceService();
    Mockito.doReturn(Sets.newHashSet("group1", "group2")).when(command).getGroupsContainingRegion(ArgumentMatchers.any(),
        ArgumentMatchers.any());

    Mockito.doReturn(Collections.singleton(Mockito.mock(DistributedMember.class))).when(command).findMembers(ArgumentMatchers.any(),
        ArgumentMatchers.any());

    CliFunctionResult result = new CliFunctionResult("member", true, "reason");
    Mockito.doReturn(Collections.singletonList(result)).when(command).executeAndGetFunctionResult(ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any());

    gfshParser.executeAndAssertThat(command,
        "create index --name=index --expression=abc --region=/regionA --groups=group1,group2")
        .statusIsSuccess();

    Mockito.verify(ccService).updateCacheConfig(ArgumentMatchers.eq("group1"), ArgumentMatchers.any());
    Mockito.verify(ccService).updateCacheConfig(ArgumentMatchers.eq("group2"), ArgumentMatchers.any());
  }

  @Test
  public void commandWithWrongGroup() {
    Mockito.doReturn(ccService).when(command).getConfigurationPersistenceService();
    Mockito.doReturn(Sets.newHashSet("group1", "group2")).when(command).getGroupsContainingRegion(ArgumentMatchers.any(),
        ArgumentMatchers.any());

    Mockito.doReturn(Collections.singleton(Mockito.mock(DistributedMember.class))).when(command).findMembers(ArgumentMatchers.any(),
        ArgumentMatchers.any());

    gfshParser.executeAndAssertThat(command,
        "create index --name=index --expression=abc --region=/regionA --groups=group1,group3")
        .statusIsError()
        .containsOutput("Region /regionA does not exist in some of the groups");

    Mockito.verify(ccService, Mockito.never()).updateCacheConfig(ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void csServiceIsDisabled() throws Exception {
    Mockito.doReturn(null).when(command).getConfigurationPersistenceService();
    Set<DistributedMember> targetMembers = Collections.singleton(Mockito.mock(DistributedMember.class));
    Mockito.doReturn(targetMembers).when(command).findMembers(ArgumentMatchers.any(),
        ArgumentMatchers.any());
    CliFunctionResult result = new CliFunctionResult("member", true, "result:xyz");
    Mockito.doReturn(Collections.singletonList(result)).when(command).executeAndGetFunctionResult(ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any());

    gfshParser.executeAndAssertThat(command,
        "create index --name=index --expression=abc --region=/regionA")
        .statusIsSuccess()
        .containsOutput("result:xyz")
        .containsOutput(
            "Cluster configuration service is not running. Configuration change is not persisted.");

    Mockito.verify(command).executeAndGetFunctionResult(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.eq(targetMembers));
  }

  @Test
  public void commandWithMember() throws Exception {
    Mockito.doReturn(ccService).when(command).getConfigurationPersistenceService();
    Set<DistributedMember> targetMembers = Collections.singleton(Mockito.mock(DistributedMember.class));
    Mockito.doReturn(targetMembers).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());
    CliFunctionResult result = new CliFunctionResult("member", true, "result:xyz");
    Mockito.doReturn(Collections.singletonList(result)).when(command).executeAndGetFunctionResult(ArgumentMatchers.any(),
        ArgumentMatchers.any(), ArgumentMatchers.any());

    gfshParser.executeAndAssertThat(command,
        "create index --name=index --expression=abc --region=/regionA --member=member")
        .statusIsSuccess()
        .containsOutput("result:xyz")
        .containsOutput(
            "Configuration change is not persisted because the command is executed on specific member.");

    Mockito.verify(ccService, Mockito.never()).updateCacheConfig(ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void regionBelongsToCluster() throws Exception {
    Mockito.doReturn(ccService).when(command).getConfigurationPersistenceService();
    Region region = Mockito.mock(Region.class);
    ClusterManagementListResult listResult = Mockito.mock(ClusterManagementListResult.class);
    Mockito.when(cms.list(ArgumentMatchers.any(Region.class))).thenReturn(listResult);
    Mockito.when(listResult.getConfigResult()).thenReturn(Collections.singletonList(region));

    Mockito.doReturn(Sets.newHashSet((String) null)).when(command).getGroupsContainingRegion(ArgumentMatchers.any(),
        ArgumentMatchers.any());
    Mockito.doReturn(Collections.emptySet()).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());

    gfshParser.executeAndAssertThat(command,
        "create index --name=index --expression=abc --region=/regionA")
        .containsOutput("No Members Found");


    Mockito.verify(command).findMembers(new String[] {}, null);

  }
}
