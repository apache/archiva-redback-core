package org.apache.archiva.redback.rbac.cached;

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

import org.apache.archiva.components.cache.Cache;
import org.apache.archiva.redback.rbac.Operation;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RBACManagerListener;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.RbacObjectInvalidException;
import org.apache.archiva.redback.rbac.RbacObjectNotFoundException;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CachedRbacManager is a wrapped RBACManager with caching.
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
@Service( "rbacManager#cached" )
public class CachedRbacManager
    implements RBACManager, RBACManagerListener
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "rbacManager#jpa" )
    private RBACManager rbacImpl;

    @Inject
    @Named( value = "cache#operations" )
    private Cache<String, Operation> operationsCache;

    @Inject
    @Named( value = "cache#permissions" )
    private Cache<String, Permission> permissionsCache;

    @Inject
    @Named( value = "cache#resources" )
    private Cache<String, Resource> resourcesCache;

    @Inject
    @Named( value = "cache#roles" )
    private Cache<String, Role> rolesCache;

    @Inject
    @Named( value = "cache#rolesById" )
    private Cache<String, Role> rolesByIdCache;

    @Inject
    @Named( value = "cache#userAssignments" )
    private Cache<String, UserAssignment> userAssignmentsCache;

    @Inject
    @Named( value = "cache#userPermissions" )
    private Cache<String, Map<String, List<? extends Permission>>> userPermissionsCache;

    @Inject
    @Named( value = "cache#effectiveRoleSet" )
    private Cache<String, Set<? extends Role>> effectiveRoleSetCache;

    @Override
    public void initialize()
    {
        // no op
    }

    @Override
    public void addChildRole( Role role, Role childRole )
        throws RbacObjectInvalidException, RbacManagerException
    {
        try
        {
            this.rbacImpl.addChildRole( role, childRole );
        }
        finally
        {
            invalidateCachedRole( role );
            invalidateCachedRole( childRole );
        }
    }

    @Override
    public void addListener( RBACManagerListener listener )
    {
        this.rbacImpl.addListener( listener );
    }

    @Override
    public Operation createOperation( String name )
        throws RbacManagerException
    {
        operationsCache.remove( name );
        return this.rbacImpl.createOperation( name );
    }

    @Override
    public Permission createPermission( String name )
        throws RbacManagerException
    {
        permissionsCache.remove( name );
        return this.rbacImpl.createPermission( name );
    }

    @Override
    public Permission createPermission( String name, String operationName, String resourceIdentifier )
        throws RbacManagerException
    {
        permissionsCache.remove( name );
        return this.rbacImpl.createPermission( name, operationName, resourceIdentifier );
    }

    @Override
    public Resource createResource( String identifier )
        throws RbacManagerException
    {
        resourcesCache.remove( identifier );
        return this.rbacImpl.createResource( identifier );
    }

    @Override
    public Role createRole( String name )
    {
        if (rolesCache.hasKey( name ))
        {
            Role role = rolesCache.remove( name );
            rolesByIdCache.remove( role.getId( ) );
        }
        return this.rbacImpl.createRole( name );
    }

    @Override
    public Role createRole( String id, String name )
    {
        if (rolesByIdCache.hasKey( id ))
        {
            Role role = rolesByIdCache.remove( id );
            rolesCache.remove( role.getName( ) );
        }
        return this.rbacImpl.createRole( id, name );
    }

    @Override
    public UserAssignment createUserAssignment( String principal )
        throws RbacManagerException
    {
        invalidateCachedUserAssignment( principal );
        return this.rbacImpl.createUserAssignment( principal );
    }

    @Override
    public void eraseDatabase()
    {
        try
        {
            this.rbacImpl.eraseDatabase();
        }
        finally
        {
            // FIXME cleanup
            //EhcacheUtils.clearAllCaches( log() );
        }
    }

    /**
     * @see org.apache.archiva.redback.rbac.RBACManager#getAllAssignableRoles()
     */
    @Override
    public List<? extends Role> getAllAssignableRoles()
        throws RbacManagerException, RbacObjectNotFoundException
    {
        log.debug( "NOT CACHED - .getAllAssignableRoles()" );
        return this.rbacImpl.getAllAssignableRoles();
    }

    @Override
    public List<? extends Operation> getAllOperations()
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getAllOperations()" );
        return this.rbacImpl.getAllOperations();
    }

    @Override
    public List<? extends Permission> getAllPermissions()
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getAllPermissions()" );
        return this.rbacImpl.getAllPermissions();
    }

    @Override
    public List<? extends Resource> getAllResources()
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getAllResources()" );
        return this.rbacImpl.getAllResources();
    }

    @Override
    public List<? extends Role> getAllRoles()
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getAllRoles()" );
        return this.rbacImpl.getAllRoles();
    }

    @Override
    public List<? extends UserAssignment> getAllUserAssignments()
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getAllUserAssignments()" );
        return this.rbacImpl.getAllUserAssignments();
    }

    /**
     * @see org.apache.archiva.redback.rbac.RBACManager#getAssignedPermissionMap(java.lang.String)
     */
    @Override
    @SuppressWarnings( "unchecked" )
    public Map<String, List<? extends Permission>> getAssignedPermissionMap( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Map<String, List<? extends Permission>> el = userPermissionsCache.get( principal );

        if ( el != null )
        {
            log.debug( "using cached user permission map" );
            return el;
        }

        log.debug( "building user permission map" );
        Map<String, List<? extends Permission>> userPermMap = this.rbacImpl.getAssignedPermissionMap( principal );
        userPermissionsCache.put( principal, userPermMap );
        return userPermMap;

    }

    @Override
    public Set<? extends Permission> getAssignedPermissions( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        log.debug( "NOT CACHED - .getAssignedPermissions(String)" );
        return this.rbacImpl.getAssignedPermissions( principal );
    }

    @Override
    public Collection<? extends Role> getAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        log.debug( "NOT CACHED - .getAssignedRoles(String)" );
        return this.rbacImpl.getAssignedRoles( principal );
    }

    @Override
    public Collection<? extends Role> getAssignedRoles( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        log.debug( "NOT CACHED - .getAssignedRoles(UserAssignment)" );
        return this.rbacImpl.getAssignedRoles( userAssignment );
    }

    @Override
    public Map<String, ? extends Role> getChildRoles( Role role )
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getChildRoles(Role)" );
        return this.rbacImpl.getChildRoles( role );
    }

    @Override
    public Map<String, ? extends Role> getParentRoles( Role role )
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getParentRoles(Role)" );
        return this.rbacImpl.getParentRoles( role );
    }

    @Override
    public Collection<? extends Role> getEffectivelyAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        log.debug( "NOT CACHED - .getEffectivelyAssignedRoles(String)" );
        return this.rbacImpl.getEffectivelyAssignedRoles( principal );
    }

    @Override
    public Collection<? extends Role> getEffectivelyUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException
    {
        log.debug( "NOT CACHED - .getEffectivelyUnassignedRoles(String)" );
        return this.rbacImpl.getEffectivelyUnassignedRoles( principal );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Set<? extends Role> getEffectiveRoles( Role role )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Set<? extends Role> el = effectiveRoleSetCache.get( role.getName() );

        if ( el != null )
        {
            log.debug( "using cached effective role set" );
            return el;
        }
        else
        {
            log.debug( "building effective role set" );
            Set<? extends Role> effectiveRoleSet = this.rbacImpl.getEffectiveRoles( role );
            effectiveRoleSetCache.put( role.getName(), effectiveRoleSet );
            return effectiveRoleSet;
        }
    }

    @Override
    public Resource getGlobalResource()
        throws RbacManagerException
    {
        /* this is very light */
        log.debug( "NOT CACHED - .getGlobalResource()" );
        return this.rbacImpl.getGlobalResource();
    }

    @Override
    public Operation getOperation( String operationName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Operation el = operationsCache.get( operationName );
        if ( el != null )
        {
            return el;
        }
        else
        {
            Operation operation = this.rbacImpl.getOperation( operationName );
            operationsCache.put( operationName, operation );
            return operation;
        }
    }

    @Override
    public Permission getPermission( String permissionName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Permission el = permissionsCache.get( permissionName );
        if ( el != null )
        {
            return el;
        }
        else
        {
            Permission permission = this.rbacImpl.getPermission( permissionName );
            permissionsCache.put( permissionName, permission );
            return permission;
        }
    }

    @Override
    public Resource getResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Resource el = resourcesCache.get( resourceIdentifier );
        if ( el != null )
        {
            return el;
        }
        else
        {
            Resource resource = this.rbacImpl.getResource( resourceIdentifier );
            resourcesCache.put( resourceIdentifier, resource );
            return resource;
        }
    }

    @Override
    public Role getRole( String roleName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Role el = rolesCache.get( roleName );
        if ( el != null )
        {
            return el;
        }
        else
        {
            Role role = this.rbacImpl.getRole( roleName );
            rolesCache.put( roleName, role );
            rolesByIdCache.put( role.getId( ), role );
            return role;
        }
    }

    @Override
    public Role getRoleById( String id ) throws RbacObjectNotFoundException, RbacManagerException
    {
        if (rolesByIdCache.hasKey( id )) {
            return rolesByIdCache.get( id );
        } else {
            return this.rbacImpl.getRoleById( id );
        }
    }

    @Override
    public Map<String, ? extends Role> getRoles( Collection<String> roleNames )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        log.debug( "NOT CACHED - .getRoles(Collection)" );
        return this.rbacImpl.getRoles( roleNames );
    }

    @Override
    public Collection<? extends Role> getUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException
    {
        log.debug( "NOT CACHED - .getUnassignedRoles(String)" );
        return this.rbacImpl.getUnassignedRoles( principal );
    }

    @Override
    public UserAssignment getUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        UserAssignment el = userAssignmentsCache.get( principal );
        if ( el != null )
        {
            return el;
        }
        else
        {
            UserAssignment userAssignment = this.rbacImpl.getUserAssignment( principal );
            userAssignmentsCache.put( principal, userAssignment );
            return userAssignment;
        }
    }

    @Override
    public List<? extends UserAssignment> getUserAssignmentsForRoles( Collection<String> roleNames )
        throws RbacManagerException
    {
        log.debug( "NOT CACHED - .getUserAssignmentsForRoles(Collection)" );
        return this.rbacImpl.getUserAssignmentsForRoles( roleNames );
    }

    @Override
    public boolean operationExists( Operation operation )
    {
        if ( operation == null )
        {
            return false;
        }

        if ( operationsCache.hasKey( operation.getName() ) )
        {
            return true;
        }

        return this.rbacImpl.operationExists( operation );
    }

    @Override
    public boolean operationExists( String name )
    {
        if ( operationsCache.hasKey( name ) )
        {
            return true;
        }

        return this.rbacImpl.operationExists( name );
    }

    @Override
    public boolean permissionExists( Permission permission )
    {
        if ( permission == null )
        {
            return false;
        }

        if ( permissionsCache.hasKey( permission.getName() ) )
        {
            return true;
        }

        return this.rbacImpl.permissionExists( permission );
    }

    @Override
    public boolean permissionExists( String name )
    {
        if ( permissionsCache.hasKey( name ) )
        {
            return true;
        }

        return this.rbacImpl.permissionExists( name );
    }

    @Override
    public void rbacInit( boolean freshdb )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacInit( freshdb );
        }
        // lookup all Cache and clear all ?
        this.resourcesCache.clear();
        this.operationsCache.clear();
        this.permissionsCache.clear();
        this.rolesCache.clear();
        this.rolesByIdCache.clear();
        this.userAssignmentsCache.clear();
        this.userPermissionsCache.clear();
    }

    @Override
    public void rbacPermissionRemoved( Permission permission )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacPermissionRemoved( permission );
        }

        invalidateCachedPermission( permission );
    }

    @Override
    public void rbacPermissionSaved( Permission permission )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacPermissionSaved( permission );
        }

        invalidateCachedPermission( permission );
    }

    @Override
    public void rbacRoleRemoved( Role role )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacRoleRemoved( role );
        }

        invalidateCachedRole( role );
    }

    @Override
    public void rbacRoleSaved( Role role )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacRoleSaved( role );
        }

        invalidateCachedRole( role );
    }

    @Override
    public void rbacUserAssignmentRemoved( UserAssignment userAssignment )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacUserAssignmentRemoved( userAssignment );
        }

        invalidateCachedUserAssignment( userAssignment );
    }

    @Override
    public void rbacUserAssignmentSaved( UserAssignment userAssignment )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacUserAssignmentSaved( userAssignment );
        }

        invalidateCachedUserAssignment( userAssignment );
    }

    @Override
    public void removeListener( RBACManagerListener listener )
    {
        this.rbacImpl.removeListener( listener );
    }

    @Override
    public void removeOperation( Operation operation )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedOperation( operation );
        this.rbacImpl.removeOperation( operation );
    }

    @Override
    public void removeOperation( String operationName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        operationsCache.remove( operationName );
        this.rbacImpl.removeOperation( operationName );
    }

    @Override
    public void removePermission( Permission permission )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedPermission( permission );
        this.rbacImpl.removePermission( permission );
    }

    @Override
    public void removePermission( String permissionName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        permissionsCache.remove( permissionName );
        this.rbacImpl.removePermission( permissionName );
    }

    @Override
    public void removeResource( Resource resource )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedResource( resource );
        this.rbacImpl.removeResource( resource );
    }

    @Override
    public void removeResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        resourcesCache.remove( resourceIdentifier );
        this.rbacImpl.removeResource( resourceIdentifier );
    }

    @Override
    public void removeRole( Role role )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedRole( role );
        this.rbacImpl.removeRole( role );
    }

    @Override
    public void removeRole( String roleName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        Role role = rolesCache.remove( roleName );
        if (role!=null) {
            rolesByIdCache.remove( role.getId( ) );
        }
        this.rbacImpl.removeRole( roleName );
    }

    @Override
    public void removeRoleById( String id ) throws RbacObjectNotFoundException, RbacManagerException
    {
        Role role = rolesByIdCache.remove( id );
        if (role!=null) {
            rolesCache.remove( role.getName( ) );
        }
        this.rbacImpl.removeRoleById( id );
    }

    @Override
    public void removeUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedUserAssignment( principal );
        this.rbacImpl.removeUserAssignment( principal );
    }

    @Override
    public void removeUserAssignment( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedUserAssignment( userAssignment );
        this.rbacImpl.removeUserAssignment( userAssignment );
    }

    @Override
    public boolean resourceExists( Resource resource )
    {
        if ( resourcesCache.hasKey( resource.getIdentifier() ) )
        {
            return true;
        }

        return this.rbacImpl.resourceExists( resource );
    }

    @Override
    public boolean resourceExists( String identifier )
    {
        if ( resourcesCache.hasKey( identifier ) )
        {
            return true;
        }

        return this.rbacImpl.resourceExists( identifier );
    }

    @Override
    public boolean roleExists( Role role )
        throws RbacManagerException
    {
        if ( rolesByIdCache.hasKey( role.getId() ) )
        {
            return true;
        }

        return this.rbacImpl.roleExists( role );
    }

    @Override
    public boolean roleExists( String name )
        throws RbacManagerException
    {
        if ( rolesCache.hasKey( name ) )
        {
            return true;
        }

        return this.rbacImpl.roleExists( name );
    }

    @Override
    public boolean roleExistsById( String id ) throws RbacManagerException
    {
        if (rolesByIdCache.hasKey( id )) {
            return true;
        } else {
            return this.rbacImpl.roleExistsById( id );
        }
    }

    @Override
    public Operation saveOperation( Operation operation )
        throws RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedOperation( operation );
        return this.rbacImpl.saveOperation( operation );
    }

    @Override
    public Permission savePermission( Permission permission )
        throws RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedPermission( permission );
        return this.rbacImpl.savePermission( permission );
    }

    @Override
    public Resource saveResource( Resource resource )
        throws RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedResource( resource );
        return this.rbacImpl.saveResource( resource );
    }

    @Override
    public synchronized Role saveRole( Role role )
        throws RbacObjectInvalidException, RbacManagerException
    {
        /*
        List assignments = this.rbacImpl.getUserAssignmentsForRoles( Collections.singletonList( role.getName() ) );

        for ( Iterator i = assignments.iterator(); i.hasNext();  )
        {
            log.debug( "invalidating user assignment with role " + role.getName() );
            invalidateCachedUserAssignment( (UserAssignment)i.next() );
        }
        */

        /*
        the above commented out section would try and invalidate just that user caches that are effected by
        changes in the users permissions map due to role changes.

        however the implementations of those do not take into account child role hierarchies so wipe all
        user caches on role saving...which is a heavy handed way to solve the problem, but not going to
        happen frequently for current applications so not a huge deal.
         */
        invalidateAllCachedUserAssignments();
        invalidateCachedRole( role );
        return this.rbacImpl.saveRole( role );
    }

    @Override
    public synchronized void saveRoles( Collection<Role> roles )
        throws RbacObjectInvalidException, RbacManagerException
    {

        for ( Role role : roles )
        {
            invalidateCachedRole( role );
        }

        /*
        List assignments = this.rbacImpl.getUserAssignmentsForRoles( roles );

        for ( Iterator i = assignments.iterator(); i.hasNext();  )
        {
            log.debug( "invalidating user assignment with roles" );
            invalidateCachedUserAssignment( (UserAssignment)i.next() );
        }
        */
        invalidateAllCachedUserAssignments();
        this.rbacImpl.saveRoles( roles );
    }

    @Override
    public UserAssignment saveUserAssignment( UserAssignment userAssignment )
        throws RbacObjectInvalidException, RbacManagerException
    {
        invalidateCachedUserAssignment( userAssignment );
        return this.rbacImpl.saveUserAssignment( userAssignment );
    }

    @Override
    public boolean userAssignmentExists( String principal )
    {
        if ( userAssignmentsCache.hasKey( principal ) )
        {
            return true;
        }

        return this.rbacImpl.userAssignmentExists( principal );
    }

    @Override
    public boolean userAssignmentExists( UserAssignment assignment )
    {
        if ( userAssignmentsCache.hasKey( assignment.getPrincipal() ) )
        {
            return true;
        }

        return this.rbacImpl.userAssignmentExists( assignment );
    }

    private void invalidateCachedRole( Role role )
    {
        if ( role != null )
        {
            rolesCache.remove( role.getName() );
            rolesByIdCache.remove( role.getId( ) );
            // if a role changes we need to invalidate the entire effective role set cache
            // since we have no concept of the heirarchy involved in the role sets
            effectiveRoleSetCache.clear();
        }

    }

    private void invalidateCachedOperation( Operation operation )
    {
        if ( operation != null )
        {
            operationsCache.remove( operation.getName() );
        }
    }

    private void invalidateCachedPermission( Permission permission )
    {
        if ( permission != null )
        {
            permissionsCache.remove( permission.getName() );
        }
    }

    private void invalidateCachedResource( Resource resource )
    {
        if ( resource != null )
        {
            resourcesCache.remove( resource.getIdentifier() );
        }
    }

    private void invalidateCachedUserAssignment( UserAssignment userAssignment )
    {
        if ( userAssignment != null )
        {
            userAssignmentsCache.remove( userAssignment.getPrincipal() );
            userPermissionsCache.remove( userAssignment.getPrincipal() );
        }
    }

    private void invalidateCachedUserAssignment( String principal )
    {
        userAssignmentsCache.remove( principal );
        userPermissionsCache.remove( principal );
    }

    private void invalidateAllCachedUserAssignments()
    {
        userAssignmentsCache.clear();
        userPermissionsCache.clear();
    }

    public Cache<String, ? extends Operation> getOperationsCache()
    {
        return operationsCache;
    }

    @SuppressWarnings( "unchecked" )
    public void setOperationsCache( Cache<String, ? extends Operation> operationsCache )
    {
        this.operationsCache = (Cache<String, Operation>) operationsCache;
    }

    public Cache<String, ? extends Permission> getPermissionsCache()
    {
        return permissionsCache;
    }

    @SuppressWarnings( "unchecked" )
    public void setPermissionsCache( Cache<String, ? extends Permission> permissionsCache )
    {
        this.permissionsCache = (Cache<String, Permission>) permissionsCache;
    }

    public Cache<String, ? extends Resource> getResourcesCache()
    {
        return resourcesCache;
    }

    @SuppressWarnings( "unchecked" )
    public void setResourcesCache( Cache<String, ? extends Resource> resourcesCache )
    {
        this.resourcesCache = (Cache<String, Resource>) resourcesCache;
    }

    public Cache<String, ? extends Role> getRolesCache()
    {
        return rolesCache;
    }


    @SuppressWarnings( "unchecked" )
    public void setRolesCache( Cache<String, ? extends Role> rolesCache )
    {
        this.rolesCache = (Cache<String, Role>) rolesCache;
    }

    public Cache<String, ? extends Role> getRolesByIdCache( )
    {
        return rolesByIdCache;
    }

    public void setRolesByIdCache( Cache<String, ? extends Role> rolesByIdCache )
    {
        this.rolesByIdCache = (Cache<String, Role>) rolesByIdCache;
    }

    public Cache<String, ? extends UserAssignment> getUserAssignmentsCache()
    {
        return userAssignmentsCache;
    }

    @SuppressWarnings( "unchecked" )
    public void setUserAssignmentsCache( Cache<String, ? extends UserAssignment> userAssignmentsCache )
    {
        this.userAssignmentsCache = (Cache<String, UserAssignment>) userAssignmentsCache;
    }

    public Cache<String, Map<String, List<? extends Permission>>> getUserPermissionsCache()
    {
        return userPermissionsCache;
    }

    public void setUserPermissionsCache( Cache<String, Map<String, List<? extends Permission>>> userPermissionsCache )
    {
        this.userPermissionsCache = userPermissionsCache;
    }

    public Cache<String, Set<? extends Role>> getEffectiveRoleSetCache()
    {
        return effectiveRoleSetCache;
    }

    public void setEffectiveRoleSetCache( Cache<String, Set<? extends Role>> effectiveRoleSetCache )
    {
        this.effectiveRoleSetCache = effectiveRoleSetCache;
    }

    public RBACManager getRbacImpl()
    {
        return rbacImpl;
    }

    public void setRbacImpl( RBACManager rbacImpl )
    {
        this.rbacImpl = rbacImpl;
    }


    @Override
    public boolean isFinalImplementation()
    {
        return false;
    }

    @Override
    public String getDescriptionKey()
    {
        return "archiva.redback.rbacmanager.cached";
    }

    @Override
    public boolean isReadOnly()
    {
        return false;
    }
}
