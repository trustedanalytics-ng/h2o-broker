/**
 * Copyright (c) 2016 Intel Corporation
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

import org.trustedanalytics.servicebroker.h2o.tapcontainerbroker.ContainerBrokerOperations;

import feign.Param;

import java.util.ArrayList;
import java.util.List;

public class ContainerBrokerMock implements ContainerBrokerOperations {
  private List<ContainerBrokerCallInfo> callHistory = new ArrayList<>();

  @Override
  public String[] addExpose(@Param("instanceId") String instanceId, String body) {
    callHistory.add(new ContainerBrokerCallInfo("addExpose", instanceId, body));
    return new String[0];
  }

  @Override
  public void deleteExpose(@Param("instanceId") String instanceId) {
    callHistory.add(new ContainerBrokerCallInfo("deleteExpose", instanceId, null));
  }

  public List<ContainerBrokerCallInfo> getCallHistory() {
    return callHistory;
  }

  public static class ContainerBrokerCallInfo {
    public String methodName;
    public String instanceId;
    public String body;

    public ContainerBrokerCallInfo(String methodName, String instanceId, String body) {
      this.methodName = methodName;
      this.instanceId = instanceId;
      this.body = body;
    }
  }
}
