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
import org.apache.archiva.components.apacheds.ApacheDs;
import org.apache.archiva.redback.rest.api.model.GroupMapping;
import org.apache.archiva.redback.rest.api.services.v2.GroupService;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;


/**
 * @author Olivier Lamy
 */
@ExtendWith( SpringExtension.class )
@ContextConfiguration(
    locations = {"classpath:/ldap-spring-test.xml"} )
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
public class GroupServiceTest
    extends AbstractRestServicesTestV2
{

    @Inject
    @Named( value = "apacheDS#test" )
    private ApacheDs apacheDs;

    List<String> groups =
        Arrays.asList( "Archiva System Administrator", "Internal Repo Manager", "Internal Repo Observer" );

    private String suffix;

    private String groupSuffix;

    protected GroupService getGroupService( String authzHeader )
    {
        GroupService service =
            JAXRSClientFactory.create( "http://localhost:" + getServerPort( ) + "/" + getRestServicesPath( ) + "/v2/redback/",
                GroupService.class,
                Collections.singletonList( new JacksonJaxbJsonProvider( ) ) );

        // for debuging purpose
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

    @Override
    protected String getSpringConfigLocation( )
    {
        return "classpath*:spring-context.xml,classpath*:META-INF/spring-context.xml,classpath:/ldap-spring-test.xml";
    }

    @BeforeAll
    public void startup( )
        throws Exception
    {
        super.init( );
        super.startServer( );

        suffix = "ou=People,dc=archiva,dc=apache,dc=org";
        log.info( "DN Suffix: {}", suffix );
        if ( apacheDs.isStopped( ) )
        {
            groupSuffix = apacheDs.addSimplePartition( "test", new String[]{"archiva", "apache", "org"} ).getSuffix( );

            log.info( "groupSuffix: {}", groupSuffix );
            apacheDs.startServer( );
            if ( !exists( apacheDs.getAdminContext( ), suffix ) )
            {
                BasicAttribute objectClass = new BasicAttribute( "objectClass" );
                objectClass.add( "top" );
                objectClass.add( "organizationalUnit" );

                Attributes attributes = new BasicAttributes( true );
                attributes.put( objectClass );
                attributes.put( "organizationalUnitName", "foo" );

                apacheDs.getAdminContext( ).createSubcontext( suffix, attributes );
            }
        }
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

    private void removeAllGroups( )
    {
        if (!apacheDs.isStopped())
        {
            InitialDirContext context = null;
            try
            {
                context = apacheDs.getAdminContext( );
                for ( String group : this.groups )
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
                log.error( "Could not remove groups {}", e.getMessage( ), e );
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
    }

    @AfterAll
    public void stop( ) throws Exception

    {

        removeAllGroups( );
        // cleanup ldap entries
        try
        {
            InitialDirContext context = null;
            try
            {
                context = apacheDs.getAdminContext( );
                context.unbind( suffix );
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
        finally
        {
            super.stopServer( );
            super.destroy( );
        }
    }

    private void createGroups( )
        throws Exception
    {
        InitialDirContext context = null;
        try
        {
            context = apacheDs.getAdminContext( );

            for ( String group : groups )
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

    private void createGroup( DirContext context, String groupName, String dn )
        throws Exception
    {
        if ( !exists( context, dn ) )
        {
            Attributes attributes = new BasicAttributes( true );
            BasicAttribute objectClass = new BasicAttribute( "objectClass" );
            objectClass.add( "top" );
            objectClass.add( "groupOfUniqueNames" );
            attributes.put( objectClass );
            attributes.put( "cn", groupName );
            BasicAttribute basicAttribute = new BasicAttribute( "uniquemember" );

            basicAttribute.add( "uid=admin," + suffix );

            attributes.put( basicAttribute );

            context.createSubcontext( dn, attributes );
        }
        else
        {
            log.error( "Group {} exists already", dn );
        }
    }

    private String createGroupDn( String cn )
    {
        return "cn=" + cn + "," + groupSuffix;
    }

    @Test
    public void getAllGroups( )
        throws Exception
    {
        String authorizationHeader = getAdminAuthzHeader( );

        try
        {
            GroupService service = getGroupService( authorizationHeader );

            List<String> allGroups = service.getGroups( Integer.valueOf( 0 ), Integer.valueOf( Integer.MAX_VALUE ) ).getData( ).stream( ).map( group -> group.getName( ) ).collect( Collectors.toList( ) );

            assertNotNull( allGroups );
            assertEquals( 3, allGroups.size( ) );
            for (String group : groups) {
                assertTrue( allGroups.contains( group ) );
            }
        }
        catch ( Exception e )
        {
            log.error( e.getMessage( ), e );
            throw e;
        }
    }

    @Test
    public void getGroupMappings( )
        throws Exception
    {

        String authorizationHeader = getAdminAuthzHeader( );
        try
        {
            GroupService service = getGroupService( authorizationHeader );

            List<GroupMapping> mappings = service.getGroupMappings( );

            assertNotNull( mappings );
            assertEquals( 3, mappings.size( ) );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage( ), e );
            throw e;
        }
    }

    @Test
    public void addThenRemove( )
        throws Exception
    {
        String authorizationHeader = getAdminAuthzHeader( );

        try
        {
            GroupService service = getGroupService( authorizationHeader );

            List<GroupMapping> mappings = service.getGroupMappings( );

            assertNotNull( mappings );
            assertEquals( 3, mappings.size( ) );

            GroupMapping groupMapping = new GroupMapping( "ldap group", Arrays.asList( "redback role" ) );

            service.addGroupMapping( groupMapping );

            mappings = service.getGroupMappings( );

            assertNotNull( mappings );
            assertEquals( 4, mappings.size( ) );
            for (GroupMapping mapping : mappings) {
                if ( StringUtils.equals( "ldap group", mapping.getGroup( ) ) )
                {
                    Collection<String> names = mapping.getRoleNames( );
                    assertNotNull( names );
                    assertTrue( names.size( ) > 0 );
                    for (String name : names) {
                        assertEquals( "redback role", name );
                    }
                }

            }
            service.removeGroupMapping( "ldap group" );

            mappings = service.getGroupMappings( );

            assertNotNull( mappings );
            assertEquals( 3, mappings.size( ) );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage( ), e );
            throw e;
        }
    }
}
