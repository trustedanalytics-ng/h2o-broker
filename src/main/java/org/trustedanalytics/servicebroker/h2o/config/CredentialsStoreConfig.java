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
package org.trustedanalytics.servicebroker.h2o.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.trustedanalytics.cfbroker.store.api.BrokerStore;
import org.trustedanalytics.servicebroker.h2o.store.CatalogStore;
import org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogInstanceToCredentialsMapper;
import org.trustedanalytics.servicebroker.h2o.tapcatalog.CatalogOperations;
import org.trustedanalytics.servicebroker.h2oprovisioner.rest.api.H2oCredentials;

import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.auth.BasicAuthRequestInterceptor;
import feign.jackson.JacksonDecoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;

@Configuration
@Profile({"cloud", "default"})
public class CredentialsStoreConfig {

  @Autowired
  private ExternalConfiguration configuration;

  @Bean
  public BrokerStore<H2oCredentials> credentialsStore(CatalogOperations catalogOperations)
      throws IOException {
    return new CatalogStore(catalogOperations,
        new CatalogInstanceToCredentialsMapper(configuration.getCatalogLoginKey(),
            configuration.getCatalogPasswordKey(), configuration.getCatalogHostnameKey()));
  }

  @Bean
  public CatalogOperations catalogOperations() {

    return Feign.builder().decoder(new JacksonDecoder())
        .logger(new Slf4jLogger(CredentialsStoreConfig.class))
        .options(new Request.Options(30 * 1000, 10 * 1000)).logLevel(Logger.Level.BASIC)
        .requestInterceptor(new BasicAuthRequestInterceptor(configuration.getCatalogUser(),
            configuration.getCatalogPassword()))
        .client(new OkHttpClient())
        .target(CatalogOperations.class, configuration.getCatalogUrl());
  }
}
