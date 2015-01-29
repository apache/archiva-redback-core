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

import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authorization.AuthorizationException;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.integration.filter.authentication.basic.HttpBasicAuthentication;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * @author Olivier Lamy
 * @since 1.3
 */
@Service( "permissionInterceptor#rest" )
@Provider
public class PermissionsInterceptor
    extends AbstractInterceptor
    implements ContainerRequestFilter
{

    @Inject
    @Named( value = "securitySystem" )
    private SecuritySystem securitySystem;

    @Inject
    @Named( value = "httpAuthenticator#basic" )
    private HttpBasicAuthentication httpAuthenticator;

    private Logger log = LoggerFactory.getLogger( getClass() );

    public void filter( ContainerRequestContext containerRequestContext )
    {
        Message message = JAXRSUtils.getCurrentMessage();

        RedbackAuthorization redbackAuthorization = getRedbackAuthorization( message );

        if ( redbackAuthorization != null )
        {
            if ( redbackAuthorization.noRestriction() )
            {
                // we are fine this services is marked as non restrictive acces
                return;
            }
            String[] permissions = redbackAuthorization.permissions();
            //olamy: no value is an array with an empty String
            if ( permissions != null && permissions.length > 0 && !( permissions.length == 1 && StringUtils.isEmpty(
                permissions[0] ) ) )
            {
                HttpServletRequest request = getHttpServletRequest( message );
                SecuritySession session = httpAuthenticator.getSecuritySession( request.getSession() );
                AuthenticationResult authenticationResult = message.get( AuthenticationResult.class );
                if ( authenticationResult != null && authenticationResult.isAuthenticated() )
                {
                    for ( String permission : permissions )
                    {
                        if ( StringUtils.isBlank( permission ) )
                        {
                            continue;
                        }
                        try
                        {
                            if ( securitySystem.isAuthorized( session, permission,
                                                              StringUtils.isBlank( redbackAuthorization.resource() )
                                                                  ? null
                                                                  : redbackAuthorization.resource() ) )
                            {
                                return;
                            }
                            else
                            {
                                log.debug( "user {} not authorized for permission {}", session.getUser().getUsername(),
                                           permission );
                            }
                        }
                        catch ( AuthorizationException e )
                        {
                            log.debug( e.getMessage(), e );
                            containerRequestContext.abortWith( Response.status( Response.Status.FORBIDDEN ).build() );
                            return;
                        }
                    }

                }
                else
                {
                    if ( session != null && session.getUser() != null )
                    {
                        log.debug( "user {} not authenticated", session.getUser().getUsername() );
                    }
                }
            }
            else
            {
                if ( redbackAuthorization.noPermission() )
                {
                    log.debug( "path {} doesn't need special permission", message.get( Message.REQUEST_URI ) );
                    return;
                }
                containerRequestContext.abortWith( Response.status( Response.Status.FORBIDDEN ).build() );
                return;
            }
        }
        log.warn( "http path {} doesn't contain any informations regarding permissions ",
                  message.get( Message.REQUEST_URI ) );
        // here we failed to authenticate so 403 as there is no detail on karma for this
        // it must be marked as it's exposed
        containerRequestContext.abortWith( Response.status( Response.Status.FORBIDDEN ).build() );
    }
}
