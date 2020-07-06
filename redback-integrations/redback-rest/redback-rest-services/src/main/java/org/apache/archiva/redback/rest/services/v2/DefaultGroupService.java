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
import org.apache.archiva.redback.common.ldap.connection.LdapConnection;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.common.ldap.connection.LdapException;
import org.apache.archiva.redback.common.ldap.role.LdapGroup;
import org.apache.archiva.redback.common.ldap.role.LdapRoleMapper;
import org.apache.archiva.redback.common.ldap.role.LdapRoleMapperConfiguration;
import org.apache.archiva.redback.rest.api.model.ActionStatus;
import org.apache.archiva.redback.rest.api.model.Group;
import org.apache.archiva.redback.rest.api.model.GroupMapping;
import org.apache.archiva.redback.rest.api.model.GroupMappingUpdateRequest;
import org.apache.archiva.redback.rest.api.model.StringList;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.api.services.v2.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
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
@Service("v2.groupService#rest")
public class DefaultGroupService
    implements GroupService
{
    private final Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named(value = "ldapRoleMapper#default")
    private LdapRoleMapper ldapRoleMapper;

    @Inject
    @Named(value = "ldapRoleMapperConfiguration#default")
    private LdapRoleMapperConfiguration ldapRoleMapperConfiguration;

    @Inject
    @Named(value = "ldapConnectionFactory#configurable")
    private LdapConnectionFactory ldapConnectionFactory;

    private static final Group getGroupFromLdap( LdapGroup ldapGroup ) {
        Group group = new Group( );
        group.setName( ldapGroup.getName() );
        group.setUniqueName( ldapGroup.getDn() );
        group.setDescription( ldapGroup.getDescription() );
        group.setMemberList( ldapGroup.getMemberList() );
        return group;
    }

    @Override
    public List<Group> getGroups( Long offset, Long limit ) throws RedbackServiceException
    {
        LdapConnection ldapConnection = null;

        DirContext context = null;

        try
        {
            ldapConnection = ldapConnectionFactory.getConnection();
            context = ldapConnection.getDirContext();
            return ldapRoleMapper.getAllGroupObjects( context ).stream( ).skip( offset ).limit( limit ).map( DefaultGroupService::getGroupFromLdap ).collect( Collectors.toList( ) );
        }
        catch ( LdapException | MappingException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        finally
        {
            closeContext( context );
            closeLdapConnection( ldapConnection );
        }
    }

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
                GroupMapping ldapGroupMapping = new GroupMapping( entry.getKey(), entry.getValue() );
                ldapGroupMappings.add( ldapGroupMapping );
            }

            return ldapGroupMappings;
        }
        catch ( MappingException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
    }

    @Override
    public ActionStatus addGroupMapping( GroupMapping ldapGroupMapping )
        throws RedbackServiceException
    {
        try
        {
            ldapRoleMapperConfiguration.addLdapMapping( ldapGroupMapping.getGroup(),
                                                        new ArrayList<>( ldapGroupMapping.getRoleNames() ) );
        }
        catch ( MappingException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        return ActionStatus.SUCCESS;
    }

    @Override
    public ActionStatus removeGroupMapping( String group )
        throws RedbackServiceException
    {
        try
        {
            ldapRoleMapperConfiguration.removeLdapMapping( group );
        }
        catch ( MappingException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        return ActionStatus.SUCCESS;
    }

    @Override
    public ActionStatus updateGroupMapping( String groupName, GroupMapping groupMapping ) throws RedbackServiceException
    {
        try
        {
            ldapRoleMapperConfiguration.getLdapGroupMapping( groupName );
        }
        catch ( MappingException e )
        {
            throw new RedbackServiceException( "Group mapping not found ", 404 );
        }
        try
        {
            ldapRoleMapperConfiguration.updateLdapMapping( groupName,
                new ArrayList<>( groupMapping.getRoleNames() ) );
            return ActionStatus.SUCCESS;
        }
        catch ( MappingException e )
        {
            log.error( "Could not update mapping {}", e.getMessage( ) );
            throw new RedbackServiceException( e.getMessage( ) );
        }
    }

    @Override
    public ActionStatus updateGroupMapping( GroupMappingUpdateRequest groupMappingUpdateRequest )
        throws RedbackServiceException
    {
        try
        {
            for ( GroupMapping ldapGroupMapping : groupMappingUpdateRequest.getGroupMapping() )
            {
                ldapRoleMapperConfiguration.updateLdapMapping( ldapGroupMapping.getGroup(),
                                                               new ArrayList<>( ldapGroupMapping.getRoleNames() ) );
            }
        }
        catch ( MappingException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        return ActionStatus.SUCCESS;
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
