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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.components.apacheds.ApacheDs;
import org.apache.archiva.redback.rest.api.model.LdapGroupMapping;
import org.apache.archiva.redback.rest.api.services.LdapGroupMappingService;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * @author Olivier Lamy
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration(
    locations = { "classpath:/ldap-spring-test.xml" } )
@DirtiesContext( classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD )
public class LdapGroupMappingServiceTest
    extends AbstractRestServicesTest
{

    @Inject
    @Named( value = "apacheDS#test" )
    private ApacheDs apacheDs;

    List<String> groups =
        Arrays.asList( "Archiva System Administrator", "Internal Repo Manager", "Internal Repo Observer" );

    private String suffix;

    private String groupSuffix;

    @Override
    protected String getSpringConfigLocation()
    {
        return "classpath*:spring-context.xml,classpath*:META-INF/spring-context.xml,classpath:/ldap-spring-test.xml";
    }

    @Override
    public void startServer()
        throws Exception
    {
        super.startServer();

        groupSuffix = apacheDs.addSimplePartition( "test", new String[]{ "archiva", "apache", "org" } ).getSuffix();

        log.info( "groupSuffix: {}", groupSuffix );

        suffix = "ou=People,dc=archiva,dc=apache,dc=org";

        log.info( "DN Suffix: {}", suffix );

        apacheDs.startServer();

        BasicAttribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "organizationalUnit" );

        Attributes attributes = new BasicAttributes( true );
        attributes.put( objectClass );
        attributes.put( "organizationalUnitName", "foo" );

        apacheDs.getAdminContext().createSubcontext( suffix, attributes );

        createGroups();
    }

    @Override
    public void stopServer()
        throws Exception
    {

        // cleanup ldap entries
        InitialDirContext context = apacheDs.getAdminContext();

        for ( String group : this.groups )
        {
            context.unbind( createGroupDn( group ) );
        }

        context.unbind( suffix );

        context.close();

        apacheDs.stopServer();

        super.stopServer();
    }

    private void createGroups()
        throws Exception
    {
        InitialDirContext context = apacheDs.getAdminContext();

        for ( String group : groups )
        {
            createGroup( context, group, createGroupDn( group ) );
        }

    }

    private void createGroup( DirContext context, String groupName, String dn )
        throws Exception
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

    private String createGroupDn( String cn )
    {
        return "cn=" + cn + "," + groupSuffix;
    }

    @Test
    public void getAllGroups()
        throws Exception
    {

        try
        {
            LdapGroupMappingService service = getLdapGroupMappingService( authorizationHeader );

            List<String> allGroups = service.getLdapGroups().getStrings();

            assertNotNull( allGroups );
            assertTrue( allGroups.size( ) > 0 );
            assertEquals( 3, allGroups.size( ) );
            for (String group : groups) {
                assertTrue( allGroups.contains( group ) );
            }
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw e;
        }
    }

    @Test
    public void getLdapGroupMappings()
        throws Exception
    {
        try
        {
            LdapGroupMappingService service = getLdapGroupMappingService( authorizationHeader );

            List<LdapGroupMapping> mappings = service.getLdapGroupMappings();

            assertNotNull( mappings );
            assertEquals( 3, mappings.size() );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw e;
        }
    }

    @Test
    public void addThenRemove()
        throws Exception
    {
        try
        {
            LdapGroupMappingService service = getLdapGroupMappingService( authorizationHeader );

            List<LdapGroupMapping> mappings = service.getLdapGroupMappings();

            assertNotNull( mappings );
            assertEquals( 3, mappings.size( ) );

            LdapGroupMapping ldapGroupMapping = new LdapGroupMapping( "ldap group", Arrays.asList( "redback role" ) );

            service.addLdapGroupMapping( ldapGroupMapping );

            mappings = service.getLdapGroupMappings();

            assertNotNull( mappings );
            assertEquals( 4, mappings.size( ) );
            for (LdapGroupMapping mapping : mappings) {
                if (StringUtils.equals( "ldap group", mapping.getGroup( ) ) ) {
                    Collection<String> names = mapping.getRoleNames( );
                    assertNotNull( names );
                    assertTrue( names.size( ) > 0 );
                    for (String name : names) {
                        assertEquals( "redback role", name );
                    }
                }

            }
            service.removeLdapGroupMapping( "ldap group" );

            mappings = service.getLdapGroupMappings();

            assertNotNull( mappings );
            assertEquals( 3, mappings.size( ) );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            throw e;
        }
    }
}
