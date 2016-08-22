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

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;

import org.trustedanalytics.hadoop.config.ConfigurationHelper;
import org.trustedanalytics.hadoop.config.ConfigurationHelperImpl;
import org.trustedanalytics.hadoop.config.ConfigurationLocator;
import org.trustedanalytics.servicebroker.h2o.Application;
import org.trustedanalytics.servicebroker.h2o.config.ExternalConfiguration;
import org.trustedanalytics.servicebroker.h2o.service.CfBrokerRequestsFactory;
import org.trustedanalytics.servicebroker.h2o.config.ServiceInstanceBindingServiceMock;
import org.trustedanalytics.servicebroker.h2oprovisioner.rest.api.H2oProvisionerRequestData;
import org.trustedanalytics.servicebroker.h2oprovisioner.rest.api.H2oProvisionerRestApi;

import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.model.ServiceInstanceBinding;
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

  @Autowired
  private ExternalConfiguration conf;

  @Autowired
  private ServiceInstanceService instanceService;

  @Autowired
  private ServiceInstanceBindingServiceMock bindingService;

  @Autowired
  private H2oProvisionerRestApi h2oProvisionerRestApi;

  private Map<String, String> yarnConfig;

  @Before
  public void setup() throws IOException {
    ConfigurationHelper confHelper = ConfigurationHelperImpl.getInstance();
    reset(h2oProvisionerRestApi);
    yarnConfig =
        confHelper.getConfigurationFromJson(conf.getYarnConfig(), ConfigurationLocator.HADOOP);
  }

  @Test
  public void testCreateServiceInstance_success_shouldReturnCreatedInstance()
      throws Exception {

    // arrange
    final String INSTANCE_ID = "instanceId0";
    when(h2oProvisionerRestApi.createH2oInstance(eq(INSTANCE_ID), eq(conf.getH2oMapperNodes()),
        eq(conf.getH2oMapperMemory()), eq(true), any(H2oProvisionerRequestData.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

    // act
    CreateServiceInstanceRequest request =
        CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN);
    ServiceInstance createdInstance = instanceService.createServiceInstance(request);

    // assert
    assertThat(createdInstance, equalTo(null));
  }

  @Test
  public void testDeleteServiceInstance_success_shouldReturnRemovedInstance() throws Exception {
    // arrange
    final String INSTANCE_ID = "instanceId1";
    when(h2oProvisionerRestApi.createH2oInstance(eq(INSTANCE_ID), eq(conf.getH2oMapperNodes()),
        eq(conf.getH2oMapperMemory()), eq(true), any(H2oProvisionerRequestData.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));
    when(h2oProvisionerRestApi.deleteH2oInstance(INSTANCE_ID, yarnConfig, true))
        .thenReturn(new ResponseEntity<>("test-job-id", HttpStatus.OK));
    ServiceInstance instance = instanceService
        .createServiceInstance(CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN));

    // act
    ServiceInstance removedInstance = instanceService
        .deleteServiceInstance(new DeleteServiceInstanceRequest(INSTANCE_ID, INSTANCE_ID, INSTANCE_ID));

    // assert
    assertThat(null, equalTo(removedInstance));
  }

  @Test
  public void testDeleteServiceInstance_withoutYarnJob_ShouldReturnRemovedInstance() throws Exception {
    // arrange
    final String INSTANCE_ID = "instanceId2";
    when(h2oProvisionerRestApi.createH2oInstance(eq(INSTANCE_ID), eq(conf.getH2oMapperNodes()),
            eq(conf.getH2oMapperMemory()), eq(true), any(H2oProvisionerRequestData.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));
    when(h2oProvisionerRestApi.deleteH2oInstance(INSTANCE_ID, yarnConfig, true))
            .thenReturn(new ResponseEntity<>("Some kind of warning...", HttpStatus.GONE));
    ServiceInstance instance = instanceService
            .createServiceInstance(CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN));

    // act
    ServiceInstance removedInstance = instanceService
        .deleteServiceInstance(new DeleteServiceInstanceRequest(INSTANCE_ID, INSTANCE_ID, INSTANCE_ID));

    // assert
    assertThat(null, equalTo(removedInstance));
  }


  @Test
  public void testDeleteServiceBinding_success_shouldReturnRemovedInstance() throws Exception {
    // arrange
    final String INSTANCE_ID = "instanceId4";
    final String BINDING_ID = "bindingId4";
    when(h2oProvisionerRestApi.createH2oInstance(eq(INSTANCE_ID), eq(conf.getH2oMapperNodes()),
        eq(conf.getH2oMapperMemory()), eq(true), any(H2oProvisionerRequestData.class)))
            .thenReturn(new ResponseEntity<>(HttpStatus.OK));

    CreateServiceInstanceRequest createInstanceReq =
        CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN);
    ServiceInstance createdInstance = instanceService.createServiceInstance(createInstanceReq);

    CreateServiceInstanceBindingRequest bindReq =
        CfBrokerRequestsFactory.getCreateServiceBindingRequest(INSTANCE_ID, BINDING_ID);

    bindingService.createServiceInstanceBinding(bindReq);

    // act
    DeleteServiceInstanceBindingRequest request =
        new DeleteServiceInstanceBindingRequest(bindReq.getBindingId(), createdInstance,
            createInstanceReq.getServiceDefinitionId(), bindReq.getPlanId());
    ServiceInstanceBinding removedBinding = bindingService.deleteServiceInstanceBinding(request);

    // assert
    assertThat(removedBinding, equalTo(null));
  }
}
