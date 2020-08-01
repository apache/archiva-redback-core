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
import org.apache.archiva.redback.rest.api.model.v2.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.replaceFiltersWith;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@ExtendWith( SpringExtension.class )
@ContextConfiguration(
    locations = {"classpath:/ldap-spring-test.xml"} )
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@Tag("rest-native")
@TestMethodOrder( MethodOrderer.Random.class )
public class NativeUserServiceTest extends AbstractNativeRestServices
{
    @Override
    protected String getServicePath( )
    {
        return "/users";
    }

    @BeforeAll
    void setup() throws Exception
    {
        super.setupNative();
    }

    @AfterAll
    void destroy() throws Exception
    {
        super.shutdownNative();
    }

    @Test
    void getUsers() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( ).get( ).then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        List<User> userData = response.body( ).jsonPath( ).getList( "data", User.class );
        assertNotNull( userData );
        assertEquals( 2, userData.size( ) );
        assertEquals( Integer.valueOf( 0 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
        assertEquals( Integer.valueOf( 1000 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
        assertEquals( Integer.valueOf( 2 ), response.body( ).jsonPath( ).get( "pagination.totalCount" ) );
    }

    @Test
    void getUsersWithoutLogin() {
        given( ).spec( getRequestSpec(  ) ).contentType( JSON )
            .when( ).get( ).then( ).statusCode( 403 );
    }

    @Test
    void getUser() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( ).get( "admin" ).then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        assertEquals( "jpa:admin", response.body( ).jsonPath( ).get( "id" ) );
        assertEquals( "admin", response.body( ).jsonPath( ).get( "user_id" ) );
        assertEquals( "the admin user", response.body( ).jsonPath( ).get( "fullName" ) );
    }

    @Test
    void getUserWithoutLogin() {
        given( ).spec( getRequestSpec(  ) ).contentType( JSON )
            .when( ).get( "admin" ).then( ).statusCode( 403 );
    }


    @Test
    void createUser() {
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

        } finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).delete( "aragorn").then( ).statusCode( 200 );

        }
    }

    @Test
    void createInvalidUser() {
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
            .then( ).statusCode( 405 );

    }

    @Test
    void createInvalidMeUser() {
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
            .then( ).statusCode( 405 );

    }


    @Test
    void createUserAndPermissionFail() {
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


        } finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).delete( "aragorn").then( ).statusCode( 200 );

        }
    }

    @Test
    void createUserExistsAlready() {
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
                .redirects().follow( false ) // Rest assured default is following the 303 redirect
                .post( )
                .prettyPeek()
                .peek()
                .then( ).statusCode( 303 ).extract().response();
            assertTrue( response.getHeader( "Location" ).endsWith( "/aragorn" ) );
        } finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).delete( "aragorn").then( ).statusCode( 200 );

        }
    }

    @Test
    void createExistingAdminUser() {
        String token = null;
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "admin" );
        jsonAsMap.put( "email", "admin@lordoftherings.org" );
        jsonAsMap.put( "fullName", "Admin" );
        jsonAsMap.put( "password", "pAssw0rD" );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .body( jsonAsMap )
            .when( )
            .redirects().follow( false )
            .post( "admin" )
            .then( ).statusCode( 303 ).extract( ).response( );
        assertTrue( response.getHeader( "Location" ).endsWith( "/users/admin" ) );
    }

    @Test
    void checkAdminStatus() {
        String token = null;
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .get( "admin/status" )
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        assertTrue( response.body( ).jsonPath( ).getBoolean("exists" ) );
        assertNotNull( response.body( ).jsonPath( ).get( "since" ) );
    }

    @Test
    void deleteUser() {
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
    void deleteNonexistingUser() {
        String token = getAdminToken( );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .delete( "galadriel" )
            .then( ).statusCode( 404 ).extract( ).response( );
    }

    @Test
    void deleteUserPermissionDenied() {
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
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 401 ).extract( ).response( );
        } finally
        {
            given( ).spec( getRequestSpec( adminToken ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void updateUser() {
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
        }finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void updateNonExistingUser() {
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
    void updateUserPasswordViolation() {
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
                .prettyPeek()
                .then( ).statusCode( 422 ).extract( ).response( );
            assertNotNull( response );
            assertEquals( "user.password.violation.reuse", response.body( ).jsonPath( ).get( "errorMessages[0].errorKey" ) );
        }finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .delete( "aragorn" )
                .then( ).statusCode( 200 );
        }
    }

}
