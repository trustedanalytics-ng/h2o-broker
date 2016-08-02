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

import com.google.common.collect.ImmutableMap;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceBindingRequest;
import org.cloudfoundry.community.servicebroker.model.CreateServiceInstanceRequest;
import org.cloudfoundry.community.servicebroker.model.DeleteServiceInstanceRequest;

import java.util.Map;

public class CfBrokerRequestsFactory {

  static final public String FAKE_TOKEN = "not-necessary-character-string";

  public static CreateServiceInstanceRequest getCreateInstanceRequest(String serviceInstanceId, String userToken) {
    Map<String, Object> createInstanceParams = ImmutableMap.of("name", "test-service-name",
            "userToken", userToken);
    return new CreateServiceInstanceRequest("serviceDefinitionId", "planId", "organizationGuid",
        "spaceGuid", createInstanceParams).withServiceInstanceId(serviceInstanceId);
  }

  public static CreateServiceInstanceBindingRequest getCreateServiceBindingRequest(
      String instanceId, String bindingId) {

    return new CreateServiceInstanceBindingRequest(
        getCreateInstanceRequest(instanceId, FAKE_TOKEN).getServiceDefinitionId(), "planId", "appGuid")
            .withBindingId(bindingId).withServiceInstanceId(instanceId);
  }

  public static DeleteServiceInstanceRequest getDeleteServiceInstanceRequest(
      String serviceInstanceId) {
    return new DeleteServiceInstanceRequest(serviceInstanceId, "serviceId", "planId");
  }
}
