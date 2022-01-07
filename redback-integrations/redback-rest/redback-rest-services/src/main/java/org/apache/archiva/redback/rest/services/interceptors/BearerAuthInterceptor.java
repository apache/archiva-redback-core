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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationFailureCause;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.BearerTokenAuthenticationDataSource;
import org.apache.archiva.redback.authentication.jwt.BearerError;
import org.apache.archiva.redback.authentication.jwt.JwtAuthenticator;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticationException;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.apache.archiva.redback.rest.services.RedbackRequestInformation;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Interceptor that checks for the Bearer Header value and tries to verify the token.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
@Service( "bearerAuthInterceptor#rest" )
@Provider
@Priority( Priorities.AUTHENTICATION )
public class BearerAuthInterceptor extends AbstractInterceptor
    implements ContainerRequestFilter
{

    private static final Logger log = LoggerFactory.getLogger( BearerAuthInterceptor.class );

    @Inject
    @Named( value = "userManager#default" )
    private UserManager userManager;

    @Inject
    @Named( value = "rbacManager#default" )
    RBACManager rbacManager;

    @Inject
    @Named( value = "securitySystem" )
    SecuritySystem securitySystem;

    @Inject
    JwtAuthenticator jwtAuthenticator;

    @Context
    private ResourceInfo resourceInfo;

    protected void setUserManager( UserManager userManager )
    {
        this.userManager = userManager;
    }

    protected void setJwtAuthenticator( JwtAuthenticator jwtAuthenticator )
    {
        this.jwtAuthenticator = jwtAuthenticator;
    }

    protected void setResourceInfo( ResourceInfo resourceInfo )
    {
        this.resourceInfo = resourceInfo;
    }

    @Override
    public void filter( ContainerRequestContext requestContext ) throws IOException
    {
        log.debug( "Intercepting request for bearer token" );
        log.debug( "Request {}", requestContext.getUriInfo( ).getPath( ) );
        final String requestPath = requestContext.getUriInfo( ).getPath( );
        if (ignoreAuth( requestPath )) {
            return;
        }

        // If no redback resource info, we deny the request
        RedbackAuthorization redbackAuthorization = getRedbackAuthorization( resourceInfo );
        if ( redbackAuthorization == null )
        {

            log.warn( "Request path {} doesn't contain any information regarding permissions. Denying access.",
                requestContext.getUriInfo( ).getRequestUri( ) );
            // here we failed to authenticate so 403 as there is no detail on karma for this
            // it must be marked as it's exposed
            requestContext.abortWith( Response.status( Response.Status.FORBIDDEN ).build( ) );
            return;
        }
        String bearerHeader = StringUtils.defaultIfEmpty( requestContext.getHeaderString( "Authorization" ), "" ).trim( );
        if ( !"".equals( bearerHeader ) )
        {
            log.debug( "Found Bearer token in header" );
            String bearerToken = bearerHeader.replaceFirst( "\\s*Bearer\\s+(\\S+)\\s*", "$1" );
            final HttpServletRequest request = getHttpServletRequest( );
            BearerTokenAuthenticationDataSource source = new BearerTokenAuthenticationDataSource( "", bearerToken );

            if ( redbackAuthorization.noRestriction( ) )
            {
                log.debug( "No restriction for method {}#{}", resourceInfo.getResourceClass( ), resourceInfo.getResourceMethod( ) );
                // maybe session exists so put it in threadLocal
                // some services need the current user if logged
                // maybe there is some authz in the request so try it but not fail so catch Exception !
                try
                {
                    SecuritySession securitySession = securitySystem.authenticate( source );
                    AuthenticationResult authenticationResult = securitySession.getAuthenticationResult( );

                    if ( ( authenticationResult == null ) || ( !authenticationResult.isAuthenticated( ) ) )
                    {
                        return;
                    }

                    User user = authenticationResult.getUser( ) == null ? userManager.findUser(
                        authenticationResult.getPrincipal( ) ) : authenticationResult.getUser( );
                    RedbackRequestInformation redbackRequestInformation =
                        new RedbackRequestInformation( securitySession, user, request.getRemoteAddr( ) );

                    RedbackAuthenticationThreadLocal.set( redbackRequestInformation );
                    requestContext.setProperty( AUTHENTICATION_RESULT, authenticationResult );
                    requestContext.setProperty( SECURITY_SESSION, securitySession );
                    RedbackSecurityContext securityContext = new RedbackSecurityContext(requestContext.getUriInfo(), user, securitySession );

                    if (rbacManager!=null)
                    {
                        List<String> roleNames = rbacManager.getAssignedRoles( user.getUsername( ) ).stream( )
                            .flatMap( role -> Stream.concat( Stream.of( role.getName( ) ), role.getChildRoleNames( ).stream( ) ) )
                            .collect( Collectors.toList( ) );
                        securityContext.setRoles( roleNames );
                    }
                    requestContext.setSecurityContext( securityContext );
                }
                catch ( Exception e )
                {
                    log.debug( "Authentication failed {}", e.getMessage( ), e );
                    // ignore here
                }
                return;
            }
            HttpServletResponse response = getHttpServletResponse( );
            try
            {
                SecuritySession securitySession = securitySystem.authenticate( source );
                AuthenticationResult authenticationResult = securitySession.getAuthenticationResult( );

                if ( ( authenticationResult == null ) || ( !authenticationResult.isAuthenticated( ) ) )
                {
                    String error;
                    String message;
                    if ( authenticationResult.getAuthenticationFailureCauses( ).size( ) > 0 )
                    {
                        AuthenticationFailureCause cause = authenticationResult.getAuthenticationFailureCauses( ).get( 0 );
                        error = BearerError.get( cause.getCause( ) ).getError( );
                        message = cause.getMessage( );
                    }
                    else
                    {
                        error = "invalid_token";
                        message = "Unknown error";
                    }
                    response.setHeader( "WWW-Authenticate", "Bearer realm=\"" + request.getRemoteHost( ) + "\",error=\""
                        + error + "\",error_description=\"" + message + "\"" );
                    requestContext.abortWith( Response.status( Response.Status.UNAUTHORIZED ).build( ) );
                    return;
                }

                User user = authenticationResult.getUser( ) == null
                    ? userManager.findUser( authenticationResult.getPrincipal( ) )
                    : authenticationResult.getUser( );

                RedbackRequestInformation redbackRequestInformation =
                    new RedbackRequestInformation( user, request.getRemoteAddr( ) );
                redbackRequestInformation.setSecuritySession( securitySession );
                RedbackAuthenticationThreadLocal.set( redbackRequestInformation );
                // message.put( AuthenticationResult.class, authenticationResult );
                requestContext.setProperty( AUTHENTICATION_RESULT, authenticationResult );
                requestContext.setProperty( SECURITY_SESSION, securitySession );
                RedbackSecurityContext securityContext = new RedbackSecurityContext(requestContext.getUriInfo(), user, securitySession );
                requestContext.setSecurityContext( securityContext );
                return;
            }
            catch ( AuthenticationException e )
            {
                response.setHeader( "WWW-Authenticate", "Bearer realm=\"" + request.getRemoteHost( )
                    + "\",error=\"invalid_token\",error_description=\"" + e.getMessage( ) + "\"" );
                requestContext.abortWith( Response.status( Response.Status.UNAUTHORIZED ).build( ) );
            }
            catch ( UserNotFoundException e )
            {
                response.setHeader( "WWW-Authenticate", "Bearer realm=\"" + request.getRemoteHost( )
                    + "\",error=\"invalid_token\",error_description=\"user not found\"" );
                requestContext.abortWith( Response.status( Response.Status.UNAUTHORIZED ).build( ) );
            }
            catch ( UserManagerException e )
            {
                log.error( "Error from user manager " + e.getMessage( ) );
                requestContext.abortWith( Response.status( Response.Status.INTERNAL_SERVER_ERROR ).build( ) );
            }
            catch ( AccountLockedException e )
            {
                response.setHeader( "WWW-Authenticate", "Bearer realm=\"" + request.getRemoteHost( )
                    + "\",error=\"invalid_token\",error_description=\"account locked\"" );
                requestContext.abortWith( Response.status( Response.Status.UNAUTHORIZED ).build( ) );
            }
            catch ( MustChangePasswordException e )
            {
                response.setHeader( "WWW-Authenticate", "Bearer realm=\"" + request.getRemoteHost( )
                    + "\",error=\"invalid_token\",error_description=\"password change required\"" );
                requestContext.abortWith( Response.status( Response.Status.UNAUTHORIZED ).build( ) );
            }


        } else {
            log.debug( "No Bearer token found" );
        }
    }
}
