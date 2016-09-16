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
package org.trustedanalytics.servicebroker.h2o.tapcatalog;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.trustedanalytics.servicebroker.h2oprovisioner.rest.api.H2oCredentials;

import lombok.Getter;

@Getter
public class CatalogInstanceToCredentialsMapper {

  private final String userKey;
  private final String passwordKey;
  private final String hostnameKey;

  public CatalogInstanceToCredentialsMapper(String userKey, String passwordKey,
      String hostnameKey) {
    this.userKey = userKey;
    this.passwordKey = passwordKey;
    this.hostnameKey = hostnameKey;
  }

  public H2oCredentials mapToH2oCredentials(CatalogServiceInstance instance) {
    Map<String, String> meatadata = getCredentialsMap(instance);
    String host = meatadata.get(hostnameKey).split(":")[0];
    String port = meatadata.get(hostnameKey).split(":")[1];

    return new H2oCredentials(host, port, meatadata.get(userKey), meatadata.get(passwordKey));
  }

  private Map<String, String> getCredentialsMap(CatalogServiceInstance instance) {

    if (instance.getMetadata() == null) {
      throw new IllegalArgumentException("No Metadata field in tap-catalog service instance data.");
    }

    Map<String, String> metadataMap = instance.getMetadata().stream()
        .collect(Collectors.toMap(InstanceMetadata::getKey, InstanceMetadata::getValue));

    if (metadataMap.containsKey(userKey) && metadataMap.containsKey(passwordKey)
        && metadataMap.containsKey(hostnameKey)
        && metadataMap.get(hostnameKey).split(":").length == 2) {
      return metadataMap;
    } else {
      throw new IllegalArgumentException(createErrorMessage(metadataMap));
    }
  }

  private String createErrorMessage(Map<String, String> metadata) {
    String message = "Metadata field does not contain valid H2O credentials. Expected keys: "
        + userKey + ", " + passwordKey + "," + hostnameKey + ". Given keys: ";

    Optional<String> givenKeys = metadata.entrySet().stream().map(entry -> entry.getKey())
        .reduce((key1, key2) -> key1 + ", " + key2);

    return message + givenKeys;
  }

}
