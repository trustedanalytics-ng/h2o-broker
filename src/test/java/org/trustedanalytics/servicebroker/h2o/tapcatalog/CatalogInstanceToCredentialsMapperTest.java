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

import static org.junit.Assert.assertEquals;
import static org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogServiceInstanceBuilder.HOSTNAME_KEY;
import static org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogServiceInstanceBuilder.PASSWORD_KEY;
import static org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogServiceInstanceBuilder.USER_KEY;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogInstanceToCredentialsMapper;
import org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogServiceInstance;
import org.trustedanalytics.servicebroker.h2oprovisioner.rest.api.H2oCredentials;

public class CatalogInstanceToCredentialsMapperTest {

  private String testHost = "some-host";
  private String testPort = "1234";
  private String testHostname = testHost + ":" + testPort;
  private String testUser = "user";
  private String testPassword = "password";

  private CatalogServiceInstanceBuilder testInstanceBuilder =
      new CatalogServiceInstanceBuilder(testHostname, testUser, testPassword);

  @Rule
  public final ExpectedException thrown = ExpectedException.none();

  @Test
  public void mapToH2oCredentials_validInstanceProvided_shouldReturnCredentials() {

    // given
    CatalogServiceInstance instance = testInstanceBuilder.createValidServiceInstance();
    CatalogInstanceToCredentialsMapper sut =
        new CatalogInstanceToCredentialsMapper(USER_KEY, PASSWORD_KEY, HOSTNAME_KEY);
    H2oCredentials expectedCredentials =
        new H2oCredentials(testHost, testPort, testUser, testPassword);

    // when
    H2oCredentials actualCredentials = sut.mapToH2oCredentials(instance);

    // then
    assertEquals(expectedCredentials, actualCredentials);
  }

  @Test
  public void mapToH2oCredentials_instanceWithoutMetadata_ExceptionThrown() {
    // given
    CatalogServiceInstance instance = testInstanceBuilder.createServiceInstanceWithoutMetadata();
    CatalogInstanceToCredentialsMapper sut =
        new CatalogInstanceToCredentialsMapper(USER_KEY, PASSWORD_KEY, HOSTNAME_KEY);

    // when
    // then
    thrown.expect(IllegalArgumentException.class);
    sut.mapToH2oCredentials(instance);
  }

  @Test
  public void mapToH2oCredentials_instanceWithoutUserKey_ExceptionThrown() {
    // given
    CatalogServiceInstance instance = testInstanceBuilder.createServiceInstanceWithoutUserKey();
    CatalogInstanceToCredentialsMapper sut =
        new CatalogInstanceToCredentialsMapper(USER_KEY, PASSWORD_KEY, HOSTNAME_KEY);

    // when
    // then
    thrown.expect(IllegalArgumentException.class);
    sut.mapToH2oCredentials(instance);
  }

  @Test
  public void mapToH2oCredentials_instanceWithoutPasswordKey_ExceptionThrown() {
    // given
    CatalogServiceInstance instance = testInstanceBuilder.createServiceInstanceWithoutPasswordKey();
    CatalogInstanceToCredentialsMapper sut =
        new CatalogInstanceToCredentialsMapper(USER_KEY, PASSWORD_KEY, HOSTNAME_KEY);

    // when
    // then
    thrown.expect(IllegalArgumentException.class);
    sut.mapToH2oCredentials(instance);
  }

  @Test
  public void mapToH2oCredentials_instanceWithoutHostnameKey_ExceptionThrown() {
    // given
    CatalogServiceInstance instance = testInstanceBuilder.createServiceInstanceWithoutHostanemKey();
    CatalogInstanceToCredentialsMapper sut =
        new CatalogInstanceToCredentialsMapper(USER_KEY, PASSWORD_KEY, HOSTNAME_KEY);

    // when
    // then
    thrown.expect(IllegalArgumentException.class);
    sut.mapToH2oCredentials(instance);
  }

  @Test
  public void mapToH2oCredentials_instanceWithInvalidHostname_ExceptionThrown() {
    // given
    CatalogServiceInstance instance = testInstanceBuilder.createServiceInstanceWithInvalidHostname();
    CatalogInstanceToCredentialsMapper sut =
        new CatalogInstanceToCredentialsMapper(USER_KEY, PASSWORD_KEY, HOSTNAME_KEY);

    // when
    // then
    thrown.expect(IllegalArgumentException.class);
    sut.mapToH2oCredentials(instance);
  }


}
