package org.apache.archiva.redback.rest.services.v2;

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

import org.apache.archiva.redback.authentication.AuthenticationConstants;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationFailureCause;
import org.apache.archiva.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.apache.archiva.redback.authentication.Token;
import org.apache.archiva.redback.authentication.TokenType;
import org.apache.archiva.redback.authentication.jwt.JwtAuthenticator;
import org.apache.archiva.redback.authentication.jwt.TokenAuthenticationException;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.rest.api.MessageKeys;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.model.GrantType;
import org.apache.archiva.redback.rest.api.model.v2.TokenResponse;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.model.UserLogin;
import org.apache.archiva.redback.rest.api.model.v2.PingResult;
import org.apache.archiva.redback.rest.api.model.v2.TokenRefreshRequest;
import org.apache.archiva.redback.rest.api.model.v2.TokenRequest;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.api.services.v2.AuthenticationService;
import org.apache.archiva.redback.rest.services.interceptors.RedbackPrincipal;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * Authentication service provides REST methods for authentication and verification.
 *
 * @author Olivier Lamy
 * @author Martin Stockhammer
 * @since 3.0
 */
@Service( "v2.authenticationService#rest" )
public class DefaultAuthenticationService
    implements AuthenticationService
{

    private static final Logger log = LoggerFactory.getLogger( DefaultAuthenticationService.class );

    private SecuritySystem securitySystem;

    @Context
    private HttpServletRequest httpServletRequest;

    @Context
    private SecurityContext securityContext;

    @Context
    private ContainerRequestContext requestContext;

    @Context
    private HttpServletResponse response;

    @Inject
    private JwtAuthenticator jwtAuthenticator;

    @Inject
    public DefaultAuthenticationService( SecuritySystem securitySystem )
    {
        this.securitySystem = securitySystem;
    }


    @Override
    public PingResult ping()
    {
        return new PingResult( true);
    }

    @Override
    public PingResult pingWithAutz()
    {
        return new PingResult( true );
    }

    RedbackPrincipal getPrincipal() {
        if (this.securityContext!=null) {
            Principal pri = this.securityContext.getUserPrincipal( );
            if (pri!=null && pri instanceof RedbackPrincipal) {
                return (RedbackPrincipal) pri;
            }
        }
        return null;
    }

    @Override
    public TokenResponse logIn( TokenRequest loginRequest )
        throws RedbackServiceException
    {
        log.debug( "Login request: grantType={}, code={}", loginRequest.getGrantType( ), loginRequest.getCode( ) );
        if (!GrantType.AUTHORIZATION_CODE.equals(loginRequest.getGrantType())) {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_BAD_CODE ), Response.Status.FORBIDDEN.getStatusCode( ) );
        }
        String userName = loginRequest.getUserId(), password = loginRequest.getPassword();
        PasswordBasedAuthenticationDataSource authDataSource =
            new PasswordBasedAuthenticationDataSource( userName, password );
        log.debug("Login for {}",userName);
        try
        {
            SecuritySession securitySession = securitySystem.authenticate( authDataSource );
            log.debug("Security session {}", securitySession);
            if ( securitySession.getAuthenticationResult() != null
            && securitySession.getAuthenticationResult().isAuthenticated() )
            {
                org.apache.archiva.redback.users.User user = securitySession.getUser();
                org.apache.archiva.redback.authentication.Token token = jwtAuthenticator.generateToken( user.getUsername( ) );
                log.debug("User {} authenticated", user.getUsername());
                if ( !user.isValidated() )
                {
                    log.info( "user {} not validated", user.getUsername() );
                    throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_VALIDATED, user.getUsername() ), Response.Status.FORBIDDEN.getStatusCode() );
                }
                // Stateless services no session
                // httpAuthenticator.authenticate( authDataSource, httpServletRequest.getSession( true ) );
                org.apache.archiva.redback.authentication.Token refreshToken = jwtAuthenticator.generateToken( user.getUsername( ), TokenType.REFRESH_TOKEN );
                response.setHeader( "Cache-Control", "no-store" );
                response.setHeader( "Pragma", "no-cache" );
                return new TokenResponse(token, refreshToken, "", loginRequest.getState());
            } else if ( securitySession.getAuthenticationResult() != null
                && securitySession.getAuthenticationResult().getAuthenticationFailureCauses() != null )
            {
                List<ErrorMessage> errorMessages = new ArrayList<ErrorMessage>();
                for ( AuthenticationFailureCause authenticationFailureCause : securitySession.getAuthenticationResult().getAuthenticationFailureCauses() )
                {
                    if ( authenticationFailureCause.getCause() == AuthenticationConstants.AUTHN_NO_SUCH_USER )
                    {
                        errorMessages.add( ErrorMessage.of( MessageKeys.ERR_AUTH_INVALID_CREDENTIALS ) );
                    }
                    else
                    {
                        errorMessages.add( ErrorMessage.of( MessageKeys.ERR_AUTH_FAIL_MSG, authenticationFailureCause.getMessage() ) );
                    }
                }
                response.setHeader( "WWW-Authenticate", "redback-login realm="+httpServletRequest.getRemoteHost() );
                throw new RedbackServiceException( errorMessages , Response.Status.UNAUTHORIZED.getStatusCode());
            }
            response.setHeader( "WWW-Authenticate", "redback-login realm="+httpServletRequest.getRemoteHost() );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_FAIL_MSG ), Response.Status.UNAUTHORIZED.getStatusCode() );
        }

        catch ( AuthenticationException e )
        {
            log.debug( "Authentication error: {}", e.getMessage( ), e );
            throw new RedbackServiceException(ErrorMessage.of( MessageKeys.ERR_AUTH_FAIL_MSG ), Response.Status.UNAUTHORIZED.getStatusCode() );
        }
        catch ( UserNotFoundException e )
        {
            log.debug( "User not found: {}", e.getMessage( ), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_INVALID_CREDENTIALS ), Response.Status.UNAUTHORIZED.getStatusCode() );
        }
        catch (AccountLockedException e) {
            log.info( "Account locked: {}", e.getMessage( ), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_ACCOUNT_LOCKED ), Response.Status.FORBIDDEN.getStatusCode() );
        }
        catch ( MustChangePasswordException e )
        {
            log.debug( "Password change required: {}", e.getMessage( ), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_PASSWORD_CHANGE_REQUIRED ), Response.Status.FORBIDDEN.getStatusCode( ) );
        }
        catch ( UserManagerException e )
        {
            log.warn( "UserManagerException: {}", e.getMessage() );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ) );
        }

    }

    @Override
    public TokenResponse token( TokenRefreshRequest request ) throws RedbackServiceException
    {
        if (!GrantType.REFRESH_TOKEN.equals(request.getGrantType())) {
            log.debug( "Bad grant type {}, expected: refresh_token", request.getGrantType( ).name( ).toLowerCase( ) );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_UNSUPPORTED_GRANT_TYPE, request.getGrantType().getLabel() ), Response.Status.FORBIDDEN.getStatusCode( ) );
        }
        try
        {
            Token accessToken = jwtAuthenticator.refreshAccessToken( request.getRefreshToken( ) );
            Token refreshToken = jwtAuthenticator.tokenFromString( request.getRefreshToken( ) );
            response.setHeader( "Cache-Control", "no-store" );
            response.setHeader( "Pragma", "no-cache" );
            return new TokenResponse( accessToken, refreshToken );
        }
        catch ( TokenAuthenticationException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_INVALID_TOKEN, e.getError( ).getError( )), Response.Status.UNAUTHORIZED.getStatusCode( ) );
        }
    }

    @Override
    public User getAuthenticatedUser()
        throws RedbackServiceException
    {
        RedbackPrincipal pri = getPrincipal( );
        if (pri!=null)
        {
            return buildRestUser( pri.getUser( ) );
        } else {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_UNAUTHORIZED_REQUEST ), Response.Status.UNAUTHORIZED.getStatusCode( ) );
        }
    }

    private UserLogin buildRestUser( org.apache.archiva.redback.users.User user )
    {
        UserLogin restUser = new UserLogin();
        restUser.setEmail( user.getEmail() );
        restUser.setUsername( user.getUsername() );
        restUser.setPasswordChangeRequired( user.isPasswordChangeRequired() );
        restUser.setLocked( user.isLocked() );
        restUser.setValidated( user.isValidated() );
        restUser.setFullName( user.getFullName() );
        return restUser;
    }
}
