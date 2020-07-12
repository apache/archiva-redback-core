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
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.configuration.UserConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;
import java.util.Map;

import static org.apache.archiva.redback.configuration.UserConfigurationKeys.AUTHENTICATION_JWT_KEYSTORETYPE;
import static org.apache.archiva.redback.configuration.UserConfigurationKeys.AUTHENTICATION_JWT_KEYSTORETYPE_MEMORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@TestMethodOrder( MethodOrderer.OrderAnnotation.class )
class JwtAuthenticatorMemorybasedTest extends AbstractJwtTest
{
    @BeforeEach
    void init() throws RegistryException, UserConfigurationException, AuthenticationException
    {
        Map<String, String> params = new HashMap<>();
        params.put( AUTHENTICATION_JWT_KEYSTORETYPE, AUTHENTICATION_JWT_KEYSTORETYPE_MEMORY );
        super.init( params );
    }


    @Test
    void authenticate( )
    {
    }

    @Test
    void getKeystoreType( )
    {
        assertEquals( "memory", jwtAuthenticator.getKeystoreType( ) );
    }

    @Test
    void getKeystoreFilePath( )
    {
        assertNull( jwtAuthenticator.getKeystoreFilePath( ) );
    }

    @Test
    void getMaxInMemoryKeys( )
    {
        assertEquals( 5, jwtAuthenticator.getMaxInMemoryKeys( ) );
    }


}