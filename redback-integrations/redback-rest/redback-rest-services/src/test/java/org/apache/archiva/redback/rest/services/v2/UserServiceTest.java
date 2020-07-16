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

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.apache.archiva.redback.rest.api.model.Operation;
import org.apache.archiva.redback.rest.api.model.Permission;
import org.apache.archiva.redback.rest.api.model.RequestTokenRequest;
import org.apache.archiva.redback.rest.api.model.ResetPasswordRequest;
import org.apache.archiva.redback.rest.api.model.TokenResponse;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.model.UserRegistrationRequest;
import org.apache.archiva.redback.rest.api.services.UserService;
import org.apache.archiva.redback.rest.services.FakeCreateAdminService;
import org.apache.archiva.redback.rest.services.mock.EmailMessage;
import org.apache.archiva.redback.rest.services.mock.MockJavaMailSender;
import org.apache.archiva.redback.rest.services.mock.ServicesAssert;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Olivier Lamy
 */
@ExtendWith( SpringExtension.class )
@ContextConfiguration(
    locations = {"classpath:/spring-context.xml"} )
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
public class UserServiceTest
    extends AbstractRestServicesTestV2
{

    @Inject
    MockJavaMailSender mockJavaMailSender;

    @AfterEach
    void cleanup() {
        mockJavaMailSender.getSendedEmails( ).clear( );
    }

    @BeforeAll
    void setup( ) throws Exception
    {
        super.init( );
        super.startServer( );
    }

    @AfterAll
    void shutdown( ) throws Exception
    {
        super.stopServer( );
        super.destroy( );
    }

    private UserService getUserService( String authzHeader )
    {
        UserService service =
            JAXRSClientFactory.create( "http://localhost:" + getServerPort( ) + "/" + getRestServicesPath( ) + "/v2/redback/",
                UserService.class, Collections.singletonList( new JacksonJaxbJsonProvider( ) ) );

        // time out for debuging purpose
        WebClient.getConfig( service ).getHttpConduit( ).getClient( ).setReceiveTimeout( getTimeout( ) );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.client( service ).header( "Referer", "http://localhost:" + getServerPort( ) );
        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );

        return service;
    }

    @Test
    public void ping( )
        throws Exception
    {
        Boolean res = getUserService( null ).ping( );
        assertTrue( res );
    }

    @Test
    public void getUsers( )
        throws Exception
    {
        String adminHeader = getAdminAuthzHeader( );
        UserService userService = getUserService( adminHeader );
        List<User> users = userService.getUsers( );
        assertNotNull( users );
        assertFalse( users.isEmpty( ) );
    }

    @Test()
    @Disabled
    public void getUsersWithoutAuthz( )
        throws Exception
    {
        UserService userService = getUserService( null );
        assertThrows( ForbiddenException.class, ( ) -> {
            try
            {
                userService.getUsers( );
            }
            catch ( ForbiddenException e )
            {
                assertEquals( 403, e.getResponse( ).getStatus( ) );
                throw e;
            }
        } );


    }

    @Test
    public void getNoPermissionNotAuthz( )
    {

        UserService userService = getUserService( null );
        WebClient.client( userService ).header( "Origin", "http://localhost/myrequest" );

        try
        {
            getFakeCreateAdminService( ).testAuthzWithoutKarmasNeededButAuthz( );
        }
        catch ( ForbiddenException e )
        {
            assertEquals( 403, e.getResponse( ).getStatus( ) );
            throw e;
        }
    }

    @Test
    public void getNoPermissionAuthz( )
    {

        try
        {
            FakeCreateAdminService service = getFakeCreateAdminService( );

            WebClient.client( service ).header( "Authorization", getAdminAuthzHeader( ) );

            assertTrue( service.testAuthzWithoutKarmasNeededButAuthz( ) );

        }
        catch ( ForbiddenException e )
        {
            assertEquals( 403, e.getResponse( ).getStatus( ) );
            throw e;
        }
    }

    @Test
    @Disabled
    public void register( )
        throws Exception
    {
        try
        {
            mockJavaMailSender.getSendedEmails( ).clear( );
            UserService service = getUserService( getAdminAuthzHeader( ) );
            User u = new User( );
            u.setFullName( "the toto" );
            u.setUsername( "toto" );
            u.setEmail( "toto@toto.fr" );
            u.setPassword( "toto123" );
            u.setConfirmPassword( "toto123" );
            String key = service.registerUser( new UserRegistrationRequest( u, "http://wine.fr/bordeaux" ) ).getKey( );

            assertNotEquals( "-1", key );

            ServicesAssert assertService =
                JAXRSClientFactory.create( "http://localhost:" + getServerPort( ) + "/" + getRestServicesPath( ) + "/testsService/",
                    ServicesAssert.class,
                    Collections.singletonList( new JacksonJaxbJsonProvider( ) ) );

            List<EmailMessage> emailMessages = assertService.getEmailMessageSended( );
            assertEquals( 1, emailMessages.size( ) );
            assertEquals( "toto@toto.fr", emailMessages.get( 0 ).getTos( ).get( 0 ) );

            assertEquals( "Welcome", emailMessages.get( 0 ).getSubject( ) );
            String messageContent = emailMessages.get( 0 ).getText( );

            log.info( "messageContent: {}", messageContent );

            assertNotNull( messageContent );
            assertTrue( messageContent.contains( "Use the following URL to validate your account." ) );
            assertTrue( messageContent.contains( "http://wine.fr/bordeaux" ) );
            assertTrue( messageContent.contains( "toto" ) );

            assertTrue( service.validateUserFromKey( key ).isSuccess( ) );

            service = getUserService( getAdminAuthzHeader( ) );

            u = service.getUser( "toto" );

            assertNotNull( u );
            assertTrue( u.isValidated( ) );
            assertTrue( u.isPasswordChangeRequired( ) );

            assertTrue( service.validateUserFromKey( key ).isSuccess( ) );

        }
        catch ( Exception e )
        {
            log.error( e.getMessage( ), e );
            throw e;
        }
        finally
        {
            deleteUserQuietly( "toto" );
        }

    }

    @Test
    public void registerNoUrl( )
        throws Exception
    {
        try
        {
            UserService service = getUserService( getAdminAuthzHeader( ) );
            User u = new User( );
            u.setFullName( "the toto" );
            u.setUsername( "toto" );
            u.setEmail( "toto@toto.fr" );
            u.setPassword( "toto123" );
            u.setConfirmPassword( "toto123" );
            String key = service.registerUser( new UserRegistrationRequest( u, null ) ).getKey( );

            assertNotEquals( "-1", key );

            ServicesAssert assertService =
                JAXRSClientFactory.create( "http://localhost:" + getServerPort( ) + "/" + getRestServicesPath( ) + "/testsService/",
                    ServicesAssert.class,
                    Collections.singletonList( new JacksonJaxbJsonProvider( ) ) );

            List<EmailMessage> emailMessages = assertService.getEmailMessageSended( );
            assertEquals( 1, emailMessages.size( ) );
            assertEquals( "toto@toto.fr", emailMessages.get( 0 ).getTos( ).get( 0 ) );

            assertEquals( "Welcome", emailMessages.get( 0 ).getSubject( ) );
            String messageContent = emailMessages.get( 0 ).getText( );

            log.info( "messageContent: {}", messageContent );
            assertNotNull( messageContent );
            assertTrue( messageContent.contains( "Use the following URL to validate your account." ) );
            assertTrue( messageContent.contains( "http://localhost:" + getServerPort( ) ) );
            assertTrue( messageContent.toLowerCase( ).contains( "toto" ) );

            assertTrue( service.validateUserFromKey( key ).isSuccess( ) );

            service = getUserService( getAdminAuthzHeader( ) );

            u = service.getUser( "toto" );

            assertNotNull( u );
            assertTrue( u.isValidated( ) );
            assertTrue( u.isPasswordChangeRequired( ) );

            assertTrue( service.validateUserFromKey( key ).isSuccess( ) );

        }
        catch ( Exception e )
        {
            log.error( e.getMessage( ), e );
            throw e;
        }
        finally
        {
            deleteUserQuietly( "toto" );
        }

    }

    @Test
    @Disabled
    public void resetPassword( )
        throws Exception
    {
        try
        {
            mockJavaMailSender.getSendedEmails().clear();

            UserService service = getUserService( getAdminAuthzHeader( ) );
            User u = new User( );
            u.setFullName( "the toto" );
            u.setUsername( "toto" );
            u.setEmail( "toto@toto.fr" );
            u.setPassword( "toto123" );
            u.setConfirmPassword( "toto123" );
            String key = service.registerUser( new UserRegistrationRequest( u, "http://wine.fr/bordeaux" ) ).getKey( );

            assertNotEquals( "-1", key );

            ServicesAssert assertService =
                JAXRSClientFactory.create( "http://localhost:" + getServerPort( ) + "/" + getRestServicesPath( ) + "/testsService/",
                    ServicesAssert.class,
                    Collections.singletonList( new JacksonJaxbJsonProvider( ) ) );

            WebClient.client( assertService ).accept( MediaType.APPLICATION_JSON_TYPE );
            WebClient.client( assertService ).type( MediaType.APPLICATION_JSON_TYPE );

            List<EmailMessage> emailMessages = assertService.getEmailMessageSended( );
            assertEquals( 1, emailMessages.size( ) );
            assertEquals( "toto@toto.fr", emailMessages.get( 0 ).getTos( ).get( 0 ) );

            assertEquals( "Welcome", emailMessages.get( 0 ).getSubject( ) );
            assertTrue(
                emailMessages.get( 0 ).getText( ).contains( "Use the following URL to validate your account." ) );

            assertTrue( service.validateUserFromKey( key ).isSuccess( ) );

            service = getUserService( getAdminAuthzHeader( ) );

            u = service.getUser( "toto" );

            assertNotNull( u );
            assertTrue( u.isValidated( ) );
            assertTrue( u.isPasswordChangeRequired( ) );

            assertTrue( service.validateUserFromKey( key ).isSuccess( ) );

            assertTrue( service.resetPassword( new ResetPasswordRequest( "toto", "http://foo.fr/bar" ) ).isSuccess( ) );

            emailMessages = assertService.getEmailMessageSended( );
            assertEquals( 2, emailMessages.size( ) );
            assertEquals( "toto@toto.fr", emailMessages.get( 1 ).getTos( ).get( 0 ) );

            String messageContent = emailMessages.get( 1 ).getText( );

            assertNotNull( messageContent );
            assertTrue( messageContent.contains( "Password Reset" ) );
            assertTrue( messageContent.contains( "Username: toto" ) );
            assertTrue( messageContent.contains( "http://foo.fr/bar" ) );


        }
        catch ( Exception e )
        {
            log.error( e.getMessage( ), e );
            throw e;
        }
        finally
        {
            deleteUserQuietly( "toto" );
        }

    }

    private void deleteUserQuietly( String userName )
    {
        try
        {
            getUserService( getAdminAuthzHeader( ) ).deleteUser( userName );
        }
        catch ( Exception e )
        {
            log.warn( "ignore fail to delete user " + e.getMessage( ), e );
        }
    }

    @Test
    public void getAdminPermissions( )
        throws Exception
    {
        Collection<Permission> permissions = getUserService( getAdminAuthzHeader( ) ).getUserPermissions( "admin" );
        log.info( "admin permisssions: {}", permissions );
    }

    @Test
    public void getGuestPermissions( )
        throws Exception
    {
        createGuestIfNeeded( );
        Collection<Permission> permissions = getUserService( null ).getCurrentUserPermissions( );
        log.info( "guest permisssions: {}", permissions );
    }

    @Test
    public void getAdminOperations( )
        throws Exception
    {
        Collection<Operation> operations = getUserService( getAdminAuthzHeader( ) ).getUserOperations( "admin" );
        log.info( "admin operations: {}", operations );
    }

    @Test
    public void getGuestOperations( )
        throws Exception
    {
        createGuestIfNeeded( );
        Collection<Operation> operations = getUserService( null ).getCurrentUserOperations( );
        log.info( "guest operations: {}", operations );
    }

    @Test
    public void updateMe( )
        throws Exception
    {
        User u = new User( );
        u.setFullName( "the toto" );
        u.setUsername( "toto" );
        u.setEmail( "toto@toto.fr" );
        u.setPassword( "toto123" );
        u.setConfirmPassword( "toto123" );
        u.setValidated( true );
        getUserService( getAdminAuthzHeader( ) ).createUser( u );

        u.setFullName( "the toto123" );
        u.setEmail( "toto@titi.fr" );
        u.setPassword( "toto1234" );
        u.setPreviousPassword( "toto123" );
        getUserService( getUserAuthzHeader( "toto" ) ).updateMe( u );

        u = getUserService( getAdminAuthzHeader( ) ).getUser( "toto" );
        assertEquals( "the toto123", u.getFullName( ) );
        assertEquals( "toto@titi.fr", u.getEmail( ) );

        u.setFullName( "the toto1234" );
        u.setEmail( "toto@tititi.fr" );
        u.setPassword( "toto12345" );
        u.setPreviousPassword( "toto1234" );
        getUserService( getUserAuthzHeader( "toto" )) .updateMe( u );

        u = getUserService( getAdminAuthzHeader( ) ).getUser( "toto" );
        assertEquals( "the toto1234", u.getFullName( ) );
        assertEquals( "toto@tititi.fr", u.getEmail( ) );

        getUserService( getAdminAuthzHeader( ) ).deleteUser( "toto" );
    }

    @Test
    @Disabled
    public void lockUnlockUser( )
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
            UserService userService = getUserService( getAdminAuthzHeader( ) );
            userService.createUser( user );
            // END SNIPPET: create-user
            user = userService.getUser( "toto" );
            assertNotNull( user );
            assertEquals( "toto the king", user.getFullName( ) );
            assertEquals( "toto@toto.fr", user.getEmail( ) );
            TokenResponse result = getLoginServiceV2( null ).logIn( new RequestTokenRequest( "toto", "foo123" ) );
            getLoginServiceV2( "Bearer " + result.getAccessToken( ) ).pingWithAutz( );

            userService.lockUser( "toto" );

            assertTrue( userService.getUser( "toto" ).isLocked( ) );

            userService.unlockUser( "toto" );

            assertFalse( userService.getUser( "toto" ).isLocked( ) );
        }
        finally
        {
            getUserService( getAdminAuthzHeader( ) ).deleteUser( "toto" );
            getUserService( getAdminAuthzHeader( ) ).removeFromCache( "toto" );
            assertNull( getUserService( getAdminAuthzHeader( ) ).getUser( "toto" ) );
        }
    }

    public void guestUserCreate( )
        throws Exception
    {
        UserService userService = getUserService( getAdminAuthzHeader( ) );
        assertNull( userService.getGuestUser( ) );
        assertNull( userService.createGuestUser( ) );

    }

    protected void createGuestIfNeeded( )
        throws Exception
    {
        UserService userService = getUserService( getAdminAuthzHeader( ) );
        if ( userService.getGuestUser( ) == null )
        {
            userService.createGuestUser( );
        }
    }

}
