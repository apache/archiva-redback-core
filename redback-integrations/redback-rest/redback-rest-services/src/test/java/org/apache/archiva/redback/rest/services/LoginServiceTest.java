package org.apache.archiva.redback.rest.services;
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
import org.apache.archiva.redback.rest.api.model.LoginRequest;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.services.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Olivier Lamy
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration(
        locations = { "classpath:/spring-context.xml" } )
public class LoginServiceTest
    extends AbstractRestServicesTest
{
    @Test
    public void loginAdmin()
        throws Exception
    {
        assertNotNull( getLoginService( null ).logIn( new LoginRequest( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME,
                                                                        BaseSetup.getAdminPwd() ) ) );
    }

    @Test
    public void ping()
        throws Exception
    {
        assertNotNull( getLoginService( null ).ping( ) );
    }

    @Test
    public void createUserThenLog()
        throws Exception
    {
        try
        {

            // START SNIPPET: create-user
            User user = new User( "toto", "toto the king", "toto@toto.fr", false, false );
            user.setPassword( "foo123" );
            user.setPermanent( false );
            user.setPasswordChangeRequired( false );
            user.setLocked( false );
            user.setValidated( true );
            UserService userService = getUserService( authorizationHeader );
            userService.createUser( user );
            // END SNIPPET: create-user
            user = userService.getUser( "toto" );
            assertNotNull( user );
            assertEquals( "toto the king", user.getFullName() );
            assertEquals( "toto@toto.fr", user.getEmail() );
            getLoginService( encode( "toto", "foo123" ) ).pingWithAutz();
        }
        finally
        {
            getUserService( authorizationHeader ).deleteUser( "toto" );
            getUserService( authorizationHeader ).removeFromCache( "toto" );
            assertNull( getUserService( authorizationHeader ).getUser( "toto" ) );
        }
    }

}
