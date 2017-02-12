package org.apache.archiva.redback.rest.services;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import junit.framework.TestCase;
import org.apache.archiva.redback.authentication.TokenManager;
import org.apache.archiva.redback.configuration.UserConfigurationException;
import org.apache.archiva.redback.rest.services.interceptors.RequestValidationInterceptor;
import org.apache.archiva.redback.rest.services.mock.MockContainerRequestContext;
import org.apache.archiva.redback.rest.services.mock.MockUserConfiguration;
import org.apache.archiva.redback.system.SecuritySystem;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;



/**
 * Created by Martin Stockhammer on 21.01.17.
 *
 * Unit Test for RequestValidationInterceptor. The unit tests are all without token validation.
 *
 */
@RunWith(JUnit4.class)
public class RequestValidationInterceptorTest extends TestCase {



    @Test
    public void validateRequestWithoutHeader() throws UserConfigurationException, IOException {
        TokenManager tm = new TokenManager();
        MockUserConfiguration cfg = new MockUserConfiguration();
        cfg.addValue(RequestValidationInterceptor.CFG_REST_CSRF_DISABLE_TOKEN_VALIDATION,"true");
        RequestValidationInterceptor interceptor = new RequestValidationInterceptor(cfg);
        MockHttpServletRequest request = new MockHttpServletRequest();
        interceptor.setHttpRequest(request);
        interceptor.init();
        MockContainerRequestContext ctx = new MockContainerRequestContext();
        interceptor.filter(ctx);
        assertTrue(ctx.isAborted());
    }

    @Test
    public void validateRequestWithOrigin() throws UserConfigurationException, IOException {
        TokenManager tm = new TokenManager();
        MockUserConfiguration cfg = new MockUserConfiguration();
        cfg.addValue(RequestValidationInterceptor.CFG_REST_CSRF_DISABLE_TOKEN_VALIDATION,"true");
        RequestValidationInterceptor interceptor = new RequestValidationInterceptor(cfg);
        MockHttpServletRequest request = new MockHttpServletRequest("GET","/api/v1/userService");
        request.setServerName("test.archiva.org");
        request.addHeader("Origin","http://test.archiva.org/myservlet");
        interceptor.setHttpRequest(request);
        interceptor.init();
        MockContainerRequestContext ctx = new MockContainerRequestContext();
        interceptor.filter(ctx);
        assertFalse(ctx.isAborted());
    }

    @Test
    public void validateRequestWithBadOrigin() throws UserConfigurationException, IOException {
        TokenManager tm = new TokenManager();
        MockUserConfiguration cfg = new MockUserConfiguration();
        cfg.addValue(RequestValidationInterceptor.CFG_REST_CSRF_DISABLE_TOKEN_VALIDATION,"true");
        RequestValidationInterceptor interceptor = new RequestValidationInterceptor(cfg);
        MockHttpServletRequest request = new MockHttpServletRequest("GET","/api/v1/userService");
        request.setServerName("test.archiva.org");
        request.addHeader("Origin","http://test2.archiva.org/myservlet");
        interceptor.setHttpRequest(request);
        interceptor.init();
        MockContainerRequestContext ctx = new MockContainerRequestContext();
        interceptor.filter(ctx);
        assertTrue(ctx.isAborted());
    }

    @Test
    public void validateRequestWithReferer() throws UserConfigurationException, IOException {
        TokenManager tm = new TokenManager();
        MockUserConfiguration cfg = new MockUserConfiguration();
        cfg.addValue(RequestValidationInterceptor.CFG_REST_CSRF_DISABLE_TOKEN_VALIDATION,"true");
        RequestValidationInterceptor interceptor = new RequestValidationInterceptor(cfg);
        MockHttpServletRequest request = new MockHttpServletRequest("GET","/api/v1/userService");
        request.setServerName("test.archiva.org");
        request.addHeader("Referer","http://test.archiva.org/myservlet2");
        interceptor.setHttpRequest(request);
        interceptor.init();
        MockContainerRequestContext ctx = new MockContainerRequestContext();
        interceptor.filter(ctx);
        assertFalse(ctx.isAborted());
    }

    @Test
    public void validateRequestWithBadReferer() throws UserConfigurationException, IOException {
        TokenManager tm = new TokenManager();
        MockUserConfiguration cfg = new MockUserConfiguration();
        cfg.addValue(RequestValidationInterceptor.CFG_REST_CSRF_DISABLE_TOKEN_VALIDATION,"true");
        RequestValidationInterceptor interceptor = new RequestValidationInterceptor(cfg);
        MockHttpServletRequest request = new MockHttpServletRequest("GET","/api/v1/userService");
        request.setServerName("test.archiva.org");
        request.addHeader("Referer","http://test3.archiva.org/myservlet2");
        interceptor.setHttpRequest(request);
        interceptor.init();
        MockContainerRequestContext ctx = new MockContainerRequestContext();
        interceptor.filter(ctx);
        assertTrue(ctx.isAborted());
    }

    @Test
    public void validateRequestWithOriginAndReferer() throws UserConfigurationException, IOException {
        TokenManager tm = new TokenManager();
        MockUserConfiguration cfg = new MockUserConfiguration();
        cfg.addValue(RequestValidationInterceptor.CFG_REST_CSRF_DISABLE_TOKEN_VALIDATION,"true");
        RequestValidationInterceptor interceptor = new RequestValidationInterceptor(cfg);
        MockHttpServletRequest request = new MockHttpServletRequest("GET","/api/v1/userService");
        request.setServerName("test.archiva.org");
        request.addHeader("Origin","http://test.archiva.org/myservlet");
        request.addHeader("Referer","http://test.archiva.org/myservlet2");
        interceptor.setHttpRequest(request);
        interceptor.init();
        MockContainerRequestContext ctx = new MockContainerRequestContext();
        interceptor.filter(ctx);
        assertFalse(ctx.isAborted());
    }


    @Test
    public void validateRequestWithOriginAndStaticUrl() throws UserConfigurationException, IOException {
        MockUserConfiguration cfg = new MockUserConfiguration();
        cfg.addValue("rest.baseUrl","http://test.archiva.org");
        cfg.addValue(RequestValidationInterceptor.CFG_REST_CSRF_DISABLE_TOKEN_VALIDATION,"true");
        TokenManager tm = new TokenManager();
        RequestValidationInterceptor interceptor = new RequestValidationInterceptor(cfg);
        MockHttpServletRequest request = new MockHttpServletRequest("GET","/api/v1/userService");
        request.setServerName("test4.archiva.org");
        request.addHeader("Origin","http://test.archiva.org/myservlet");
        interceptor.setHttpRequest(request);
        interceptor.init();
        MockContainerRequestContext ctx = new MockContainerRequestContext();
        interceptor.filter(ctx);
        assertFalse(ctx.isAborted());
    }

    @Test
    public void validateRequestWithBadOriginAndStaticUrl() throws UserConfigurationException, IOException {
        MockUserConfiguration cfg = new MockUserConfiguration();
        cfg.addValue("rest.baseUrl","http://mytest.archiva.org");
        cfg.addValue(RequestValidationInterceptor.CFG_REST_CSRF_DISABLE_TOKEN_VALIDATION,"true");
        TokenManager tm = new TokenManager();
        RequestValidationInterceptor interceptor = new RequestValidationInterceptor(cfg);
        MockHttpServletRequest request = new MockHttpServletRequest("GET","/api/v1/userService");
        request.setServerName("mytest.archiva.org");
        request.addHeader("Origin","http://test.archiva.org/myservlet");
        interceptor.setHttpRequest(request);
        interceptor.init();
        MockContainerRequestContext ctx = new MockContainerRequestContext();
        interceptor.filter(ctx);
        assertTrue(ctx.isAborted());
    }


}
