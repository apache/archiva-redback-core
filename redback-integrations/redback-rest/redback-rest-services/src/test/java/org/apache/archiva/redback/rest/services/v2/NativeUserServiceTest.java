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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import io.restassured.response.Response;
import org.apache.archiva.redback.rest.api.model.v2.Operation;
import org.apache.archiva.redback.rest.api.model.v2.Permission;
import org.apache.archiva.redback.rest.api.model.v2.RegistrationKey;
import org.apache.archiva.redback.rest.api.model.v2.User;
import org.apache.archiva.redback.rest.api.model.v2.UserInfo;
import org.apache.archiva.redback.rest.api.model.v2.VerificationStatus;
import org.apache.archiva.redback.rest.services.mock.EmailMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@ExtendWith( SpringExtension.class )
@ContextConfiguration(
    locations = {"classpath:/ldap-spring-test.xml"} )
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@Tag( "rest-native" )
@TestMethodOrder( MethodOrderer.Random.class )
@DisplayName( "Native REST tests for V2 UserService" )
public class NativeUserServiceTest extends AbstractNativeRestServices
{
    @Override
    protected String getServicePath( )
    {
        return "/users";
    }

    @BeforeAll
    void setup( ) throws Exception
    {
        super.setupNative( );
    }

    @AfterAll
    void destroy( ) throws Exception
    {
        super.shutdownNative( );
    }

    @Test
    void getUsers( )
    {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( ).get( ).then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        List<UserInfo> userData = response.body( ).jsonPath( ).getList( "data", UserInfo.class );
        assertNotNull( userData );
        assertEquals( 2, userData.size( ) );
        assertEquals( Integer.valueOf( 0 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
        assertEquals( Integer.valueOf( 1000 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
        assertEquals( Integer.valueOf( 2 ), response.body( ).jsonPath( ).get( "pagination.totalCount" ) );
    }

    @Nested
    @DisplayName( "Test User queries" )
    @ContextConfiguration(
        locations = {"classpath:/ldap-spring-test.xml"} )
    @TestInstance( TestInstance.Lifecycle.PER_CLASS )
    class TestUserRetrieval
    {
        int userNum = 25;
        String token;

        @BeforeAll
        void initUsers( )
        {
            this.token = getAdminToken( );
            for ( int i = 0; i < userNum; i++ )
            {
                String suffix = String.format( "%03d", i );
                String reverseSuffix = String.format( "%03d", userNum - i );
                String modSuffix = String.format( "%03d", ( i + 5 ) % userNum );
                Map<String, Object> jsonAsMap = new HashMap<>( );
                jsonAsMap.put( "user_id", "aragorn" + suffix );
                jsonAsMap.put( "email", "aragorn" + reverseSuffix + "@lordoftherings.org" );
                jsonAsMap.put( "fullName", "Aragorn King of Gondor " + modSuffix );
                jsonAsMap.put( "password", "pAssw0rD" );
                Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                    .body( jsonAsMap )
                    .when( )
                    .post( )
                    .then( ).statusCode( 201 ).extract( ).response( );
            }
        }

        @Test
        void getMultipleUsersWithoutParams( )
        {
            Map<String, String> params = new HashMap<>( );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).get( ).then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            List<UserInfo> userData = response.body( ).jsonPath( ).getList( "data", UserInfo.class );
            assertNotNull( userData );
            assertEquals( "admin", userData.get( 0 ).getUserId( ) );
            assertEquals( userNum + 2, userData.size( ) );
            assertEquals( Integer.valueOf( 0 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
            assertEquals( Integer.valueOf( 1000 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
            assertEquals( Integer.valueOf( userNum + 2 ), response.body( ).jsonPath( ).get( "pagination.totalCount" ) );

        }

        @Test
        void getMultipleUsersWithPaging( )
        {
            HashMap<String, String> params = new HashMap<>( );
            params.put( "limit", Integer.toString( 10 ) );
            params.put( "offset", Integer.toString( 1 ) );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).params( params ).get( ).then( ).statusCode( 200 ).extract( ).response( );
            List<UserInfo> userData = response.body( ).jsonPath( ).getList( "data", UserInfo.class );
            assertNotNull( userData );
            assertEquals( "aragorn000", userData.get( 0 ).getUserId( ) );
            assertEquals( "aragorn009", userData.get( 9 ).getUserId( ) );
            assertEquals( 10, userData.size( ) );
            assertEquals( Integer.valueOf( 1 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
            assertEquals( Integer.valueOf( 10 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
            assertEquals( Integer.valueOf( userNum + 2 ), response.body( ).jsonPath( ).get( "pagination.totalCount" ) );
        }

        @Test
        void getMultipleUsersWithPagingOrderByMail( )
        {
            HashMap<String, String> params = new HashMap<>( );
            params.put( "limit", Integer.toString( 5 ) );
            params.put( "offset", Integer.toString( 3 ) );
            params.put( "orderBy", "email" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).params( params ).get( ).then( ).statusCode( 200 ).extract( ).response( );
            List<UserInfo> userData = response.body( ).jsonPath( ).getList( "data", UserInfo.class );
            assertNotNull( userData );
            // admin user has toto@toto.org as email so is after aragorn
            assertEquals( "aragorn003@lordoftherings.org", userData.get( 0 ).getEmail() );
            assertEquals( "aragorn022", userData.get( 0 ).getUserId() );
            assertEquals( "aragorn007@lordoftherings.org", userData.get( 4 ).getEmail( ) );
            assertEquals( 5, userData.size( ) );
            assertEquals( Integer.valueOf( 3 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
            assertEquals( Integer.valueOf( 5 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
            assertEquals( Integer.valueOf( userNum + 2 ), response.body( ).jsonPath( ).get( "pagination.totalCount" ) );
        }

        @Test
        void getMultipleUsersWithPagingOrderByFullName( )
        {
            HashMap<String, String> params = new HashMap<>( );
            params.put( "limit", Integer.toString( 8 ) );
            params.put( "offset", Integer.toString( 10 ) );
            params.put( "orderBy", "fullName" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).params( params ).get( ).then( ).statusCode( 200 ).extract( ).response( );
            List<UserInfo> userData = response.body( ).jsonPath( ).getList( "data", UserInfo.class );
            assertNotNull( userData );
            // admin user has toto@toto.org as email so is after aragorn
            assertEquals( "Aragorn King of Gondor 010", userData.get( 0 ).getFullName() );
            assertEquals( "aragorn005", userData.get(0 ).getUserId() );
            assertEquals( "Aragorn King of Gondor 017", userData.get( 7 ).getFullName() );
            assertEquals( 8, userData.size( ) );
            assertEquals( Integer.valueOf( 10 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
            assertEquals( Integer.valueOf( 8 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
            assertEquals( Integer.valueOf( userNum + 2 ), response.body( ).jsonPath( ).get( "pagination.totalCount" ) );
        }

        @Test
        void getMultipleUsersWithPagingReverseOrder( )
        {
            HashMap<String, String> params = new HashMap<>( );
            params.put( "limit", Integer.toString( 10 ) );
            params.put( "offset", Integer.toString( 0 ) );
            params.put( "order", "desc" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).params( params ).get( ).then( ).statusCode( 200 ).extract( ).response( );
            List<UserInfo> userData = response.body( ).jsonPath( ).getList( "data", UserInfo.class );
            assertNotNull( userData );
            assertEquals( "guest", userData.get( 0 ).getUserId( ) );
            assertEquals( "aragorn016", userData.get( 9 ).getUserId( ) );
            assertEquals( 10, userData.size( ) );
            assertEquals( Integer.valueOf( 0 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
            assertEquals( Integer.valueOf( 10 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
            assertEquals( Integer.valueOf( userNum + 2 ), response.body( ).jsonPath( ).get( "pagination.totalCount" ) );
        }
        @Test
        void getMultipleUsersWithPagingAndQuery( ) {
        HashMap<String, String> params = new HashMap<>( );
            params.put( "limit", Integer.toString( 10 ) );
            params.put( "offset", Integer.toString( 0 ) );
            params.put( "order", "asc" );
            params.put( "q", "015" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).params( params ).get( ).then( ).statusCode( 200 ).extract( ).response( );
            List<UserInfo> userData = response.body( ).jsonPath( ).getList( "data", UserInfo.class );
            assertNotNull( userData );
            assertEquals( "aragorn010", userData.get( 0 ).getUserId( ) );
            assertEquals( "aragorn015@lordoftherings.org", userData.get( 0 ).getEmail( ) );
            assertEquals( "aragorn015", userData.get( 1 ).getUserId( ) );
            assertEquals( 2, userData.size( ) );
            assertEquals( Integer.valueOf( 0 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
            assertEquals( Integer.valueOf( 10 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
            assertEquals( Integer.valueOf( 2 ), response.body( ).jsonPath( ).get( "pagination.totalCount" ) );

        }


        @AfterAll
        void cleanupUsers( )
        {
            for ( int i = 0; i < userNum; i++ )
            {
                String suffix = String.format( "%03d", i );
                given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                    .when( ).delete( "aragorn" + suffix ).then( ).statusCode( 200 );
            }

        }
    }

    @Test
    void getUsersWithoutLogin( )
    {
        given( ).spec( getRequestSpec( ) ).contentType( JSON )
            .when( ).get( ).then( ).statusCode( 403 );
    }

    @Test
    void getUser( )
    {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( ).get( "admin" ).then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        assertEquals( "jpa:admin", response.body( ).jsonPath( ).get( "id" ) );
        assertEquals( "admin", response.body( ).jsonPath( ).get( "user_id" ) );
        assertEquals( "the admin user", response.body( ).jsonPath( ).get( "fullName" ) );
    }

    @Test
    void getUserWithoutLogin( )
    {
        given( ).spec( getRequestSpec( ) ).contentType( JSON )
            .when( ).get( "admin" ).then( ).statusCode( 403 );
    }


    @Test
    void createUser( )
    {
        String token = getAdminToken( );
        try
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "user_id", "aragorn" );
            jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
            jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
            jsonAsMap.put( "password", "pAssw0rD" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 ).extract( ).response( );
            assertTrue( response.getHeader( "Location" ).endsWith( "/aragorn" ) );

        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).delete( "aragorn" ).then( ).statusCode( 200 );

        }
    }

    @Test
    void createInvalidUser( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 422 );

    }

    @Test
    void createInvalidMeUser( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "me" );
        jsonAsMap.put( "email", "me@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Its just me" );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 422 );

    }


    @Test
    void createUserAndPermissionFail( )
    {
        String token = getAdminToken( );
        try
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "user_id", "aragorn" );
            jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
            jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
            jsonAsMap.put( "validated", true );
            jsonAsMap.put( "password", "pAssw0rD" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 ).extract( ).response( );
            assertTrue( response.getHeader( "Location" ).endsWith( "/aragorn" ) );

            String userToken = getUserToken( "aragorn", "pAssw0rD" );

            jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "user_id", "arwen" );
            jsonAsMap.put( "email", "arwen@lordoftherings.org" );
            jsonAsMap.put( "fullName", "Arwen Daughter of Elrond" );
            jsonAsMap.put( "password", "pAssw0rD" );
            given( ).spec( getRequestSpec( userToken ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 403 );


        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).delete( "aragorn" ).then( ).statusCode( 200 );

        }
    }

    @Test
    void createUserExistsAlready( )
    {
        String token = getAdminToken( );
        try
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "user_id", "aragorn" );
            jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
            jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
            jsonAsMap.put( "validated", true );
            jsonAsMap.put( "password", "pAssw0rD" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 ).extract( ).response( );
            assertTrue( response.getHeader( "Location" ).endsWith( "/aragorn" ) );

            jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "user_id", "aragorn" );
            jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
            jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
            jsonAsMap.put( "validated", true );
            jsonAsMap.put( "password", "pAssw0rD" );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .redirects( ).follow( false ) // Rest assured default is following the 303 redirect
                .post( )
                .prettyPeek( )
                .peek( )
                .then( ).statusCode( 303 ).extract( ).response( );
            assertTrue( response.getHeader( "Location" ).endsWith( "/aragorn" ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).delete( "aragorn" ).then( ).statusCode( 200 );

        }
    }

    @Test
    void deleteUser( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );

        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .delete( "aragorn" )
            .then( ).statusCode( 200 ).extract( ).response( );
    }

    @Test
    void deleteNonexistingUser( )
    {
        String token = getAdminToken( );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .delete( "galadriel" )
            .then( ).statusCode( 404 ).extract( ).response( );
    }

    @Test
    void deleteUserPermissionDenied( )
    {
        String adminToken = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {
            String token = null;
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 401 ).extract( ).response( );
        }
        finally
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void updateUser( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );

        try
        {
            jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "email", "aragorn2@lordoftherings.org" );
            jsonAsMap.put( "fullName", "Aragorn King of Gondor the Second" );
            jsonAsMap.put( "password", "pAssw0rDXX" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .put( "aragorn" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            assertEquals( "aragorn2@lordoftherings.org", response.body( ).jsonPath( ).getString( "email" ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void updateNonExistingUser( )
    {
        String token = getAdminToken( );
        HashMap<Object, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "email", "aragorn2@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor the Second" );
        jsonAsMap.put( "password", "pAssw0rDXX" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .put( "aragorn" )
            .then( ).statusCode( 404 );
    }

    @Test
    void updateUserWithPasswordViolation( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );

        try
        {
            jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "email", "aragorn2@lordoftherings.org" );
            jsonAsMap.put( "fullName", "Aragorn King of Gondor the Second" );
            jsonAsMap.put( "password", "pAssw0rD" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .put( "aragorn" )
                .prettyPeek( )
                .then( ).statusCode( 422 ).extract( ).response( );
            assertNotNull( response );
            assertEquals( "user.password.violation.reuse", response.body( ).jsonPath( ).get( "errorMessages[0].errorKey" ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }


    @Test
    void createExistingAdminUser( )
    {
        String token = null;
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "admin" );
        jsonAsMap.put( "email", "admin@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Admin" );
        jsonAsMap.put( "password", "pAssw0rD" );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .redirects( ).follow( false )
            .post( "admin" )
            .then( ).statusCode( 303 ).extract( ).response( );
        assertTrue( response.getHeader( "Location" ).endsWith( "/users/admin" ) );
    }

    @Test
    void checkAdminStatus( )
    {
        String token = null;
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .get( "admin/status" )
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        assertTrue( response.body( ).jsonPath( ).getBoolean( "exists" ) );
        assertNotNull( response.body( ).jsonPath( ).get( "since" ) );
    }


    @Test
    void lockUser( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "locked", false );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .post( "aragorn/lock/set" )
                .then( ).statusCode( 200 );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .get( "aragorn" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertTrue( response.getBody( ).jsonPath( ).getBoolean( "locked" ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void lockUnknownUser( )
    {
        String token = getAdminToken( );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .post( "aragorn/lock/set" )
            .then( ).statusCode( 404 );
    }

    @Test
    void unlockUser( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "locked", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .get( "aragorn" )
            .then( ).statusCode( 200 ).extract( ).response( );
        assertTrue( response.getBody( ).jsonPath( ).getBoolean( "locked" ) );
        try
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .post( "aragorn/lock/clear" )
                .then( ).statusCode( 200 );
            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .get( "aragorn" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertFalse( response.getBody( ).jsonPath( ).getBoolean( "locked" ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }

    }

    @Test
    void unlockUnknownUser( )
    {
        String token = getAdminToken( );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .post( "aragorn/unlock" )
            .then( ).statusCode( 404 );
    }

    @Test
    void setPasswordChangeRequire( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "locked", false );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .post( "aragorn/password/require/set" )
                .then( ).statusCode( 200 );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .get( "aragorn" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertTrue( response.getBody( ).jsonPath( ).getBoolean( "passwordChangeRequired" ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void clearPasswordChangeRequire( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "locked", false );
        jsonAsMap.put( "passwordChangeRequired", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .get( "aragorn" )
            .then( ).statusCode( 200 ).extract( ).response( );
        assertTrue( response.getBody( ).jsonPath( ).getBoolean( "passwordChangeRequired" ) );
        try
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .post( "aragorn/password/require/clear" )
                .then( ).statusCode( 200 );
            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .get( "aragorn" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertFalse( response.getBody( ).jsonPath( ).getBoolean( "passwordChangeRequired" ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void setPasswordChangeRequireNonExistingUser( )
    {
        String token = getAdminToken( );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .post( "aragorn2/password/require/set" )
            .then( ).statusCode( 404 );
    }

    @Test
    void clearPasswordChangeRequireNonExistingUser( )
    {
        String token = getAdminToken( );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .post( "aragorn2/password/require/clear" )
            .then( ).statusCode( 404 );
    }

    @Test
    void setPasswordChangeRequireNoPermission( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "locked", false );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "passwordChangeRequired", false );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );

        jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "elrond" );
        jsonAsMap.put( "email", "elrond@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Elrond King of Elves" );
        jsonAsMap.put( "locked", false );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "passwordChangeRequired", false );
        jsonAsMap.put( "password", "pAssw0rDElrond" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );


        try
        {
            String userToken = getUserToken( "elrond", "pAssw0rDElrond" );
            given( ).spec( getRequestSpec( userToken ) ).contentType( JSON )
                .post( "aragorn/password/require/set" )
                .then( ).statusCode( 403 );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .get( "aragorn" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertFalse( response.getBody( ).jsonPath( ).getBoolean( "passwordChangeRequired" ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "elrond" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void clearPasswordChangeRequireNoPermission( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "locked", false );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "passwordChangeRequired", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );

        jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "elrond" );
        jsonAsMap.put( "email", "elrond@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Elrond King of Elves" );
        jsonAsMap.put( "locked", false );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "passwordChangeRequired", false );
        jsonAsMap.put( "password", "pAssw0rDElrond" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );


        try
        {
            String userToken = getUserToken( "elrond", "pAssw0rDElrond" );
            given( ).spec( getRequestSpec( userToken ) ).contentType( JSON )
                .post( "aragorn/password/require/clear" )
                .then( ).statusCode( 403 );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .get( "aragorn" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertTrue( response.getBody( ).jsonPath( ).getBoolean( "passwordChangeRequired" ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "elrond" )
                .then( ).statusCode( 200 );
        }
    }


    @Test
    void updateMe( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {

            String userToken = getUserToken( "aragorn", "pAssw0rD" );
            Map<String, Object> updateMap = new HashMap<>( );
            updateMap.put( "email", "aragorn-swiss@lordoftherings.org" );
            updateMap.put( "fullName", "Aragorn King of Switzerland" );
            Response response = given( ).spec( getRequestSpec( userToken ) ).contentType( JSON )
                .body( updateMap )
                .when( )
                .put( "me" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertEquals( "Aragorn King of Switzerland", response.getBody( ).jsonPath( ).getString( "fullName" ) );
            assertEquals( "aragorn-swiss@lordoftherings.org", response.getBody( ).jsonPath( ).getString( "email" ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void updateMeWithPassword( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {

            String userToken = getUserToken( "aragorn", "pAssw0rD" );
            Map<String, Object> updateMap = new HashMap<>( );
            updateMap.put( "user_id", "aragorn" );
            updateMap.put( "email", "aragorn-sweden@lordoftherings.org" );
            updateMap.put( "fullName", "Aragorn King of Sweden" );
            updateMap.put( "currentPassword", "pAssw0rD" );
            updateMap.put( "password", "x1y2z3a4b5c6d8##" );
            Response response = given( ).spec( getRequestSpec( userToken ) ).contentType( JSON )
                .body( updateMap )
                .when( )
                .put( "me" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertEquals( "Aragorn King of Sweden", response.getBody( ).jsonPath( ).getString( "fullName" ) );
            assertEquals( "aragorn-sweden@lordoftherings.org", response.getBody( ).jsonPath( ).getString( "email" ) );
            userToken = getUserToken( "aragorn", "x1y2z3a4b5c6d8##" );
            given( ).spec( getRequestSpec( userToken ) ).contentType( JSON ).get( "aragorn" )
                .then( ).statusCode( 200 );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void getLoggedInUser( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {

            String userToken = getUserToken( "aragorn", "pAssw0rD" );
            Response response = given( ).spec( getRequestSpec( userToken ) ).contentType( JSON )
                .when( )
                .get( "me" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertEquals( "aragorn", response.getBody( ).jsonPath( ).getString( "user_id" ) );
            assertEquals( "Aragorn King of Gondor", response.getBody( ).jsonPath( ).getString( "fullName" ) );
            assertEquals( "aragorn@lordoftherings.org", response.getBody( ).jsonPath( ).getString( "email" ) );
            assertTrue( response.getBody( ).jsonPath( ).getBoolean( "validated" ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void getNotLoggedInUser( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {

            given( ).spec( getRequestSpec( ) ).contentType( JSON )
                .when( )
                .get( "me" )
                .then( ).statusCode( 401 );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void clearCache( )
    {
        String adminToken = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {

            Response response = given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .when( )
                .post( "aragorn/cache/clear" )
                .then( ).statusCode( 200 ).extract( ).response( );

            assertTrue( response.getBody( ).jsonPath( ).getBoolean( "success" ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void clearCacheNoPermission( )
    {
        String adminToken = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {

            String token = getUserToken( "aragorn", "pAssw0rD" );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .post( "admin/cache/clear" )
                .then( ).statusCode( 403 );

        }
        finally
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void register( )
    {
        String adminToken = getAdminToken( );

        given( ).spec( getRequestSpec( adminToken, "/api/testsService" ) )
            .when( )
            .post( "DefaultServicesAssert/clearEmailMessages" )
            .then( ).statusCode( 204 );

        Map<String, Object> requestMap = new HashMap<>( );

        Map<String, Object> userMap = new HashMap<>( );

        userMap.put( "user_id", "bilbo" );
        userMap.put( "email", "bilbo@lordoftherings.org" );
        userMap.put( "fullName", "Bilbo Beutlin" );
        userMap.put( "validated", true );
        userMap.put( "password", "pAssw0rD" );
        userMap.put( "confirmPassword", "pAssw0rD" );
        requestMap.put( "user", userMap );
        requestMap.put( "applicationUrl", "http://localhost" );

        try
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .body( requestMap )
                .when( )
                .post( "bilbo/register" )
                .then( ).statusCode( 200 );

            Response response = given( ).spec( getRequestSpec( adminToken, "/api/testsService" ) ).contentType( JSON )
                .get( "DefaultServicesAssert/getEmailMessageSended" ).then( ).statusCode( 200 )
                .extract( ).response( );
            List<EmailMessage> emailMessages = response.jsonPath( ).getList( "", EmailMessage.class );
            assertEquals( 1, emailMessages.size( ) );
            assertEquals( "bilbo@lordoftherings.org", emailMessages.get( 0 ).getTos( ).get( 0 ) );

            assertEquals( "Welcome", emailMessages.get( 0 ).getSubject( ) );
            assertTrue(
                emailMessages.get( 0 ).getText( ).contains( "Use the following URL to validate your account." ) );

        }
        finally
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .delete( "bilbo" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void registerWithInvalidData( )
    {
        String adminToken = getAdminToken( );
        Map<String, Object> requestMap = new HashMap<>( );

        Map<String, Object> userMap = new HashMap<>( );

        userMap.put( "user_id", "bilbo" );
        userMap.put( "email", "bilbo@lordoftherings.org" );
        userMap.put( "fullName", "Bilbo Beutlin" );
        userMap.put( "validated", true );
        userMap.put( "password", "pAssw0rD" );
        userMap.put( "confirmPassword", "xxx" );
        requestMap.put( "user", userMap );
        requestMap.put( "applicationUrl", "http://localhost" );
        given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
            .body( requestMap )
            .when( )
            .post( "bilbo/register" )
            .then( ).statusCode( 422 );

    }

    @Test
    void askForPasswordReset( )
    {
        String adminToken = getAdminToken( );

        given( ).spec( getRequestSpec( adminToken, "/api/testsService" ) )
            .when( )
            .post( "DefaultServicesAssert/clearEmailMessages" )
            .then( ).statusCode( 204 );

        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {

            given( ).spec( getRequestSpec( null ) ).contentType( JSON )
                .when( )
                .post( "aragorn/password/reset" )
                .then( ).statusCode( 200 );

            Response response = given( ).spec( getRequestSpec( adminToken, "/api/testsService" ) ).contentType( JSON )
                .get( "DefaultServicesAssert/getEmailMessageSended" ).then( ).statusCode( 200 )
                .extract( ).response( );
            List<EmailMessage> emailMessages = response.jsonPath( ).getList( "", EmailMessage.class );
            assertEquals( 1, emailMessages.size( ) );
            assertEquals( "aragorn@lordoftherings.org", emailMessages.get( 0 ).getTos( ).get( 0 ) );
            String messageContent = emailMessages.get( 0 ).getText( );

            assertTrue( messageContent.contains( "Password Reset" ) );
            assertTrue( messageContent.contains( "Username: aragorn" ) );


            given( ).spec( getRequestSpec( null ) ).contentType( JSON )
                .when( )
                .post( "xxyy/password/reset" )
                .then( ).statusCode( 404 );
        }
        finally
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }


    @Test
    void getUserPermissions( )
    {
        String adminToken = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {

            String token = getUserToken( "aragorn", "pAssw0rD" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "aragorn/permissions" )
                .then( ).statusCode( 200 ).extract( ).response( );
            List<Permission> result = response.getBody( ).jsonPath( ).getList( "", Permission.class );
            assertNotNull( result );
            assertEquals( 2, result.size( ) );
            assertTrue( result.stream( ).anyMatch( permission -> permission.getName( ).equals( "Edit User Data by Username" ) ) );
            assertTrue( result.stream( ).anyMatch( permission -> permission.getName( ).equals( "View User Data by Username" ) ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void getUserPermissionsInvalidPermission( )
    {
        String adminToken = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {

            String token = getUserToken( "aragorn", "pAssw0rD" );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "admin/permissions" )
                .then( ).statusCode( 403 );
        }
        finally
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void getUserOperations( )
    {
        String adminToken = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {

            String token = getUserToken( "aragorn", "pAssw0rD" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "aragorn/operations" )
                .prettyPeek( )
                .then( ).statusCode( 200 ).extract( ).response( );
            List<Operation> result = response.getBody( ).jsonPath( ).getList( "", Operation.class );
            assertNotNull( result );
            assertEquals( 2, result.size( ) );
            assertTrue( result.stream( ).anyMatch( operation -> operation.getName( ).equals( "user-management-user-edit" ) ) );
            assertTrue( result.stream( ).anyMatch( operation -> operation.getName( ).equals( "user-management-user-view" ) ) );


        }
        finally
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void getUserOperationsInvalidPermission( )
    {
        String adminToken = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {

            String token = getUserToken( "aragorn", "pAssw0rD" );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "admin/operations" )
                .prettyPeek( )
                .then( ).statusCode( 403 );
        }
        finally
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void getOwnPermissions( )
    {
        String adminToken = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {

            String token = getUserToken( "aragorn", "pAssw0rD" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "me/permissions" )
                .then( ).statusCode( 200 ).extract( ).response( );
            List<Permission> result = response.getBody( ).jsonPath( ).getList( "", Permission.class );
            assertNotNull( result );
            assertEquals( 2, result.size( ) );
            assertTrue( result.stream( ).anyMatch( permission -> permission.getName( ).equals( "Edit User Data by Username" ) ) );
            assertTrue( result.stream( ).anyMatch( permission -> permission.getName( ).equals( "View User Data by Username" ) ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void getOwnOperations( )
    {
        String adminToken = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Aragorn King of Gondor" );
        jsonAsMap.put( "validated", true );
        jsonAsMap.put( "password", "pAssw0rD" );
        given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .post( )
            .then( ).statusCode( 201 );
        try
        {

            String token = getUserToken( "aragorn", "pAssw0rD" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "me/operations" )
                .prettyPeek( )
                .then( ).statusCode( 200 ).extract( ).response( );
            List<Operation> result = response.getBody( ).jsonPath( ).getList( "", Operation.class );
            assertNotNull( result );
            assertEquals( 2, result.size( ) );
            assertTrue( result.stream( ).anyMatch( operation -> operation.getName( ).equals( "user-management-user-edit" ) ) );
            assertTrue( result.stream( ).anyMatch( operation -> operation.getName( ).equals( "user-management-user-view" ) ) );


        }
        finally
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void validateUserRegistration( )
    {
        String adminToken = getAdminToken( );

        Map<String, Object> userMap = new HashMap<>( );
        Map<String, Object> requestMap = new HashMap<>( );


        userMap.put( "user_id", "bilbo" );
        userMap.put( "email", "bilbo@lordoftherings.org" );
        userMap.put( "fullName", "Bilbo Beutlin" );
        userMap.put( "validated", true );
        userMap.put( "password", "pAssw0rD" );
        userMap.put( "confirmPassword", "pAssw0rD" );
        requestMap.put( "user", userMap );
        requestMap.put( "applicationUrl", "http://localhost" );

        try
        {
            Response response = given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .body( requestMap )
                .when( )
                .post( "bilbo/register" )
                .then( ).statusCode( 200 ).extract( ).response( );
            RegistrationKey key = response.getBody( ).jsonPath( ).getObject( "", RegistrationKey.class );
            assertNotNull( key );
            assertNotNull( key.getKey( ) );

            response = given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .body( requestMap )
                .when( )
                .post( "bilbo/register/" + key.getKey( ) + "/validate" )
                .then( ).statusCode( 200 ).extract( ).response( );

            assertNotNull( response );
            VerificationStatus verificationStatus = response.getBody( ).jsonPath( ).getObject( "", VerificationStatus.class );
            assertNotNull( verificationStatus );
            assertTrue( verificationStatus.isSuccess( ) );

        }
        finally
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .delete( "bilbo" )
                .then( ).statusCode( 200 );
        }
    }
}
