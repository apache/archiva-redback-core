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
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.redback.rest.api.model.v2.BaseUserInfo;
import org.apache.archiva.redback.rest.api.model.v2.Permission;
import org.apache.archiva.redback.rest.api.model.v2.RoleInfo;
import org.apache.archiva.redback.rest.api.model.v2.RoleTemplate;
import org.apache.archiva.redback.rest.api.model.v2.UserInfo;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.archiva.redback.rest.api.Constants.DEFAULT_PAGE_LIMIT;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
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
@DisplayName( "Native REST tests for V2 RoleService" )
public class NativeRoleServiceTest extends AbstractNativeRestServices
{
    @Override
    protected String getServicePath( )
    {
        return "/roles";
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

    private String getUserServicePath( )
    {
        return new StringBuilder( )
            .append( getContextRoot( ) )
            .append( getServiceBasePath( ) )
            .append( "/users" ).toString( );
    }


    @Test
    void createTemplatedRole( )
    {
        String token = getAdminToken( );
        try
        {
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "templates/archiva-repository-manager/repository01" )
                .then( ).statusCode( 201 ).extract( ).response( );
            assertNotNull( response );
            RoleInfo roleInfo = response.getBody( ).jsonPath( ).getObject( "", RoleInfo.class );
            assertNotNull( response.getHeader( "Location" ) );
            assertTrue( response.getHeader( "Location" ).endsWith( "/roles/" + roleInfo.getId( ) ) );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "templates/archiva-repository-manager/repository01" )
                .then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .head( "templates/archiva-repository-manager/repository01" )
                .then( ).statusCode( 200 );
            // Repository observer is child template of repository-manager and will be created too
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .head( "templates/archiva-repository-observer/repository01" )
                .then( ).statusCode( 200 );

        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-manager/repository01" )
                .then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-observer/repository01" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void createTemplatedRoleWithNonexistentTemplate( )
    {
        String token = getAdminToken( );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .put( "templates/abcdefg/repository01" )
            .then( ).statusCode( 404 );
    }

    @Test
    void deleteTemplatedRole( )
    {
        String token = getAdminToken( );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .put( "templates/archiva-repository-manager/repository05" )
            .then( ).statusCode( 201 ).extract( ).response( );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .delete( "templates/archiva-repository-manager/repository01" )
            .then( ).statusCode( 404 );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .delete( "templates/archiva-repository-manager/repository05" )
            .then( ).statusCode( 200 );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .delete( "templates/archiva-repository-manager/repository05" )
            .then( ).statusCode( 404 );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .delete( "templates/archiva-repository-observer/repository05" )
            .then( ).statusCode( 200 );

    }


    @Test
    void checkTemplatedRole( )
    {
        String token = getAdminToken( );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .put( "templates/archiva-repository-observer/repository06" )
            .then( ).statusCode( 201 );
        try
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .head( "templates/archiva-repository-observer/repository06" )
                .then( ).statusCode( 200 );

            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .head( "archiva-repository-observer.repository06" )
                .then( ).statusCode( 200 );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-observer/repository06" )
                .then( ).statusCode( 200 );
        }

    }


    @Nested
    @DisplayName( "Test Role queries" )
    @ContextConfiguration(
        locations = {"classpath:/ldap-spring-test.xml"} )
    @TestInstance( TestInstance.Lifecycle.PER_CLASS )
    class TestRoleRetrieval
    {
        int roleInstances = 3;
        String token;

        @BeforeAll
        void initRoles( )
        {
            this.token = getAdminToken( );
            for ( int i = 0; i < roleInstances; i++ )
            {
                String suffix = String.format( "%03d", i );
                given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                    .when( )
                    .put( "templates/archiva-repository-manager/repo" + suffix )
                    .then( ).statusCode( 201 ).extract( ).response( );
                given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                    .when( )
                    .put( "templates/archiva-repository-observer/repo" + suffix )
                    .then( ).statusCode( anyOf( equalTo( 200 ), equalTo( 201 ) ) ).extract( ).response( );
            }
        }

        @Test
        void getMultipleRolesWithoutParams( )
        {
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).get( ).then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            List<RoleInfo> roleData = response.body( ).jsonPath( ).getList( "data", RoleInfo.class );
            assertNotNull( roleData );
            assertEquals( roleInstances * 2 + 9, roleData.size( ) );
            assertEquals( Integer.valueOf( 0 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
            assertEquals( Integer.valueOf( DEFAULT_PAGE_LIMIT ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
            assertEquals( Integer.valueOf( roleInstances * 2 + 9 ), response.body( ).jsonPath( ).get( "pagination.total_count" ) );

        }

        @Test
        void getMultipleRolesWithPaging( )
        {
            HashMap<String, String> params = new HashMap<>( );
            params.put( "limit", Integer.toString( 10 ) );
            params.put( "offset", Integer.toString( 1 ) );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).params( params ).get( ).then( ).statusCode( 200 ).extract( ).response( );
            List<RoleInfo> userData = response.body( ).jsonPath( ).getList( "data", RoleInfo.class );
            assertNotNull( userData );
            response.getBody( ).jsonPath( ).prettyPrint( );
            assertEquals( 10, userData.size( ) );
            assertEquals( Integer.valueOf( 1 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
            assertEquals( Integer.valueOf( 10 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
            assertEquals( Integer.valueOf( roleInstances * 2 + 9 ), response.body( ).jsonPath( ).get( "pagination.total_count" ) );
        }

        @Test
        void getMultipleUsersWithPagingOrderByIdAndResource( )
        {
            HashMap<String, Object> params = new HashMap<>( );
            params.put( "limit", Integer.toString( 8 ) );
            params.put( "offset", Integer.toString( 5 ) );
            params.put( "orderBy", Arrays.asList( "id", "resource" ) );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).params( params ).get( ).then( ).statusCode( 200 ).extract( ).response( );
            List<RoleInfo> userData = response.body( ).jsonPath( ).getList( "data", RoleInfo.class );
            assertNotNull( userData );
            // admin user has toto@toto.org as email so is after aragorn
            assertEquals( "repo002", userData.get( 0 ).getResource( ) );
            assertEquals( "repo000", userData.get( 1 ).getResource( ) );
            assertEquals( "archiva-repository-observer.repo000", userData.get( 1 ).getId( ) );
            assertEquals( "archiva-system-administrator", userData.get( 4 ).getId( ) );
            assertEquals( 8, userData.size( ) );
            assertEquals( Integer.valueOf( 5 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
            assertEquals( Integer.valueOf( 8 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
            assertEquals( Integer.valueOf( roleInstances * 2 + 9 ), response.body( ).jsonPath( ).get( "pagination.total_count" ) );
        }

        @Test
        void getMultipleUsersWithPagingOrderByIdAndResourceReverse( )
        {
            HashMap<String, Object> params = new HashMap<>( );
            params.put( "limit", Integer.toString( 7 ) );
            params.put( "offset", Integer.toString( 1 ) );
            params.put( "orderBy", Arrays.asList( "id", "resource" ) );
            params.put( "order", "desc" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).params( params ).get( ).then( ).statusCode( 200 ).extract( ).response( );
            response.getBody( ).jsonPath( ).prettyPrint( );
            List<RoleInfo> userData = response.body( ).jsonPath( ).getList( "data", RoleInfo.class );
            assertNotNull( userData );
            // admin user has toto@toto.org as email so is after aragorn
            assertEquals( "system-administrator", userData.get( 0 ).getId( ) );
            assertEquals( "registered-user", userData.get( 1 ).getId( ) );
            assertEquals( "guest", userData.get( 2 ).getId( ) );
            assertEquals( "archiva-repository-observer.repo002", userData.get( 5 ).getId( ) );
            assertEquals( 7, userData.size( ) );
            assertEquals( Integer.valueOf( 1 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
            assertEquals( Integer.valueOf( 7 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
            assertEquals( Integer.valueOf( roleInstances * 2 + 9 ), response.body( ).jsonPath( ).get( "pagination.total_count" ) );
        }

        @Test
        void getMultipleRolesWithPagingAndQuery( )
        {
            HashMap<String, String> params = new HashMap<>( );
            params.put( "limit", Integer.toString( 10 ) );
            params.put( "offset", Integer.toString( 0 ) );
            params.put( "order", "asc" );
            params.put( "q", "system" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).params( params ).get( ).then( ).statusCode( 200 ).extract( ).response( );
            List<RoleInfo> userData = response.body( ).jsonPath( ).getList( "data", RoleInfo.class );
            assertNotNull( userData );
            assertEquals( 2, userData.size( ) );
            assertEquals( Integer.valueOf( 0 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
            assertEquals( Integer.valueOf( 10 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
            assertEquals( Integer.valueOf( 2 ), response.body( ).jsonPath( ).get( "pagination.total_count" ) );

        }


        @AfterAll
        void cleanupRoles( )
        {
            for ( int i = 0; i < roleInstances; i++ )
            {
                String suffix = String.format( "%03d", i );
                given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                    .when( ).delete( "templates/archiva-repository-manager/repo" + suffix ).then( ).statusCode( 200 );
                given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                    .when( ).delete( "templates/archiva-repository-observer/repo" + suffix ).then( ).statusCode( 200 );
            }

        }
    }

    @Test
    void getRole( )
    {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( ).get( "archiva-system-administrator" ).then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        RoleInfo roleInfo = response.getBody( ).jsonPath( ).getObject( "", RoleInfo.class );
        assertNotNull( roleInfo );
        assertEquals( "archiva-system-administrator", roleInfo.getId( ) );
        assertEquals( "Archiva System Administrator", roleInfo.getName( ) );
        List<Permission> perms = roleInfo.getPermissions( );
        assertNotNull( perms );
        assertTrue( perms.size( ) > 0 );
        assertTrue( perms.stream( ).filter( perm -> "archiva-manage-configuration".equals( perm.getName( ) ) ).findAny( ).isPresent( ) );
        List<String> childs = roleInfo.getChildRoleIds( );
        assertNotNull( childs );
        assertTrue( childs.size( ) > 0 );
        assertTrue( childs.stream( ).filter( id -> "archiva-global-repository-manager".equals( id ) ).findAny( ).isPresent( ) );
    }

    @Test
    void getNonExistingRole( )
    {
        String token = getAdminToken( );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( ).get( "abcdefg" ).then( ).statusCode( 404 );
    }

    @Test
    void checkRole( )
    {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( ).head( "archiva-system-administrator" ).then( ).statusCode( 200 ).extract( ).response( );
        assertEquals( 0, response.getBody( ).asByteArray( ).length );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( ).head( "abcdefg" ).then( ).statusCode( 404 );

    }

    @Test
    void moveRole( )
    {
        String token = getAdminToken( );
        try
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "templates/archiva-repository-manager/repository07" )
                .then( ).statusCode( 201 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).head( "templates/archiva-repository-observer/repository07" ).then( ).statusCode( 200 );

            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).post( "templates/archiva-repository-manager/repository07/moveto/repository08" ).then( ).statusCode( 201 ).extract( ).response( );
            RoleInfo role = response.getBody( ).jsonPath( ).getObject( "", RoleInfo.class );
            assertNotNull( role );
            assertEquals( "archiva-repository-manager.repository08", role.getId( ) );
            assertEquals( "repository08", role.getResource( ) );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).head( "templates/archiva-repository-manager/repository07" ).then( ).statusCode( 404 );
            // Child templates are copied and not moved
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).head( "templates/archiva-repository-observer/repository07" ).then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).head( "templates/archiva-repository-observer/repository08" ).then( ).statusCode( 200 );

        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-manager/repository08" )
                .then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-observer/repository07" )
                .then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-observer/repository08" )
                .then( ).statusCode( 200 );

        }

    }

    @Test
    void moveRoleToExistingDestination( )
    {
        String token = getAdminToken( );
        try
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "templates/archiva-repository-manager/repository09" )
                .then( ).statusCode( 201 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "templates/archiva-repository-manager/repository10" )
                .then( ).statusCode( 201 );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).redirects( ).follow( false )
                .post( "templates/archiva-repository-manager/repository09/moveto/repository10" ).then( ).statusCode( 303 )
                .extract( ).response( );
            assertTrue( response.getHeader( "Location" ).endsWith( "/roles/templates/archiva-repository-manager/repository10" ) );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( ).head( "templates/archiva-repository-manager/repository09" ).then( ).statusCode( 200 );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-manager/repository09" )
                .then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-observer/repository09" )
                .then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-manager/repository10" )
                .then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-observer/repository10" )
                .then( ).statusCode( 200 );

        }

    }

    @Test
    void getAssignedUsersNonRecursive( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "full_name", "Aragorn King of Gondor " );
        jsonAsMap.put( "password", "pAssw0rD" );

        try
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "archiva-global-repository-observer/user/aragorn" )
                .then( ).statusCode( 200 );
            Response result = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "archiva-global-repository-observer/user" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull(result);
            PagedResult<UserInfo> userResult = result.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
            assertNotNull( userResult );
            assertEquals( 1, userResult.getPagination( ).getTotalCount( ) );
            List<UserInfo> users = result.getBody( ).jsonPath( ).getList( "data", UserInfo.class );
            assertArrayEquals( new String[] {"aragorn"}, users.stream( ).map( BaseUserInfo::getUserId ).sorted().toArray(String[]::new) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .delete( "aragorn" ).then( ).statusCode( 200 );
        }

    }

    @Test
    void getUnAssignedUsersNonRecursive( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "full_name", "Aragorn King of Gondor " );
        jsonAsMap.put( "password", "pAssw0rD" );

        try
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "archiva-global-repository-observer/user/aragorn" )
                .then( ).statusCode( 200 );
            Response result = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "archiva-global-repository-observer/unassigned" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull(result);
            PagedResult<UserInfo> userResult = result.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
            assertNotNull( userResult );
            assertEquals( 2, userResult.getPagination( ).getTotalCount( ) );
            List<UserInfo> users = result.getBody( ).jsonPath( ).getList( "data", UserInfo.class );
            assertFalse( users.stream( ).filter(user -> "aragorn".equals(user.getUserId())).findAny().isPresent() );
        }
        finally
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .delete( "aragorn" ).then( ).statusCode( 200 );
        }

    }


    @Test
    void getAssignedUsersRecursive( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "full_name", "Aragorn King of Gondor " );
        jsonAsMap.put( "password", "pAssw0rD" );

        try
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "archiva-global-repository-observer/user/aragorn" )
                .then( ).statusCode( 200 );
            Response result = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .param( "recurse")
                .get( "archiva-global-repository-observer/user" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull(result);
            PagedResult<UserInfo> userResult = result.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
            assertNotNull( userResult );
            assertEquals( 2, userResult.getPagination( ).getTotalCount( ) );
            List<UserInfo> users = result.getBody( ).jsonPath( ).getList( "data", UserInfo.class );
            assertArrayEquals( new String[] {"admin","aragorn"}, users.stream( ).map( BaseUserInfo::getUserId ).sorted().toArray(String[]::new) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .delete( "aragorn" ).then( ).statusCode( 200 );
        }

    }

    @Test
    void getUnAssignedUsersRecursive( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "full_name", "Aragorn King of Gondor " );
        jsonAsMap.put( "password", "pAssw0rD" );

        try
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "archiva-global-repository-observer/user/aragorn" )
                .then( ).statusCode( 200 );
            Response result = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .param( "recurse" )
                .get( "archiva-global-repository-observer/unassigned" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull(result);
            PagedResult<UserInfo> userResult = result.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
            assertNotNull( userResult );
            assertEquals( 1, userResult.getPagination( ).getTotalCount( ) );
            List<UserInfo> users = result.getBody( ).jsonPath( ).getList( "data", UserInfo.class );
            assertTrue( "guest".equals( users.get( 0 ).getUserId( ) ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .delete( "aragorn" ).then( ).statusCode( 200 );
        }

    }


    @Test
    void getAssignedUsersRecursiveParentsOnly( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "full_name", "Aragorn King of Gondor " );
        jsonAsMap.put( "password", "pAssw0rD" );

        try
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "archiva-global-repository-observer/user/aragorn" )
                .then( ).statusCode( 200 );
            Response result = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .param( "recurse","parentsOnly")
                .get( "archiva-global-repository-observer/user" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull(result);
            PagedResult<UserInfo> userResult = result.getBody( ).jsonPath( ).getObject( "", PagedResult.class );
            assertNotNull( userResult );
            assertEquals( 1, userResult.getPagination( ).getTotalCount( ) );
            List<UserInfo> users = result.getBody( ).jsonPath( ).getList( "data", UserInfo.class );
            assertArrayEquals( new String[] {"admin"}, users.stream( ).map( BaseUserInfo::getUserId ).sorted().toArray(String[]::new) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .delete( "aragorn" ).then( ).statusCode( 200 );
        }

    }

    @Test
    void assignRole( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "full_name", "Aragorn King of Gondor " );
        jsonAsMap.put( "password", "pAssw0rD" );

        try
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 );

            Response response = given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .get( "aragorn/roles" )
                .then( ).statusCode( 200 ).extract( ).response( );
            List<RoleInfo> roles = response.getBody( ).jsonPath( ).getList( "", RoleInfo.class );
            assertFalse( roles.stream( ).filter( role -> "system-administrator".equals( role.getId( ) ) ).findAny( ).isPresent( ) );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "system-administrator/user/aragorn" )
                .then( ).statusCode( 200 );
            response = given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .get( "aragorn/roles" )
                .then( ).statusCode( 200 ).extract( ).response( );
            roles = response.getBody( ).jsonPath( ).getList( "", RoleInfo.class );
            assertTrue( roles.stream( ).filter( role -> "system-administrator".equals( role.getId( ) ) ).findAny( ).isPresent( ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .delete( "aragorn" ).then( ).statusCode( 200 );
        }
    }

    @Test
    void assignNonexistentRole( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "full_name", "Aragorn King of Gondor " );
        jsonAsMap.put( "password", "pAssw0rD" );

        try
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 );

            Response response = given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .get( "aragorn/roles" )
                .then( ).statusCode( 200 ).extract( ).response( );
            List<RoleInfo> roles = response.getBody( ).jsonPath( ).getList( "", RoleInfo.class );
            assertFalse( roles.stream( ).filter( role -> "abcdefg".equals( role.getId( ) ) ).findAny( ).isPresent( ) );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "abcdefg/user/aragorn" )
                .then( ).statusCode( 404 );
            response = given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .get( "aragorn/roles" )
                .then( ).statusCode( 200 ).extract( ).response( );
            roles = response.getBody( ).jsonPath( ).getList( "", RoleInfo.class );
            assertFalse( roles.stream( ).filter( role -> "abcdefg".equals( role.getId( ) ) ).findAny( ).isPresent( ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .delete( "aragorn" ).then( ).statusCode( 200 );
        }
    }

    @Test
    void assignRoleToNonexistentUser( )
    {
        String token = getAdminToken( );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .put( "system-administrator/user/aragorn" )
            .then( ).statusCode( 404 );
    }


    @Test
    void assignTemplatedRole( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "full_name", "Aragorn King of Gondor " );
        jsonAsMap.put( "password", "pAssw0rD" );

        try
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "templates/archiva-repository-manager/repository11" )
                .then( ).statusCode( 201 );

            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 );

            Response response = given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .get( "aragorn/roles" )
                .then( ).statusCode( 200 ).extract( ).response( );
            List<RoleInfo> roles = response.getBody( ).jsonPath( ).getList( "", RoleInfo.class );
            assertFalse( roles.stream( ).filter( role -> "archiva-repository-manager.repository11".equals( role.getId( ) ) ).findAny( ).isPresent( ) );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "templates/archiva-repository-manager/repository11/user/aragorn" )
                .then( ).statusCode( 200 );
            response = given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .get( "aragorn/roles" )
                .then( ).statusCode( 200 ).extract( ).response( );
            roles = response.getBody( ).jsonPath( ).getList( "", RoleInfo.class );
            assertTrue( roles.stream( ).filter( role -> "archiva-repository-manager.repository11".equals( role.getId( ) ) ).findAny( ).isPresent( ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .delete( "aragorn" ).then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-manager/repository11" ).then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-observer/repository11" ).then( ).statusCode( 200 );

        }
    }

    @Test
    void unAssignRole( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "full_name", "Aragorn King of Gondor " );
        jsonAsMap.put( "password", "pAssw0rD" );

        try
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 );

            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "system-administrator/user/aragorn" )
                .then( ).statusCode( 200 );
            Response response = given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .get( "aragorn/roles" )
                .then( ).statusCode( 200 ).extract( ).response( );
            List<RoleInfo> roles = response.getBody( ).jsonPath( ).getList( "", RoleInfo.class );
            assertTrue( roles.stream( ).filter( role -> "system-administrator".equals( role.getId( ) ) ).findAny( ).isPresent( ) );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "system-administrator/user/aragorn" )
                .then( ).statusCode( 200 );
            response = given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .get( "aragorn/roles" )
                .then( ).statusCode( 200 ).extract( ).response( );
            roles = response.getBody( ).jsonPath( ).getList( "", RoleInfo.class );
            assertFalse( roles.stream( ).filter( role -> "system-administrator".equals( role.getId( ) ) ).findAny( ).isPresent( ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .delete( "aragorn" ).then( ).statusCode( 200 );
        }
    }

    @Test
    void unAssignTemplatedRole( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "full_name", "Aragorn King of Gondor " );
        jsonAsMap.put( "password", "pAssw0rD" );

        try
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "templates/archiva-repository-manager/repository12" )
                .then( ).statusCode( 201 );
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "templates/archiva-repository-manager/repository12/user/aragorn" )
                .then( ).statusCode( 200 );
            Response response = given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .get( "aragorn/roles" )
                .then( ).statusCode( 200 ).extract( ).response( );
            List<RoleInfo> roles = response.getBody( ).jsonPath( ).getList( "", RoleInfo.class );
            assertTrue( roles.stream( ).filter( role -> "archiva-repository-manager.repository12".equals( role.getId( ) ) ).findAny( ).isPresent( ) );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "archiva-repository-manager.repository12/user/aragorn" )
                .then( ).statusCode( 200 );
            response = given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .get( "aragorn/roles" )
                .then( ).statusCode( 200 ).extract( ).response( );
            roles = response.getBody( ).jsonPath( ).getList( "", RoleInfo.class );
            assertFalse( roles.stream( ).filter( role -> "archiva-repository-manager.repository12".equals( role.getId( ) ) ).findAny( ).isPresent( ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .delete( "aragorn" ).then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-manager/repository12" ).then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-observer/repository12" ).then( ).statusCode( 200 );

        }
    }

    @Test
    void updateRole( )
    {
        String token = getAdminToken( );
        try
        {
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "templates/archiva-repository-manager/repository13" )
                .then( ).statusCode( 201 ).extract( ).response( );
            assertNotNull( response );
            RoleInfo roleInfo = response.getBody( ).jsonPath( ).getObject( "", RoleInfo.class );
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "id", roleInfo.getId( ) );
            jsonAsMap.put( "name", roleInfo.getName( ) );
            jsonAsMap.put( "description", "This description was updated." );
            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .patch( roleInfo.getId( ) )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            RoleInfo updatedRole = response.getBody( ).jsonPath( ).getObject( "", RoleInfo.class );
            assertEquals( roleInfo.getId( ), updatedRole.getId( ) );
            assertEquals( roleInfo.getName( ), updatedRole.getName( ) );
            assertEquals( "This description was updated.", updatedRole.getDescription( ) );
            assertEquals( true, updatedRole.isAssignable( ) );
            assertEquals( false, updatedRole.isPermanent( ) );
            response  = given().spec(getRequestSpec(token)).contentType( JSON )
                .when()
                .get("archiva-repository-manager.repository13/user")
                .then()
                .extract( ).response( );
            List<UserInfo> userList = response.getBody( ).jsonPath( ).getList( "data", UserInfo.class );
            assertEquals( 0, userList.size( ) );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-manager/repository13" )
                .then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-observer/repository13" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void updateRoleWithAssignedUsers( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "user_id", "aragorn" );
        jsonAsMap.put( "email", "aragorn@lordoftherings.org" );
        jsonAsMap.put( "full_name", "Aragorn King of Gondor " );
        jsonAsMap.put( "password", "pAssw0rD" );
        String id = "";

        try
        {
            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( )
                .then( ).statusCode( 201 );

            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "templates/archiva-repository-manager/repository14" )
                .then( ).statusCode( 201 ).extract( ).response( );
            assertNotNull( response );
            RoleInfo roleInfo = response.getBody( ).jsonPath( ).getObject( "", RoleInfo.class );
            id = roleInfo.getId( );
            jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "id", roleInfo.getId( ) );
            jsonAsMap.put( "name", roleInfo.getName( ) );
            jsonAsMap.put( "description", "New description" );
            jsonAsMap.put( "assignable", "false" );
            jsonAsMap.put( "permanent", "true" );

            HashMap<Object, Object> aragornMap = new HashMap<>( );
            aragornMap.put( "id", "jpa:aragorn" );
            aragornMap.put( "user_id", "aragorn" );
            jsonAsMap.put( "assigned_users", Arrays.asList( aragornMap ) );
            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .patch( roleInfo.getId( ) )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            RoleInfo updatedRole = response.getBody( ).jsonPath( ).getObject( "", RoleInfo.class );
            assertEquals( roleInfo.getId( ), updatedRole.getId( ) );
            assertEquals( roleInfo.getName( ), updatedRole.getName( ) );
            assertEquals( "New description", updatedRole.getDescription( ) );
            assertEquals( false, updatedRole.isAssignable( ) );
            assertEquals( true, updatedRole.isPermanent( ) );

            response  = given().spec(getRequestSpec(token)).contentType( JSON )
                .when()
                .get("archiva-repository-manager.repository14/user")
                .then()
                .extract( ).response( );
            List<UserInfo> userList = response.getBody( ).jsonPath( ).getList( "data", UserInfo.class );
            assertEquals( 1, userList.size( ) );
            assertTrue( userList.stream( ).filter( user -> "aragorn".equals( user.getUserId( ) ) ).findAny( ).isPresent( ) );
        }
        finally
        {
            // Switching back permanent flag
            jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "id", id );
            jsonAsMap.put( "permanent", "false" );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .patch( id )
                .then( ).statusCode( 200 ).extract( ).response( );

            given( ).spec( getRequestSpec( token, getUserServicePath( ) ) ).contentType( JSON )
                .when( )
                .delete( "aragorn" ).then( ).statusCode( 200 );

            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-manager/repository14" )
                .then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-observer/repository14" )
                .then( ).statusCode( 200 );
        }
    }


    @Test
    void updateRoleWithBadId( )
    {
        String token = getAdminToken( );
        try
        {
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "templates/archiva-repository-manager/repository15" )
                .then( ).statusCode( 201 ).extract( ).response( );
            assertNotNull( response );
            RoleInfo roleInfo = response.getBody( ).jsonPath( ).getObject( "", RoleInfo.class );
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "id", "abcdefg" );
            jsonAsMap.put( "name", roleInfo.getName( ) );
            jsonAsMap.put( "description", "This description was updated." );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .patch( roleInfo.getId( ) )
                .then( ).statusCode( 422 );
        }
        finally
        {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-manager/repository15" )
                .then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-observer/repository15" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void deleteTemplatedRolePermanentThrowsError( )
    {
        String token = getAdminToken( );
        String id = "";
        try
        {
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .put( "templates/archiva-repository-manager/repository16" )
                .then( ).statusCode( 201 ).extract( ).response( );
            assertNotNull( response );
            RoleInfo roleInfo = response.getBody( ).jsonPath( ).getObject( "", RoleInfo.class );
            id = roleInfo.getId( );
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "id", roleInfo.getId( ) );
            jsonAsMap.put( "name", roleInfo.getName( ) );
            jsonAsMap.put( "description", "This description was updated." );
            jsonAsMap.put( "permanent", "true" );
            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .patch( roleInfo.getId( ) )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );

            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-manager/repository16" )
                .then( ).statusCode( 400 );

        }
        finally
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "id", id );
            jsonAsMap.put( "permanent", "false" );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body( jsonAsMap )
                .patch( id )
                .then( ).statusCode( 200 ).extract( ).response( );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-manager/repository16" )
                .then( ).statusCode( 200 );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "templates/archiva-repository-observer/repository16" )
                .then( ).statusCode( 200 );
        }


    }

    @Test
    void updateRoleNotExist( )
    {
        String token = getAdminToken( );
        Map<String, Object> jsonAsMap = new HashMap<>( );
        jsonAsMap.put( "id", "abcdefg" );
        jsonAsMap.put( "name", "abcdefg" );
        jsonAsMap.put( "description", "This description was updated." );
        given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .body( jsonAsMap )
            .patch( "abcdefg" )
            .then( ).statusCode( 404 );
    }


    @Test
    void getTemplates( )
    {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .get( "templates" )
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        List<RoleTemplate> templates = response.getBody( ).jsonPath( ).getList( "", RoleTemplate.class );
        assertEquals( 2, templates.size( ) );
        assertTrue( templates.stream( ).filter( tmpl -> "archiva-repository-manager".equals( tmpl.getId( ) ) ).findAny( ).isPresent( ) );
        assertTrue( templates.stream( ).filter( tmpl -> "archiva-repository-observer".equals( tmpl.getId( ) ) ).findAny( ).isPresent( ) );
    }

}
