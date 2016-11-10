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
package org.trustedanalytics.servicebroker.h2o.config;

import org.trustedanalytics.servicebroker.h2o.tapcontainerbroker.ContainerBrokerOperations;

import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.auth.BasicAuthRequestInterceptor;
import feign.jackson.JacksonDecoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"cloud", "default"})
public class ContainerBrokerOperationsConfig {

  @Autowired
  private ExternalConfiguration configuration;

  @Bean
  public ContainerBrokerOperations containerBrokerOperations() {

    return Feign.builder().decoder(new JacksonDecoder())
        .logger(new Slf4jLogger(ContainerBrokerOperationsConfig.class))
        .options(new Request.Options(30 * 1000, 10 * 1000)).logLevel(Logger.Level.BASIC)
        .requestInterceptor(new BasicAuthRequestInterceptor(configuration.getContainerbrokerUser(),
            configuration.getContainerbrokerPassword()))
        .client(new OkHttpClient())
        .target(ContainerBrokerOperations.class, configuration.getContainerbrokerUrl());
  }
}
