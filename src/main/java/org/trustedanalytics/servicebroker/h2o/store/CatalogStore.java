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
package org.trustedanalytics.servicebroker.h2o.store;

import org.trustedanalytics.cfbroker.store.api.BrokerStore;
import org.trustedanalytics.cfbroker.store.api.Location;
import org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogInstanceToCredentialsMapper;
import org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogOperations;
import org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogServiceInstance;
import org.trustedanalytics.servicebroker.h2oprovisioner.rest.api.H2oCredentials;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class CatalogStore implements BrokerStore<H2oCredentials> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogStore.class);

  private static final String ADD_METADATA_OPERATION_STRING = "Add";

  private final CatalogOperations catalogOperations;
  private final CatalogInstanceToCredentialsMapper mapper;


  public CatalogStore(CatalogOperations catalogOperations,
      CatalogInstanceToCredentialsMapper mapper) {
    this.catalogOperations = catalogOperations;
    this.mapper = mapper;
  }

  @Override
  public Optional<H2oCredentials> getById(Location location) throws IOException {
    CatalogServiceInstance instance = catalogOperations.fetchInstance(location.getId());
    return Optional.ofNullable(mapper.mapToH2oCredentials(instance));
  }

  @Override
  public void save(Location location, H2oCredentials credentials) throws IOException {
    saveCredentials(location.getId(), credentials);
  }

  @Override
  public Optional<H2oCredentials> deleteById(Location location) throws IOException {
    // No need to delete credentials - tapng core components manage instances lifecycle
    CatalogServiceInstance instance = catalogOperations.fetchInstance(location.getId());
    return Optional.ofNullable(mapper.mapToH2oCredentials(instance));
  }

  private void saveCredentials(String instanceId, H2oCredentials credentials) {
    String patchBody = preparePatchBody(credentials);
    LOGGER.info("Sending patch to tap-catalog with body: " + patchBody);
    catalogOperations.patchInstance(instanceId, preparePatchBody(credentials));
  }

  private String preparePatchBody(H2oCredentials credentials) {
    ObjectMapper jacksonMapper = new ObjectMapper();

    ArrayNode patchBody = jacksonMapper.createArrayNode();
    patchBody.add(createAddMetadataNode(ADD_METADATA_OPERATION_STRING, mapper.getHostnameKey(),
        credentials.getHostname() + ":" + credentials.getPort()));
    patchBody.add(createAddMetadataNode(ADD_METADATA_OPERATION_STRING, mapper.getUserKey(),
        credentials.getUsername()));
    patchBody.add(createAddMetadataNode(ADD_METADATA_OPERATION_STRING, mapper.getPasswordKey(),
        credentials.getPassword()));

    return patchBody.toString();
  }

  private ObjectNode createAddMetadataNode(String operation, String key, String value) {
    ObjectMapper jacksonMapper = new ObjectMapper();

    ObjectNode valueNode = jacksonMapper.createObjectNode();
    valueNode.put("key", key);
    valueNode.put("value", value);

    ObjectNode patchBody = jacksonMapper.createObjectNode();
    patchBody.put("op", operation);
    patchBody.put("field", "Metadata");
    patchBody.set("value", valueNode);

    return patchBody;
  }
}
