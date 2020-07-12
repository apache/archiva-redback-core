package org.apache.archiva.redback.authentication.jwt;

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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import org.apache.archiva.components.registry.RegistryException;
import org.apache.archiva.components.registry.commons.CommonsConfigurationRegistry;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.BearerTokenAuthenticationDataSource;
import org.apache.archiva.redback.authentication.PasswordBasedAuthenticationDataSource;
import org.apache.archiva.redback.authentication.Token;
import org.apache.archiva.redback.authentication.TokenBasedAuthenticationDataSource;
import org.apache.archiva.redback.configuration.DefaultUserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationException;
import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public abstract class AbstractJwtTest
{
    protected JwtAuthenticator jwtAuthenticator;
    protected DefaultUserConfiguration configuration;
    protected CommonsConfigurationRegistry registry;
    protected BaseConfiguration saveConfig;

    protected void init( Map<String, String> parameters) throws UserConfigurationException, RegistryException, AuthenticationException
    {
        this.registry = new CommonsConfigurationRegistry( );
        String baseDir = System.getProperty( "basedir", "" );
        if ( !StringUtils.isEmpty( baseDir ) && !StringUtils.endsWith(baseDir, "/" ) )
        {
            baseDir = baseDir + "/";
        }
        this.registry.setInitialConfiguration( "<configuration>\n" +
            "          <system/>\n" +
            "          <properties fileName=\""+baseDir+"src/test/resources/security.properties\" config-optional=\"true\"\n" +
            "                      config-at=\"org.apache.archiva.redback\"/>\n" +
            "        </configuration>" );
        this.registry.initialize();
        this.saveConfig = new BaseConfiguration( );
        this.registry.addConfiguration( this.saveConfig, "save", "org.apache.archiva.redback" );
        for (Map.Entry<String, String> entry :  parameters.entrySet())
        {
            saveConfig.setProperty( entry.getKey( ), entry.getValue( ) );
        }

        this.configuration = new DefaultUserConfiguration( );
        this.configuration.setRegistry( registry );
        this.configuration.initialize();

        jwtAuthenticator = new JwtAuthenticator( );
        jwtAuthenticator.setUserConfiguration( configuration );
        jwtAuthenticator.init( );
    }

    @Test
    void getId( )
    {
        assertEquals( "JwtAuthenticator", jwtAuthenticator.getId( ) );
    }

    @Test
    void supportsDataSource( )
    {
        assertTrue( jwtAuthenticator.supportsDataSource( new BearerTokenAuthenticationDataSource(  ) ) );
        assertFalse( jwtAuthenticator.supportsDataSource( new TokenBasedAuthenticationDataSource( ) ) );
        assertFalse( jwtAuthenticator.supportsDataSource( new PasswordBasedAuthenticationDataSource( ) ) );
    }


    @Test
    void generateToken( )
    {
        Token token = jwtAuthenticator.generateToken( "frodo" );
        assertNotNull( token );
        assertTrue( token.getData( ).length( ) > 0 );
        Jws<Claims> parsed = jwtAuthenticator.parseToken( token.getData( ) );
        assertNotNull( parsed.getHeader( ).get( JwsHeader.KEY_ID ) );
        assertNotNull( token.getMetadata( ).created( ) );
        try
        {
            Thread.sleep( 2 );
        }
        catch ( InterruptedException e )
        {
            //
        }

        assertTrue( Instant.now( ).isAfter( token.getMetadata( ).created( ) ) );
        assertTrue( Instant.now( ).isBefore( token.getMetadata( ).validBefore( ) ) );
    }


    @Test
    void authenticate( )
    {
    }

    @Test
    void renewSigningKey( )
    {

        assertEquals( 5, jwtAuthenticator.getMaxInMemoryKeys( ) );
        assertEquals( 1, jwtAuthenticator.getCurrentKeyListSize( ) );
        jwtAuthenticator.renewSigningKey( );
        assertEquals( 2, jwtAuthenticator.getCurrentKeyListSize( ) );
        jwtAuthenticator.renewSigningKey( );
        assertEquals( 3, jwtAuthenticator.getCurrentKeyListSize( ) );
        jwtAuthenticator.renewSigningKey( );
        assertEquals( 4, jwtAuthenticator.getCurrentKeyListSize( ) );
        jwtAuthenticator.renewSigningKey( );
        assertEquals( 5, jwtAuthenticator.getCurrentKeyListSize( ) );
        jwtAuthenticator.renewSigningKey( );
        assertEquals( 5, jwtAuthenticator.getCurrentKeyListSize( ) );
        jwtAuthenticator.renewSigningKey( );
        assertEquals( 5, jwtAuthenticator.getCurrentKeyListSize( ) );


    }

    @Test
    void verify( ) throws TokenAuthenticationException
    {
        Token token = jwtAuthenticator.generateToken( "frodo_baggins" );
        assertEquals( "frodo_baggins", jwtAuthenticator.verify( token.getData( ) ) );
    }

    @Test
    void usesSymmetricAlgorithm( )
    {
        assertTrue( jwtAuthenticator.usesSymmetricAlgorithm( ) );
    }

    @Test
    void getSignatureAlgorithm( )
    {
        assertEquals( "HS384", jwtAuthenticator.getSignatureAlgorithm( ) );
    }

    @Test
    void getMaxInMemoryKeys( )
    {
        assertEquals( 5, jwtAuthenticator.getMaxInMemoryKeys( ) );
    }

    @Order( 0 )
    @Test
    void getCurrentKeyListSize( )
    {
        assertEquals( 1, jwtAuthenticator.getCurrentKeyListSize( ) );
    }

    @Test
    void invalidKeySignature() throws TokenAuthenticationException
    {
        Token token = jwtAuthenticator.generateToken( "samwise_gamgee" );
        assertEquals( "samwise_gamgee", jwtAuthenticator.verify( token.getData( ) ) );
        jwtAuthenticator.revokeSigningKeys( );
        assertThrows( TokenAuthenticationException.class, ( ) -> {
            jwtAuthenticator.verify( token.getData( ) );
        } );
    }


    @Test
    void invalidKeyDate( )
    {
        Duration lifetime = jwtAuthenticator.getTokenLifetime( );
        try
        {
            jwtAuthenticator.setTokenLifetime( Duration.ofNanos( 0 ) );
            Token token = jwtAuthenticator.generateToken( "samwise_gamgee" );
            assertThrows( TokenAuthenticationException.class, ( ) -> {
                jwtAuthenticator.verify( token.getData( ) );
            } );
        } finally
        {
            jwtAuthenticator.setTokenLifetime( lifetime );
        }

    }

    @Test
    void validAuthenticate() throws AuthenticationException
    {
        Token token = jwtAuthenticator.generateToken( "bilbo_baggins" );
        BearerTokenAuthenticationDataSource source = new BearerTokenAuthenticationDataSource( );
        source.setTokenData( token.getData() );
        AuthenticationResult result = jwtAuthenticator.authenticate( source );
        assertNotNull( result );
        assertTrue( result.isAuthenticated( ) );
        assertEquals( "bilbo_baggins", result.getPrincipal( ) );
    }

    @Test
    void invalidAuthenticate() throws AuthenticationException
    {
        BearerTokenAuthenticationDataSource source = new BearerTokenAuthenticationDataSource( );
        source.setTokenData( "invalidToken" );
        AuthenticationResult result = jwtAuthenticator.authenticate( source );
        assertNotNull( result );
        assertFalse( result.isAuthenticated( ) );
    }


}
