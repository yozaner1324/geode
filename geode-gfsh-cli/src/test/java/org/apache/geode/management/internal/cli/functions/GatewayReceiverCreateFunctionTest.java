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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.configuration.GatewayReceiverConfig;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.ResultSender;
import org.apache.geode.cache.wan.GatewayReceiver;

public class GatewayReceiverCreateFunctionTest {

  private GatewayReceiverCreateFunction function = Mockito.mock(GatewayReceiverCreateFunction.class);

  private FunctionContext context = Mockito.mock(FunctionContext.class);
  private Cache cache = Mockito.mock(Cache.class);
  private GatewayReceiverConfig args = Mockito.mock(GatewayReceiverConfig.class);
  private GatewayReceiver receiver = Mockito.mock(GatewayReceiver.class);
  private ResultSender resultSender = Mockito.mock(ResultSender.class);

  @Before
  public void setup() {
    Mockito.doReturn(cache).when(context).getCache();
    Mockito.doReturn(new Object[] {args, Boolean.FALSE}).when(context).getArguments();
    Mockito.doReturn("server-1").when(context).getMemberName();
    Mockito.doReturn(resultSender).when(context).getResultSender();

    Mockito.doCallRealMethod().when(function).execute(context);
    Mockito.doReturn(false).when(function).gatewayReceiverExists(ArgumentMatchers.any());
    Mockito.doReturn(receiver).when(function).createGatewayReceiver(cache, args);

    Mockito.doReturn(5555).when(receiver).getPort();
  }

  @Test
  public void testFunctionSuccessResult() {
    function.execute(context);
    ArgumentCaptor<Object> resultObject = ArgumentCaptor.forClass(Object.class);
    Mockito.verify(resultSender, Mockito.times(1)).lastResult(resultObject.capture());

    CliFunctionResult result = (CliFunctionResult) resultObject.getValue();

    assertThat(result.getStatusMessage()).contains("5555");
  }
}
