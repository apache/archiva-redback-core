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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.model.GrantType;
import org.apache.archiva.redback.rest.api.model.RequestTokenRequest;
import org.apache.archiva.redback.rest.api.model.TokenResponse;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.services.BaseSetup;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Olivier Lamy
 */
@ExtendWith( SpringExtension.class )
@ContextConfiguration(
        locations = { "classpath:/spring-context.xml" } )
public class AuthenticationServiceTest
    extends AbstractRestServicesTestV2
{
    @BeforeEach
    void setup() throws Exception
    {
        super.init();
        super.startServer();
    }

    @AfterEach
    void stop() throws Exception
    {
        super.stopServer();
        super.destroy();
    }

    @Test
    public void loginAdmin()
        throws Exception
    {
        RequestTokenRequest request = new RequestTokenRequest( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME,
            BaseSetup.getAdminPwd() );
        request.setGrantType( GrantType.AUTHORIZATION_CODE );


        assertNotNull( getLoginServiceV2( null ).logIn( request ) );
    }

    @Test
    public void createUserThenLog()
        throws Exception
    {
        try
        {

            // START SNIPPET: create-user
            User user = addUser( "toto", "foo123", "toto the king", "toto@toto.fr" );
            // END SNIPPET: create-user
            assertNotNull( user );
            assertEquals( "toto the king", user.getFullName() );
            assertEquals( "toto@toto.fr", user.getEmail() );
            getLoginServiceV2( getAuthHeader( "toto" ) ).pingWithAutz();
        }
        finally
        {
            deleteUser( "toto" );
        }
    }

    @Test
    public void simpleLogin() throws RedbackServiceException, UserManagerException
    {
        String authorizationHeader = getAdminAuthzHeader( );
        try
        {

            // START SNIPPET: create-user
            User user = addUser( "toto", "foo123", "toto the king", "toto@toto.fr" );
            // END SNIPPET: create-user
            RequestTokenRequest request = new RequestTokenRequest( "toto", "foo123" );
            request.setGrantType( GrantType.AUTHORIZATION_CODE );
            TokenResponse result = getLoginServiceV2( "" ).logIn( request );
            assertNotNull( result );
            assertTrue( StringUtils.isNotEmpty( result.getAccessToken( ) ) );

        }
        finally
        {
            deleteUser( "toto" );
        }

    }

}
