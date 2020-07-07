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
import org.apache.archiva.redback.authentication.EncryptionFailedException;
import org.apache.archiva.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.archiva.redback.keys.KeyManager;
import org.apache.archiva.redback.keys.jpa.model.JpaAuthenticationKey;
import org.apache.archiva.redback.keys.memory.MemoryAuthenticationKey;
import org.apache.archiva.redback.keys.memory.MemoryKeyManager;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.rest.api.model.ActionStatus;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.model.LoginRequest;
import org.apache.archiva.redback.rest.api.model.PingResult;
import org.apache.archiva.redback.rest.api.model.Token;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.model.UserLogin;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.api.services.v2.AuthenticationService;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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

    private HttpAuthenticator httpAuthenticator;

    @Context
    private HttpServletRequest httpServletRequest;

    // validation token lifetime: 3 hours
    long tokenLifetime = 1000*3600*3;

    @Inject
    public DefaultAuthenticationService( SecuritySystem securitySystem,
                                         @Named( "httpAuthenticator#basic" ) HttpAuthenticator httpAuthenticator )
    {
        this.securitySystem = securitySystem;
        this.httpAuthenticator = httpAuthenticator;
    }


    @Override
    public Token requestOnetimeToken( String providedKey, String principal, String purpose, int expirationSeconds )
        throws RedbackServiceException
    {
        KeyManager keyManager = securitySystem.getKeyManager();
        AuthenticationKey key;

        if ( keyManager instanceof MemoryKeyManager )
        {
            key = new MemoryAuthenticationKey();
        }
        else
        {
            key = new JpaAuthenticationKey();
        }

        key.setKey( providedKey );
        key.setForPrincipal( principal );
        key.setPurpose( purpose );

        Instant now = Instant.now( );
        key.setDateCreated( Date.from( now ) );

        if ( expirationSeconds >= 0 )
        {
            Duration expireDuration = Duration.ofSeconds( expirationSeconds );
            key.setDateExpires( Date.from( now.plus( expireDuration ) ) );
        }
        keyManager.addKey( key );
        return Token.of( key );
    }

    @Override
    public PingResult ping()
        throws RedbackServiceException
    {
        return new PingResult( true);
    }

    @Override
    public PingResult pingWithAutz()
        throws RedbackServiceException
    {
        return new PingResult( true );
    }

    @Override
    public UserLogin logIn( LoginRequest loginRequest )
        throws RedbackServiceException
    {
        String userName = loginRequest.getUsername(), password = loginRequest.getPassword();
        PasswordBasedAuthenticationDataSource authDataSource =
            new PasswordBasedAuthenticationDataSource( userName, password );
        log.debug("Login for {}",userName);
        try
        {
            SecuritySession securitySession = securitySystem.authenticate( authDataSource );
            log.debug("Security session {}", securitySession);
            if ( securitySession.getAuthenticationResult().isAuthenticated() )
            {
                org.apache.archiva.redback.users.User user = securitySession.getUser();
                log.debug("user {} authenticated", user.getUsername());
                if ( !user.isValidated() )
                {
                    log.info( "user {} not validated", user.getUsername() );
                    return null;
                }
                UserLogin restUser = buildRestUser( user );
                restUser.setReadOnly( securitySystem.userManagerReadOnly() );
                // validationToken only set during login
                try {
                    String validationToken = securitySystem.getTokenManager().encryptToken(user.getUsername(), tokenLifetime);
                    restUser.setValidationToken(validationToken);
                    log.debug("Validation Token set {}",validationToken);

                } catch (EncryptionFailedException e) {
                    log.error("Validation token could not be created "+e.getMessage());
                }

                // here create an http session
                httpAuthenticator.authenticate( authDataSource, httpServletRequest.getSession( true ) );
                return restUser;
            }
            if ( securitySession.getAuthenticationResult() != null
                && securitySession.getAuthenticationResult().getAuthenticationFailureCauses() != null )
            {
                List<ErrorMessage> errorMessages = new ArrayList<ErrorMessage>();
                for ( AuthenticationFailureCause authenticationFailureCause : securitySession.getAuthenticationResult().getAuthenticationFailureCauses() )
                {
                    if ( authenticationFailureCause.getCause() == AuthenticationConstants.AUTHN_NO_SUCH_USER )
                    {
                        errorMessages.add( new ErrorMessage( "incorrect.username.password" ) );
                    }
                    else
                    {
                        errorMessages.add( new ErrorMessage().message( authenticationFailureCause.getMessage() ) );
                    }
                }

                throw new RedbackServiceException( errorMessages );
            }
            return null;
        }
        catch ( AuthenticationException e )
        {
            throw new RedbackServiceException( e.getMessage(), Response.Status.FORBIDDEN.getStatusCode() );
        }
        catch ( UserNotFoundException | AccountLockedException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        catch ( MustChangePasswordException e )
        {
            return buildRestUser( e.getUser() );
        }
        catch ( UserManagerException e )
        {
            log.info( "UserManagerException: {}", e.getMessage() );
            List<ErrorMessage> errorMessages =
                Arrays.asList( new ErrorMessage().message( "UserManagerException: " + e.getMessage() ) );
            throw new RedbackServiceException( errorMessages );
        }

    }

    @Override
    public User isLogged()
        throws RedbackServiceException
    {
        SecuritySession securitySession = httpAuthenticator.getSecuritySession( httpServletRequest.getSession( true ) );
        Boolean isLogged = securitySession != null;
        log.debug( "isLogged {}", isLogged );
        return isLogged && securitySession.getUser() != null ? buildRestUser( securitySession.getUser() ) : null;
    }

    @Override
    public ActionStatus logout()
        throws RedbackServiceException
    {
        HttpSession httpSession = httpServletRequest.getSession();
        if ( httpSession != null )
        {
            httpSession.invalidate();
        }
        return ActionStatus.SUCCESS;
    }

    private Calendar getNowGMT()
    {
        return Calendar.getInstance( TimeZone.getTimeZone( "GMT" ) );
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
