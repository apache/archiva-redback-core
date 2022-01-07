package org.apache.archiva.redback.rest.services.interceptors;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.apache.archiva.redback.rest.services.RedbackRequestInformation;
import org.apache.archiva.redback.system.SecuritySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 1.3
 */
public abstract class AbstractInterceptor
{

    private static final Logger log = LoggerFactory.getLogger( AbstractInterceptor.class );

    private static final String API_DOCS = "api-docs";
    private static final String OPENAPI_JSON = "openapi.json";
    private static final String API_DOCS1 = "api-docs/";

    private Map<Method, RedbackAuthorization> authorizationCache = new HashMap<>( );

    public static final String AUTHENTICATION_RESULT = "org.apache.archiva.authResult";
    public static final String SECURITY_SESSION = "org.apache.archiva.securitySession";

    @Context
    private HttpServletRequest httpServletRequest;

    @Context
    private HttpServletResponse httpServletResponse;

    public HttpServletRequest getHttpServletRequest( )
    {
        return httpServletRequest;
    }

    public HttpServletResponse getHttpServletResponse( )
    {
        return httpServletResponse;
    }

    protected void setHttpServletRequest(HttpServletRequest request) {
        this.httpServletRequest = request;
    }

    protected void setHttpServletResponse(HttpServletResponse response) {
        this.httpServletResponse = response;
    }


    public static final boolean ignoreAuth(final String requestPath) {
        final int len = requestPath.length( );
        return len >= 8 && ( ( len == 12 && OPENAPI_JSON.equals( requestPath ) ) ||
            ( requestPath.startsWith( API_DOCS ) && ( len == 8 || requestPath.startsWith( API_DOCS1 ) ) ) );
    }

    public RedbackAuthorization getRedbackAuthorization( ResourceInfo resourceInfo ) {
        Method method = resourceInfo.getResourceMethod( );
        RedbackAuthorization redbackAuthorization = getAuthorizationForMethod( method );
        log.debug( "resourceClass {}, method {}, redbackAuthorization {}", //
                resourceInfo.getResourceClass( ), //
                method, //
                redbackAuthorization );
        return redbackAuthorization;
    }

    private RedbackAuthorization getAuthorizationForMethod(Method method) {
        if (authorizationCache.containsKey( method )) {
            return authorizationCache.get( method );
        } else {
            RedbackAuthorization authorization = AnnotationUtils.findAnnotation( method, RedbackAuthorization.class );
            authorizationCache.put( method, authorization );
            return authorization;
        }
    }

    protected SecuritySession getSecuritySession(ContainerRequestContext containerRequestContext, HttpAuthenticator httpAuthenticator,
    HttpServletRequest request) {
        if ( containerRequestContext.getProperty( SECURITY_SESSION ) != null ) {
            return (SecuritySession) containerRequestContext.getProperty( SECURITY_SESSION );
        }
        RedbackRequestInformation info = RedbackAuthenticationThreadLocal.get( );
        SecuritySession securitySession = info == null ? null : info.getSecuritySession( );
        if  (securitySession!=null) {
            return securitySession;
        } else
        {
            return httpAuthenticator.getSecuritySession( request.getSession( true ) );
        }
    }

    protected AuthenticationResult getAuthenticationResult( ContainerRequestContext containerRequestContext, HttpAuthenticator httpAuthenticator, HttpServletRequest request )
    {
        AuthenticationResult authenticationResult = null;

        if ( containerRequestContext.getProperty( AUTHENTICATION_RESULT ) == null )
        {
            try
            {
                authenticationResult =
                    httpAuthenticator.getAuthenticationResult( request, getHttpServletResponse( ) );

                if (authenticationResult!=null) {
                    containerRequestContext.setProperty( AUTHENTICATION_RESULT, authenticationResult );
                }

                log.debug( "authenticationResult from request: {}", authenticationResult );
            }
            catch ( AuthenticationException e )
            {
                log.debug( "failed to authenticate for path {}", containerRequestContext.getUriInfo().getRequestUri() );
            }
            catch ( AccountLockedException e )
            {
                log.debug( "account locked for path {}", containerRequestContext.getUriInfo().getRequestUri() );
            }
            catch ( MustChangePasswordException e )
            {
                log.debug( "must change password for path {}", containerRequestContext.getUriInfo().getRequestUri() );
            }
        } else {
            authenticationResult = (AuthenticationResult) containerRequestContext.getProperty( AUTHENTICATION_RESULT );
        }
        log.debug( "authenticationResult from message: {}", authenticationResult );
        return authenticationResult;
    }
}
