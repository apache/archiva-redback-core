package org.apache.archiva.redback.rbac.ldap;

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
import org.apache.archiva.redback.common.ldap.role.LdapRoleMapper;
import org.apache.archiva.redback.common.ldap.role.LdapRoleMapperConfiguration;
import org.apache.archiva.components.cache.Cache;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.rbac.AbstractRBACManager;
import org.apache.archiva.redback.rbac.AbstractRole;
import org.apache.archiva.redback.rbac.Operation;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RBACManagerListener;
import org.apache.archiva.redback.rbac.RBACObjectAssertions;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.RbacObjectInvalidException;
import org.apache.archiva.redback.rbac.RbacObjectNotFoundException;
import org.apache.archiva.redback.rbac.RbacPermanentException;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.ldap.ctl.LdapController;
import org.apache.archiva.redback.users.ldap.ctl.LdapControllerException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * LdapRbacManager will read datas from ldap for mapping groups to role.
 * Write operations will delegate to cached implementation.
 *
 * @author Olivier Lamy
 */
@Service("rbacManager#ldap")
public class LdapRbacManager
    extends AbstractRBACManager
    implements RBACManager, RBACManagerListener
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named(value = "rbacManager#cached")
    private RBACManager rbacImpl;

    @Inject
    @Named(value = "ldapRoleMapper#default")
    private LdapRoleMapper ldapRoleMapper;

    @Inject
    @Named(value = "userConfiguration#default")
    private UserConfiguration userConf;

    @Inject
    @Named(value = "userManager#ldap")
    private UserManager userManager;

    @Inject
    private LdapConnectionFactory ldapConnectionFactory;

    @Inject
    private LdapController ldapController;

    @Inject
    @Named(value = "ldapRoleMapperConfiguration#default")
    private LdapRoleMapperConfiguration ldapRoleMapperConfiguration;

    @Inject
    @Named(value = "cache#ldapRoles")
    private Cache<String, Role> rolesCache;

    @Inject
    @Named(value = "cache#userAssignments")
    private Cache<String, UserAssignment> userAssignmentsCache;

    private boolean writableLdap = false;

    @Override
    @PostConstruct
    public void initialize()
    {
        this.writableLdap = userConf.getBoolean( UserConfigurationKeys.LDAP_WRITABLE, this.writableLdap );
    }


    @Override
    public void addChildRole( Role role, Role childRole )
        throws RbacObjectInvalidException, RbacManagerException
    {
        this.rbacImpl.addChildRole( role, childRole );
    }

    @Override
    public void addListener( RBACManagerListener listener )
    {
        super.addListener( listener );
        this.rbacImpl.addListener( listener );
    }

    @Override
    public Operation createOperation( String name )
        throws RbacManagerException
    {
        return this.rbacImpl.createOperation( name );
    }

    @Override
    public Permission createPermission( String name )
        throws RbacManagerException
    {
        return this.rbacImpl.createPermission( name );
    }

    @Override
    public Permission createPermission( String name, String operationName, String resourceIdentifier )
        throws RbacManagerException
    {
        return this.rbacImpl.createPermission( name, operationName, resourceIdentifier );
    }

    @Override
    public Resource createResource( String identifier )
        throws RbacManagerException
    {
        return this.rbacImpl.createResource( identifier );
    }

    @Override
    public Role createRole( String name )
    {
        return this.rbacImpl.createRole( name );
    }

    @Override
    public UserAssignment createUserAssignment( String username )
        throws RbacManagerException
    {
        // TODO ldap cannot or isWritable ldap ?
        return this.rbacImpl.createUserAssignment( username );
    }

    @Override
    public void eraseDatabase()
    {
        if ( writableLdap )
        {
            LdapConnection ldapConnection = null;
            DirContext context = null;
            try
            {
                ldapConnection = ldapConnectionFactory.getConnection();
                context = ldapConnection.getDirContext();
                ldapRoleMapper.removeAllRoles( context );
            }
            catch ( MappingException e )
            {
                log.warn( "skip error removing all roles {}", e.getMessage() );
            }
            catch ( LdapException e )
            {
                log.warn( "skip error removing all roles {}", e.getMessage() );
            }
            finally
            {
                closeContext( context );
                closeLdapConnection( ldapConnection );
            }
        }
        this.rolesCache.clear();
        this.userAssignmentsCache.clear();
        this.rbacImpl.eraseDatabase();
    }

    /**
     * @see org.apache.archiva.redback.rbac.RBACManager#getAllAssignableRoles()
     */
    @Override
    public List<Role> getAllAssignableRoles()
        throws RbacManagerException
    {
        try
        {
            Collection<Collection<String>> roleNames = ldapRoleMapperConfiguration.getLdapGroupMappings().values();

            Set<Role> roles = new HashSet<Role>();

            for ( Collection<String> names : roleNames )
            {
                for ( String name : names )
                {
                    roles.add( new RoleImpl( name ) );
                }
            }

            return new ArrayList<Role>( roles );
        }
        catch ( MappingException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
    }

    @Override
    public List<? extends Operation> getAllOperations()
        throws RbacManagerException
    {
        return this.rbacImpl.getAllOperations();
    }

    @Override
    public List<? extends Permission> getAllPermissions()
        throws RbacManagerException
    {
        return this.rbacImpl.getAllPermissions();
    }

    @Override
    public List<? extends Resource> getAllResources()
        throws RbacManagerException
    {
        return this.rbacImpl.getAllResources();
    }

    @Override
    public List<Role> getAllRoles()
        throws RbacManagerException
    {
        LdapConnection ldapConnection = null;
        DirContext context = null;
        try
        {
            ldapConnection = ldapConnectionFactory.getConnection();
            context = ldapConnection.getDirContext();

            List<String> groups = ldapRoleMapper.getAllGroups( context );
            return mapToRoles( groups );
        }
        catch ( MappingException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        catch ( LdapException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        finally
        {
            closeContext( context );
            closeLdapConnection( ldapConnection );
        }
        //return this.rbacImpl.getAllRoles();
    }


    @Override
    public List<UserAssignment> getAllUserAssignments()
        throws RbacManagerException
    {
        LdapConnection ldapConnection = null;
        DirContext context = null;
        try
        {
            ldapConnection = ldapConnectionFactory.getConnection();
            context = ldapConnection.getDirContext();
            Map<String, Collection<String>> usersWithRoles = ldapController.findUsersWithRoles( context );
            List<UserAssignment> userAssignments = new ArrayList<UserAssignment>( usersWithRoles.size() );

            for ( Map.Entry<String, Collection<String>> entry : usersWithRoles.entrySet() )
            {
                UserAssignment userAssignment = new UserAssignmentImpl( entry.getKey(), entry.getValue() );
                userAssignments.add( userAssignment );
                userAssignmentsCache.put( userAssignment.getPrincipal(), userAssignment );
            }

            return userAssignments;
        }
        catch ( LdapControllerException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        catch ( LdapException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        finally
        {
            closeContext( context );
            closeLdapConnection( ldapConnection );
        }
    }

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

    /**
     * public Map<String, List<Permission>> getAssignedPermissionMap( String username )
     * throws RbacManagerException
     * {
     * return this.rbacImpl.getAssignedPermissionMap( username );
     * }*
     */

    /*public Set<Permission> getAssignedPermissions( String username )
        throws RbacObjectNotFoundException, RbacManagerException
    {
        // TODO here !!
        return this.rbacImpl.getAssignedPermissions( username );
    }*/
    private List<Role> mapToRoles( List<String> groups )
        throws MappingException, RbacManagerException
    {
        if ( groups == null || groups.isEmpty() )
        {
            return Collections.emptyList();
        }

        List<Role> roles = new ArrayList<Role>( groups.size() );
        Map<String, Collection<String>> mappedGroups = ldapRoleMapperConfiguration.getLdapGroupMappings();
        for ( String group : groups )
        {
            Collection<String> roleNames = mappedGroups.get( group );
            if ( roleNames != null )
            {
                for ( String roleName : roleNames )
                {
                    Role role = buildRole( roleName );
                    roles.add( role );
                }
            }
            else if ( this.ldapRoleMapper.isUseDefaultRoleName() )
            {
                Role role = buildRole( group );
                roles.add( role );


            }
        }
        return roles;

    }

    private Role buildRole( String group )
        throws RbacManagerException
    {
        Role role = null;
        try
        {
            role = this.rbacImpl.getRole( group );
        }
        catch ( RbacObjectNotFoundException e )
        {
            // if it's mapped role to a group it doesn't exist in jpa
        }
        role = ( role == null ) ? new RoleImpl( group ) : role;
        if ( role != null )
        {
            rolesCache.put( role.getName(), role );

        }
        return role;
    }

    protected List<String> getRealRoles()
        throws RbacManagerException
    {
        List<? extends Role> roles = this.rbacImpl.getAllRoles();
        List<String> roleNames = new ArrayList<String>( roles.size() );
        for ( Role role : roles )
        {
            roleNames.add( role.getName() );
        }
        return roleNames;
    }

    @Override
    public Collection<Role> getAssignedRoles( String username )
        throws RbacManagerException
    {

        LdapConnection ldapConnection = null;
        DirContext context = null;

        try
        {

            ldapConnection = ldapConnectionFactory.getConnection();
            context = ldapConnection.getDirContext();
            List<String> roleNames = ldapRoleMapper.getRoles( username, context, getRealRoles() );

            if ( roleNames.isEmpty() )
            {
                return Collections.emptyList();
            }

            List<Role> roles = new ArrayList<Role>( roleNames.size() );

            for ( String name : roleNames )
            {
                roles.add( this.rbacImpl.getRole( name ) );// new RoleImpl( name ) );
            }

            return roles;
        }
        catch ( MappingException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        catch ( LdapException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
    }

    @Override
    public Collection<Role> getAssignedRoles( UserAssignment userAssignment )
        throws RbacManagerException
    {
        return getAssignedRoles( userAssignment.getPrincipal() );
    }

    @Override
    public Map<String, ? extends Role> getChildRoles( Role role )
        throws RbacManagerException
    {
        return this.rbacImpl.getChildRoles( role );
    }

    @Override
    public Map<String, ? extends Role> getParentRoles( Role role )
        throws RbacManagerException
    {
        return this.rbacImpl.getParentRoles( role );
    }

    //
    // public Collection<Role> getEffectivelyAssignedRoles( String username )
    // throws RbacManagerException
    // {
    // TODO here !!
    // return this.rbacImpl.getEffectivelyAssignedRoles( username );
    // }

    //public Collection<Role> getEffectivelyUnassignedRoles( String username )
    //throws RbacManagerException
    //{
    // TODO here !!
    // return this.rbacImpl.getEffectivelyUnassignedRoles( username );
    // }


    @Override
    public Set<? extends Role> getEffectiveRoles( Role role )
        throws RbacManagerException
    {
        return this.rbacImpl.getEffectiveRoles( role );
    }

    @Override
    public Resource getGlobalResource()
        throws RbacManagerException
    {
        return this.rbacImpl.getGlobalResource();
    }

    @Override
    public Operation getOperation( String operationName )
        throws RbacManagerException
    {
        return this.rbacImpl.getOperation( operationName );
    }

    @Override
    public Permission getPermission( String permissionName )
        throws RbacManagerException
    {
        return this.rbacImpl.getPermission( permissionName );
    }

    @Override
    public Resource getResource( String resourceIdentifier )
        throws RbacManagerException
    {
        return this.rbacImpl.getResource( resourceIdentifier );
    }

    @Override
    public Role getRole( String roleName )
        throws RbacManagerException
    {

        Role role = rolesCache.get( roleName );
        if ( role != null )
        {
            return role;
        }
        LdapConnection ldapConnection = null;
        DirContext context = null;
        //verify it's a ldap group
        try
        {
            ldapConnection = ldapConnectionFactory.getConnection();
            context = ldapConnection.getDirContext();
            if ( !ldapRoleMapper.hasRole( context, roleName ) )
            {
                return null;
            }
        }
        catch ( MappingException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        catch ( LdapException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        role = this.rbacImpl.getRole( roleName );
        role = ( role == null ) ? new RoleImpl( roleName ) : role;

        rolesCache.put( roleName, role );

        return role;
    }

    @Override
    public Map<String, ? extends Role> getRoles( Collection<String> roleNames )
        throws RbacManagerException
    {
        return this.rbacImpl.getRoles( roleNames );
    }

    @Override
    public Collection<Role> getUnassignedRoles( String username )
        throws RbacManagerException
    {
        LdapConnection ldapConnection = null;

        DirContext context = null;

        try
        {

            ldapConnection = ldapConnectionFactory.getConnection();

            context = ldapConnection.getDirContext();

            List<String> allRoles = ldapRoleMapper.getAllRoles( context );
            final List<String> userRoles = ldapRoleMapper.getRoles( username, context, getRealRoles() );

            List<Role> unassignedRoles = new ArrayList<Role>();

            for ( String roleName : allRoles )
            {
                if ( !userRoles.contains( roleName ) )
                {
                    unassignedRoles.add( rbacImpl.getRole( roleName ) );
                }
            }
            return unassignedRoles;
        }
        catch ( MappingException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        catch ( LdapException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        finally
        {
            closeContext( context );
            closeLdapConnection( ldapConnection );
        }
    }

    @Override
    public UserAssignment getUserAssignment( String username )
        throws RbacManagerException
    {
        UserAssignment ua = userAssignmentsCache.get( username );
        if ( ua != null )
        {
            return ua;
        }
        LdapConnection ldapConnection = null;
        DirContext context = null;
        try
        {
            ldapConnection = ldapConnectionFactory.getConnection();
            context = ldapConnection.getDirContext();
            List<String> roles = ldapRoleMapper.getRoles( username, context, getRealRoles() );

            ua = new UserAssignmentImpl( username, roles );

            userAssignmentsCache.put( username, ua );

            return ua;
        }
        catch ( MappingException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        catch ( LdapException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        finally
        {
            closeContext( context );
            closeLdapConnection( ldapConnection );
        }

        //return this.rbacImpl.getUserAssignment( username );
    }

    @Override
    public List<? extends UserAssignment> getUserAssignmentsForRoles( Collection<String> roleNames )
        throws RbacManagerException
    {
        // TODO from ldap
        return this.rbacImpl.getUserAssignmentsForRoles( roleNames );
    }

    @Override
    public boolean operationExists( Operation operation )
    {
        return this.rbacImpl.operationExists( operation );
    }

    @Override
    public boolean operationExists( String name )
    {
        return this.rbacImpl.operationExists( name );
    }

    @Override
    public boolean permissionExists( Permission permission )
    {
        return this.rbacImpl.permissionExists( permission );
    }

    @Override
    public boolean permissionExists( String name )
    {
        return this.rbacImpl.permissionExists( name );
    }

    @Override
    public void rbacInit( boolean freshdb )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacInit( freshdb );
        }
    }

    @Override
    public void rbacPermissionRemoved( Permission permission )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacPermissionRemoved( permission );
        }

    }

    @Override
    public void rbacPermissionSaved( Permission permission )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacPermissionSaved( permission );
        }

    }

    @Override
    public void rbacRoleRemoved( Role role )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacRoleRemoved( role );
        }

    }

    @Override
    public void rbacRoleSaved( Role role )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacRoleSaved( role );
        }

    }

    @Override
    public void rbacUserAssignmentRemoved( UserAssignment userAssignment )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacUserAssignmentRemoved( userAssignment );
        }

    }

    @Override
    public void rbacUserAssignmentSaved( UserAssignment userAssignment )
    {
        if ( rbacImpl instanceof RBACManagerListener )
        {
            ( (RBACManagerListener) this.rbacImpl ).rbacUserAssignmentSaved( userAssignment );
        }

    }

    @Override
    public void removeListener( RBACManagerListener listener )
    {
        this.rbacImpl.removeListener( listener );
    }

    @Override
    public void removeOperation( Operation operation )
        throws RbacManagerException
    {
        this.rbacImpl.removeOperation( operation );
    }

    @Override
    public void removeOperation( String operationName )
        throws RbacManagerException
    {
        this.rbacImpl.removeOperation( operationName );
    }

    @Override
    public void removePermission( Permission permission )
        throws RbacManagerException
    {
        this.rbacImpl.removePermission( permission );
    }

    @Override
    public void removePermission( String permissionName )
        throws RbacManagerException
    {
        this.rbacImpl.removePermission( permissionName );
    }

    @Override
    public void removeResource( Resource resource )
        throws RbacManagerException
    {
        this.rbacImpl.removeResource( resource );
    }

    @Override
    public void removeResource( String resourceIdentifier )
        throws RbacManagerException
    {
        this.rbacImpl.removeResource( resourceIdentifier );
    }

    @Override
    public void removeRole( Role role )
        throws RbacManagerException
    {
        RBACObjectAssertions.assertValid( role );

        if ( role.isPermanent() )
        {
            throw new RbacPermanentException( "Unable to delete permanent role [" + role.getName() + "]" );
        }
        rolesCache.remove( role.getName() );
        if ( writableLdap )
        {
            LdapConnection ldapConnection = null;
            DirContext context = null;
            try
            {
                ldapConnection = ldapConnectionFactory.getConnection();
                context = ldapConnection.getDirContext();
                ldapRoleMapper.removeRole( role.getName(), context );
            }
            catch ( MappingException e )
            {
                throw new RbacManagerException( e.getMessage(), e );
            }
            catch ( LdapException e )
            {
                throw new RbacManagerException( e.getMessage(), e );
            }
            fireRbacRoleRemoved( role );
        }
    }

    @Override
    public void removeRole( String roleName )
        throws RbacManagerException
    {
        if ( roleName == null )
        {
            return;
        }
        removeRole( new RoleImpl( roleName ) );
    }

    @Override
    public void removeUserAssignment( String username )
        throws RbacManagerException
    {
        // TODO ldap cannot or isWritable ldap ?
        userAssignmentsCache.remove( username );
        this.rbacImpl.removeUserAssignment( username );
    }

    @Override
    public void removeUserAssignment( UserAssignment userAssignment )
        throws RbacManagerException
    {
        if ( userAssignment != null )
        {
            userAssignmentsCache.remove( userAssignment.getPrincipal() );
        }
        // TODO ldap cannot or isWritable ldap ?
        this.rbacImpl.removeUserAssignment( userAssignment );
    }

    @Override
    public boolean resourceExists( Resource resource )
    {
        return this.rbacImpl.resourceExists( resource );
    }

    @Override
    public boolean resourceExists( String identifier )
    {
        return this.rbacImpl.resourceExists( identifier );
    }

    @Override
    public boolean roleExists( Role role )
        throws RbacManagerException
    {
        if ( role == null )
        {
            return false;
        }
        return roleExists( role.getName() );
    }

    @Override
    public boolean roleExists( String name )
        throws RbacManagerException
    {
        if ( StringUtils.isEmpty( name ) )
        {
            return false;
        }
        if ( rolesCache.get( name ) != null )
        {
            return true;
        }
        LdapConnection ldapConnection = null;
        DirContext context = null;
        try
        {
            ldapConnection = ldapConnectionFactory.getConnection();
            context = ldapConnection.getDirContext();
            if ( rolesCache.hasKey( name ) )
            {
                return true;
            }
            return ldapRoleMapper.hasRole( context, name );
        }
        catch ( MappingException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        catch ( LdapException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        finally
        {
            closeContext( context );
            closeLdapConnection( ldapConnection );
        }
    }

    @Override
    public Operation saveOperation( Operation operation )
        throws RbacManagerException
    {
        return this.rbacImpl.saveOperation( operation );
    }

    @Override
    public Permission savePermission( Permission permission )
        throws RbacManagerException
    {
        return this.rbacImpl.savePermission( permission );
    }

    @Override
    public Resource saveResource( Resource resource )
        throws RbacManagerException
    {
        return this.rbacImpl.saveResource( resource );
    }

    @Override
    public synchronized Role saveRole( Role role )
        throws RbacManagerException
    {
        if ( writableLdap )
        {
            LdapConnection ldapConnection = null;
            DirContext context = null;
            try
            {
                ldapConnection = ldapConnectionFactory.getConnection();
                context = ldapConnection.getDirContext();
                ldapRoleMapper.saveRole( role.getName(), context );

                if ( !role.getChildRoleNames().isEmpty() )
                {
                    for ( String roleName : role.getChildRoleNames() )
                    {
                        ldapRoleMapper.saveRole( roleName, context );
                    }
                }
                fireRbacRoleSaved( role );
            }
            catch ( MappingException e )
            {
                throw new RbacManagerException( e.getMessage(), e );
            }
            catch ( LdapException e )
            {
                throw new RbacManagerException( e.getMessage(), e );
            }
        }
        role = this.rbacImpl.saveRole( role );
        rolesCache.put( role.getName(), role );

        return role;
        //return new RoleImpl( role.getName(), role.getPermissions() );
    }

    @Override
    public synchronized void saveRoles( Collection<Role> roles )
        throws RbacManagerException
    {
        if ( writableLdap )
        {
            LdapConnection ldapConnection = null;
            DirContext context = null;
            try
            {

                ldapConnection = ldapConnectionFactory.getConnection();
                context = ldapConnection.getDirContext();
                for ( Role role : roles )
                {
                    ldapRoleMapper.saveRole( role.getName(), context );
                    fireRbacRoleSaved( role );
                }
            }
            catch ( MappingException e )
            {
                throw new RbacManagerException( e.getMessage(), e );
            }
            catch ( LdapException e )
            {
                throw new RbacManagerException( e.getMessage(), e );
            }
        }
        this.rbacImpl.saveRoles( roles );

    }

    @Override
    public UserAssignment saveUserAssignment( UserAssignment userAssignment )
        throws RbacManagerException
    {
        LdapConnection ldapConnection = null;
        DirContext context = null;
        try
        {
            if ( !userManager.userExists( userAssignment.getPrincipal() ) )
            {
                User user = userManager.createUser( userAssignment.getPrincipal(), null, null );
                userManager.addUser( user );
            }
            ldapConnection = ldapConnectionFactory.getConnection();
            context = ldapConnection.getDirContext();
            List<String> allRoles = ldapRoleMapper.getAllRoles( context );

            List<String> currentUserRoles =
                ldapRoleMapper.getRoles( userAssignment.getPrincipal(), context, getRealRoles() );

            for ( String role : userAssignment.getRoleNames() )
            {
                if ( !currentUserRoles.contains( role ) && writableLdap )
                {
                    // role exists in ldap ?
                    if ( !allRoles.contains( role ) )
                    {
                        ldapRoleMapper.saveRole( role, context );
                        allRoles.add( role );
                    }
                    ldapRoleMapper.saveUserRole( role, userAssignment.getPrincipal(), context );
                    currentUserRoles.add( role );
                }
            }

            for ( String role : currentUserRoles )
            {
                if ( !userAssignment.getRoleNames().contains( role ) && writableLdap )
                {
                    ldapRoleMapper.removeUserRole( role, userAssignment.getPrincipal(), context );
                }
            }

            userAssignmentsCache.put( userAssignment.getPrincipal(), userAssignment );
            return userAssignment;
        }
        catch ( UserManagerException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        catch ( MappingException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        catch ( LdapException e )
        {
            throw new RbacManagerException( e.getMessage(), e );
        }
        finally
        {
            closeContext( context );
            closeLdapConnection( ldapConnection );
        }
    }

    @Override
    public boolean userAssignmentExists( String principal )
    {
        if ( userAssignmentsCache.hasKey( principal ) )
        {
            return true;
        }
        LdapConnection ldapConnection = null;
        DirContext context = null;
        try
        {
            ldapConnection = ldapConnectionFactory.getConnection();
            context = ldapConnection.getDirContext();
            List<String> roles = ldapRoleMapper.getRoles( principal, context, getRealRoles() );
            if ( roles == null || roles.isEmpty() )
            {
                return false;
            }
            return true;
        }
        catch ( RbacManagerException e )
        {
            log.warn( "fail to call userAssignmentExists: {}", e.getMessage() );
        }
        catch ( LdapException e )
        {
            log.warn( "fail to call userAssignmentExists: {}", e.getMessage() );
        }
        catch ( MappingException e )
        {
            log.warn( "fail to call userAssignmentExists: {}", e.getMessage() );
        }
        finally
        {
            closeContext( context );
            closeLdapConnection( ldapConnection );
        }
        return false;
    }

    @Override
    public boolean userAssignmentExists( UserAssignment assignment )
    {
        if ( assignment == null )
        {
            return false;
        }
        return this.userAssignmentExists( assignment.getPrincipal() );
    }

    public RBACManager getRbacImpl()
    {
        return rbacImpl;
    }

    public void setRbacImpl( RBACManager rbacImpl )
    {
        this.rbacImpl = rbacImpl;
    }

    public boolean isWritableLdap()
    {
        return writableLdap;
    }

    public void setWritableLdap( boolean writableLdap )
    {
        this.writableLdap = writableLdap;
    }

    public LdapRoleMapper getLdapRoleMapper()
    {
        return ldapRoleMapper;
    }

    public void setLdapRoleMapper( LdapRoleMapper ldapRoleMapper )
    {
        this.ldapRoleMapper = ldapRoleMapper;
    }

    private static class RoleImpl
        extends AbstractRole
    {
        private String name;

        private String description;

        private List<Permission> permissions = new ArrayList<Permission>();

        private List<String> childRoleNames = new ArrayList<String>();

        private RoleImpl( String name )
        {
            this.name = name;
        }

        private RoleImpl( String name, List<Permission> permissions )
        {
            this.name = name;
            this.permissions = permissions;
        }

        @Override
        public void addPermission( Permission permission )
        {
            this.permissions.add( permission );
        }

        @Override
        public void addChildRoleName( String name )
        {
            this.childRoleNames.add( name );
        }

        @Override
        public List<String> getChildRoleNames()
        {
            return this.childRoleNames;
        }

        @Override
        public String getDescription()
        {
            return this.description;
        }

        @Override
        public String getName()
        {
            return this.name;
        }

        @Override
        public List<Permission> getPermissions()
        {
            return this.permissions;
        }

        @Override
        public boolean isAssignable()
        {
            return true;
        }

        @Override
        public void removePermission( Permission permission )
        {
            this.permissions.remove( permission );
        }

        @Override
        public void setAssignable( boolean assignable )
        {
            // no op
        }

        @Override
        public void setChildRoleNames( List<String> names )
        {
            this.childRoleNames = names;
        }

        @Override
        public void setDescription( String description )
        {
            this.description = description;
        }

        @Override
        public void setName( String name )
        {
            this.name = name;
        }

        @Override
        public void setPermissions( List<Permission> permissions )
        {
            this.permissions = permissions;
        }

        @Override
        public boolean isPermanent()
        {
            return true;
        }

        @Override
        public void setPermanent( boolean permanent )
        {
            // no op
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "RoleImpl" );
            sb.append( "{name='" ).append( name ).append( '\'' );
            sb.append( '}' );
            return sb.toString();
        }

        @Override
        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }

            RoleImpl role = (RoleImpl) o;

            if ( name != null ? !name.equals( role.name ) : role.name != null )
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            return name != null ? name.hashCode() : 0;
        }
    }

    private static class UserAssignmentImpl
        implements UserAssignment
    {
        private String username;

        private List<String> roleNames;

        private boolean permanent;

        private UserAssignmentImpl( String username, Collection<String> roleNames )
        {
            this.username = username;

            if ( roleNames == null )
            {
                this.roleNames = new ArrayList<String>();
            }
            else
            {
                this.roleNames = new ArrayList<String>( roleNames );
            }
        }

        @Override
        public String getPrincipal()
        {
            return this.username;
        }

        @Override
        public List<String> getRoleNames()
        {
            return this.roleNames;
        }

        @Override
        public void addRoleName( Role role )
        {
            if ( role == null )
            {
                return;
            }
            this.roleNames.add( role.getName() );
        }

        @Override
        public void addRoleName( String roleName )
        {
            if ( roleName == null )
            {
                return;
            }
            this.roleNames.add( roleName );
        }

        @Override
        public void removeRoleName( Role role )
        {
            if ( role == null )
            {
                return;
            }
            this.roleNames.remove( role.getName() );
        }

        @Override
        public void removeRoleName( String roleName )
        {
            if ( roleName == null )
            {
                return;
            }
            this.roleNames.remove( roleName );
        }

        @Override
        public void setPrincipal( String principal )
        {
            this.username = principal;
        }

        @Override
        public void setRoleNames( List<String> roles )
        {
            this.roleNames = roles;
        }

        @Override
        public boolean isPermanent()
        {
            return this.permanent;
        }

        @Override
        public void setPermanent( boolean permanent )
        {
            this.permanent = permanent;
        }

        @Override
        public String toString()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append( "UserAssignmentImpl" );
            sb.append( "{username='" ).append( username ).append( '\'' );
            sb.append( ", roleNames=" ).append( roleNames );
            sb.append( ", permanent=" ).append( permanent );
            sb.append( '}' );
            return sb.toString();
        }
    }


    @Override
    public boolean isFinalImplementation()
    {
        return true;
    }

    @Override
    public String getDescriptionKey()
    {
        return "archiva.redback.rbacmanager.ldap";
    }

    @Override
    public boolean isReadOnly()
    {
        return !writableLdap;
    }
}
