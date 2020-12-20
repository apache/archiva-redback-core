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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.RbacObjectNotFoundException;
import org.apache.archiva.redback.rest.api.MessageKeys;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.model.v2.PagedResult;
import org.apache.archiva.redback.rest.api.model.v2.Role;
import org.apache.archiva.redback.rest.api.model.v2.RoleInfo;
import org.apache.archiva.redback.rest.api.model.v2.RoleTemplate;
import org.apache.archiva.redback.rest.api.model.v2.UserInfo;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.api.services.v2.RoleService;
import org.apache.archiva.redback.role.PermanentRoleDeletionInvalid;
import org.apache.archiva.redback.role.RoleExistsException;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.role.RoleNotFoundException;
import org.apache.archiva.redback.role.util.RoleModelUtils;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Olivier Lamy
 * @since 1.3
 */
@Service("v2.roleService#rest")
public class DefaultRoleService extends BaseRedbackService
    implements RoleService
{

    private Logger log = LoggerFactory.getLogger( DefaultRoleService.class );

    private RoleManager roleManager;

    @Context
    private HttpServletRequest httpServletRequest;

    @Context
    private HttpServletResponse httpServletResponse;

    @Context
    private UriInfo uriInfo;

    private static final String[] DEFAULT_SEARCH_FIELDS = {"id", "name", "description"};
    private static final Map<String, BiPredicate<String, org.apache.archiva.redback.rbac.Role>> FILTER_MAP = new HashMap<>( );
    private static final Map<String, Comparator<org.apache.archiva.redback.rbac.Role>> ORDER_MAP = new HashMap<>( );
    private static final QueryHelper<org.apache.archiva.redback.rbac.Role> QUERY_HELPER;

    static
    {

        QUERY_HELPER = new QueryHelper<>( FILTER_MAP, ORDER_MAP, DEFAULT_SEARCH_FIELDS );
        QUERY_HELPER.addStringFilter( "id", org.apache.archiva.redback.rbac.Role::getId );
        QUERY_HELPER.addStringFilter( "name", org.apache.archiva.redback.rbac.Role::getName );
        QUERY_HELPER.addStringFilter( "description", org.apache.archiva.redback.rbac.Role::getDescription );
        QUERY_HELPER.addBooleanFilter( "assignable", org.apache.archiva.redback.rbac.Role::isAssignable );

        // The simple Comparator.comparing(attribute) is not null safe
        // As there are attributes that may have a null value, we have to use a comparator with nullsLast(naturalOrder)
        // and the wrapping Comparator.nullsLast(Comparator.comparing(attribute)) does not work, because the attribute is not checked by the nullsLast-Comparator
        QUERY_HELPER.addNullsafeFieldComparator( "name", org.apache.archiva.redback.rbac.Role::getName );
        QUERY_HELPER.addNullsafeFieldComparator( "id", org.apache.archiva.redback.rbac.Role::getId );
        QUERY_HELPER.addNullsafeFieldComparator( "resource", org.apache.archiva.redback.rbac.Role::getResource );
        QUERY_HELPER.addNullsafeFieldComparator( "assignable", org.apache.archiva.redback.rbac.Role::isAssignable );
        QUERY_HELPER.addNullsafeFieldComparator( "description", org.apache.archiva.redback.rbac.Role::getDescription );
        QUERY_HELPER.addNullsafeFieldComparator( "template_instance", org.apache.archiva.redback.rbac.Role::isTemplateInstance );
    }

    @Inject
    public DefaultRoleService( RoleManager roleManager,
                               @Named(value = "rbacManager#default") RBACManager rbacManager,
                               @Named(value = "userManager#default") UserManager userManager )
    {
        super( rbacManager, userManager );
        this.roleManager = roleManager;

        log.debug( "use rbacManager impl: {}", rbacManager.getClass().getName() );
        log.debug( "use userManager impl: {}", userManager.getClass().getName() );
    }

    @Override
    public PagedResult<RoleInfo> getAllRoles( String searchTerm, Integer offset, Integer limit, List<String> orderBy, String order ) throws RedbackServiceException
    {
        boolean ascending = isAscending( order );
        try
        {
            // UserQuery does not work here, because the configurable user manager does only return the query for
            // the first user manager in the list. So we have to fetch the whole role list
            List<? extends org.apache.archiva.redback.rbac.Role> rawRoles = rbacManager.getAllRoles( );
            Predicate<org.apache.archiva.redback.rbac.Role> filter = QUERY_HELPER.getQueryFilter( searchTerm );
            long size = rawRoles.stream( ).filter( filter ).count( );
            List<RoleInfo> users = rawRoles.stream( )
                .filter( filter )
                .sorted( QUERY_HELPER.getComparator( orderBy, ascending ) ).skip( offset ).limit( limit )
                .map( role -> {
                    try
                    {
                        return Optional.of( getRoleInfo( role ) );
                    }
                    catch ( RedbackServiceException e )
                    {
                        return Optional.<RoleInfo>empty();
                    }
                } ).filter(Optional::isPresent)
                .map(Optional::get)
                .collect( Collectors.toList( ) );
            return new PagedResult<>( (int) size, offset, limit, users );
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL , e.getMessage( )) );
        }

    }

    @Override
    public RoleInfo getRole( String roleId ) throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.rbac.Role rbacRole = rbacManager.getRoleById( roleId );
            RoleInfo role = getRoleInfo( rbacRole );
            return role;
        }
        catch ( RbacObjectNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_NOT_FOUND, roleId ), 404 );
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
    }

    @Override
    public Response checkRole( String roleId ) throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.rbac.Role rbacRole = rbacManager.getRoleById( roleId );
            if (rbacRole==null) {
                return Response.status( 404 ).build();
            } else
            {
                return Response.ok( ).build( );
            }
        }
        catch ( RbacObjectNotFoundException e )
        {
            return Response.status( 404 ).build();
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
    }



    @Override
    public RoleInfo moveTemplatedRole( String templateId, String oldResource, String newResource )
        throws RedbackServiceException
    {
        try
        {
            if (StringUtils.isEmpty( templateId ) || StringUtils.isEmpty( oldResource ) || StringUtils.isEmpty( newResource )) {
                throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_NOT_FOUND ), 404 );
            }
            boolean sourceExists = roleManager.templatedRoleExists( templateId, oldResource );
            if (!sourceExists) {
                throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_INSTANCE_NOT_FOUND, templateId, oldResource ), 404 );
            }
            boolean destExists = roleManager.templatedRoleExists( templateId, newResource );
            if (destExists) {
                httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePathBuilder().path("../../..").path(newResource).build(  ).normalize().toString() );
                throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_INSTANCE_EXISTS, templateId, newResource ), 303 );
            }
            String roleId = roleManager.moveTemplatedRole( templateId, oldResource, newResource );
            httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePathBuilder().path("../../..").path(newResource).build(  ).normalize().toString() );
            httpServletResponse.setStatus( 201 );
            return getRoleInfo( rbacManager.getRoleById( roleId ) );
        }
        catch ( RoleExistsException e ) {
            httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePathBuilder().path("../../..").path(newResource).build(  ).normalize().toString() );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_INSTANCE_EXISTS, templateId, newResource ), 303 );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLEMANAGER_FAIL, e.getMessage( ) ) );
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
    }


    @Override
    public Response checkTemplateRole( String templateId, String resource )
        throws RedbackServiceException
    {
        try
        {
            if (roleManager.templatedRoleExists( templateId, resource )) {
                return Response.ok( ).build( );
            } else {
                return Response.status( 404 ).build();
            }
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }

    }

    @Override
    public RoleInfo createTemplatedRole( String templateId, String resource )
        throws RedbackServiceException
    {
        if (StringUtils.isEmpty( templateId )) {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_NOT_FOUND ), 404 );
        }
        if (StringUtils.isEmpty( resource )) {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_NOT_FOUND ), 404 );
        }
        try
        {
            boolean exists = roleManager.templatedRoleExists( templateId, resource );
            String roleId = roleManager.createTemplatedRole( templateId, resource );
            httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePathBuilder().path("../../..").path(roleId).build(  ).normalize().toString() );
            if (exists)
            {
                httpServletResponse.setStatus( 200 );
            } else {
                httpServletResponse.setStatus( 201 );
            }
            return getRoleInfo( rbacManager.getRoleById( roleId ) );
        } catch (RoleNotFoundException e) {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_NOT_FOUND, templateId, resource ), 404 );
        } catch (RoleExistsException e) {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_INSTANCE_EXISTS, templateId, resource ), 303 );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLEMANAGER_FAIL, e.getMessage( ) ) );
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
    }

    @Override
    public Response removeTemplatedRole( String templateId, String resource )
        throws RedbackServiceException
    {

        try
        {
            roleManager.removeTemplatedRole( templateId, resource );
            return Response.ok( ).build( );
        }
        catch ( PermanentRoleDeletionInvalid e ) {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_DELETION_WITH_PERMANENT_FLAG, RoleModelUtils.getRoleId( templateId, resource ) ), 400 );
        }
        catch ( RoleNotFoundException e ) {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_INSTANCE_NOT_FOUND, templateId, resource ), 404 );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLEMANAGER_FAIL, e.getMessage( ) ) );
        }
    }



    @Override
    public RoleInfo assignRole( String roleId, String userId )
        throws RedbackServiceException
    {
        try
        {
            userManager.findUser( userId );
            roleManager.assignRole( roleId, userId );
            return getRoleInfo( rbacManager.getRoleById( roleId ) );
        }
        catch ( RoleNotFoundException e ) {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_NOT_FOUND, e.getMessage( ) ), 404 );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLEMANAGER_FAIL, e.getMessage( ) ) );
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_FOUND, e.getMessage( ) ), 404 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ) );
        }
        catch ( RbacObjectNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
    }


    @Override
    public RoleInfo assignTemplatedRole( String templateId, String resource, String userId )
        throws RedbackServiceException
    {
        try
        {
            userManager.findUser( userId );
            roleManager.assignTemplatedRole( templateId, resource, userId );
            String roleId = RoleModelUtils.getRoleId( templateId, resource );
            return getRoleInfo( rbacManager.getRoleById( roleId ) );

        }
        catch ( RoleNotFoundException e ) {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_NOT_FOUND, e.getMessage( ) ), 404 );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_FOUND, e.getMessage( ) ), 404 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ) );
        }
        catch ( RbacObjectNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
    }

    @Override
    public RoleInfo deleteRoleAssignment( String roleId, String userId )
        throws RedbackServiceException
    {
        try
        {
            userManager.findUser( userId );
            roleManager.unassignRole( roleId, userId );
            return getRoleInfo( rbacManager.getRoleById( roleId ) );
        }
        catch ( RoleNotFoundException e ) {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_NOT_FOUND, e.getMessage( ) ), 404 );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( e.getMessage() );
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_FOUND, e.getMessage( ) ), 404 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ) );
        }
        catch ( RbacObjectNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_NOT_FOUND, e.getMessage( ) ), 404 );
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
    }

    @Override
    public PagedResult<UserInfo> getRoleUsers( String roleId, String searchTerm, Integer offset, Integer limit, List<String> orderBy, String order )  throws RedbackServiceException
    {
        boolean ascending = isAscending( order );
        try
        {
            org.apache.archiva.redback.rbac.Role rbacRole = rbacManager.getRoleById( roleId );
            List<User> rawUsers = getAssignedRedbackUsersRecursive( rbacRole );
            return getUserInfoPagedResult( rawUsers, searchTerm, offset, limit, orderBy, ascending );
        }
        catch ( RbacObjectNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_NOT_FOUND, e.getMessage( ) ), 404 );
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
    }

    @Override
    public RoleInfo updateRole( String roleId, Role role ) throws RedbackServiceException
    {
        try
        {
            if (role==null) {
                throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_EMPTY_DATA ), 400 );
            }
            if ( !StringUtils.equals( roleId, role.getId( ) ) )
            {
                throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_ID_INVALID ), 422 );
            }
            org.apache.archiva.redback.rbac.Role rbacRole = rbacManager.getRoleById( roleId );
            if (StringUtils.isNotEmpty( role.getName()) && !StringUtils.equals(rbacRole.getName(), role.getName()) ) {
                rbacRole.setName( role.getName( ) );
            }
            if (StringUtils.isNotEmpty( role.getDescription()) && !StringUtils.equals(rbacRole.getDescription(), role.getDescription()) ) {
                rbacRole.setDescription( role.getDescription( ) );
            }
            if (role.isPermanent()!=null && rbacRole.isPermanent()!=role.isPermanent().booleanValue()) {
                rbacRole.setPermanent( role.isPermanent( ) );
            }
            if (role.isAssignable()!=null && rbacRole.isAssignable()!=role.isAssignable().booleanValue()) {
                rbacRole.setAssignable( role.isAssignable( ) );
            }
            if (role.getAssignedUsers()!=null && role.getAssignedUsers().size()>0) {
                role.getAssignedUsers().stream().forEach( user ->
                {
                    try
                    {
                        roleManager.assignRole( role.getId( ), user.getUserId( ) );
                    }
                    catch ( RoleManagerException e )
                    {
                        // silently ignore
                    }
                }
                );
            }
            org.apache.archiva.redback.rbac.Role updatedRole = rbacManager.saveRole( rbacRole );
            return getRoleInfo( updatedRole );
        }
        catch (RbacObjectNotFoundException e) {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLE_NOT_FOUND, roleId ), 404 );
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage() ));
        }
    }


    @Override
    public List<RoleTemplate> getTemplates( ) throws RedbackServiceException
    {
        return roleManager.getModel( ).getApplications( ).stream( ).flatMap( app ->
            app.getTemplates( ).stream( ).map( modelTempl -> RoleTemplate.of( app, modelTempl ) )
        ).collect( Collectors.toList( ) );
    }


}
