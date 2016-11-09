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

import static org.mockito.Mockito.mock;

import org.trustedanalytics.cfbroker.store.api.BrokerStore;
import org.trustedanalytics.servicebroker.h2o.config.ExternalConfiguration;
import org.trustedanalytics.servicebroker.h2o.nats.NatsMessageBuilder;
import org.trustedanalytics.servicebroker.h2o.nats.NatsNotifier;
import org.trustedanalytics.servicebroker.h2o.store.MapInMemoryStore;
import org.trustedanalytics.servicebroker.h2o.tapcontainerbroker.ContainerBrokerOperations;
import org.trustedanalytics.servicebroker.h2oprovisioner.rest.api.H2oCredentials;
import org.trustedanalytics.servicebroker.h2oprovisioner.rest.api.H2oProvisionerRestApi;

import nats.client.Nats;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.ServiceInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Configuration
@Profile("test")
public class TestConfig {

  @Bean
  public H2oProvisionerRestApi h2oProvisionerRestApi() {
    return mock(H2oProvisionerRestApi.class);
  }

  @Bean
  public boolean isKerberosEnabled() {
    return true;
  }

  @Bean
  public Nats natsClient(ExternalConfiguration config) {
    return mock(Nats.class);
  }

  @Bean
  public String natsServiceCreationTopic(ExternalConfiguration config) {
    return config.getNatsServiceCreationTopic();
  }

  @Bean
  public NatsNotifier natsNotifier(Nats natsClient, String natsServiceCreationTopic) {
    return new NatsNotifier(natsClient, natsServiceCreationTopic, new NatsMessageBuilder());
  }

  @Bean
  public BrokerStore<ServiceInstance> serviceInstanceStore() throws IOException {
    return new MapInMemoryStore<>();
  }

  @Bean
  public BrokerStore<CreateServiceInstanceBindingRequest> serviceBindingStore() throws IOException {
    return new MapInMemoryStore<>();
  }

  @Bean
  public BrokerStore<H2oCredentials> credentialsStore() throws IOException {
    return new MapInMemoryStore<>();
  }

  @Bean
  public ContainerBrokerOperations containerBrokerOperations() {
    return new ContainerBrokerMock();
  }
}
