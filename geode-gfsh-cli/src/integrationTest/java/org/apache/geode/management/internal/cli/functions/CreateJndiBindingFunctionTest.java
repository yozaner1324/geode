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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.naming.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

import org.apache.geode.cache.configuration.JndiBindingsType;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.ResultSender;
import org.apache.geode.distributed.DistributedSystem;
import org.apache.geode.internal.datasource.ConfigProperty;
import org.apache.geode.internal.jndi.JNDIInvoker;
import org.apache.geode.internal.logging.LocalLogWriter;
import org.apache.geode.internal.util.DriverJarUtil;
import org.apache.geode.management.internal.cli.commands.CreateJndiBindingCommand;
import org.apache.geode.test.junit.categories.GfshTest;

@Category({GfshTest.class})
public class CreateJndiBindingFunctionTest {

  private CreateJndiBindingFunction createBindingFunction;
  private FunctionContext<Object[]> context;
  private DistributedSystem distributedSystem;
  private ResultSender resultSender;
  private ArgumentCaptor<CliFunctionResult> resultCaptor;

  @Before
  public void setup() {
    createBindingFunction = Mockito.spy(new CreateJndiBindingFunction());
    context = Mockito.mock(FunctionContext.class);
    distributedSystem = Mockito.mock(DistributedSystem.class);
    resultSender = Mockito.mock(ResultSender.class);
    resultCaptor = ArgumentCaptor.forClass(CliFunctionResult.class);

    Mockito.when(distributedSystem.getLogWriter()).thenReturn(new LocalLogWriter(Level.FINE.intValue()));

    JNDIInvoker.mapTransactions(distributedSystem);
  }

  @Test
  public void createJndiBindingIsSuccessful() throws Exception {
    JndiBindingsType.JndiBinding config = new JndiBindingsType.JndiBinding();
    config.setJndiName("jndi1");
    config.setType(CreateJndiBindingCommand.DATASOURCE_TYPE.SIMPLE.getType());
    config.setJdbcDriverClass("org.apache.derby.jdbc.EmbeddedDriver");
    config.setConnectionUrl("jdbc:derby:newDB;create=true");
    Object[] arguments = new Object[] {config, false};
    Mockito.when(context.getArguments()).thenReturn(arguments);
    Mockito.when(context.getMemberName()).thenReturn("mock-member");
    Mockito.when(context.getResultSender()).thenReturn(resultSender);

    createBindingFunction.execute(context);

    Mockito.verify(resultSender).lastResult(resultCaptor.capture());
    CliFunctionResult result = resultCaptor.getValue();
    assertThat(result.isSuccessful()).isTrue();
    assertThat(result.toString()).contains("jndi-binding");

    Context ctx = JNDIInvoker.getJNDIContext();
    Map<String, String> bindings = JNDIInvoker.getBindingNamesRecursively(ctx);

    Assertions.assertThat(bindings.keySet()).containsExactlyInAnyOrder("java:jndi1", "java:UserTransaction",
        "java:TransactionManager");
  }

  @Test
  public void createDataSourceIsSuccessful() throws Exception {
    JndiBindingsType.JndiBinding config = new JndiBindingsType.JndiBinding();
    final String NAME = "jndi1";
    final String MEMBER = "mock-member";
    config.setJndiName(NAME);
    config.setType(CreateJndiBindingCommand.DATASOURCE_TYPE.SIMPLE.getType());
    config.setJdbcDriverClass(null);
    config.setConnectionUrl("jdbc:derby:newDB;create=true");
    Object[] arguments = new Object[] {config, true};
    Mockito.when(context.getArguments()).thenReturn(arguments);
    Mockito.when(context.getMemberName()).thenReturn(MEMBER);
    Mockito.when(context.getResultSender()).thenReturn(resultSender);

    createBindingFunction.execute(context);

    Mockito.verify(resultSender).lastResult(resultCaptor.capture());
    CliFunctionResult result = resultCaptor.getValue();
    assertThat(result.isSuccessful()).isTrue();
    assertThat(result.toString())
        .contains("Created data-source \"" + NAME + "\" on \"" + MEMBER + "\".");

    Context ctx = JNDIInvoker.getJNDIContext();
    Map<String, String> bindings = JNDIInvoker.getBindingNamesRecursively(ctx);

    Assertions.assertThat(bindings.keySet()).containsExactlyInAnyOrder("java:jndi1", "java:UserTransaction",
        "java:TransactionManager");
  }

  @Test
  public void createDataSourceIsSuccessfulWithJarSpecified() throws Exception {
    DriverJarUtil driverJarUtil = Mockito.mock(DriverJarUtil.class);
    JndiBindingsType.JndiBinding config = Mockito.spy(new JndiBindingsType.JndiBinding());
    final String NAME = "jndi1";
    final String MEMBER = "mock-member";
    final String DRIVER_CLASS_NAME = "org.apache.derby.jdbc.EmbeddedDriver";
    config.setJndiName(NAME);
    config.setType(CreateJndiBindingCommand.DATASOURCE_TYPE.SIMPLE.getType());
    config.setJdbcDriverClass(DRIVER_CLASS_NAME);
    config.setConnectionUrl("jdbc:derby:newDB;create=true");
    Object[] arguments = new Object[] {config, true};
    Mockito.when(context.getArguments()).thenReturn(arguments);
    Mockito.when(context.getMemberName()).thenReturn(MEMBER);
    Mockito.when(context.getResultSender()).thenReturn(resultSender);
    Mockito.doReturn(driverJarUtil).when(createBindingFunction).getDriverJarUtil();

    createBindingFunction.execute(context);

    Mockito.verify(resultSender).lastResult(resultCaptor.capture());
    CliFunctionResult result = resultCaptor.getValue();
    assertThat(result.isSuccessful()).isTrue();
    assertThat(result.toString())
        .contains("Created data-source \"" + NAME + "\" on \"" + MEMBER + "\".");
    Mockito.verify(config).setJdbcDriverClass(DRIVER_CLASS_NAME);

    Context ctx = JNDIInvoker.getJNDIContext();
    Map<String, String> bindings = JNDIInvoker.getBindingNamesRecursively(ctx);

    Assertions.assertThat(bindings.keySet()).containsExactlyInAnyOrder("java:jndi1", "java:UserTransaction",
        "java:TransactionManager");
  }

  @Test
  public void convert() {
    JndiBindingsType.JndiBinding.ConfigProperty propA =
        new JndiBindingsType.JndiBinding.ConfigProperty("name", "type", "value");

    List<ConfigProperty> converted =
        CreateJndiBindingFunction.convert(Collections.singletonList(propA));
    Assertions.assertThat(converted).hasSize(1);
    ConfigProperty propB = converted.get(0);
    Assertions.assertThat(propB.getName()).isEqualTo("name");
    Assertions.assertThat(propB.getType()).isEqualTo("type");
    Assertions.assertThat(propB.getValue()).isEqualTo("value");
  }
}
