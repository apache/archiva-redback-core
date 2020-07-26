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
import org.apache.archiva.redback.authentication.Token;
import org.apache.archiva.redback.authentication.jwt.JwtAuthenticator;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.apache.archiva.redback.rest.services.RedbackRequestInformation;
import org.apache.archiva.redback.rest.services.v2.DefaultAuthenticationService;
import org.apache.archiva.redback.system.DefaultSecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.memory.SimpleUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@ExtendWith( MockitoExtension.class )
class BearerAuthInterceptorTest
{

    @Mock
    UserConfiguration userConfiguration;

    @Mock
    UserManager userManager;

    @Mock
    private ResourceInfo resourceInfo;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private SecuritySystem securitySystem;

    private JwtAuthenticator jwtAuthenticator;

    BearerAuthInterceptor interceptor;

    @BeforeEach
    void setup() throws AuthenticationException, UserManagerException, AccountLockedException, MustChangePasswordException
    {
        // when( userConfiguration.getString( anyString( ) ) ).thenReturn( null );
        doAnswer( invocation -> invocation.getArgument( 1 ).toString() ).when( userConfiguration ).getString( anyString( ), anyString( ) );
        doAnswer( invocation -> (int)invocation.getArgument( 1 ) ).when( userConfiguration ).getInt( anyString( ), anyInt() );

        interceptor = new BearerAuthInterceptor( );
        interceptor.setHttpServletRequest( httpServletRequest );
        interceptor.setHttpServletResponse( httpServletResponse );
        interceptor.setResourceInfo( resourceInfo );
        interceptor.setUserManager( userManager );
        interceptor.securitySystem = securitySystem;
        this.jwtAuthenticator = new JwtAuthenticator( );
        jwtAuthenticator.setUserConfiguration( userConfiguration );
        jwtAuthenticator.init();
        interceptor.setJwtAuthenticator( jwtAuthenticator );
        doAnswer( invocation -> new DefaultSecuritySession( jwtAuthenticator.authenticate( invocation.getArgument( 0 ) )) )
            .when( securitySystem ).authenticate( any( ) );

    }

    @Test
    void filter() throws IOException, NoSuchMethodException, UserManagerException, URISyntaxException
    {
        Token token = jwtAuthenticator.generateToken( "gandalf" );
        when( resourceInfo.getResourceMethod( ) ).thenReturn( DefaultAuthenticationService.class.getDeclaredMethod( "ping" ) );
        doReturn( DefaultAuthenticationService.class ).when( resourceInfo ).getResourceClass( );
        ContainerRequestContext context = mock( ContainerRequestContext.class );
        when( context.getHeaderString( "Authorization" ) ).thenReturn( "Bearer " + token.getData( ) );
        UriInfo uriInfo = mock( UriInfo.class );
        when( context.getUriInfo( ) ).thenReturn( uriInfo );
        when( uriInfo.getPath( ) ).thenReturn( "/api/v2/redback/auth/ping" );
        when( uriInfo.getAbsolutePath( ) ).thenReturn( new URI( "https://localhost:1010/api/v2/redback/auth/ping" ) );
        User user = new SimpleUser( );
        user.setUsername( "gandalf" );
        when( userManager.findUser( "gandalf" ) ).thenReturn( user );
        interceptor.filter( context);
        verify( context, never() ).abortWith( any() );
        RedbackRequestInformation info = RedbackAuthenticationThreadLocal.get( );
        assertNotNull( info );
        assertEquals( "gandalf", info.getUser( ).getUsername( ) );
        verify( context, times(1)).setSecurityContext( argThat( securityContext -> securityContext.getUserPrincipal().getName().equals("gandalf") ) );
    }


    @Test
    void filterWithInvalidToken() throws IOException, NoSuchMethodException
    {
        RedbackAuthenticationThreadLocal.set( null );
        Token token = jwtAuthenticator.generateToken( "gandalf" );
        when( resourceInfo.getResourceMethod( ) ).thenReturn( DefaultAuthenticationService.class.getDeclaredMethod( "pingWithAutz") );
        doReturn( DefaultAuthenticationService.class ).when( resourceInfo ).getResourceClass( );
        ContainerRequestContext context = mock( ContainerRequestContext.class );
        when( context.getHeaderString( "Authorization" ) ).thenReturn( "Bearer xxxxx" );
        UriInfo uriInfo = mock( UriInfo.class );
        when( context.getUriInfo( ) ).thenReturn( uriInfo );
        when( uriInfo.getPath( ) ).thenReturn( "/api/v2/redback/auth/ping/authenticated" );

        interceptor.filter( context);
        verify( context, times(1) ).abortWith( argThat( response -> response.getStatus() == 401 )  );
        verify( httpServletResponse, times(1) ).setHeader( eq("WWW-Authenticate"), anyString( ) );
        RedbackRequestInformation info = RedbackAuthenticationThreadLocal.get( );
        assertNull( info );
    }

    @Test
    void filterWithInvalidTokenUnrestrictedMethod() throws IOException, NoSuchMethodException
    {
        RedbackAuthenticationThreadLocal.set( null );
        Token token = jwtAuthenticator.generateToken( "gandalf" );
        when( resourceInfo.getResourceMethod( ) ).thenReturn( DefaultAuthenticationService.class.getDeclaredMethod( "ping") );
        doReturn( DefaultAuthenticationService.class ).when( resourceInfo ).getResourceClass( );
        ContainerRequestContext context = mock( ContainerRequestContext.class );
        when( context.getHeaderString( "Authorization" ) ).thenReturn( "Bearer xxxxx" );
        UriInfo uriInfo = mock( UriInfo.class );
        when( context.getUriInfo( ) ).thenReturn( uriInfo );
        when( uriInfo.getPath( ) ).thenReturn( "/api/v2/redback/auth/ping" );

        interceptor.filter( context);
        RedbackRequestInformation info = RedbackAuthenticationThreadLocal.get( );
        assertNull( info );
    }

}