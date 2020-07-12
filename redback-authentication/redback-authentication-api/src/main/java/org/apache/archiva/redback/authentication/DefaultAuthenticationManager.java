package org.apache.archiva.redback.authentication;

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

import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * DefaultAuthenticationManager: the goal of the authentication manager is to act as a conduit for
 * authentication requests into different authentication schemes
 * <p>
 * For example, the default implementation can be configured with any number of authenticators and will
 * sequentially try them for an authenticated result.  This allows you to have the standard user/pass
 * auth procedure followed by authentication based on a known key for 'remember me' type functionality.
 *
 * @author: Jesse McConnell
 */
@Service( "authenticationManager" )
public class DefaultAuthenticationManager
    implements AuthenticationManager
{

    private static final Logger log = LoggerFactory.getLogger( DefaultAuthenticationManager.class );

    final private AtomicReference<List<Authenticator>> authenticators = new AtomicReference<>( );
    final private AtomicReference<Map<String, AuthenticatorControl>> controls = new AtomicReference<>( );
    final private AtomicReference<Map<String, AuthenticatorControl>> modfiedControls = new AtomicReference<>( );
    private Map<String, Authenticator> availableAuthenticators;

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    @Named("userConfiguration#default")
    private UserConfiguration userConfiguration;

    @Inject
    @Named( "userManager#default" )
    private UserManager userManager;

    @SuppressWarnings( "unchecked" )
    @PostConstruct
    public void initialize( )
    {
        this.availableAuthenticators =
            applicationContext.getBeansOfType( Authenticator.class ).values( ).stream( ).collect( Collectors.toMap( a -> a.getId( ), authenticator -> authenticator ) );
        this.modfiedControls.set( new HashMap<>( ) );
        initializeOrder(  );
    }

    private void initializeOrder() {
        Stream<AuthenticatorControl> controlStream = initControls( );
        final List<Authenticator> authenticators = new ArrayList<>( );
        final Map<String, AuthenticatorControl> controls = new LinkedHashMap<>( );
        controlStream.forEachOrdered( control -> {
            authenticators.add( this.availableAuthenticators.get( control.getName( ) ) );
            controls.put( control.getName( ), control );
        } );
        this.authenticators.set( authenticators );
        this.controls.set( controls );
    }

    Map<String, AuthenticatorControl> getConfigControls( )
    {
        return new HashMap<>( );
    }

    Map<String, Authenticator> getAvailableAuthenticators() {
        return this.availableAuthenticators;
    }

    Map<String, AuthenticatorControl> getModifiedControls() {
        return this.modfiedControls.get();
    }

    void setModfiedControls(Map<String, AuthenticatorControl> newControls) {
        this.modfiedControls.set( newControls );
    }

    Map<String, AuthenticatorControl> getControlMap() {
        return controls.get();
    }


    private Stream<AuthenticatorControl> initControls( )
    {
        Collection<Authenticator> authenticators = getAvailableAuthenticators().values();
        Map<String, AuthenticatorControl> nondefault = getModifiedControls( );
        Map<String, AuthenticatorControl> configControlMap = getConfigControls( );
        Spliterator<Authenticator> spliterator = Spliterators.spliterator( authenticators, Spliterator.NONNULL );
        return StreamSupport.stream( spliterator, false ).map( authenticator -> {
            final String id = authenticator.getId( );
            return nondefault.containsKey( id ) ? nondefault.get(id) : (configControlMap.containsKey( id ) ? configControlMap.get( id ) : new AuthenticatorControl( id, 100, AuthenticationControl.SUFFICIENT, true ));
        } ).sorted( Collections.reverseOrder( ) );
    }


    public String getId( )
    {
        return "Default Authentication Manager - " + this.getClass( ).getName( ) + " : managed authenticators - " +
            knownAuthenticators( );
    }

    public AuthenticationResult authenticate( AuthenticationDataSource source )
        throws AccountLockedException, AuthenticationException, MustChangePasswordException
    {
        List<Authenticator> authenticators = this.authenticators.get( );
        if ( authenticators == null || authenticators.size( ) == 0 )
        {
            return ( new AuthenticationResult( false, null, new AuthenticationException(
                "no valid authenticators, can't authenticate" ) ) );
        }

        // put AuthenticationResult exceptions in a map
        List<AuthenticationFailureCause> authnResultErrors = new ArrayList<AuthenticationFailureCause>( );
        for ( Authenticator authenticator : authenticators )
        {
            final AuthenticatorControl control = getControlMap( ).get( authenticator.getId( ) );
            assert control != null;
            if ( authenticator.isValid( ) && control.isActive())
            {
                if ( authenticator.supportsDataSource( source ) )
                {
                    AuthenticationResult authResult = authenticator.authenticate( source );
                    List<AuthenticationFailureCause> authenticationFailureCauses =
                        authResult.getAuthenticationFailureCauses( );

                    if ( authResult.isAuthenticated( ) )
                    {
                        //olamy: as we can chain various user managers with Archiva
                        // user manager authenticator can lock accounts in the following case :
                        // 2 user managers: ldap and jdo.
                        // ldap correctly find the user but cannot compare hashed password
                        // jdo reject password so increase loginAttemptCount
                        // now ldap bind authenticator work but loginAttemptCount has been increased.
                        // so we restore here loginAttemptCount to 0 if in authenticationFailureCauses

                        for ( AuthenticationFailureCause authenticationFailureCause : authenticationFailureCauses )
                        {
                            User user = authenticationFailureCause.getUser( );
                            if ( user != null )
                            {
                                if ( user.getCountFailedLoginAttempts( ) > 0 )
                                {
                                    user.setCountFailedLoginAttempts( 0 );
                                    if ( !userManager.isReadOnly( ) )
                                    {
                                        try
                                        {
                                            userManager.updateUser( user );
                                        }
                                        catch ( UserManagerException e )
                                        {
                                            log.debug( e.getMessage( ), e );
                                            log.warn( "skip error updating user: {}", e.getMessage( ) );
                                        }
                                    }
                                }
                            }
                        }
                        return authResult;
                    }

                    if ( authenticationFailureCauses != null )
                    {
                        authnResultErrors.addAll( authenticationFailureCauses );
                    }
                    else
                    {
                        if ( authResult.getException( ) != null )
                        {
                            authnResultErrors.add(
                                new AuthenticationFailureCause( AuthenticationConstants.AUTHN_RUNTIME_EXCEPTION,
                                    authResult.getException( ).getMessage( ) ) );
                        }
                    }


                }
            }
            else
            {
                log.warn( "Invalid authenticator found: " + authenticator.getId( ) );
            }
        }

        return ( new AuthenticationResult( false, null, new AuthenticationException(
            "authentication failed on authenticators: " + knownAuthenticators( ) ), authnResultErrors ) );
    }

    @Override
    public List<AuthenticatorControl> getControls( )
    {
        return getControlMap().values().stream().collect( Collectors.toList());
    }

    @Override
    public void setControls( List<AuthenticatorControl> controlList )
    {
        setModfiedControls( controlList.stream( ).collect( Collectors.toMap( c -> c.getName( ), c -> c ) ) );
        initializeOrder(  );
    }

    @Override
    public void modifyControl( AuthenticatorControl control )
    {
        Map<String, AuthenticatorControl> myControls = getModifiedControls( );
        if (availableAuthenticators.containsKey( control.getName() )) {
            myControls.put( control.getName( ), control );
        } else {
            log.warn( "Cannot modify control for authenticator {}. It does not exist.", control.getName( ) );
        }
        initializeOrder();
    }

    public List<Authenticator> getAuthenticators( )
    {
        return authenticators.get();
    }

    private String knownAuthenticators( )
    {
        StringBuilder strbuf = new StringBuilder( );

        for ( Authenticator authenticator : getAuthenticators() )
        {
            strbuf.append( '(' ).append( authenticator.getId( ) ).append( ") " );
        }

        return strbuf.toString( );
    }
}
