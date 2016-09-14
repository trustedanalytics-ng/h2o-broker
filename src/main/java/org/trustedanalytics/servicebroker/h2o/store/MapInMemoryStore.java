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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.trustedanalytics.cfbroker.store.api.BrokerStore;
import org.trustedanalytics.cfbroker.store.api.Location;

public class MapInMemoryStore<T> implements BrokerStore<T> {
  
  Map<Location, T> store = new HashMap<>();

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.trustedanalytics.cfbroker.store.api.BrokerStore#getById(org.trustedanalytics.cfbroker.store
   * .api.Location)
   */
  @Override
  public Optional<T> getById(Location location) throws IOException {
    return Optional.ofNullable(store.get(location));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.trustedanalytics.cfbroker.store.api.BrokerStore#save(org.trustedanalytics.cfbroker.store.
   * api.Location, java.lang.Object)
   */
  @Override
  public void save(Location location, T t) throws IOException {
    store.put(location, t);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.trustedanalytics.cfbroker.store.api.BrokerStore#deleteById(org.trustedanalytics.cfbroker.
   * store.api.Location)
   */
  @Override
  public Optional<T> deleteById(Location location) throws IOException {
    return Optional.ofNullable(store.remove(location));
  }

}
