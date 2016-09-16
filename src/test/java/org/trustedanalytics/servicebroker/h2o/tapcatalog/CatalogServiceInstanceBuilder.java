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

import java.util.Collection;
import java.util.HashSet;

import org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogServiceInstance;
import org.trustedanalytics.servicebroker.h2o.tapcatalog.InstanceMetadata;

public class CatalogServiceInstanceBuilder {
  
  public static final String HOSTNAME_KEY = "hostname";
  public static final String USER_KEY = "user";
  public static final String PASSWORD_KEY = "password";
  
  private final String testHostname;
  private final String testUser;
  private final String testPassword;
  
  public CatalogServiceInstanceBuilder(String testHostname, String testUser, String testPassword) {
    this.testHostname = testHostname;
    this.testUser = testUser;
    this.testPassword = testPassword;
  }

  public CatalogServiceInstance createValidServiceInstance() {
    Collection<InstanceMetadata> metadata = new HashSet<>();
    metadata.add(new InstanceMetadata(HOSTNAME_KEY, testHostname));
    metadata.add(new InstanceMetadata(USER_KEY, testUser));
    metadata.add(new InstanceMetadata(PASSWORD_KEY, testPassword));

    return new CatalogServiceInstance(metadata);
  }

  public CatalogServiceInstance createServiceInstanceWithoutMetadata() {
    return new CatalogServiceInstance(null);
  }

  public CatalogServiceInstance createServiceInstanceWithoutUserKey() {
    Collection<InstanceMetadata> metadata = new HashSet<>();
    metadata.add(new InstanceMetadata(HOSTNAME_KEY, testHostname));
    metadata.add(new InstanceMetadata(PASSWORD_KEY, testPassword));

    return new CatalogServiceInstance(metadata);
  }

  public CatalogServiceInstance createServiceInstanceWithoutPasswordKey() {
    Collection<InstanceMetadata> metadata = new HashSet<>();
    metadata.add(new InstanceMetadata(HOSTNAME_KEY, testHostname));
    metadata.add(new InstanceMetadata(USER_KEY, testUser));

    return new CatalogServiceInstance(metadata);
  }

  public CatalogServiceInstance createServiceInstanceWithoutHostanemKey() {
    Collection<InstanceMetadata> metadata = new HashSet<>();
    metadata.add(new InstanceMetadata(USER_KEY, testUser));
    metadata.add(new InstanceMetadata(PASSWORD_KEY, testPassword));

    return new CatalogServiceInstance(metadata);
  }

  public CatalogServiceInstance createServiceInstanceWithInvalidHostname() {
    Collection<InstanceMetadata> metadata = new HashSet<>();
    metadata.add(new InstanceMetadata(USER_KEY, testUser));
    metadata.add(new InstanceMetadata(PASSWORD_KEY, testPassword));
    metadata.add(new InstanceMetadata(HOSTNAME_KEY, "some-host-without-port"));

    return new CatalogServiceInstance(metadata);
  }
}
