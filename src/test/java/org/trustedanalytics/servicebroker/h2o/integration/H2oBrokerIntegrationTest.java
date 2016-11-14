/**
 * Copyright (c) 2015 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.trustedanalytics.servicebroker.h2o.integration;

import static com.jayway.awaitility.Awaitility.with;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;

import org.trustedanalytics.cfbroker.store.api.BrokerStore;
import org.trustedanalytics.cfbroker.store.api.Location;
import org.trustedanalytics.hadoop.config.ConfigurationHelper;
import org.trustedanalytics.hadoop.config.ConfigurationHelperImpl;
import org.trustedanalytics.hadoop.config.ConfigurationLocator;
import org.trustedanalytics.servicebroker.h2o.Application;
import org.trustedanalytics.servicebroker.h2o.config.ExternalConfiguration;
import org.trustedanalytics.servicebroker.h2o.service.CfBrokerRequestsFactory;
import org.trustedanalytics.servicebroker.h2o.tapcontainerbroker.ContainerBrokerOperations;
import org.trustedanalytics.servicebroker.h2oprovisioner.rest.api.H2oCredentials;
import org.trustedanalytics.servicebroker.h2oprovisioner.rest.api.H2oProvisionerRequestData;
import org.trustedanalytics.servicebroker.h2oprovisioner.rest.api.H2oProvisionerRestApi;

import com.jayway.awaitility.core.ConditionFactory;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceBindingService;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Map;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {Application.class, TestConfig.class})
@IntegrationTest
@ActiveProfiles("test")
public class H2oBrokerIntegrationTest {

  final String USER_TOKEN = "some-user-token";
  final H2oCredentials CREDENTIALS = new H2oCredentials("http://10.0.0.1", "54321", "user", "pass");

  @Autowired
  private ExternalConfiguration conf;

  @Autowired
  private ServiceInstanceService instanceService;

  @Autowired
  private ServiceInstanceBindingService bindingService;

  @Autowired
  public BrokerStore<H2oCredentials> credentialsStore;

  @Autowired
  private ContainerBrokerOperations containerBrokerOperations;

  @Autowired
  private H2oProvisionerRestApi h2oProvisionerRestApi;

  private Map<String, String> yarnConfig;

  private ContainerBrokerMock containerBrokerMock;

  @Before
  public void setup() throws IOException {
    ConfigurationHelper confHelper = ConfigurationHelperImpl.getInstance();
    reset(h2oProvisionerRestApi);
    yarnConfig =
        confHelper.getConfigurationFromJson(conf.getYarnConfig(), ConfigurationLocator.HADOOP);
    containerBrokerMock = (ContainerBrokerMock)containerBrokerOperations;
  }

  @Test
  public void testCreateServiceInstance_success_shouldReturnCreatedInstanceAndStoreCredentials()
      throws Exception {

    // arrange
    final String INSTANCE_ID = "instanceId0";
    when(h2oProvisionerRestApi.createH2oInstance(eq(INSTANCE_ID), eq(conf.getH2oMapperNodes()),
        eq(conf.getH2oMapperMemory()), eq(true), any(H2oProvisionerRequestData.class)))
            .thenReturn(new ResponseEntity<>(CREDENTIALS, HttpStatus.OK));

    // act
    CreateServiceInstanceRequest request =
        CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN);
    ServiceInstance createdInstance = instanceService.createServiceInstance(request);

    // assert
    assertThat(createdInstance.getServiceInstanceId(), equalTo(INSTANCE_ID));

    freeze().until(() -> credentialsStore.getById(Location.newInstance(INSTANCE_ID)).get(),
        equalTo(CREDENTIALS));
    ContainerBrokerMock.ContainerBrokerCallInfo callInfo =
        containerBrokerMock.getCallHistory().get(containerBrokerMock.getCallHistory().size() - 1);
    assertThat(callInfo.methodName, equalTo("addExpose"));
    assertThat(callInfo.instanceId, equalTo(INSTANCE_ID));
    assertThat(callInfo.body, equalTo("{\"ports\":[54321],\"hostname\":\"test-service-name\",\"ip\":\"10.0.0.1\"}"));

    verify(h2oProvisionerRestApi).createH2oInstance(eq(INSTANCE_ID), eq(conf.getH2oMapperNodes()),
            eq(conf.getH2oMapperMemory()), eq(true), eq(new H2oProvisionerRequestData(yarnConfig, USER_TOKEN)));
  }

  @Test
  public void testDeleteServiceInstance_success_shouldReturnRemovedInstance() throws Exception {
    // arrange
    final String INSTANCE_ID = "instanceId1";
    when(h2oProvisionerRestApi.createH2oInstance(eq(INSTANCE_ID), eq(conf.getH2oMapperNodes()),
        eq(conf.getH2oMapperMemory()), eq(true), any(H2oProvisionerRequestData.class)))
            .thenReturn(new ResponseEntity<>(CREDENTIALS, HttpStatus.OK));
    when(h2oProvisionerRestApi.deleteH2oInstance(INSTANCE_ID, yarnConfig, true))
        .thenReturn(new ResponseEntity<>("test-job-id", HttpStatus.OK));
    ServiceInstance instance = instanceService
        .createServiceInstance(CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN));

    // act
    ServiceInstance removedInstance = instanceService
        .deleteServiceInstance(new DeleteServiceInstanceRequest(instance.getServiceInstanceId(),
            instance.getServiceDefinitionId(), instance.getPlanId()));

    // assert
    verify(h2oProvisionerRestApi, times(1)).deleteH2oInstance(INSTANCE_ID, yarnConfig, true);
    assertThat(instance.getServiceInstanceId(), equalTo(removedInstance.getServiceInstanceId()));
  }

  @Test
  public void testDeleteServiceInstance_withoutYarnJob_ShouldReturnRemovedInstance() throws Exception {
    // arrange
    final String INSTANCE_ID = "instanceId2";
    when(h2oProvisionerRestApi.createH2oInstance(eq(INSTANCE_ID), eq(conf.getH2oMapperNodes()),
            eq(conf.getH2oMapperMemory()), eq(true), any(H2oProvisionerRequestData.class)))
            .thenReturn(new ResponseEntity<>(CREDENTIALS, HttpStatus.OK));
    when(h2oProvisionerRestApi.deleteH2oInstance(INSTANCE_ID, yarnConfig, true))
            .thenReturn(new ResponseEntity<>("Some kind of warning...", HttpStatus.GONE));
    ServiceInstance instance = instanceService
            .createServiceInstance(CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN));

    // act
    ServiceInstance removedInstance = instanceService
        .deleteServiceInstance(new DeleteServiceInstanceRequest(instance.getServiceInstanceId(),
            instance.getServiceDefinitionId(), instance.getPlanId()));

    // assert
    verify(h2oProvisionerRestApi, times(1)).deleteH2oInstance(INSTANCE_ID, yarnConfig, true);
    assertThat(instance.getServiceInstanceId(), equalTo(removedInstance.getServiceInstanceId()));
  }

  @Test
  public void testCreateInstanceAndBinding_success_shouldReturnCredentialsMap() throws Exception {
    // arrange
    final String INSTANCE_ID = "instanceId3";
    final String BINDING_ID = "bindingId3";
    when(h2oProvisionerRestApi.createH2oInstance(eq(INSTANCE_ID), eq(conf.getH2oMapperNodes()),
            eq(conf.getH2oMapperMemory()), eq(true), any(H2oProvisionerRequestData.class)))
            .thenReturn(new ResponseEntity<>(CREDENTIALS, HttpStatus.OK));
    instanceService
        .createServiceInstance(CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN));
    freeze().until(() -> credentialsStore.getById(Location.newInstance(INSTANCE_ID)).isPresent());

    // act
    CreateServiceInstanceBindingRequest request =
        CfBrokerRequestsFactory.getCreateServiceBindingRequest(INSTANCE_ID, BINDING_ID);
    ServiceInstanceBinding createdBinding = bindingService.createServiceInstanceBinding(request);

    // assert
    Map<String, Object> credentials = createdBinding.getCredentials();
    assertThat(createdBinding.getServiceInstanceId(), equalTo(INSTANCE_ID));
    assertThat(credentials.get("hostname"), equalTo("http://10.0.0.1"));
    assertThat(credentials.get("port"), equalTo("54321"));
    assertThat(credentials.get("username"), equalTo("user"));
    assertThat(credentials.get("password"), equalTo("pass"));
  }

  @Test
  public void testDeleteServiceBinding_success_shouldReturnRemovedInstance() throws Exception {
    // arrange
    final String INSTANCE_ID = "instanceId4";
    final String BINDING_ID = "bindingId4";
    when(h2oProvisionerRestApi.createH2oInstance(eq(INSTANCE_ID), eq(conf.getH2oMapperNodes()),
        eq(conf.getH2oMapperMemory()), eq(true), any(H2oProvisionerRequestData.class)))
            .thenReturn(new ResponseEntity<>(CREDENTIALS, HttpStatus.OK));

    CreateServiceInstanceRequest createInstanceReq =
        CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN);
    ServiceInstance createdInstance = instanceService.createServiceInstance(createInstanceReq);
    freeze().until(() -> credentialsStore.getById(Location.newInstance(INSTANCE_ID)).isPresent());

    CreateServiceInstanceBindingRequest bindReq =
        CfBrokerRequestsFactory.getCreateServiceBindingRequest(INSTANCE_ID, BINDING_ID);

    bindingService.createServiceInstanceBinding(bindReq);

    // act
    DeleteServiceInstanceBindingRequest request =
        new DeleteServiceInstanceBindingRequest(bindReq.getBindingId(), createdInstance,
            createInstanceReq.getServiceDefinitionId(), bindReq.getPlanId());
    ServiceInstanceBinding removedBinding = bindingService.deleteServiceInstanceBinding(request);

    // assert
    assertThat(removedBinding.getId(), equalTo(BINDING_ID));
  }

  private static ConditionFactory freeze() {
    return with().pollInterval(30, MILLISECONDS).await().atMost(200, MILLISECONDS);
  }
}
