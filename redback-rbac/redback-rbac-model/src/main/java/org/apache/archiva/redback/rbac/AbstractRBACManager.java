package org.apache.archiva.redback.rbac;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AbstractRBACManager
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
public abstract class AbstractRBACManager
    implements RBACManager
{
    protected Logger log = LoggerFactory.getLogger( getClass() );

    private List<RBACManagerListener> listeners = new ArrayList<RBACManagerListener>( 0 );

    private Resource globalResource;

    @Override
    @PostConstruct
    public void initialize()
    {
        //no op
    }

    @Override
    public boolean isFinalImplementation()
    {
        return false;
    }


    @Override
    public void addListener( RBACManagerListener listener )
    {
        if ( !listeners.contains( listener ) )
        {
            listeners.add( listener );
        }
    }

    @Override
    public void removeListener( RBACManagerListener listener )
    {
        listeners.remove( listener );
    }

    public void fireRbacInit( boolean freshdb )
    {
        for ( RBACManagerListener listener : listeners )
        {
            try
            {
                listener.rbacInit( freshdb );
            }
            catch ( Exception e )
            {
                log.warn( "Unable to trigger .rbacInit( boolean ) to {}", listener.getClass().getName(), e );
            }
        }
    }

    public void fireRbacRoleSaved( Role role )
    {
        for ( RBACManagerListener listener : listeners )
        {
            try
            {
                listener.rbacRoleSaved( role );
            }
            catch ( Exception e )
            {
                log.warn( "Unable to trigger .rbacRoleSaved( Role ) to {}", listener.getClass().getName(), e );
            }
        }
    }

    public void fireRbacRoleRemoved( Role role )
    {
        for ( RBACManagerListener listener : listeners )
        {
            try
            {
                listener.rbacRoleRemoved( role );
            }
            catch ( Exception e )
            {
                log.warn( "Unable to trigger .rbacRoleRemoved( Role ) to {}", listener.getClass().getName(), e );
            }
        }
    }

    public void fireRbacPermissionSaved( Permission permission )
    {
        for ( RBACManagerListener listener : listeners )
        {
            try
            {
                listener.rbacPermissionSaved( permission );
            }
            catch ( Exception e )
            {
                log.warn( "Unable to trigger .rbacPermissionSaved( Permission ) to {}", listener.getClass().getName(),
                          e );
            }
        }
    }

    public void fireRbacPermissionRemoved( Permission permission )
    {
        for ( RBACManagerListener listener : listeners )
        {
            try
            {
                listener.rbacPermissionRemoved( permission );
            }
            catch ( Exception e )
            {
                log.warn( "Unable to trigger .rbacPermissionRemoved( Permission ) to {}", listener.getClass().getName(),
                          e );
            }
        }
    }

    public void fireRbacUserAssignmentSaved( UserAssignment userAssignment )
    {
        for ( RBACManagerListener listener : listeners )
        {
            try
            {
                listener.rbacUserAssignmentSaved( userAssignment );
            }
            catch ( Exception e )
            {
                log.warn( "Unable to trigger .rbacUserAssignmentSaved( UserAssignment ) to {}",
                          listener.getClass().getName(), e );
            }
        }
    }

    public void fireRbacUserAssignmentRemoved( UserAssignment userAssignment )
    {
        for ( RBACManagerListener listener : listeners )
        {
            try
            {
                listener.rbacUserAssignmentRemoved( userAssignment );
            }
            catch ( Exception e )
            {
                log.warn( "Unable to trigger .rbacUserAssignmentRemoved( UserAssignment ) to {}",
                          listener.getClass().getName(), e );
            }
        }
    }

    @Override
    public Role createRole(String name) {
        final String id = DigestUtils.sha1Hex( name );
        return createRole( id, name );
    }

    @Override
    public void removeRole( String roleName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        removeRole( getRole( roleName ) );
    }

    @Override
    public void removeRoleById( String id ) throws RbacManagerException
    {
        removeRole( getRoleById( id ) );
    }

    @Override
    public void removePermission( String permissionName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        removePermission( getPermission( permissionName ) );
    }

    @Override
    public void removeOperation( String operationName )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        removeOperation( getOperation( operationName ) );
    }

    @Override
    public void removeResource( String resourceIdentifier )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        removeResource( getResource( resourceIdentifier ) );
    }

    @Override
    public void removeUserAssignment( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        removeUserAssignment( getUserAssignment( principal ) );
    }

    @Override
    public boolean resourceExists( Resource resource )
    {
        try
        {
            return getAllResources().contains( resource );
        }
        catch ( RbacManagerException e )
        {
            return false;
        }
    }

    @Override
    public boolean resourceExists( String identifier )
    {
        try
        {
            for ( Resource resource : getAllResources() )
            {
                if ( StringUtils.equals( resource.getIdentifier(), identifier ) )
                {
                    return true;
                }
            }
        }
        catch ( RbacManagerException e )
        {
            return false;
        }

        return false;
    }

    @Override
    public boolean operationExists( Operation operation )
    {
        try
        {
            return getAllOperations().contains( operation );
        }
        catch ( RbacManagerException e )
        {
            return false;
        }
    }

    @Override
    public boolean operationExists( String name )
    {
        try
        {
            for ( Operation operation : getAllOperations() )
            {
                if ( StringUtils.equals( operation.getName(), name ) )
                {
                    return true;
                }
            }
        }
        catch ( RbacManagerException e )
        {
            return false;
        }

        return false;
    }

    @Override
    public boolean permissionExists( Permission permission )
    {
        try
        {
            return getAllPermissions().contains( permission );
        }
        catch ( RbacManagerException e )
        {
            return false;
        }
    }

    @Override
    public boolean permissionExists( String name )
    {
        try
        {
            for ( Permission permission : getAllPermissions() )
            {
                if ( StringUtils.equals( permission.getName(), name ) )
                {
                    return true;
                }
            }
        }
        catch ( RbacManagerException e )
        {
            return false;
        }

        return false;
    }

    @Override
    public boolean roleExists( Role role )
        throws RbacManagerException
    {
        try
        {
            return getAllRoles().contains( role );
        }
        catch ( RbacManagerException e )
        {
            return false;
        }
    }

    @Override
    public boolean roleExists( String name )
        throws RbacManagerException
    {
        try
        {
            for ( Role role : getAllRoles() )
            {
                if ( StringUtils.equals( role.getName(), name ) )
                {
                    return true;
                }
            }
        }
        catch ( RbacManagerException e )
        {
            return false;
        }

        return false;
    }

    @Override
    public boolean roleExistsById( final String id )
        throws RbacManagerException
    {
        return getAllRoles( ).stream( ).filter( role -> StringUtils.equals( role.getId( ), id ) )
            .findAny( ).isPresent( );
    }

    @Override
    public boolean userAssignmentExists( String principal )
    {
        try
        {
            for ( UserAssignment assignment : getAllUserAssignments() )
            {
                if ( StringUtils.equals( assignment.getPrincipal(), principal ) )
                {
                    return true;
                }
            }
        }
        catch ( RbacManagerException e )
        {
            return false;
        }

        return false;
    }

    @Override
    public boolean userAssignmentExists( UserAssignment assignment )
    {
        try
        {
            return getAllUserAssignments().contains( assignment );
        }
        catch ( RbacManagerException e )
        {
            return false;
        }
    }

    /**
     * returns a set of all permissions that are in all active roles for a given
     * principal
     *
     * @param principal
     * @return
     * @throws RbacObjectNotFoundException
     * @throws RbacManagerException
     */
    @Override
    public Set<? extends Permission> getAssignedPermissions( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {

        UserAssignment ua = getUserAssignment( principal );

        Set<Permission> permissionSet = new HashSet<Permission>();

        if ( ua.getRoleIds() != null )
        {
            boolean childRoleNamesUpdated = false;

            Iterator<String> it = ua.getRoleIds().listIterator();
            while ( it.hasNext() )
            {
                String roleId = it.next();
                try
                {
                    Role role = getRoleById( roleId );
                    gatherUniquePermissions( role, permissionSet );
                }
                catch ( RbacObjectNotFoundException e )
                {
                    // Found a bad role name. remove it!
                    it.remove();
                    childRoleNamesUpdated = true;
                }
            }

            if ( childRoleNamesUpdated )
            {
                saveUserAssignment( ua );
            }
        }

        return permissionSet;
    }

    /**
     * returns a map of assigned permissions keyed off of operations
     *
     * @param principal
     * @return
     * @throws RbacObjectNotFoundException
     * @throws RbacManagerException
     */
    @Override
    public Map<String, List<? extends Permission>> getAssignedPermissionMap( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        return getPermissionMapByOperation( getAssignedPermissions( principal ) );
    }

    private Map<String, List<? extends Permission>> getPermissionMapByOperation( Collection<? extends Permission> permissions )
    {
        Map<String, List<? extends Permission>> tList = new HashMap<>(  );
        tList.putAll( permissions.stream().collect( Collectors.<Permission, String>groupingBy( p -> p.getOperation( ).getName( ) ) ) );
        return tList;
    }

    private void gatherUniquePermissions( Role role, Collection<Permission> coll )
        throws RbacManagerException
    {
        if ( role.getPermissions() != null )
        {
            for ( Permission permission : role.getPermissions() )
            {
                if ( !coll.contains( permission ) )
                {
                    coll.add( permission );
                }
            }
        }

        if ( role.hasChildRoles() )
        {
            Map<String, ? extends Role> childRoles = getChildRoleNames( role );
            Iterator<? extends Role> it = childRoles.values().iterator();
            while ( it.hasNext() )
            {
                Role child = it.next();
                gatherUniquePermissions( child, coll );
            }
        }
    }

    @Override
    public List<Role> getAllAssignableRoles()
        throws RbacManagerException, RbacObjectNotFoundException
    {
        List<Role> assignableRoles = new ArrayList<Role>();

        for ( Role r : getAllRoles() )
        {
            Role role = getRole( r.getName() );
            if ( role.isAssignable() )
            {
                assignableRoles.add( role );
            }
        }

        return assignableRoles;
    }

    /**
     * returns the active roles for a given principal
     *
     * NOTE: roles that are returned might have have roles themselves, if
     * you just want all permissions then use {@link #getAssignedPermissions(String principal)}
     *
     * @param principal
     * @return
     * @throws RbacObjectNotFoundException
     * @throws RbacManagerException
     */
    @Override
    public Collection<Role> getAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        UserAssignment ua = getUserAssignment( principal );

        return getAssignedRoles( ua );
    }

    /**
     * returns only the roles that are assigned, not the roles that might be child roles of the
     * assigned roles.
     *
     * @param ua
     * @return
     * @throws RbacObjectNotFoundException
     * @throws RbacManagerException
     */
    @Override
    public Collection<Role> getAssignedRoles( UserAssignment ua )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Set<Role> roleSet = new HashSet<Role>();

        if ( ua.getRoleIds() != null )
        {
            boolean childRoleNamesUpdated = false;

            Iterator<String> it = ua.getRoleIds().listIterator();
            while ( it.hasNext() )
            {
                String roleId = it.next();
                try
                {
                    Role role = getRoleById( roleId );

                    if ( !roleSet.contains( role ) )
                    {
                        roleSet.add( role );
                    }
                }
                catch ( RbacObjectNotFoundException e )
                {
                    // Found a bad role name. remove it!
                    it.remove();
                    childRoleNamesUpdated = true;
                }
            }

            if ( childRoleNamesUpdated )
            {
                saveUserAssignment( ua );
            }
        }

        return roleSet;
    }

    /**
     * get all of the roles that the give role has as a child into a set
     *
     * @param role
     * @param roleSet
     * @throws RbacObjectNotFoundException
     * @throws RbacManagerException
     */
    private void gatherEffectiveRoles( Role role, Set<Role> roleSet )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        if ( role.hasChildRoles() )
        {
            for ( String roleName : role.getChildRoleNames() )
            {
                try
                {
                    Role crole = getRole( roleName );

                    if ( !roleSet.contains( crole ) )
                    {
                        gatherEffectiveRoles( crole, roleSet );
                    }
                }
                catch ( RbacObjectNotFoundException e )
                {
                    // the client application might not manage role clean up totally correctly so we want to notify
                    // of a child role issue and offer a clean up process at some point
                    log.warn( "dangling child role: {} on {}", roleName, role.getName() );
                }
            }
        }

        if ( !roleSet.contains( role ) )
        {
            roleSet.add( role );
        }
    }

    @Override
    public Collection<Role> getEffectivelyAssignedRoles( String principal )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        UserAssignment ua = getUserAssignment( principal );

        return getEffectivelyAssignedRoles( ua );
    }

    public Collection<Role> getEffectivelyAssignedRoles( UserAssignment ua )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Set<Role> roleSet = new HashSet<Role>();

        if ( ua != null && ua.getRoleIds() != null )
        {
            boolean childRoleNamesUpdated = false;

            Iterator<String> it = ua.getRoleIds().listIterator();
            while ( it.hasNext() )
            {
                String roleId = it.next();
                try
                {
                    Role role = getRoleById( roleId );

                    gatherEffectiveRoles( role, roleSet );
                }
                catch ( RbacObjectNotFoundException e )
                {
                    // Found a bad role name. remove it!
                    it.remove();
                    childRoleNamesUpdated = true;
                }
            }

            if ( childRoleNamesUpdated )
            {
                saveUserAssignment( ua );
            }
        }
        return roleSet;
    }

    /**
     * @param principal
     * @return
     * @throws RbacManagerException
     * @throws RbacObjectNotFoundException
     */
    @Override
    public Collection<Role> getEffectivelyUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException
    {
        Collection<Role> assignedRoles = getEffectivelyAssignedRoles( principal );
        List<Role> allRoles = getAllAssignableRoles();

        log.debug( "UR: assigned {}", assignedRoles.size() );
        log.debug( "UR: available {}", allRoles.size() );

        return CollectionUtils.subtract( allRoles, assignedRoles );
    }


    /**
     * @param principal
     * @return
     * @throws RbacManagerException
     * @throws RbacObjectNotFoundException
     */
    @Override
    public Collection<Role> getUnassignedRoles( String principal )
        throws RbacManagerException, RbacObjectNotFoundException
    {
        Collection<Role> assignedRoles = getAssignedRoles( principal );
        List<Role> allRoles = getAllAssignableRoles();

        log.debug( "UR: assigned {}", assignedRoles.size() );
        log.debug( "UR: available {}", allRoles.size() );

        return CollectionUtils.subtract( allRoles, assignedRoles );
    }

    @Override
    public Resource getGlobalResource()
        throws RbacManagerException
    {
        if ( globalResource == null )
        {
            globalResource = createResource( Resource.GLOBAL );
            globalResource.setPermanent( true );
            globalResource = saveResource( globalResource );
        }
        return globalResource;
    }

    @Override
    public void addChildRole( Role role, Role childRole )
        throws RbacObjectInvalidException, RbacManagerException
    {
        saveRole( childRole );
        role.addChildRole( childRole );
    }

    @Override
    public Map<String, ? extends Role> getChildRoleNames( Role role )
        throws RbacManagerException
    {
        Map<String, Role> childRoles = new HashMap<String, Role>();

        boolean childRoleNamesUpdated = false;

        Iterator<String> it = role.getChildRoleNames().listIterator();

        List<String> updatedChildRoleList = new ArrayList<String>( role.getChildRoleNames().size() );

        while ( it.hasNext() )
        {
            String roleName = it.next();
            try
            {
                Role child = getRole( roleName );
                // archiva can change role manager but LDAP can be non writable so in such case
                // some roles doesn't exists !!
                if ( child != null )
                {
                    childRoles.put( child.getName(), child );
                    updatedChildRoleList.add( roleName );
                }
                else
                {
                    log.warn(
                        "error searching role with name '{}' probably some issues when migrating your role manager",
                        roleName );
                }
            }
            catch ( RbacObjectNotFoundException e )
            {
                // Found a bad roleName! - trigger new List save
                //it.remove();
                childRoleNamesUpdated = true;
            }
            catch ( RbacManagerException e )
            {
                if ( !( e.getCause() instanceof RbacObjectNotFoundException ) )
                {
                    throw e;
                }
                childRoleNamesUpdated = true;
            }
        }

        if ( childRoleNamesUpdated )
        {
            role.setChildRoleNames( updatedChildRoleList );
            saveRole( role );
        }

        return childRoles;
    }

    @Override
    public Map<String, ? extends Role> getChildRoleIds( Role role )
        throws RbacManagerException
    {
        Map<String, Role> childRoles = new HashMap<String, Role>();

        boolean childRoleNamesUpdated = false;

        Iterator<String> it = role.getChildRoleIds().listIterator();

        final List<String> updatedChildRoleList = new ArrayList<String>( role.getChildRoleIds().size() );

        while ( it.hasNext() )
        {
            String roleId = it.next();
            try
            {
                Role child = getRoleById(  roleId );
                // archiva can change role manager but LDAP can be non writable so in such case
                // some roles doesn't exists !!
                if ( child != null )
                {
                    childRoles.put( child.getId(), child );
                    updatedChildRoleList.add( roleId );
                }
                else
                {
                    log.warn(
                        "error searching role with name '{}' probably some issues when migrating your role manager",
                        roleId );
                }
            }
            catch ( RbacObjectNotFoundException e )
            {
                // Found a bad roleName! - trigger new List save
                //it.remove();
                childRoleNamesUpdated = true;
            }
            catch ( RbacManagerException e )
            {
                if ( !( e.getCause() instanceof RbacObjectNotFoundException ) )
                {
                    throw e;
                }
                childRoleNamesUpdated = true;
            }
        }

        if ( childRoleNamesUpdated )
        {
            role.setChildRoleIds( updatedChildRoleList );
            saveRole( role );
        }

        return childRoles;
    }

    @Override
    public Map<String, ? extends Role> getParentRoleNames( Role role )
        throws RbacManagerException
    {
        Map<String, Role> parentRoles = new HashMap<String, Role>();

        for ( Role r : getAllRoles() )
        {
            if ( !r.getName().equals( role.getName() ) )
            {
                Set<? extends Role> effectiveRoles = getEffectiveRoles( r );
                for ( Role currentRole : effectiveRoles )
                {
                    if ( currentRole.getName().equals( role.getName() ) )
                    {
                        if ( !parentRoles.containsKey( r.getName() ) )
                        {
                            parentRoles.put( r.getName(), r );
                        }
                    }
                }
            }
        }
        return parentRoles;
    }

    @Override
    public Map<String, ? extends Role> getParentRoleIds( final Role role ) throws RbacManagerException
    {
        return getAllRoles( ).stream( ).filter( r -> !r.getId( ).equals( role.getId( ) ) )
            .filter( r -> {
                    try
                    {
                        return getEffectiveRoles( r ).stream( ).map( Role::getId ).filter( cRoleId -> cRoleId.equals( role.getId( ) ) ).findAny( ).isPresent( );
                    }
                    catch ( RbacManagerException e )
                    {
                        return false;
                    }
                }
            ).distinct().collect( Collectors.toMap( Role::getId, Function.identity( ) ) );
    }

    @Override
    public Set<? extends Role> getEffectiveRoles( Role role )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Set<Role> roleSet = new HashSet<Role>();
        gatherEffectiveRoles( role, roleSet );

        return roleSet;
    }

    @Override
    public Map<String, ? extends Role> getRoles( Collection<String> roleNames )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        Map<String, Role> roleMap = new HashMap<String, Role>();

        for ( String roleName : roleNames )
        {
            Role child = getRole( roleName );
            roleMap.put( child.getName(), child );
        }

        return roleMap;
    }
}
