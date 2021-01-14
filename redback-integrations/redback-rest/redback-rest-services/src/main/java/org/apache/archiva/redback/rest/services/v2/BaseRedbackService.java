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

import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.components.rest.util.QueryHelper;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rest.api.MessageKeys;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.model.v2.BaseUserInfo;
import org.apache.archiva.redback.rest.api.model.v2.RoleInfo;
import org.apache.archiva.redback.rest.api.model.v2.UserInfo;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class BaseRedbackService
{
    protected static final String[] DEFAULT_SEARCH_FIELDS = {"user_id", "full_name", "email"};
    protected static final Map<String, BiPredicate<String, User>> USER_FILTER_MAP = new HashMap<>( );
    protected static final Map<String, Comparator<User>> USER_ORDER_MAP = new HashMap<>( );
    protected static final QueryHelper<User> USER_QUERY_HELPER;
    private static final Logger log = LoggerFactory.getLogger( BaseRedbackService.class );


    static
    {
        // The simple Comparator.comparing(attribute) is not null safe
        // As there are attributes that may have a null value, we have to use a comparator with nullsLast(naturalOrder)
        // and the wrapping Comparator.nullsLast(Comparator.comparing(attribute)) does not work, because the attribute is not checked by the nullsLast-Comparator
        USER_ORDER_MAP.put( "id", Comparator.comparing( org.apache.archiva.redback.users.User::getId, Comparator.nullsLast( Comparator.naturalOrder( ) ) ) );
        USER_ORDER_MAP.put( "user_id", Comparator.comparing( org.apache.archiva.redback.users.User::getUsername, Comparator.nullsLast( Comparator.naturalOrder( ) ) ) );
        USER_ORDER_MAP.put( "full_name", Comparator.comparing( org.apache.archiva.redback.users.User::getFullName, Comparator.nullsLast( Comparator.naturalOrder( ) ) ) );
        USER_ORDER_MAP.put( "email", Comparator.comparing( org.apache.archiva.redback.users.User::getEmail, Comparator.nullsLast( Comparator.naturalOrder( ) ) ) );
        USER_ORDER_MAP.put( "created", Comparator.comparing( org.apache.archiva.redback.users.User::getAccountCreationDate, Comparator.nullsLast( Comparator.naturalOrder( ) ) ) );
        USER_ORDER_MAP.put( "last_login", Comparator.comparing( org.apache.archiva.redback.users.User::getLastLoginDate, Comparator.nullsLast( Comparator.naturalOrder( ) ) ) );
        USER_ORDER_MAP.put( "validated", Comparator.comparing( org.apache.archiva.redback.users.User::isValidated, Comparator.nullsLast( Comparator.naturalOrder( ) ) ) );
        USER_ORDER_MAP.put( "locked", Comparator.comparing( org.apache.archiva.redback.users.User::isLocked, Comparator.nullsLast( Comparator.naturalOrder( ) ) ) );
        USER_ORDER_MAP.put( "password_change_required", Comparator.comparing( org.apache.archiva.redback.users.User::isPasswordChangeRequired, Comparator.nullsLast( Comparator.naturalOrder( ) ) ) );
        USER_ORDER_MAP.put( "last_password_change", Comparator.comparing( org.apache.archiva.redback.users.User::getLastPasswordChange, Comparator.nullsLast( Comparator.naturalOrder( ) ) ) );

        USER_FILTER_MAP.put( "user_id", ( String q, org.apache.archiva.redback.users.User u ) -> StringUtils.containsIgnoreCase( u.getUsername( ), q ) );
        USER_FILTER_MAP.put( "full_name", ( String q, org.apache.archiva.redback.users.User u ) -> StringUtils.containsIgnoreCase( u.getFullName( ), q ) );
        USER_FILTER_MAP.put( "email", ( String q, org.apache.archiva.redback.users.User u ) -> StringUtils.containsIgnoreCase( u.getEmail( ), q ) );

        USER_QUERY_HELPER = new QueryHelper<>( USER_FILTER_MAP, USER_ORDER_MAP, DEFAULT_SEARCH_FIELDS );
    }

    protected RBACManager rbacManager;
    protected UserManager userManager;

    public BaseRedbackService( @Named( value = "rbacManager#default" ) RBACManager rbacManager, @Named( value = "userManager#default" ) UserManager userManager )
    {
        this.rbacManager = rbacManager;
        this.userManager = userManager;
    }

    protected RoleInfo getRoleInfo( org.apache.archiva.redback.rbac.Role rbacRole ) throws RedbackServiceException
    {
        try
        {
            RoleInfo role = RoleInfo.of( rbacRole );
            role.setParentRoleIds( getParentRoles( rbacRole ) );
            role.setChildRoleIds( getChildRoles( rbacRole ) );
            return role;
        }
        catch ( RbacManagerException e )
        {
            log.error( "Error while retrieving role information {}", e.getMessage( ), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
    }

    protected boolean isAscending(String order) {
        return !"desc".equals( order );
    }

    protected List<String> getParentRoles( org.apache.archiva.redback.rbac.Role rbacRole ) throws RbacManagerException
    {
        return new ArrayList<>( rbacManager.getParentRoleIds( rbacRole ).keySet( ));
    }

    protected List<String> getChildRoles( Role rbacRole) throws RbacManagerException
    {
        return new ArrayList<>( rbacManager.getChildRoleIds( rbacRole ).keySet( ) );
    }

    protected List<BaseUserInfo> getAssignedUsersRecursive( org.apache.archiva.redback.rbac.Role rbacRole ) throws RbacManagerException
    {
        try
        {
            return rbacManager.getUserAssignmentsForRoles( recurseRoles( rbacRole ).map( role -> role.getId( ) ).collect( Collectors.toList( ) ) )
                .stream( ).map( assignment -> getUserInfo( assignment.getPrincipal( ) ) ).collect( Collectors.toList( ) );
        }
        catch ( RuntimeException e )
        {
            log.error( "Could not recurse roles for assignments {}", e.getMessage( ) );
            throw new RbacManagerException( e.getCause( ) );
        }
    }

    protected List<User> getAssignedRedbackUsers( Role rbacRole ) {
        try
        {
            return rbacManager.getUserAssignmentsForRoles( Arrays.asList( rbacRole.getId( ) ) ).stream( ).map(
                assignment -> getRedbackUser( assignment.getPrincipal( ) )
            ).collect( Collectors.toList( ) );
        }
        catch ( RbacManagerException e )
        {
            throw new RuntimeException( e );
        }
    }

    protected List<User> getAssignedRedbackUsersRecursive( final Role rbacRole, final boolean parentsOnly ) throws RbacManagerException
    {
        try
        {
            List<String> roles = recurseRoles( rbacRole ).map( role -> role.getId( ) ).filter( roleId -> ( ( !parentsOnly ) || ( !rbacRole.getId( ).equals( roleId ) ) ) ).collect( Collectors.toList( ) );
            if (roles.size()==0) {
                return Collections.emptyList( );
            }
            return rbacManager.getUserAssignmentsForRoles( roles )
                .stream( ).map( assignment -> getRedbackUser( assignment.getPrincipal( ) ) ).collect( Collectors.toList( ) );
        }
        catch ( RuntimeException e )
        {
            log.error( "Could not recurse roles for assignments {}", e.getMessage( ) );
            throw new RbacManagerException( e.getCause( ) );
        }
    }

    protected User getRedbackUser(String userId) throws RuntimeException {
        try
        {
            return userManager.findUser( userId, true );
        }
        catch ( UserManagerException e )
        {
            throw new RuntimeException( e );
        }
    }



    private Stream<Role> recurseRoles( Role startRole )
    {
        return Stream.concat( Stream.of( startRole ), getParentRoleStream( startRole ).flatMap( this::recurseRoles ) ).distinct( );
    }

    private Stream<? extends Role> getParentRoleStream( Role role )
    {
        try
        {
            return rbacManager.getParentRoleNames( role ).values( ).stream( );
        }
        catch ( RbacManagerException e )
        {
            throw new RuntimeException( e );
        }
    }

    BaseUserInfo getUserInfo( String userId )
    {
        try
        {
            User user = userManager.findUser( userId );
            return new BaseUserInfo( user.getId( ), user.getUsername( ) );
        }
        catch ( UserManagerException e )
        {
            throw new RuntimeException( e );
        }
    }

    protected Optional<RoleInfo> getRoleInfoOptional( Role rbacRole )
    {
        try
        {
            RoleInfo role = RoleInfo.of( rbacRole );
            role.setParentRoleIds( getParentRoles( rbacRole ) );
            role.setChildRoleIds( getChildRoles( rbacRole ) );
            return Optional.of( role );
        }
        catch ( RbacManagerException e )
        {
            log.error( "Error while retrieving role information {}", e.getMessage( ), e );
            return Optional.empty( );
        }
    }

    protected UserInfo getRestUser( User user )
    {
        if ( user == null )
        {
            return null;
        }
        return new UserInfo( user );
    }

    protected PagedResult<UserInfo> getUserInfoPagedResult( List<? extends User> rawUsers, String q, Integer offset, Integer limit, List<String> orderBy, boolean ascending)
    {
        Predicate<User> filter = USER_QUERY_HELPER.getQueryFilter( q );
        long size = rawUsers.stream( ).filter( filter ).count( );
        List<UserInfo> users = rawUsers.stream( )
            .filter( filter )
            .sorted( USER_QUERY_HELPER.getComparator( orderBy, ascending ) ).skip( offset ).limit( limit )
            .map( user -> getRestUser( user ) )
            .collect( Collectors.toList( ) );
        return new PagedResult<>( (int) size, offset, limit, users );
    }
}
