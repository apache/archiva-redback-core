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

import org.apache.archiva.redback.common.ldap.MappingException;
import org.apache.archiva.redback.common.ldap.ObjectNotFoundException;
import org.apache.archiva.redback.common.ldap.connection.LdapConnection;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.common.ldap.connection.LdapException;
import org.apache.archiva.redback.common.ldap.role.LdapGroup;
import org.apache.archiva.redback.common.ldap.role.LdapRoleMapper;
import org.apache.archiva.redback.common.ldap.role.LdapRoleMapperConfiguration;
import org.apache.archiva.redback.rest.api.MessageKeys;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.model.v2.Group;
import org.apache.archiva.redback.rest.api.model.v2.GroupMapping;
import org.apache.archiva.redback.rest.api.model.v2.PagedResult;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.api.services.v2.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * LDAP implementation of the group service
 *
 * @author Olivier Lamy
 * @author Martin Stockhammer
 * @since 3.0
 */
@SuppressWarnings( "SpringJavaAutowiredFieldsWarningInspection" )
@Service("v2.groupService#rest")
public class DefaultGroupService
    implements GroupService
{
    private final Logger log = LoggerFactory.getLogger( getClass() );

    @Context  //injected response proxy supporting multiple threads
    private HttpServletResponse response;

    @Context
    private HttpServletRequest request;

    @Inject
    @Named(value = "ldapRoleMapper#default")
    private LdapRoleMapper ldapRoleMapper;

    @Inject
    @Named(value = "ldapRoleMapperConfiguration#default")
    private LdapRoleMapperConfiguration ldapRoleMapperConfiguration;

    @Inject
    @Named(value = "ldapConnectionFactory#configurable")
    private LdapConnectionFactory ldapConnectionFactory;

    public DefaultGroupService( ) {
    }

    private static Group getGroupFromLdap( LdapGroup ldapGroup ) {
        Group group = new Group( );
        group.setName( ldapGroup.getName() );
        group.setUniqueName( ldapGroup.getDn() );
        group.setDescription( ldapGroup.getDescription() );
        group.setMemberList( ldapGroup.getMemberList() );
        return group;
    }

    @Override
    public PagedResult<Group> getGroups( Integer offset, Integer limit ) throws RedbackServiceException
    {
        LdapConnection ldapConnection = null;

        DirContext context = null;

        try
        {
            ldapConnection = ldapConnectionFactory.getConnection();
            context = ldapConnection.getDirContext();
            List<LdapGroup> groups = ldapRoleMapper.getAllGroupObjects( context );
            return PagedResult.of( groups.size( ), offset, limit, groups.stream( ).skip( offset ).limit( limit ).map( DefaultGroupService::getGroupFromLdap ).collect( Collectors.toList( ) ) );
        }
        catch ( LdapException  e )
        {
            log.error( "LDAP Error {}", e.getMessage(), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_LDAP_GENERIC ) );
        } catch (MappingException e) {
            log.error( "Mapping Error {}", e.getMessage(), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_MAPPING, e.getMessage( ) ) );
        }
        finally
        {
            closeContext( context );
            closeLdapConnection( ldapConnection );
        }
    }

    /**
     * Tries to retrieve the LDAP group for the mapping to add the unique name. If the group cannot
     * be found, it will set "" for the uniqueName
     *
     * @return the list of mapping
     * @throws RedbackServiceException if there was an error retrieving the mapping data
     */
    @Override
    public List<GroupMapping> getGroupMappings()
        throws RedbackServiceException
    {
        try
        {
            Map<String, Collection<String>> map = ldapRoleMapperConfiguration.getLdapGroupMappings();
            List<GroupMapping> ldapGroupMappings = new ArrayList<>( map.size( ) );
            for ( Map.Entry<String, Collection<String>> entry : map.entrySet() )
            {
                String groupName = entry.getKey( );
                DirContext context = null;
                LdapConnection ldapConnection = null;
                try
                {
                    ldapConnection = ldapConnectionFactory.getConnection( );
                    context = ldapConnection.getDirContext( );

                    LdapGroup ldapGroup = ldapRoleMapper.getGroupForName( context, groupName );
                    GroupMapping ldapGroupMapping = new GroupMapping( ldapGroup.getName(), ldapGroup.getDn(), new ArrayList<>( entry.getValue( ) ) );
                    ldapGroupMappings.add( ldapGroupMapping );
                }
                catch ( LdapException e )
                {
                    log.error( "Could not create ldap connection {}", e.getMessage( ) );
                    throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_LDAP_GENERIC, "Error while talking to group registry"), 500 );
                }
                catch ( ObjectNotFoundException e ) {
                    GroupMapping ldapGroupMapping = new GroupMapping( groupName, "", new ArrayList<>( entry.getValue( ) ) );
                    ldapGroupMappings.add( ldapGroupMapping );
                }
                finally
                {
                    closeContext( context );
                    closeLdapConnection( ldapConnection );
                }
            }

            return ldapGroupMappings;
        }
        catch ( MappingException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_MAPPING, e.getMessage( ) ) );
        }
    }

    @Override
    public Response addGroupMapping( GroupMapping ldapGroupMapping, UriInfo uriInfo)
        throws RedbackServiceException
    {
        try
        {
            ldapRoleMapperConfiguration.addLdapMapping( ldapGroupMapping.getGroupName(),
                                                        new ArrayList<>( ldapGroupMapping.getRoles() ) );
            response.setStatus( Response.Status.CREATED.getStatusCode() );
            if (uriInfo!=null)
            {
                response.setHeader( "Location", uriInfo.getAbsolutePathBuilder( ).path( ldapGroupMapping.getGroupName( ) ).build( ).toString( ) );
            }
            return Response.status( 201 ).build( );
        }
        catch ( MappingException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_MAPPING, e.getMessage( ) ) );
        }
    }

    @Override
    public void removeGroupMapping( String group )
        throws RedbackServiceException
    {
        try
        {
            ldapRoleMapperConfiguration.removeLdapMapping( group );
        }
        catch ( MappingException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_MAPPING, e.getMessage( ) ) );
        }
        response.setStatus( 200 );
    }

    @Override
    public Response updateGroupMapping( String groupName, List<String> roles ) throws RedbackServiceException
    {
        try
        {
            ldapRoleMapperConfiguration.getLdapGroupMapping( groupName );
        }
        catch ( MappingException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_MAPPING_NOT_FOUND), 404 );
        }
        try
        {
            ldapRoleMapperConfiguration.updateLdapMapping( groupName,
                roles );
            return Response.ok( ).build( );
        }
        catch ( MappingException e )
        {
            log.error( "Could not update mapping {}", e.getMessage( ) );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_MAPPING, e.getMessage( ) ) );
        }
    }

    //------------------
    // utils
    //------------------

    protected void closeLdapConnection( LdapConnection ldapConnection )
    {
        if ( ldapConnection != null )
        {
            ldapConnection.close();
        }
    }

    protected void closeContext( DirContext context )
    {
        if ( context != null )
        {
            try
            {
                context.close();
            }
            catch ( NamingException e )
            {
                log.warn( "skip issue closing context: {}", e.getMessage() );
            }
        }
    }
}
