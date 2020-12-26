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

import io.restassured.filter.log.UrlDecoder;
import io.restassured.response.Response;
import org.apache.archiva.components.apacheds.ApacheDs;
import org.apache.archiva.redback.rest.api.Constants;
import org.apache.archiva.redback.rest.api.model.v2.Group;
import org.apache.archiva.redback.rest.api.model.v2.GroupMapping;
import org.apache.archiva.redback.rest.services.BaseSetup;
import org.apache.archiva.redback.rest.services.LdapInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
@Tag("rest-native")
@TestMethodOrder( MethodOrderer.Random.class )
public class NativeGroupServiceTest extends AbstractNativeRestServices
{
    protected String peopleSuffix;
    protected String groupSuffix;

    @Inject
    @Named( value = "apacheDS#test" )
    private ApacheDs apacheDs;

    private InitialDirContext adminContext;
    private LdapInfo ldapInfo;

    private List<String> groups =
        Arrays.asList( "Archiva System Administrator", "Internal Repo Manager", "Internal Repo Observer", "Test Group 1", "Test Group 2", "Test Group 3" );

    public InitialDirContext getAdminContext() throws NamingException
    {
        if (this.ldapInfo.isRemote()) {
            Hashtable<String, String> environment = new Hashtable<>( );

            environment.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            environment.put(Context.PROVIDER_URL, ldapInfo.getURL());
            environment.put(Context.SECURITY_AUTHENTICATION, "simple");
            environment.put(Context.SECURITY_PRINCIPAL, ldapInfo.getBindDN());
            environment.put(Context.SECURITY_CREDENTIALS, ldapInfo.getBindPassword());
            environment.put("com.sun.jndi.ldap.connect.pool", "true");
            return new InitialDirContext(environment);
        } else {
            return apacheDs.getAdminContext( );
        }
    }

    public List<String> getTestGroupList() {
        return this.groups;
    }

    @Override
    protected String getServicePath( )
    {
        return "/groups";
    }

    @Override
    protected String getSpringConfigLocation( )
    {
        return "classpath*:spring-context.xml,classpath*:META-INF/spring-context.xml,classpath:/ldap-spring-test.xml";
    }

    @BeforeAll
    void setup( ) throws Exception
    {
        setupNative( );
        this.ldapInfo = BaseSetup.getLdapInfo( );
        if (!ldapInfo.isRemote()) {
            peopleSuffix = "ou=People,dc=archiva,dc=apache,dc=org";
            log.info( "DN Suffix: {}", peopleSuffix );
            if ( apacheDs.isStopped( ) )
            {
                groupSuffix = apacheDs.addSimplePartition( "test", new String[]{"archiva", "apache", "org"} ).getSuffix( );

                log.info( "groupSuffix: {}", groupSuffix );
                apacheDs.startServer( );
                if ( !exists( apacheDs.getAdminContext( ), peopleSuffix ) )
                {
                    BasicAttribute objectClass = new BasicAttribute( "objectClass" );
                    objectClass.add( "top" );
                    objectClass.add( "organizationalUnit" );

                    Attributes attributes = new BasicAttributes( true );
                    attributes.put( objectClass );
                    attributes.put( "organizationalUnitName", "foo" );

                    apacheDs.getAdminContext( ).createSubcontext( peopleSuffix, attributes );
                }
            }
        } else {
            this.peopleSuffix = "ou=People," + ldapInfo.getBaseDN( );
            this.groupSuffix = ldapInfo.getBaseDN( );
        }
    }

    @AfterAll
    void shutdown( ) throws Exception
    {
        shutdownNative();
        if (!this.ldapInfo.isRemote())
        {

            try
            {
                InitialDirContext context = null;
                try
                {
                    context = getAdminContext( );
                    context.unbind( this.peopleSuffix );
                }
                finally
                {
                    try
                    {
                        if ( context != null ) context.close( );
                    }
                    catch ( Exception e )
                    {
                        log.error( "Error during context close {}", e.getMessage( ) );
                    }
                    try
                    {
                        apacheDs.stopServer( );
                    }
                    catch ( Exception e )
                    {
                        log.error( "Could not stop apacheds {}", e.getMessage( ) );
                    }
                }
            }
            catch ( Exception e )
            {
                log.error( "Could not stop ldap {}", e.getMessage( ) );
            }
        }
        getAdminContext().close();
    }

    @BeforeEach
    public void initLdap( ) throws Exception
    {
        removeAllGroups( );
        createGroups( );
    }

    @AfterEach
    public void cleanupLdap( ) throws NamingException
    {
        removeAllGroups( );
    }

    protected boolean exists( DirContext context, String dn )
    {
        Object result = null;
        try {
            result = context.lookup( dn );
        }
        catch ( NameNotFoundException e ) {
            return false;
        }
        catch ( NamingException e )
        {
            log.error( "Unknown error during lookup: {}", e.getMessage( ) );
        }
        return result != null;
    }

    private void removeAllGroups( )
    {
            InitialDirContext context = null;
            try
            {
                context = getAdminContext( );
                for ( String group : getTestGroupList() )
                {
                    try
                    {
                        context.unbind( createGroupDn( group ) );
                    }
                    catch ( NamingException e )
                    {
                        // Ignore
                    }
                }

            }
            catch ( NamingException e )
            {
                log.error( "Naming exception {}", e.getMessage( ) );
            }
            finally
            {
                try
                {
                    if ( context != null ) context.close( );
                }
                catch ( Exception e )
                {
                    log.error( "Error during context close {}", e.getMessage( ) );
                }
            }
    }

    private String createGroupDn( String cn )
    {
        return "cn=" + cn + "," + groupSuffix;
    }

    private void createGroup( DirContext context, String group_name, String dn )
        throws Exception
    {
        if ( !exists( context, dn ) )
        {
            Attributes attributes = new BasicAttributes( true );
            BasicAttribute objectClass = new BasicAttribute( "objectClass" );
            objectClass.add( "top" );
            objectClass.add( "groupOfUniqueNames" );
            attributes.put( objectClass );
            attributes.put( "cn", group_name );
            BasicAttribute basicAttribute = new BasicAttribute( "uniquemember" );

            basicAttribute.add( "uid=admin," + peopleSuffix );

            attributes.put( basicAttribute );

            context.createSubcontext( dn, attributes );
        }
        else
        {
            log.error( "Group {} exists already", dn );
        }
    }

    private void createGroups( )
        throws Exception
    {
        InitialDirContext context = null;
        try
        {
            context = getAdminContext( );

            for ( String group : getTestGroupList() )
            {
                createGroup( context, group, createGroupDn( group ) );
            }
        }
        finally
        {
            if ( context != null )
            {
                context.close( );
            }
        }
    }


    @Test
    void getGroups() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON ).when( )
            .get( ).then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        List<Group> data = response.body( ).jsonPath( ).getList(  "data", Group.class );
        assertNotNull( data );
        assertEquals( Integer.valueOf( 0 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
        assertEquals( Integer.valueOf( Constants.DEFAULT_PAGE_LIMIT ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
        assertEquals( Integer.valueOf( 6 ), response.body( ).jsonPath( ).get( "pagination.total_count" ) );
        assertEquals( 6, data.size( ) );
        String[] values = data.stream( ).map( ldapInfo -> ldapInfo.getName( ) ).sorted( ).collect( Collectors.toList( ) ).toArray( new String[0] );
        assertArrayEquals( getTestGroupList( ).toArray( new String[0] ), values );
        assertEquals( "uid=admin," + this.peopleSuffix, data.get( 0 ).getMemberList( ).get( 0 ) );
    }

    @Test
    void getGroupsWithLimit() {
        String token = getAdminToken( );
        HashMap<String, Object> params = new HashMap<>( );
        params.put( "limit", Long.valueOf( 3 ) );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .param( "limit", Long.valueOf( 3 ) )
            .when( )
            .get( ).then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        List<Group> data = response.body( ).jsonPath( ).getList(  "data", Group.class );
        assertNotNull( data );
        assertEquals( Integer.valueOf( 0 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
        assertEquals( Integer.valueOf( 3 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
        assertEquals( Integer.valueOf( 6 ), response.body( ).jsonPath( ).get( "pagination.total_count" ) );
        assertEquals( 3, data.size( ) );
        String[] values = data.stream( ).map( ldapInfo -> ldapInfo.getName( ) ).sorted( ).collect( Collectors.toList( ) ).toArray( new String[0] );
        assertArrayEquals( getTestGroupList( ).subList( 0, 3 ).toArray( new String[0] ), values );
        assertEquals( "uid=admin," + this.peopleSuffix, data.get( 0 ).getMemberList( ).get( 0 ) );
    }

    @Test
    void getGroupsWithOffset() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .param( "offset", Long.valueOf( 2 ) )
            .when( )
            .get( ).then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        List<Group> data = response.body( ).jsonPath( ).getList(  "data", Group.class );
        assertNotNull( data );
        assertEquals( Integer.valueOf( 2 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
        assertEquals( Integer.valueOf( Constants.DEFAULT_PAGE_LIMIT ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
        assertEquals( Integer.valueOf( 6 ), response.body( ).jsonPath( ).get( "pagination.total_count" ) );
        assertEquals( 4, data.size( ) );
        String[] values = data.stream( ).map( ldapInfo -> ldapInfo.getName( ) ).sorted( ).collect( Collectors.toList( ) ).toArray( new String[0] );
        assertArrayEquals( getTestGroupList( ).subList( 2, getTestGroupList().size() ).toArray( new String[0] ), values );
        assertEquals( "uid=admin," + this.peopleSuffix, data.get( 0 ).getMemberList( ).get( 0 ) );
    }


    @Test
    void getGroupMapping() {
        String token = getAdminToken( );
        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
            .when( )
            .get( "/mappings" )
            .then( ).statusCode( 200 ).extract( ).response( );
        assertNotNull( response );
        assertEquals( 3, response.getBody( ).jsonPath( ).getList( "" ).size() );

    }

    @Test
    void addGroupMapping() {
        String token = getAdminToken( );
        try
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "group_name", "ldap group" );
            jsonAsMap.put( "roles", Arrays.asList( "role1", "role2" ) );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( "/mappings" )
                .then( ).statusCode( 201 ).extract( ).response( );
            assertNotNull( response );
            assertNotNull( response.getHeader( "Location" ) );

            assertTrue( UrlDecoder.urlDecode( response.getHeader( "Location" ), Charset.forName( "UTF-8" ), false ).endsWith( "/mappings/ldap group" ) );
        } finally  {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "/mappings/ldap group" )
                .then( )
                .statusCode( 200 );
        }
    }

    @Test
    void addAndGetGroupMapping() {
        String token = getAdminToken( );
        try
        {
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "group_name", "ldap group" );
            jsonAsMap.put( "roles", Arrays.asList( "role1", "role2" ) );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( "/mappings" )
                .then( ).statusCode( 201 ).extract( ).response( );
            assertNotNull( response );
            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "/mappings" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            List<GroupMapping> resultList = response.getBody( ).jsonPath( ).getList( "", GroupMapping.class );
            assertEquals( 4, response.getBody( ).jsonPath( ).getList( "" ).size() );
            Optional<GroupMapping> found = resultList.stream( ).filter( map -> map.getGroupName( ).equals( "ldap group" ) && map.getRoles( ).size( ) == 2 && map.getRoles( ).contains( "role1" ) ).findAny( );
            assertTrue( found.isPresent( ) );
        } finally  {
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "/mappings/ldap group" )
                .then( )
                .statusCode( 200 );
        }
    }

    @Test
    void deleteGroupMapping() {
        String token = getAdminToken( );
        try
        {
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .delete( "/mappings/archiva-admin" )
                .then( )
                .statusCode( 200 ).extract( ).response( );
        } finally {
            // Put it back
            Map<String, Object> jsonAsMap = new HashMap<>( );
            jsonAsMap.put( "group_name", "archiva-admin" );
            jsonAsMap.put( "roles", Arrays.asList( "System Administrator" ) );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .body( jsonAsMap )
                .when( )
                .post( "/mappings" )
                .then( ).statusCode( 201 );
        }
    }

    @Test
    void updateGroupMapping() {
        String token = getAdminToken( );
        try
        {
            List<String> list = Arrays.asList( "System Administrator", "role1", "role2", "role3" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body(list)
                .put( "/mappings/archiva-admin" )
                .then( )
                .statusCode( 200 ).extract( ).response( );
        } finally {
            // Put it back
            List<String> list = Arrays.asList( "System Administrator" );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .body( list )
                .when( )
                .put( "/mappings/archiva-admin" )
                .then( ).statusCode( 200 );
        }
    }

    @Test
    void updateAndGetGroupMapping() {
        String token = getAdminToken( );
        try
        {
            // The default implementation of redback uses the value from the configuration persistently
            // and adds the updates to the configuration.
            List<String> list = Arrays.asList( "role1", "role2", "role3" );
            Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .body(list)
                .put( "/mappings/archiva-admin" )
                .then( )
                .statusCode( 200 ).extract( ).response( );

            response = given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .when( )
                .get( "/mappings" )
                .then( ).statusCode( 200 ).extract( ).response( );
            assertNotNull( response );
            List<GroupMapping> resultList = response.getBody( ).jsonPath( ).getList( "", GroupMapping.class );
            assertEquals( 3, response.getBody( ).jsonPath( ).getList( "" ).size() );
            for (GroupMapping mapping : resultList) {
                System.out.println( mapping.getGroupName( ) + "/" + mapping.getUniqueGroupName( )  );
                for (String role : mapping.getRoles( )) {
                    System.out.println( "Role " + role );
                }
            }
            Optional<GroupMapping> found = resultList.stream( ).filter( map -> map.getGroupName( ).equals( "archiva-admin" ) && map.getRoles( ).size( ) == 4 && map.getRoles( ).contains( "role3" ) ).findAny( );
            assertTrue( found.isPresent( ) );

        } finally {
            // Put it back
            List<String> list = Arrays.asList( "System Administrator" );
            given( ).spec( getRequestSpec( token ) ).contentType( JSON )
                .body( list )
                .when( )
                .put( "/mappings/archiva-admin" )
                .then( ).statusCode( 200 );
        }
    }

}
