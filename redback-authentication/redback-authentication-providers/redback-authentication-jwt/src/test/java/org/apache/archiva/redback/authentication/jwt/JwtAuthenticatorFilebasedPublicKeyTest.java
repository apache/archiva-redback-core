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

import org.apache.archiva.components.registry.RegistryException;
import org.apache.archiva.redback.configuration.UserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.archiva.redback.configuration.UserConfigurationKeys.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
class JwtAuthenticatorFilebasedPublicKeyTest extends AbstractJwtTest
{

    @BeforeEach
    void init() throws RegistryException, UserConfigurationException
    {
        Map<String, String> params = new HashMap<>();
        params.put( AUTHENTICATION_JWT_KEYSTORETYPE, AUTHENTICATION_JWT_KEYSTORETYPE_PLAINFILE );
        params.put( AUTHENTICATION_JWT_SIGALG, AUTHENTICATION_JWT_SIGALG_RS256 );
        super.init( params );
    }

    @AfterEach
    void clean() {
        Path file = Paths.get( jwtAuthenticator.DEFAULT_KEYFILE ).toAbsolutePath();
        try
        {
            Files.deleteIfExists( file );
        }
        catch ( IOException e )
        {
            try
            {
                Files.move( file, file.getParent().resolve( file.getFileName().toString()+"." + System.currentTimeMillis( ) ) );
            }
            catch ( IOException ioException )
            {
                ioException.printStackTrace();
            }
            //
        }
    }

    @Test
    @Override
    void usesSymmetricAlgorithm( )
    {
        assertFalse( jwtAuthenticator.usesSymmetricAlgorithm( ) );
    }

    @Test
    @Override
    void getSignatureAlgorithm( )
    {
        assertEquals( "RS256", jwtAuthenticator.getSignatureAlgorithm( ) );
    }

    @Test
    void keyFileExists() throws IOException
    {
        Path path = jwtAuthenticator.getKeystoreFilePath( );
        assertNotNull( path );
        assertTrue( Files.exists( path ) );
        Properties props = new Properties( );
        try ( InputStream in = Files.newInputStream( path ) )
        {
            props.loadFromXML( in );
            assertTrue( StringUtils.isNotEmpty( props.getProperty( JwtAuthenticator.PROP_PRIV_ALG ) ) );
            assertTrue( StringUtils.isNotEmpty( props.getProperty( JwtAuthenticator.PROP_PRIVATEKEY ) ) );
        }
    }

    @Test
    void getKeystoreType( )
    {
        assertEquals( "plainfile", jwtAuthenticator.getKeystoreType( ) );
    }

    @Test
    void getKeystoreFilePath( )
    {
        assertNotNull( jwtAuthenticator.getKeystoreFilePath( ) );
        assertEquals( "jwt-key.xml", jwtAuthenticator.getKeystoreFilePath( ).getFileName().toString() );
    }

}