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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;

import org.apache.geode.cache.execute.Execution;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.ResultCollector;
import org.apache.geode.cache.execute.ResultSender;
import org.apache.geode.cache.query.RegionNotFoundException;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.distributed.internal.InternalDistributedSystem;
import org.apache.geode.distributed.internal.membership.InternalDistributedMember;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.InternalCacheForClientAccess;
import org.apache.geode.internal.security.SecurityService;
import org.apache.geode.security.AuthenticationRequiredException;

public class UserFunctionExecutionTest {
  private Object[] arguments;
  private Execution execution;
  private Function userFunction;
  private UserFunctionExecution function;
  private SecurityService securityService;
  private ResultCollector resultCollector;
  private FunctionContext<Object[]> context;
  private ResultSender<Object> resultSender;
  private InternalCacheForClientAccess filterCache;
  private ArgumentCaptor<CliFunctionResult> resultCaptor;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() throws Exception {
    execution = Mockito.mock(Execution.class);
    userFunction = Mockito.mock(Function.class);
    context = Mockito.mock(FunctionContext.class);
    resultSender = Mockito.mock(ResultSender.class);
    function = Mockito.spy(UserFunctionExecution.class);
    securityService = Mockito.mock(SecurityService.class);
    resultCollector = Mockito.mock(ResultCollector.class);
    filterCache = Mockito.mock(InternalCacheForClientAccess.class);
    arguments = new Object[] {"TestFunction", "key1,key2", "TestResultCollector", "arg1,arg2",
        "/TestRegion", new Properties()};

    Mockito.when(userFunction.getId()).thenReturn("TestFunction");

    InternalCache cache = Mockito.mock(InternalCache.class);
    DistributedSystem distributedSystem = Mockito.mock(InternalDistributedSystem.class);
    DistributedMember distributedMember = Mockito.mock(InternalDistributedMember.class);
    Mockito.when(distributedMember.getId()).thenReturn("MockMemberId");
    Mockito.when(distributedSystem.getDistributedMember()).thenReturn(distributedMember);
    Mockito.when(filterCache.getDistributedSystem()).thenReturn(distributedSystem);

    Mockito.when(cache.getSecurityService()).thenReturn(securityService);
    Mockito.when(cache.getCacheForProcessingClientRequests()).thenReturn(filterCache);
    Mockito.when(context.getCache()).thenReturn(cache);
    Mockito.when(context.getArguments()).thenReturn(arguments);
    Mockito.when(context.getResultSender()).thenReturn(resultSender);

    Mockito.when(execution.withFilter(ArgumentMatchers.any())).thenReturn(execution);
    Mockito.when(execution.setArguments(ArgumentMatchers.any())).thenReturn(execution);
    Mockito.when(execution.withCollector(ArgumentMatchers.any())).thenReturn(execution);
    Mockito.when(execution.execute(ArgumentMatchers.anyString())).thenReturn(resultCollector);

    Mockito.doReturn(false).when(function).loginRequired(securityService);
    Mockito.doReturn(userFunction).when(function).loadFunction("TestFunction");
    Mockito.doReturn(resultCollector).when(function).parseResultCollector("TestResultCollector");
    Mockito.doReturn(execution).when(function).buildExecution(ArgumentMatchers.any(), ArgumentMatchers.any());

    resultCaptor = ArgumentCaptor.forClass(CliFunctionResult.class);
  }

  @Test
  public void testDefaultAttributes() {
    assertThat(function.isHA()).isFalse();
    assertThat(function.getId()).isEqualTo(UserFunctionExecution.ID);
    assertThat(function.getRequiredPermissions(ArgumentMatchers.anyString())).isEmpty();
  }

  @Test
  public void parseArgumentsTest() {
    assertThat(function.parseArguments(null)).isNull();
    assertThat(function.parseArguments("")).isNull();
    assertThat(function.parseArguments("arg1,arg2")).isNotNull()
        .isEqualTo(new String[] {"arg1", "arg2"});
  }

  @Test
  public void parseFiltersTest() {
    assertThat(function.parseFilters(null)).isNotNull().isEmpty();
    assertThat(function.parseFilters("")).isNotNull().isEmpty();
    assertThat(function.parseFilters("key1,key2")).isNotNull().containsOnly("key1", "key2");
  }

  @Test
  public void buildExecutionShouldThrowExceptionWhenRegionIsRequiredButItDoesNotExist()
      throws Exception {
    Mockito.when(filterCache.getRegion("region")).thenReturn(null);
    Mockito.when(function.buildExecution(ArgumentMatchers.any(), ArgumentMatchers.any())).thenCallRealMethod();

    Assertions.assertThatThrownBy(() -> function.buildExecution(filterCache, "region"))
        .isInstanceOf(RegionNotFoundException.class);
  }

  @Test
  public void loginRequiredShouldReturnTrueWhenSubjectIsNull() {
    Mockito.when(securityService.getSubject()).thenReturn(null);
    Mockito.when(function.loginRequired(securityService)).thenCallRealMethod();

    assertThat(function.loginRequired(securityService)).isTrue();
  }

  @Test
  public void loginRequiredShouldReturnTrueWhenSubjectIsNotAuthenticated() {
    Subject subject = Mockito.mock(Subject.class);
    Mockito.when(subject.isAuthenticated()).thenReturn(false);
    Mockito.when(securityService.getSubject()).thenReturn(subject);
    Mockito.when(function.loginRequired(securityService)).thenCallRealMethod();

    assertThat(function.loginRequired(securityService)).isTrue();
  }

  @Test
  public void loginRequiredShouldReturnTrueWhenSecurityServiceFailsToLoadSubject() {
    Mockito.when(function.loginRequired(securityService)).thenCallRealMethod();
    Mockito.doThrow(new AuthenticationRequiredException("Dummy Exception")).when(securityService)
        .getSubject();

    assertThat(function.loginRequired(securityService)).isTrue();
  }

  @Test
  public void loginRequiredShouldReturnFalseWhenSubjectIsAuthenticated() {
    Subject subject = Mockito.mock(Subject.class);
    Mockito.when(subject.isAuthenticated()).thenReturn(true);
    Mockito.when(securityService.getSubject()).thenReturn(subject);
    Mockito.when(function.loginRequired(securityService)).thenCallRealMethod();

    assertThat(function.loginRequired(securityService)).isFalse();
  }

  @Test
  public void executeShouldFailWhenNoArgumentsAreProvided() {
    Mockito.when(context.getArguments()).thenReturn(null);

    function.execute(context);

    Mockito.verify(resultSender, Mockito.times(1)).lastResult(resultCaptor.capture());
    CliFunctionResult result = resultCaptor.getValue();
    assertThat(result.isSuccessful()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("Could not retrieve arguments");
  }

  @Test
  public void executeShouldFailWhenTargetFunctionCanNotBeLoaded() {
    Mockito.doReturn(null).when(function).loadFunction(ArgumentMatchers.anyString());

    function.execute(context);

    Mockito.verify(resultSender, Mockito.times(1)).lastResult(resultCaptor.capture());
    CliFunctionResult result = resultCaptor.getValue();
    assertThat(result.isSuccessful()).isFalse();
    assertThat(result.getStatusMessage())
        .isEqualTo("Function : TestFunction is not registered on member.");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void executeShouldFailWhenResultCollectorCanNotBeInstantiated() throws Exception {
    CliFunctionResult result;

    Mockito.doThrow(new ClassNotFoundException("ClassNotFoundException")).when(function)
        .parseResultCollector(ArgumentMatchers.anyString());
    function.execute(context);
    Mockito.verify(resultSender, Mockito.times(1)).lastResult(resultCaptor.capture());
    result = resultCaptor.getValue();
    assertThat(result.isSuccessful()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo(
        "ResultCollector : TestResultCollector not found. Error : ClassNotFoundException");
    Mockito.reset(resultSender);

    Mockito.doThrow(new IllegalAccessException("IllegalAccessException")).when(function)
        .parseResultCollector(ArgumentMatchers.anyString());
    function.execute(context);
    Mockito.verify(resultSender, Mockito.times(1)).lastResult(resultCaptor.capture());
    result = resultCaptor.getValue();
    assertThat(result.isSuccessful()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo(
        "ResultCollector : TestResultCollector not found. Error : IllegalAccessException");
    Mockito.reset(resultSender);

    Mockito.doThrow(new InstantiationException("InstantiationException")).when(function)
        .parseResultCollector(ArgumentMatchers.anyString());
    function.execute(context);
    Mockito.verify(resultSender, Mockito.times(1)).lastResult(resultCaptor.capture());
    result = resultCaptor.getValue();
    assertThat(result.isSuccessful()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo(
        "ResultCollector : TestResultCollector not found. Error : InstantiationException");
    Mockito.reset(resultSender);
  }

  @Test
  public void executeShouldFailWhenRegionIsSetAsArgumentButItDoesNotExist() throws Exception {
    Mockito.when(function.buildExecution(ArgumentMatchers.any(), ArgumentMatchers.any())).thenCallRealMethod();
    function.execute(context);

    Mockito.verify(resultSender, Mockito.times(1)).lastResult(resultCaptor.capture());
    CliFunctionResult result = resultCaptor.getValue();
    assertThat(result.isSuccessful()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo("/TestRegion does not exist");
  }

  @Test
  public void executeShouldFailWhenExecutorCanNotBeLoaded() throws Exception {
    Mockito.doReturn(null).when(function).buildExecution(ArgumentMatchers.any(), ArgumentMatchers.any());

    function.execute(context);

    Mockito.verify(resultSender, Mockito.times(1)).lastResult(resultCaptor.capture());
    CliFunctionResult result = resultCaptor.getValue();
    assertThat(result.isSuccessful()).isFalse();
    assertThat(result.getStatusMessage()).isEqualTo(
        "While executing function : TestFunction on member : MockMemberId one region : /TestRegion error occurred : Could not retrieve executor");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void executeShouldProperlyConfigureExecutionContext() {
    Set<String> filter = new HashSet<>();
    filter.add("key1");
    filter.add("key2");

    arguments = new Object[] {"TestFunction", "key1,key2", "TestResultCollector", "arg1,arg2",
        "/TestRegion", new Properties()};
    Mockito.when(context.getArguments()).thenReturn(arguments);
    function.execute(context);
    Mockito.verify(execution, Mockito.times(1)).withFilter(filter);
    Mockito.verify(execution, Mockito.times(1)).withCollector(resultCollector);
    Mockito.verify(execution, Mockito.times(1)).setArguments(new String[] {"arg1", "arg2"});
    Mockito.verify(resultSender, Mockito.times(1)).lastResult(resultCaptor.capture());
    CliFunctionResult resultFullArguments = resultCaptor.getValue();
    assertThat(resultFullArguments.isSuccessful()).isTrue();

    Mockito.reset(resultSender);
    Mockito.reset(execution);
    arguments = new Object[] {"TestFunction", "", "", "", "", new Properties()};
    Mockito.when(context.getArguments()).thenReturn(arguments);
    function.execute(context);
    Mockito.verify(execution, Mockito.never()).withFilter(ArgumentMatchers.any());
    Mockito.verify(execution, Mockito.never()).setArguments(ArgumentMatchers.any());
    Mockito.verify(execution, Mockito.never()).withCollector(ArgumentMatchers.any());
    Mockito.verify(resultSender, Mockito.times(1)).lastResult(resultCaptor.capture());
    CliFunctionResult resultNoArguments = resultCaptor.getValue();
    assertThat(resultNoArguments.isSuccessful()).isTrue();
  }

  @Test
  public void executeShouldWorkProperlyForFunctionsWithResults() {
    Mockito.when(userFunction.hasResult()).thenReturn(true);
    Mockito.doReturn(true).when(function).loginRequired(ArgumentMatchers.any());
    Mockito.when(resultCollector.getResult()).thenReturn(Arrays.asList("result1", "result2"));

    function.execute(context);
    Mockito.verify(resultSender, Mockito.times(1)).lastResult(resultCaptor.capture());
    CliFunctionResult result = resultCaptor.getValue();
    assertThat(result.isSuccessful()).isTrue();
    assertThat(result.getStatusMessage()).isEqualTo("[result1, result2]");
    Mockito.verify(securityService, Mockito.times(1)).login(ArgumentMatchers.any());
    Mockito.verify(securityService, Mockito.times(1)).logout();
  }

  @Test
  public void executeShouldWorkProperlyForFunctionsWithoutResults() {
    Mockito.when(userFunction.hasResult()).thenReturn(false);
    Mockito.doReturn(true).when(function).loginRequired(ArgumentMatchers.any());

    function.execute(context);
    Mockito.verify(resultSender, Mockito.times(1)).lastResult(resultCaptor.capture());
    CliFunctionResult result = resultCaptor.getValue();
    assertThat(result.isSuccessful()).isTrue();
    assertThat(result.getStatusMessage()).isEqualTo("[]");
    Mockito.verify(securityService, Mockito.times(1)).login(ArgumentMatchers.any());
    Mockito.verify(securityService, Mockito.times(1)).logout();
  }
}
