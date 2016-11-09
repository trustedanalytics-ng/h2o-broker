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

package org.trustedanalytics.servicebroker.h2o.service;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.MatcherAssertionErrors.assertThat;

import org.trustedanalytics.cfbroker.store.api.BrokerStore;
import org.trustedanalytics.cfbroker.store.api.Location;
import org.trustedanalytics.servicebroker.h2o.nats.NatsNotifier;
import org.trustedanalytics.servicebroker.h2o.nats.ServiceMetadata;
import org.trustedanalytics.servicebroker.h2o.tapcontainerbroker.ContainerBrokerOperations;
import org.trustedanalytics.servicebroker.h2oprovisioner.rest.api.H2oCredentials;

import org.cloudfoundry.community.servicebroker.exception.ServiceBrokerException;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.cloudfoundry.community.servicebroker.service.ServiceInstanceService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class H2oServiceInstanceServiceTest {

  private static final String INSTANCE_ID = "instanceId0";
  private static final String USER_TOKEN = "fake-user-token";

  private H2oServiceInstanceService instanceService;

  @Mock
  private ServiceInstanceService delegateMock;

  @Mock
  private H2oProvisioner h2oProvisioner;

  @Mock
  private BrokerStore<H2oCredentials> credentialsStoreMock;

  @Mock
  private ContainerBrokerOperations containerBrokerOperationsMock;

  @Mock
  private NatsNotifier natsNotifierMock;

  private long provisionerTimeout = 120;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    instanceService = new H2oServiceInstanceService(delegateMock, h2oProvisioner,
        credentialsStoreMock, containerBrokerOperationsMock, natsNotifierMock, provisionerTimeout);
  }

  @Test
  public void createServiceInstance_provisionerAndStoreWorks_instanceCreated() throws Exception {
    // arrange
    CreateServiceInstanceRequest request =
        CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN);
    ServiceInstance expectedInstance = new ServiceInstance(request);
    H2oCredentials expectedCredentials = new H2oCredentials("http://10.0.0.1", "8080", "c", "d");
    String expectedExposeBody = "{\"ports\":[8080],\"hostname\":\"test-service-name\",\"ip\":\"10.0.0.1\"}";

    when(delegateMock.createServiceInstance(request)).thenReturn(expectedInstance);
    when(h2oProvisioner.provisionInstance(INSTANCE_ID, USER_TOKEN)).thenReturn(expectedCredentials);
    doNothing().when(credentialsStoreMock).save(Location.newInstance(INSTANCE_ID),
        expectedCredentials);
    when(containerBrokerOperationsMock.addExpose(INSTANCE_ID, expectedExposeBody))
        .thenReturn(new String[0]);

    // act
    ServiceInstance createdInstance = instanceService.createServiceInstance(request);

    // assert
    assertThat(createdInstance, equalTo(expectedInstance));
    verify(delegateMock, timeout(200)).createServiceInstance(request);
    verify(h2oProvisioner, timeout(200)).provisionInstance(INSTANCE_ID, USER_TOKEN);
    verify(credentialsStoreMock, timeout(200)).save(Location.newInstance(INSTANCE_ID),
        expectedCredentials);
    verify(containerBrokerOperationsMock, timeout(200)).addExpose(INSTANCE_ID, expectedExposeBody);
  }

  @Test
  public void createServiceInstance_emptyToken_exceptionThrown() throws Exception {
    //arrange
    CreateServiceInstanceRequest request =
            CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, "");
    ServiceInstance expectedInstance = new ServiceInstance(request);
    when(delegateMock.createServiceInstance(request)).thenReturn(expectedInstance);

    //act
    try {
      instanceService.createServiceInstance(request);

    //assert
    } catch (Exception e) {
      assertSame(ServiceBrokerException.class, e.getClass());
      assertThat(e.getMessage(), containsString("Given user token is empty"));
    }
  }

  @Test
  public void createServiceInstance_provisionerFails_exceptionThrownAndNatsNotified()
      throws Exception {
    // arrange
    CreateServiceInstanceRequest request =
        CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN);
    ServiceInstance expectedInstance = new ServiceInstance(request);
    ServiceMetadata expectedMetadata = new ServiceMetadata(INSTANCE_ID,
        request.getParameters().get("name").toString(), request.getOrganizationGuid());

    when(delegateMock.createServiceInstance(request)).thenReturn(expectedInstance);
    when(h2oProvisioner.provisionInstance(INSTANCE_ID, USER_TOKEN)).thenThrow(new ServiceBrokerException(""));

    // act
    try {
      instanceService.createServiceInstance(request);

      // assert
    } catch (ServiceBrokerException e) {
      assertSame(ServiceBrokerException.class, e.getClass());
    }
    
    ArgumentCaptor<ServiceMetadata> captor = ArgumentCaptor.forClass(ServiceMetadata.class);
    verify(natsNotifierMock).notifyServiceCreationStarted(captor.capture());
    verify(natsNotifierMock).notifyServiceCreationStatus(captor.capture(), any());
    verifyServiceMetadata(expectedMetadata, captor.getValue());
  }

  @Test
  public void createServiceInstance_storeFails_exceptionThrownAndNatsNotified() throws Exception {
    // arrange
    CreateServiceInstanceRequest request =
        CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN);
    ServiceInstance expectedInstance = new ServiceInstance(request);
    H2oCredentials expectedCredentials = new H2oCredentials("a", "b", "c", "d");
    ServiceMetadata expectedMetadata = new ServiceMetadata(INSTANCE_ID,
        request.getParameters().get("name").toString(), request.getOrganizationGuid());

    when(delegateMock.createServiceInstance(request)).thenReturn(expectedInstance);
    when(h2oProvisioner.provisionInstance(INSTANCE_ID, USER_TOKEN)).thenReturn(expectedCredentials);
    doThrow(new IOException()).when(credentialsStoreMock).save(Location.newInstance(INSTANCE_ID),
        expectedCredentials);

    // act
    // assert
    try {
      instanceService.createServiceInstance(request);
    } catch (ServiceBrokerException e) {
      assertSame(ServiceBrokerException.class, e.getClass());
    }
    ArgumentCaptor<ServiceMetadata> captor = ArgumentCaptor.forClass(ServiceMetadata.class);
    verify(natsNotifierMock).notifyServiceCreationStarted(captor.capture());
    verify(natsNotifierMock).notifyServiceCreationStatus(captor.capture(), any());
    verifyServiceMetadata(expectedMetadata, captor.getValue());
  }

  @Test
  public void createServiceInstance_containerBrokerFails_exceptionThrownAndNatsNotified() throws Exception {
    // arrange
    CreateServiceInstanceRequest request =
        CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN);
    ServiceInstance expectedInstance = new ServiceInstance(request);
    H2oCredentials expectedCredentials = new H2oCredentials("a", "b", "c", "d");
    ServiceMetadata expectedMetadata = new ServiceMetadata(INSTANCE_ID,
        request.getParameters().get("name").toString(), request.getOrganizationGuid());

    when(delegateMock.createServiceInstance(request)).thenReturn(expectedInstance);
    when(h2oProvisioner.provisionInstance(INSTANCE_ID, USER_TOKEN)).thenReturn(expectedCredentials);
    doNothing().when(credentialsStoreMock).save(Location.newInstance(INSTANCE_ID),
        expectedCredentials);
    when(containerBrokerOperationsMock.addExpose(INSTANCE_ID, "test body"))
        .thenThrow(new RuntimeException());

    // act
    // assert
    try {
      instanceService.createServiceInstance(request);
    } catch (ServiceBrokerException e) {
      assertSame(ServiceBrokerException.class, e.getClass());
    }
    ArgumentCaptor<ServiceMetadata> captor = ArgumentCaptor.forClass(ServiceMetadata.class);
    verify(natsNotifierMock).notifyServiceCreationStarted(captor.capture());
    verify(natsNotifierMock).notifyServiceCreationStatus(captor.capture(), any());
    verify(credentialsStoreMock, timeout(200)).save(Location.newInstance(INSTANCE_ID),
        expectedCredentials);
    verifyServiceMetadata(expectedMetadata, captor.getValue());
  }

  @Test
  public void createServiceInstance_provisionerTimedOut_exceptionThrownAndNatsNotified()
      throws Exception {
    // arrange
    H2oServiceInstanceService instanceService2 = new H2oServiceInstanceService(delegateMock,
        h2oProvisioner, credentialsStoreMock, containerBrokerOperationsMock, natsNotifierMock, 1);
    CreateServiceInstanceRequest request =
        CfBrokerRequestsFactory.getCreateInstanceRequest(INSTANCE_ID, USER_TOKEN);
    ServiceInstance expectedInstance = new ServiceInstance(request);
    H2oCredentials expectedCredentials = new H2oCredentials("a", "b", "c", "d");
    ServiceMetadata expectedMetadata = new ServiceMetadata(INSTANCE_ID,
        request.getParameters().get("name").toString(), request.getOrganizationGuid());

    when(delegateMock.createServiceInstance(request)).thenReturn(expectedInstance);
    when(h2oProvisioner.provisionInstance(INSTANCE_ID, USER_TOKEN))
        .thenAnswer(invocation -> returnCredentialsLongRunning(expectedCredentials));

    // act
    try {
      instanceService2.createServiceInstance(request);

      // assert
    } catch (ServiceBrokerException e) {
      assertSame(ServiceBrokerException.class, e.getClass());
    }
    
    ArgumentCaptor<ServiceMetadata> captor = ArgumentCaptor.forClass(ServiceMetadata.class);
    verify(natsNotifierMock).notifyServiceCreationStarted(captor.capture());
    verify(natsNotifierMock).notifyServiceCreationStatus(captor.capture(), any());
    verifyServiceMetadata(expectedMetadata, captor.getValue());
  }
  
  @Test
  public void deleteServiceInstance_serviceInstanceNotExists_provisionerNotCalled() throws Exception {
    // arrange
    DeleteServiceInstanceRequest deleteRequest = CfBrokerRequestsFactory.getDeleteServiceInstanceRequest(INSTANCE_ID);
    when(delegateMock.deleteServiceInstance(deleteRequest)).thenReturn(null);
    
    // act
    instanceService.deleteServiceInstance(deleteRequest);

    // assert
    verifyNoMoreInteractions(h2oProvisioner);
  }

  @Test
  public void deleteServiceInstance_containerBrokerFails_exceptionThrown() throws Exception {
    // arrange
    DeleteServiceInstanceRequest request =
        CfBrokerRequestsFactory.getDeleteServiceInstanceRequest(INSTANCE_ID);
    ServiceInstance expectedInstance = new ServiceInstance(request);

    when(delegateMock.deleteServiceInstance(request)).thenReturn(expectedInstance);
    doThrow(new RuntimeException()).when(containerBrokerOperationsMock).deleteExpose(INSTANCE_ID);

    // act
    // assert
    try {
      instanceService.deleteServiceInstance(request);
    } catch (ServiceBrokerException e) {
      assertSame(ServiceBrokerException.class, e.getClass());
    }
  }

  private H2oCredentials returnCredentialsLongRunning(H2oCredentials credentials)
      throws InterruptedException {
    Thread.sleep(3000);
    return credentials;
  }
  
  private void verifyServiceMetadata(ServiceMetadata expected, ServiceMetadata actual) {
    assertThat(actual.getId(), equalTo(expected.getId()));
    assertThat(actual.getName(), equalTo(expected.getName()));
    assertThat(actual.getOrganizationGuid(), equalTo(expected.getOrganizationGuid()));
  }
}
