package org.apache.archiva.redback.rbac;

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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the roles, permissions and operations of the RBAC system.
 *
 * @author Jesse McConnell
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @author Martin Stockhammer <martin_s@apache.org>
 *
 */
public interface RBACManager
{

    void addListener( RBACManagerListener listener );

    void removeListener( RBACManagerListener listener );


    // ------------------------------------------------------------------
    // Role Methods
    // ------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link Role}, or return an existing {@link Role}, depending
     * on the provided <code>name</code> parameter.
     *
     * Note: Be sure to use {@link #saveRole(Role)} in order to persist any changes to the Role.
     *
     * @param name the name.
     * @return the new {@link Role} object.
     */
    Role createRole( String name );

    /**
     * Creates a new role with the given id and role name.
     * @param id the role identifier, which must be unique
     * @param name the role name, which must be unique
     * @return the new role instance
     */
    Role createRole(String id, String name);

    /**
     * Tests for the existence of a Role.
     *
     * @return true if role exists in store.
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    boolean roleExists( String name )
        throws RbacManagerException;

    /**
     * Returns <code>true</code>, if a role with the given id exists.
     * @param id the role id
     * @return <code>true</code>, if the role with the given id exists, otherwise <code>false</code>
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    boolean roleExistsById( String id ) throws RbacManagerException;

    /**
     * Returns true, if the given role exists.
     *
     * @param role the role to check
     * @return <code>true</code>, if the role exists, otherwise <code>false</code>
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    boolean roleExists( Role role )
        throws RbacManagerException;

    /**
     * Persists the given role to the backend datastore.
     *
     * @param role the role to save
     * @return the persisted role, if the method was successful
     * @throws RbacObjectInvalidException if the given role object was not valid
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Role saveRole( Role role )
        throws RbacObjectInvalidException, RbacManagerException;

    /**
     * Persists all of the given roles to the backend datastore.
     * Implementations should try to save all role instances and throw exceptions afterwards.
     *
     * @param roles the list of roles to save
     * @throws RbacObjectInvalidException if one of the given role objects was not valid
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    void saveRoles( Collection<Role> roles )
        throws RbacObjectInvalidException, RbacManagerException;

    /**
     * Returns the role identified by the given name
     *
     * @param roleName the role name
     * @return the role instance, if a role by this name was found
     * @throws RbacObjectNotFoundException if not role was found with the given name
     * @throws RbacManagerException if the access to the underlying datastore failed
     */
    Role getRole( String roleName )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Returns the role identified by the given ID
     * @param id the role id
     * @return the role object, if the role with the given id exists
     * @throws RbacObjectNotFoundException if no role was found with the given id
     * @throws RbacManagerException if the access to the underlying datastore failed
     */
    Role getRoleById( String id ) throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Returns the role instances for the given role names.
     *
     * @param roleNames the list of role names.
     * @return a map of (name,role) pairs
     * @throws RbacObjectNotFoundException if one of the given roles was not found
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Map<String, ? extends Role> getRoles( Collection<String> roleNames )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Adds a child to a role.
     *
     * @param role the parent role
     * @param childRole the child role, that is added to the parent role
     * @throws RbacObjectInvalidException if one of the role objects was not valid
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    void addChildRole( Role role, Role childRole )
        throws RbacObjectInvalidException, RbacManagerException;

    /**
     * Returns all the child roles of a given role as (name, role) pairs.
     * @param role the parent role
     * @return the list of child roles
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Map<String, ? extends Role> getChildRoleNames( Role role )
        throws RbacManagerException;

    /**
     * Returns all the child roles of a given role as (role id, role) pairs.
     * @param role the parent role
     * @return the map of child roles as (role id, role) pairs
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Map<String, ? extends Role> getChildRoleIds( Role role )
        throws RbacManagerException;

    /**
     * Returns all the parent roles of a given role as map of (name, role)  elements.
     * @param role the role to check for parent roles
     * @return the list of parent roles that have <code>role</code> als child
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Map<String, ? extends Role> getParentRoleNames( Role role )
        throws RbacManagerException;

    /**
     * Returns all the parent roles of a given role as map of (id, role) elements.
     * @param role the role to check for parents roles
     * @return a map of (role id, role) pairs that have <code>role</code> as child
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Map<String, ? extends Role> getParentRoleIds( Role role )
        throws RbacManagerException;

    /**
     * Returns all roles defined in the datastore.
     *
     * @return the list of roles defined in the datastore
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    List<? extends Role> getAllRoles()
        throws RbacManagerException;

    /**
     * Returns all effective roles. Which means a list with the current role and all child roles recursively.
     *
     * @param role the role to use as starting point
     * @return the set of roles that are found as children of the given role
     * @throws RbacObjectNotFoundException if the given role was not found
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Set<? extends Role> getEffectiveRoles( Role role )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Removes the given role from the datastore.
     *
     * @param role the role to remove
     * @throws RbacManagerException if the access to the backend datastore failed
     * @throws RbacObjectNotFoundException if the given role was not found
     * @throws RbacObjectInvalidException if the given role has invalid data
     */
    void removeRole( Role role )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    /**
     * Removes the role with the given name from the datastore.
     *
     * @param roleName the role name
     * @throws RbacObjectNotFoundException if the role with the given name was not found
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    void removeRole( String roleName )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Removes the role with the given id from the datastore.
     *
     * @param id the role id
     * @throws RbacObjectNotFoundException if no role with the given id was found
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    void removeRoleById( String id ) throws RbacObjectNotFoundException, RbacManagerException;

    // ------------------------------------------------------------------
    // Permission Methods
    // ------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link Permission}, or return an existing {@link Permission}, depending
     * on the provided <code>name</code> parameter.
     *
     * Note: Be sure to use {@link #savePermission(Permission)} in order to persist any changes to the Role.
     *
     * @param name the name.
     * @return the new Permission.
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Permission createPermission( String name )
        throws RbacManagerException;

    /**
     * Creates an implementation specific {@link Permission} with specified {@link Operation},
     * and {@link Resource} identifiers.
     *
     * Note: Be sure to use {@link #savePermission(Permission)} in order to persist any changes to the Role.
     *
     * @param name               the name.
     * @param operationName      the {@link Operation#setName(String)} value
     * @param resourceIdentifier the {@link Resource#setIdentifier(String)} value
     * @return the new Permission.
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Permission createPermission( String name, String operationName, String resourceIdentifier )
        throws RbacManagerException;

    /**
     * Tests for the existence of a permission.
     *
     * @param name the name to test for.
     * @return true if permission exists.
     */
    boolean permissionExists( String name );

    boolean permissionExists( Permission permission );

    @SuppressWarnings( "DuplicateThrows" )
    Permission savePermission( Permission permission )
        throws RbacObjectInvalidException, RbacManagerException;

    @SuppressWarnings( "DuplicateThrows" )
    Permission getPermission( String permissionName )
        throws RbacObjectNotFoundException, RbacManagerException;

    List<? extends Permission> getAllPermissions()
        throws RbacManagerException;

    @SuppressWarnings( "DuplicateThrows" )
    void removePermission( Permission permission )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    @SuppressWarnings( "DuplicateThrows" )
    void removePermission( String permissionName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    // ------------------------------------------------------------------
    // Operation Methods
    // ------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link Operation}, or return an existing {@link Operation}, depending
     * on the provided <code>name</code> parameter.
     *
     * Note: Be sure to use {@link #saveOperation(Operation)} in order to persist any changes to the Role.
     *
     * @param name the name.
     * @return the new Operation.
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Operation createOperation( String name )
        throws RbacManagerException;

    boolean operationExists( String name );

    boolean operationExists( Operation operation );

    /**
     * Save the new or existing operation to the store.
     *
     * @param operation the operation to save (new or existing)
     * @return the Operation that was saved.
     * @throws RbacObjectInvalidException if the object is not valid and cannot be saved
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Operation saveOperation( Operation operation )
        throws RbacObjectInvalidException, RbacManagerException;

    @SuppressWarnings( "DuplicateThrows" )
    Operation getOperation( String operationName )
        throws RbacObjectNotFoundException, RbacManagerException;

    List<? extends Operation> getAllOperations()
        throws RbacManagerException;

    @SuppressWarnings( "DuplicateThrows" )
    void removeOperation( Operation operation )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    @SuppressWarnings( "DuplicateThrows" )
    void removeOperation( String operationName )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    // ------------------------------------------------------------------
    // Resource Methods
    // ------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link Resource}, or return an existing {@link Resource}, depending
     * on the provided <code>identifier</code> parameter.
     *
     * Note: Be sure to use {@link #saveResource(Resource)} in order to persist any changes to the Role.
     *
     * @param identifier the identifier.
     * @return the new Resource.
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Resource createResource( String identifier )
        throws RbacManagerException;

    boolean resourceExists( String identifier );

    boolean resourceExists( Resource resource );

    @SuppressWarnings( "DuplicateThrows" )
    Resource saveResource( Resource resource )
        throws RbacObjectInvalidException, RbacManagerException;

    @SuppressWarnings( "DuplicateThrows" )
    Resource getResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacManagerException;

    List<? extends Resource> getAllResources()
        throws RbacManagerException;

    @SuppressWarnings( "DuplicateThrows" )
    void removeResource( Resource resource )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    @SuppressWarnings( "DuplicateThrows" )
    void removeResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    // ------------------------------------------------------------------
    // UserAssignment Methods
    // ------------------------------------------------------------------

    /**
     * Creates an implementation specific {@link UserAssignment}, or return an existing {@link UserAssignment},
     * depending on the provided <code>identifier</code> parameter.
     *
     * Note: Be sure to use {@link #saveUserAssignment(UserAssignment)} in order to persist any changes to the Role.
     *
     * @param principal the principal reference to the user.
     * @return the new UserAssignment object.
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    UserAssignment createUserAssignment( String principal )
        throws RbacManagerException;

    boolean userAssignmentExists( String principal );

    boolean userAssignmentExists( UserAssignment assignment );

    /**
     * Method saveUserAssignment
     *
     * @param userAssignment the user assignment instance to save
     * @throws RbacObjectInvalidException if the instance has invalid data and cannot be saved
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    UserAssignment saveUserAssignment( UserAssignment userAssignment )
        throws RbacObjectInvalidException, RbacManagerException;

    @SuppressWarnings( "DuplicateThrows" )
    UserAssignment getUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Returns all user assignments defined
     * @return list of assignments
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    List<? extends UserAssignment> getAllUserAssignments()
        throws RbacManagerException;

    /**
     * Returns the assignments for the given roles
     * @param roleNames collection of role names
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    List<? extends UserAssignment> getUserAssignmentsForRoles( Collection<String> roleNames )
        throws RbacManagerException;

    /**
     * Method removeAssignment
     *
     * @param userAssignment the assignment to remove
     * @throws RbacObjectNotFoundException if the assignment was not found
     * @throws RbacObjectInvalidException if the provided assignment instance has invalid data
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    void removeUserAssignment( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    /**
     * Method removeAssignment
     *
     * @param principal the principal for which the assignment should be removed
     * @throws RbacObjectNotFoundException if the user with the given principal name was not found
     * @throws RbacObjectInvalidException if the principal string was invalid
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    void removeUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException;

    // ------------------------------------------------------------------
    // UserAssignment Utility Methods
    // ------------------------------------------------------------------

    /**
     * Returns the active roles for a given principal
     *
     * NOTE: roles that are returned might have parent roles themselves, if
     * you just want all permissions then use {@link #getAssignedPermissions(String principal)}
     *
     * @param principal the user principal to search for assignments
     * @return Collection of {@link Role} objects.
     * @throws RbacObjectNotFoundException if the user with the given principal name was not found
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Collection<? extends Role> getAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Get the Collection of {@link Role} objects for this UserAssignment.
     *
     * @param userAssignment the user assignment instance
     * @return Collection of {@link Role} objects for the provided UserAssignment.
     * @throws RbacObjectNotFoundException if the assignment could not be found
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Collection<? extends Role> getAssignedRoles( UserAssignment userAssignment )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Get a list of all assignable roles that are currently not effectively assigned to the specific user,
     * meaning, not a child of any already granted role
     *
     * @param principal the user principal
     * @return the list of roles, that are currently not assigned to the user, or a empty list, if no such role was found.
     * @throws RbacManagerException if the access to the backend datastore failed
     * @throws RbacObjectNotFoundException if the user with the given principal was not found
     */
    Collection<? extends Role> getEffectivelyUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException;

    /**
     * Get a list of the effectively assigned roles to the specified user, this includes child roles
     *
     * @param principal the user principal
     * @return the list of roles effectively assigned to the given user
     * @throws RbacObjectNotFoundException if the user with the given principal was not found
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Collection<? extends Role> getEffectivelyAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Get a list of all assignable roles that are currently not assigned to the specific user.
     *
     * @param principal the user principal name
     * @return the list of roles not assigned to the given user
     * @throws RbacManagerException if the access to the backend datastore failed
     * @throws RbacObjectNotFoundException if the user with the given principal was not found
     */
    Collection<? extends Role> getUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException;

    /**
     * Returns a set of all permissions that are in all active roles for a given
     * principal. This includes permissions from all assigned parent roles.
     *
     * @param principal the user principal name
     * @return the list of all permissions assigned to the user
     * @throws RbacObjectNotFoundException if the user with the given principal name was not found
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Set<? extends Permission> getAssignedPermissions( String principal )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * returns a map of assigned permissions keyed off of operation with a list value of Permissions
     *
     * @param principal the user principal name
     * @return the map of (operation,permission list) pairs
     * @throws RbacObjectNotFoundException if the user with the given principal was not found
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Map<String, List<? extends Permission>> getAssignedPermissionMap( String principal )
        throws RbacObjectNotFoundException, RbacManagerException;

    /**
     * Returns a list of all assignable roles
     *
     * @return list of assignable roles
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    List<? extends Role> getAllAssignableRoles()
        throws RbacManagerException;

    /**
     * Returns the global resource object
     *
     * @return the global resource object
     * @throws RbacManagerException if the access to the backend datastore failed
     */
    Resource getGlobalResource()
        throws RbacManagerException;

    void eraseDatabase();

    /**
     * consumer of user manager can use it to reload various configuration
     * with the configurable implementation is possible to change dynamically the real implementation used.
     *
     * @since 2.1
     */
    void initialize();

    /**
     * @return true if this implementation is a final one and not a wrapper (configurable, cached)
     * @since 2.1
     */
    boolean isFinalImplementation();

    /**
     * @return a key to be able to customize label in UI
     * @since 2.1
     */
    String getDescriptionKey();

    /**
     * Is the RBACManager read only?  if so then create and modify actions are to be disabled
     *
     * @return boolean true if user manager is read only
     */
    boolean isReadOnly();
}