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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import org.apache.geode.cache.execute.ResultCollector;
import org.apache.geode.cache.query.IndexType;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.distributed.internal.InternalConfigurationPersistenceService;
import org.apache.geode.management.internal.cli.functions.CliFunctionResult;
import org.apache.geode.test.junit.rules.GfshParserRule;

public class CreateDefinedIndexesCommandTest {
  @Rule
  public GfshParserRule gfshParser = new GfshParserRule();

  private ResultCollector resultCollector;
  private CreateDefinedIndexesCommand command;

  @Before
  public void setUp() throws Exception {
    IndexDefinition.indexDefinitions.clear();
    resultCollector = Mockito.mock(ResultCollector.class);
    command = Mockito.spy(CreateDefinedIndexesCommand.class);
    Mockito.doReturn(resultCollector).when(command).executeFunction(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(Set.class));
  }

  @Test
  public void noDefinitions() throws Exception {
    gfshParser.executeAndAssertThat(command, "create defined indexes")
        .statusIsSuccess()
        .containsOutput("No indexes defined");
  }

  @Test
  public void noMembers() throws Exception {
    IndexDefinition.addIndex("indexName", "indexedExpression", "TestRegion", IndexType.FUNCTIONAL);
    Mockito.doReturn(Collections.EMPTY_SET).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());
    gfshParser.executeAndAssertThat(command, "create defined indexes")
        .statusIsError()
        .containsOutput("No Members Found");
  }

  @Test
  public void creationFailure() throws Exception {
    DistributedMember member = Mockito.mock(DistributedMember.class);
    Mockito.when(member.getId()).thenReturn("memberId");
    InternalConfigurationPersistenceService mockService =
        Mockito.mock(InternalConfigurationPersistenceService.class);

    Mockito.doReturn(mockService).when(command).getConfigurationPersistenceService();
    Mockito.doReturn(Collections.singleton(member)).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());
    Mockito.doReturn(Arrays.asList(new CliFunctionResult(member.getId(), new Exception("MockException"),
        "Exception Message."))).when(resultCollector).getResult();

    IndexDefinition.addIndex("index1", "value1", "TestRegion", IndexType.FUNCTIONAL);
    gfshParser.executeAndAssertThat(command, "create defined indexes").statusIsError();
  }

  @Test
  public void creationSuccess() throws Exception {
    DistributedMember member = Mockito.mock(DistributedMember.class);
    Mockito.when(member.getId()).thenReturn("memberId");
    InternalConfigurationPersistenceService mockService =
        Mockito.mock(InternalConfigurationPersistenceService.class);

    Mockito.doReturn(mockService).when(command).getConfigurationPersistenceService();
    Mockito.doReturn(Collections.singleton(member)).when(command).findMembers(ArgumentMatchers.any(), ArgumentMatchers.any());
    List<String> indexes = new ArrayList<>();
    indexes.add("index1");
    Mockito.doReturn(Arrays.asList(new CliFunctionResult(member.getId(), indexes))).when(resultCollector)
        .getResult();

    IndexDefinition.addIndex("index1", "value1", "TestRegion", IndexType.FUNCTIONAL);
    gfshParser.executeAndAssertThat(command, "create defined indexes").statusIsSuccess();

    Mockito.verify(command, Mockito.times(0)).updateConfigForGroup(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
  }

  @Test
  public void multipleIndexesOnMultipleRegions() throws Exception {
    DistributedMember member1 = Mockito.mock(DistributedMember.class);
    DistributedMember member2 = Mockito.mock(DistributedMember.class);
    Mockito.when(member1.getId()).thenReturn("memberId_1");
    Mockito.when(member2.getId()).thenReturn("memberId_2");

    InternalConfigurationPersistenceService mockService =
        Mockito.mock(InternalConfigurationPersistenceService.class);
    CliFunctionResult resultMember1 =
        new CliFunctionResult(member1.getId(), Arrays.asList("index1", "index2"));
    CliFunctionResult resultMember2 =
        new CliFunctionResult(member2.getId(), Arrays.asList("index1", "index2"));

    Mockito.doReturn(mockService).when(command).getConfigurationPersistenceService();
    Mockito.doReturn(new HashSet<>(Arrays.asList(member1, member2))).when(command).findMembers(ArgumentMatchers.any(),
        ArgumentMatchers.any());
    Mockito.doReturn(Arrays.asList(resultMember1, resultMember2)).when(resultCollector).getResult();

    IndexDefinition.addIndex("index1", "value1", "TestRegion1", IndexType.FUNCTIONAL);
    IndexDefinition.addIndex("index2", "value2", "TestRegion2", IndexType.FUNCTIONAL);

    gfshParser.executeAndAssertThat(command, "create defined indexes").statusIsSuccess()
        .hasTableSection()
        .hasColumn("Status")
        .containsExactly("OK", "OK");
  }
}
