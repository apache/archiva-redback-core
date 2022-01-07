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

import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.components.rest.util.QueryHelper;
import org.apache.archiva.redback.common.ldap.MappingException;
import org.apache.archiva.redback.common.ldap.ObjectNotFoundException;
import org.apache.archiva.redback.common.ldap.connection.LdapConnection;
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.common.ldap.connection.LdapException;
import org.apache.archiva.redback.common.ldap.role.LdapGroup;
import org.apache.archiva.redback.common.ldap.role.LdapRoleMapper;
import org.apache.archiva.redback.common.ldap.role.LdapRoleMapperConfiguration;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rest.api.MessageKeys;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.model.v2.Group;
import org.apache.archiva.redback.rest.api.model.v2.GroupMapping;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.api.services.v2.GroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private static final Logger log = LoggerFactory.getLogger( DefaultGroupService.class );

    private static final String[] DEFAULT_SEARCH_FIELDS = {"name"};
    private static final Map<String, BiPredicate<String, LdapGroup>> FILTER_MAP = new HashMap<>( );
    private static final Map<String, Comparator<LdapGroup>> ORDER_MAP = new HashMap<>( );
    private static final QueryHelper<LdapGroup> QUERY_HELPER;

    static
    {

        QUERY_HELPER = new QueryHelper<>( FILTER_MAP, ORDER_MAP, DEFAULT_SEARCH_FIELDS );
        QUERY_HELPER.addStringFilter( "name", LdapGroup::getName );
        QUERY_HELPER.addStringFilter( "uniqueName", LdapGroup::getDn );
        QUERY_HELPER.addStringFilter( "description", LdapGroup::getDescription );

        // The simple Comparator.comparing(attribute) is not null safe
        // As there are attributes that may have a null value, we have to use a comparator with nullsLast(naturalOrder)
        // and the wrapping Comparator.nullsLast(Comparator.comparing(attribute)) does not work, because the attribute is not checked by the nullsLast-Comparator
        QUERY_HELPER.addNullsafeFieldComparator( "name", LdapGroup::getName );
        QUERY_HELPER.addNullsafeFieldComparator( "uniqueName", LdapGroup::getDn );
        QUERY_HELPER.addNullsafeFieldComparator( "description", LdapGroup::getDescription );
    }


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
    public PagedResult<Group> getGroups( String searchTerm, Integer offset, Integer limit, List<String> orderBy, String order ) throws RedbackServiceException
    {
        try(LdapConnection ldapConnection = this.ldapConnectionFactory.getConnection())
        {
            boolean isAscending = QUERY_HELPER.isAscending( order );
            DirContext context = ldapConnection.getDirContext();
            Predicate<LdapGroup> filter = QUERY_HELPER.getQueryFilter( searchTerm );
            Comparator<LdapGroup> comparator = QUERY_HELPER.getComparator( orderBy, isAscending );
            List<LdapGroup> groups = ldapRoleMapper.getAllGroupObjects( context );
            int totalCount = Math.toIntExact( groups.stream( ).filter( filter ).count( ) );
            List<Group> result = groups.stream( ).filter( filter ).sorted( comparator ).skip( offset ).limit( limit )
            .map(DefaultGroupService::getGroupFromLdap)
            .collect( Collectors.toList());
            return PagedResult.of( totalCount, offset, limit, result );
        }
        catch ( NamingException e )
        {
            log.error( "LDAP Error {}", e.getMessage(), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_LDAP_GENERIC ) );
        } catch (MappingException e) {
            log.error( "Mapping Error {}", e.getMessage(), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_MAPPING, e.getMessage( ) ) );
        } catch (ArithmeticException e) {
            log.error( "Could not convert total count to integer:  {}", e.getMessage( ) );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_INVALID_PAGING_RESULT_ERROR ) );
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

    @Override
    public List<String> getGroupMapping( String groupName ) throws RedbackServiceException
    {
        Collection<String> mapping;
        try
        {
            mapping = ldapRoleMapperConfiguration.getLdapGroupMapping( groupName );
            return new ArrayList<>( mapping );
        }
        catch ( MappingException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_MAPPING_NOT_FOUND ), 404 );
        }
    }


    @Override
    public Response addRolesToGroupMapping( String groupName, String roleId ) throws RedbackServiceException
    {
        Collection<String> mapping;
        try
        {
            mapping = ldapRoleMapperConfiguration.getLdapGroupMapping( groupName );
        }
        catch ( MappingException e )
        {
            try
            {
                ldapRoleMapperConfiguration.addLdapMapping( groupName, Arrays.asList( roleId) );
                return Response.ok( ).build( );
            }
            catch ( MappingException mappingException )
            {
                log.error( "Could not update mapping {}", e.getMessage( ) );
                throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_MAPPING, e.getMessage( ) ) );
            }
        }
        if (mapping!=null)
        {
            try
            {
                ArrayList<String> newList = new ArrayList<>( mapping );
                if (!newList.contains( roleId )) {
                    newList.add( roleId );
                    ldapRoleMapperConfiguration.updateLdapMapping( groupName,
                        newList );
                }
                return Response.ok( ).build( );
            }
            catch ( MappingException e )
            {
                log.error( "Could not update mapping {}", e.getMessage( ) );
                throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_MAPPING, e.getMessage( ) ) );
            }
        } else {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_MAPPING_NOT_FOUND ), 404 );
        }

    }

    //------------------
    // utils
    //------------------

    protected void closeLdapConnection( LdapConnection ldapConnection )
    {
        if ( ldapConnection != null )
        {
            try
            {
                ldapConnection.close();
            }
            catch ( NamingException e )
            {
                log.error( "Could not close LDAP connection {}", e.getMessage( ) );
            }
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
