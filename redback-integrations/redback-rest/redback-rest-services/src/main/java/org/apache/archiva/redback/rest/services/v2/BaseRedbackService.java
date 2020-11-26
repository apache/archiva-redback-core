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

import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rest.api.MessageKeys;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.model.v2.BaseUserInfo;
import org.apache.archiva.redback.rest.api.model.v2.RoleInfo;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class BaseRedbackService
{
    private static final Logger log = LoggerFactory.getLogger( BaseRedbackService.class );

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
            role.setAssignedUsers( getAssignedUsersRecursive( rbacRole ) );
            return role;
        }
        catch ( RbacManagerException e )
        {
            log.error( "Error while retrieving role information {}", e.getMessage( ), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
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
            role.setAssignedUsers( getAssignedUsersRecursive( rbacRole ) );
            return Optional.of( role );
        }
        catch ( RbacManagerException e )
        {
            log.error( "Error while retrieving role information {}", e.getMessage( ), e );
            return Optional.empty( );
        }
    }
}
