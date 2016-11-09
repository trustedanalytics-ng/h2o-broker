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

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;

@Configuration
@Getter
@Setter
public class ExternalConfiguration {

  @Value("${cf.serviceid}")
  @NotNull
  private String cfServiceId;

  @Value("${cf.servicename}")
  @NotNull
  private String cfServiceName;

  @Value("${cf.baseid}")
  @NotNull
  private String cfBaseId;

  @Value("${h2o.provisioner.url}")
  @NotNull
  private String h2oProvisionerUrl;

  @Value("${h2o.provisioner.memory}")
  @NotNull
  private String h2oMapperMemory;

  @Value("${h2o.provisioner.nodes}")
  @NotNull
  private String h2oMapperNodes;

  @Value("${h2o.provisioner.timeout}")
  @NotNull
  private String provisionerTimeout;

  @Value("${metadata.imageUrl}")
  @NotNull
  private String imageUrl;

  @Value("${yarn.config}")
  @NotNull
  private String yarnConfig;

  @Value("${nats.url}")
  @NotNull
  private String natsUrl;

  @Value("${nats.serviceCreationTopic}")
  @NotNull
  private String natsServiceCreationTopic;

  @Value("${catalog.url}")
  @NotNull
  private String catalogUrl;

  @Value("${catalog.user}")
  @NotNull
  private String catalogUser;

  @Value("${catalog.password}")
  @NotNull
  private String catalogPassword;

  @Value("${containerbroker.url}")
  @NotNull
  private String containerbrokerUrl;

  @Value("${containerbroker.user}")
  @NotNull
  private String containerbrokerUser;

  @Value("${containerbroker.password}")
  @NotNull
  private String containerbrokerPassword;

  @Value("${catalog.hostname_key}")
  @NotNull
  private String catalogHostnameKey;

  @Value("${catalog.login_key}")
  @NotNull
  private String catalogLoginKey;

  @Value("${catalog.password_key}")
  @NotNull
  private String catalogPasswordKey;

  public static String getUrlWithHttpProtocol(String url) {
    return url.toLowerCase().matches("^http.?:.*$") ? url : "http://" + url;
  }

  public String getCatalogUrl() {
    return getUrlWithHttpProtocol(catalogUrl);
  }

  public String getContainerbrokerUrl() {
    return getUrlWithHttpProtocol(containerbrokerUrl);
  }
}
