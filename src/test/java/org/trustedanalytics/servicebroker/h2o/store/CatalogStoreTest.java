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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogServiceInstanceBuilder
    .HOSTNAME_KEY;
import static org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogServiceInstanceBuilder
    .PASSWORD_KEY;
import static org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogServiceInstanceBuilder
    .USER_KEY;

import org.trustedanalytics.cfbroker.store.api.Location;
import org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogInstanceToCredentialsMapper;
import org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogOperations;
import org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogServiceInstance;
import org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogServiceInstanceBuilder;
import org.trustedanalytics.servicebroker.h2oprovisioner.rest.api.H2oCredentials;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

public class CatalogStoreTest {

  private String testUser = "some-user";
  private String testPassword = "some-password";
  private String testHost = "some-host";
  private String testPort = "someport";
  private String testHostname = testHost + ":" + testPort;

  private String testInstanceId = "some-id";
  private CatalogServiceInstanceBuilder testInstanceBuilder =
      new CatalogServiceInstanceBuilder(testHostname, testUser, testPassword);
  private H2oCredentials testH2oCredentials =
      new H2oCredentials(testHost, testPort, testUser, testPassword);

  CatalogOperations catalogOperations = mock(CatalogOperations.class);

  @Before
  public void setUp() {
    CatalogServiceInstance testServiceInstance = testInstanceBuilder.createValidServiceInstance();
    when(catalogOperations.fetchInstance(testInstanceId)).thenReturn(testServiceInstance);
  }

  @Test
  public void getById_shouldCallCatalogAndReturnCredentials() throws IOException {
    // given
    CatalogStore sut = new CatalogStore(catalogOperations,
        new CatalogInstanceToCredentialsMapper(USER_KEY, PASSWORD_KEY, HOSTNAME_KEY));
    Location location = Location.newInstance(testInstanceId);

    // when
    Optional<H2oCredentials> credentials = sut.getById(location);

    // then
    verify(catalogOperations).fetchInstance(testInstanceId);
    assertEquals(testH2oCredentials, credentials.get());
  }

  @Test
  public void save_shouldCallCatalog() throws IOException {
    // given
    CatalogStore sut = new CatalogStore(catalogOperations,
        new CatalogInstanceToCredentialsMapper(USER_KEY, PASSWORD_KEY, HOSTNAME_KEY));
    Location location = Location.newInstance(testInstanceId);

    // when
    sut.save(location, testH2oCredentials);

    // then
    verify(catalogOperations).patchInstance(testInstanceId, getTapCatalogBodyString());
  }

  @Test
  public void deleteById_shouldCallCatalogAndReturnCredentials() throws IOException {
    // given
    CatalogStore sut = new CatalogStore(catalogOperations,
        new CatalogInstanceToCredentialsMapper(USER_KEY, PASSWORD_KEY, HOSTNAME_KEY));
    Location location = Location.newInstance(testInstanceId);

    // when
    Optional<H2oCredentials> credentials = sut.deleteById(location);

    // then
    verify(catalogOperations).fetchInstance(testInstanceId);
    assertEquals(testH2oCredentials, credentials.get());
  }

  // body required by tap-catalog:
  // [{"op":"Add","field":"Metadata","value":{"key":"hostname","value":"host:port"}},{"op":"Add","field":"Metadata","value":{"key":"login","value":"user"}},{"op":"Add","field":"Metadata","value":{"key":"password","value":"pass"}}]
  private String getTapCatalogBodyString() {
    return "[{\"op\":\"Add\",\"field\":\"Metadata\",\"value\":{\"key\":\"" + HOSTNAME_KEY
        + "\",\"value\":\"" + testHostname
        + "\"}},{\"op\":\"Add\",\"field\":\"Metadata\",\"value\":{\"key\":\"" + USER_KEY
        + "\",\"value\":\"" + testUser
        + "\"}},{\"op\":\"Add\",\"field\":\"Metadata\",\"value\":{\"key\":\"" + PASSWORD_KEY
        + "\",\"value\":\"" + testPassword + "\"}}]";
  }
}
