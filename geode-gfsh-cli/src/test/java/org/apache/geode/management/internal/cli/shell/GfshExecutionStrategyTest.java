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
package org.apache.geode.management.internal.cli.shell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.shell.core.CommandMarker;

import org.apache.geode.management.cli.CliMetaData;
import org.apache.geode.management.cli.Result;
import org.apache.geode.management.internal.cli.AbstractCliAroundInterceptor;
import org.apache.geode.management.internal.cli.CommandRequest;
import org.apache.geode.management.internal.cli.GfshParseResult;
import org.apache.geode.management.internal.cli.LogWrapper;
import org.apache.geode.management.internal.cli.result.model.ResultModel;

/**
 * GfshExecutionStrategyTest - Includes tests to for GfshExecutionStrategyTest
 */
public class GfshExecutionStrategyTest {
  private static final String COMMAND1_SUCCESS = "Command1 Executed successfully";
  private static final String COMMAND2_SUCCESS = "Command2 Executed successfully";
  private static final String COMMAND3_SUCCESS = "Command3 Executed successfully";
  private static final String COMMAND4_SUCCESS = "Command4 Executed successfully";

  private static final String AFTER_INTERCEPTION_MESSAGE = "After Interception";

  private Gfsh gfsh;
  private GfshParseResult parsedCommand;
  private GfshExecutionStrategy gfshExecutionStrategy;

  @Before
  public void before() {
    gfsh = Mockito.mock(Gfsh.class);
    Mockito.when(gfsh.getGfshFileLogger()).thenReturn(LogWrapper.getInstance(null));
    parsedCommand = Mockito.mock(GfshParseResult.class);
    gfshExecutionStrategy = new GfshExecutionStrategy(gfsh);
  }

  /**
   * tests execute offline command
   */
  @Test
  public void testOfflineCommand() throws Exception {
    Mockito.when(parsedCommand.getMethod()).thenReturn(Commands.class.getDeclaredMethod("offlineCommand"));
    Mockito.when(parsedCommand.getInstance()).thenReturn(new Commands());
    Result result = (Result) gfshExecutionStrategy.execute(parsedCommand);
    assertThat(result.nextLine().trim()).isEqualTo(COMMAND1_SUCCESS);
  }

  @Test
  public void testOfflineCommandThatReturnsResultModel() throws NoSuchMethodException {
    Mockito.when(parsedCommand.getMethod()).thenReturn(Commands.class.getDeclaredMethod("offlineCommand2"));
    Mockito.when(parsedCommand.getInstance()).thenReturn(new Commands());
    Result result = (Result) gfshExecutionStrategy.execute(parsedCommand);
    assertThat(result.nextLine().trim()).isEqualTo(COMMAND3_SUCCESS);
  }

  /**
   * tests execute online command
   */
  @Test
  public void testOnLineCommandWhenGfshisOffLine() throws Exception {
    Mockito.when(parsedCommand.getMethod()).thenReturn(Commands.class.getDeclaredMethod("onlineCommand"));
    Mockito.when(parsedCommand.getInstance()).thenReturn(new Commands());
    Mockito.when(gfsh.isConnectedAndReady()).thenReturn(false);
    Result result = (Result) gfshExecutionStrategy.execute(parsedCommand);
    assertThat(result).isNull();
  }

  @Test
  public void testOnLineCommandWhenGfshisOnLine() throws Exception {
    Mockito.when(parsedCommand.getMethod()).thenReturn(Commands.class.getDeclaredMethod("onlineCommand"));
    Mockito.when(parsedCommand.getInstance()).thenReturn(new Commands());
    Mockito.when(gfsh.isConnectedAndReady()).thenReturn(true);
    OperationInvoker invoker = Mockito.mock(OperationInvoker.class);

    ResultModel offLineResult = new Commands().onlineCommand();
    String jsonResult = offLineResult.toJson();
    Mockito.when(invoker.processCommand(ArgumentMatchers.any(CommandRequest.class))).thenReturn(jsonResult);
    Mockito.when(gfsh.getOperationInvoker()).thenReturn(invoker);
    Result result = (Result) gfshExecutionStrategy.execute(parsedCommand);
    assertThat(result.nextLine().trim()).isEqualTo(COMMAND2_SUCCESS);
  }

  @Test
  public void resolveInterceptorClassName() throws Exception {
    Mockito.when(parsedCommand.getMethod())
        .thenReturn(Commands.class.getDeclaredMethod("interceptedCommand"));
    Mockito.when(parsedCommand.getInstance()).thenReturn(new Commands());
    Mockito.when(gfsh.isConnectedAndReady()).thenReturn(true);
    OperationInvoker invoker = Mockito.mock(OperationInvoker.class);

    ResultModel interceptedResult = new Commands().interceptedCommand();
    String jsonResult = interceptedResult.toJson();
    Mockito.when(invoker.processCommand(ArgumentMatchers.any(CommandRequest.class))).thenReturn(jsonResult);
    Mockito.when(gfsh.getOperationInvoker()).thenReturn(invoker);
    Result result = (Result) gfshExecutionStrategy.execute(parsedCommand);
    assertThat(result.nextLine().trim()).isEqualTo(COMMAND4_SUCCESS);
    Mockito.verify(parsedCommand, Mockito.times(1)).setUserInput(AFTER_INTERCEPTION_MESSAGE);
  }

  /**
   * represents class for dummy methods
   */
  public static class Commands implements CommandMarker {
    @CliMetaData(shellOnly = true)
    public ResultModel offlineCommand() {
      return ResultModel.createInfo(COMMAND1_SUCCESS);
    }

    @CliMetaData(shellOnly = true)
    public ResultModel offlineCommand2() {
      return ResultModel.createInfo(COMMAND3_SUCCESS);
    }

    @CliMetaData(shellOnly = false)
    public ResultModel onlineCommand() {
      return ResultModel.createInfo(COMMAND2_SUCCESS);
    }

    @CliMetaData(shellOnly = false,
        interceptor = "org.apache.geode.management.internal.cli.shell.GfshExecutionStrategyTest$TestInterceptor")
    public ResultModel interceptedCommand() {
      return ResultModel.createInfo(COMMAND4_SUCCESS);
    }
  }

  /*
   * Test interceptor for use in the interceptedCommand
   */
  public static class TestInterceptor extends AbstractCliAroundInterceptor {

    @Override
    public ResultModel preExecution(GfshParseResult parseResult) {

      parseResult.setUserInput(AFTER_INTERCEPTION_MESSAGE);
      return ResultModel.createInfo("Interceptor Result");
    }
  }
}
