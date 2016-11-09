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

import org.junit.Assert;
import org.junit.Test;

public class ExternalConfigurationTest {

    @Test
    public void getUrlWithHttpProtocol_addsHttpPrefixIfMissing() {
        //arrange
        //act
        //assert
        Assert.assertEquals("http://", ExternalConfiguration.getUrlWithHttpProtocol(""));
        Assert.assertEquals(
            "http://some.url", ExternalConfiguration.getUrlWithHttpProtocol("some.url"));
        Assert.assertEquals(
            "http://127.0.0.1", ExternalConfiguration.getUrlWithHttpProtocol("127.0.0.1"));
        Assert.assertEquals(
            "http://htp://some.url", ExternalConfiguration.getUrlWithHttpProtocol("htp://some.url"));
        Assert.assertEquals(
            "http:// http://some.url.with.leading.space",
            ExternalConfiguration.getUrlWithHttpProtocol(" http://some.url.with.leading.space"));
    }

    @Test
    public void getUrlWithHttpProtocol_doesNotAddHttpPrefixIfItIsPresent() {
        //arrange
        //act
        //assert
        Assert.assertEquals("http://", ExternalConfiguration.getUrlWithHttpProtocol("http://"));
        Assert.assertEquals(
            "http://some.url", ExternalConfiguration.getUrlWithHttpProtocol("http://some.url"));
        Assert.assertEquals(
            "https://some.url", ExternalConfiguration.getUrlWithHttpProtocol("https://some.url"));
        Assert.assertEquals(
            "HTTPS://127.0.0.1", ExternalConfiguration.getUrlWithHttpProtocol("HTTPS://127.0.0.1"));
        Assert.assertEquals(
            "Http://some.url", ExternalConfiguration.getUrlWithHttpProtocol("Http://some.url"));
    }
}
