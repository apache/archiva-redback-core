package org.apache.archiva.redback.authentication.ldap;

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

import org.apache.archiva.redback.authentication.AbstractAuthenticator;
import org.apache.archiva.redback.common.ldap.LdapUtils;
import org.apache.archiva.redback.common.ldap.connection.DefaultLdapConnection;
import org.apache.archiva.redback.common.ldap.connection.LdapConnection;
import org.apache.archiva.redback.common.ldap.user.UserMapper;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.commons.lang3.StringUtils;
import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.Authenticator;
import org.apache.archiva.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.apache.archiva.redback.common.ldap.connection.LdapException;
import org.apache.archiva.redback.users.ldap.service.LdapCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * LdapBindAuthenticator:
 *
 * @author: Jesse McConnell
 */
@Service( "authenticator#ldap" )
public class LdapBindAuthenticator
    extends AbstractAuthenticator
    implements Authenticator
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "userMapper#ldap" )
    private UserMapper mapper;

    @Inject
    @Named( value = "ldapConnectionFactory#configurable" )
    private LdapConnectionFactory connectionFactory;

    @Inject
    @Named( value = "userConfiguration#default" )
    private UserConfiguration config;

    @Inject
    private LdapCacheService ldapCacheService;

    public String getId()
    {
        return "LdapBindAuthenticator";
    }

    public AuthenticationResult authenticate( AuthenticationDataSource s )
        throws AuthenticationException
    {
        PasswordBasedAuthenticationDataSource source = (PasswordBasedAuthenticationDataSource) s;

        if ( !config.getBoolean( UserConfigurationKeys.LDAP_BIND_AUTHENTICATOR_ENABLED ) || (
            !config.getBoolean( UserConfigurationKeys.LDAP_BIND_AUTHENTICATOR_ALLOW_EMPTY_PASSWORDS, false )
                && StringUtils.isEmpty( source.getPassword() ) ) )
        {
            return new AuthenticationResult( false, source.getUsername(), null );
        }

        SearchControls ctls = new SearchControls();

        ctls.setCountLimit( 1 );

        ctls.setDerefLinkFlag( true );
        ctls.setSearchScope( SearchControls.SUBTREE_SCOPE );

        String filter = "(&(objectClass=" + mapper.getUserObjectClass() + ")" + ( mapper.getUserFilter() != null
            ? mapper.getUserFilter()
            : "" ) + "(" + mapper.getUserIdAttribute() + "=" + LdapUtils.encodeFilterValue( source.getUsername() ) + "))";

        log.debug( "Searching for users with filter: '{}' from base dn: {}", filter, mapper.getUserBaseDn() );

        LdapConnection ldapConnection = null;
        LdapConnection authLdapConnection = null;
        NamingEnumeration<SearchResult> results = null;
        try
        {
            ldapConnection = getLdapConnection();
            // check the cache for user's userDn in the ldap server
            String userDn = ldapCacheService.getLdapUserDn( source.getUsername() );

            if ( userDn == null )
            {
                log.debug( "userDn for user {} not found in cache. Retrieving from ldap server..",
                           source.getUsername() );

                DirContext context = ldapConnection.getDirContext();

                results = context.search( mapper.getUserBaseDn(), filter, ctls );

                boolean moreElements = results.hasMoreElements();

                log.debug( "Found user '{}': {}", source.getUsername(), moreElements );

                if ( moreElements )
                {
                    try {
                        SearchResult result = results.nextElement();

                        userDn = result.getNameInNamespace();

                        log.debug("Adding userDn {} for user {} to the cache..", userDn, source.getUsername());

                        // REDBACK-289/MRM-1488 cache the ldap user's userDn to lessen calls to ldap server
                        ldapCacheService.addLdapUserDn(source.getUsername(), userDn);
                    } catch (Exception e) {
                        log.error("Error occured on LDAP result retrieval: {}, {}", userDn, e.getMessage());
                        return new AuthenticationResult( false, source.getUsername(), e);
                    }
                }
                else
                {
                    return new AuthenticationResult( false, source.getUsername(), null );
                }
            }

            log.debug( "Attempting Authenication: {}", userDn );

            authLdapConnection = connectionFactory.getConnection( userDn, source.getPassword() );

            log.info( "user '{}' authenticated", source.getUsername() );

            return new AuthenticationResult( true, source.getUsername(), null );
        }
        catch ( LdapException e )
        {
            return new AuthenticationResult( false, source.getUsername(), e );
        }
        catch ( NamingException e )
        {
            return new AuthenticationResult( false, source.getUsername(), e );
        }
        finally
        {
            closeNamingEnumeration( results );
            closeLdapConnection( ldapConnection );
            if ( authLdapConnection != null )
            {
                closeLdapConnection( authLdapConnection );
            }
        }
    }

    public boolean supportsDataSource( AuthenticationDataSource source )
    {
        return ( source instanceof PasswordBasedAuthenticationDataSource );
    }

    private LdapConnection getLdapConnection()
        throws LdapException
    {
        return connectionFactory.getConnection();
    }

    private void closeLdapConnection( LdapConnection ldapConnection )
    {
        if ( ldapConnection != null )
        {
            ldapConnection.close();
        }
    }

    private void closeNamingEnumeration( NamingEnumeration<SearchResult> results )
    {
        try
        {
            if ( results != null )
            {
                results.close();
            }
        }
        catch ( NamingException e )
        {
            log.warn( "skip exception closing naming search result {}", e.getMessage() );
        }
    }

    @Override
    public boolean isValid() {
        return connectionFactory.isValid();
    }
}
