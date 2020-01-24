package org.apache.archiva.redback.common.ldap.role;
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
import org.apache.archiva.redback.common.ldap.connection.LdapConnectionFactory;
import org.apache.archiva.redback.common.ldap.connection.LdapException;
import org.apache.archiva.redback.common.ldap.user.LdapUser;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Olivier Lamy
 * @since 2.1
 */
@Service( "ldapRoleMapper#default" )
public class DefaultLdapRoleMapper
    implements LdapRoleMapper
{

    private Logger log = LoggerFactory.getLogger( getClass( ) );

    @Inject
    @Named( value = "ldapConnectionFactory#configurable" )
    private LdapConnectionFactory ldapConnectionFactory;

    @Inject
    @Named( value = "userConfiguration#default" )
    private UserConfiguration userConf;

    @Inject
    @Named( value = "ldapRoleMapperConfiguration#default" )
    private LdapRoleMapperConfiguration ldapRoleMapperConfiguration;

    @Inject
    @Named( value = "userManager#default" )
    private UserManager userManager;

    //---------------------------
    // fields
    //---------------------------

    private String ldapGroupClass = "groupOfUniqueNames";

    private String groupsDn;

    private String groupFilter;

    private String baseDn;

    private String ldapGroupMemberAttribute = "uniqueMember";

    private boolean useDefaultRoleName = false;

    private String dnAttr = "dn";

    /**
     * possible to user cn=beer or uid=beer or sn=beer etc
     * so make it configurable
     */
    public static String DEFAULT_USER_ID_ATTRIBUTE = "uid";
    private String userIdAttribute = DEFAULT_USER_ID_ATTRIBUTE;

    public static String DEFAULT_GROUP_NAME_ATTRIBUTE = "cn";
    private String groupNameAttribute = DEFAULT_GROUP_NAME_ATTRIBUTE;

    // True, if the member attribute stores the DN, otherwise the userkey is used as entry value
    private boolean useDnAsMemberValue = true;

    private static final String POSIX_GROUP = "posixGroup";

    @PostConstruct
    public void initialize( )
    {
        this.ldapGroupClass = userConf.getString( UserConfigurationKeys.LDAP_GROUPS_CLASS, this.ldapGroupClass );

        if (StringUtils.equalsIgnoreCase( POSIX_GROUP, this.ldapGroupClass )) {
            this.useDnAsMemberValue = false;
        }

        this.useDnAsMemberValue = userConf.getBoolean( UserConfigurationKeys.LDAP_GROUPS_USE_DN_AS_MEMBER_VALUE, this.useDnAsMemberValue );

        this.baseDn = userConf.getConcatenatedList( UserConfigurationKeys.LDAP_BASEDN, this.baseDn );

        this.groupsDn = userConf.getConcatenatedList( UserConfigurationKeys.LDAP_GROUPS_BASEDN, this.groupsDn );

        if ( StringUtils.isEmpty( this.groupsDn ) )
        {
            this.groupsDn = this.baseDn;
        }

        this.groupFilter = userConf.getString( UserConfigurationKeys.LDAP_GROUPS_FILTER, this.groupFilter );

        this.useDefaultRoleName =
            userConf.getBoolean( UserConfigurationKeys.LDAP_GROUPS_USE_ROLENAME, this.useDefaultRoleName );

        this.userIdAttribute = userConf.getString( UserConfigurationKeys.LDAP_USER_ID_ATTRIBUTE, DEFAULT_USER_ID_ATTRIBUTE );

        this.ldapGroupMemberAttribute = userConf.getString( UserConfigurationKeys.LDAP_GROUPS_MEMBER, this.ldapGroupMemberAttribute );

        this.dnAttr = userConf.getString( UserConfigurationKeys.LDAP_DN_ATTRIBUTE, this.dnAttr );

        this.groupNameAttribute = userConf.getString( UserConfigurationKeys.LDAP_GROUP_NAME_ATTRIBUTE, DEFAULT_GROUP_NAME_ATTRIBUTE );
    }


    private String getGroupNameFromResult( SearchResult searchResult ) throws NamingException
    {
        Attribute gNameAtt = searchResult.getAttributes( ).get( groupNameAttribute );
        if ( gNameAtt != null )
        {
            return gNameAtt.get( ).toString( );
        }
        else
        {
            log.error( "Could not get group name from attribute {}. Group DN: {}", groupNameAttribute, searchResult.getNameInNamespace( ) );
            return "";
        }


    }

    public List<String> getAllGroups( DirContext context )
        throws MappingException
    {

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {

            SearchControls searchControls = new SearchControls( );

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            searchControls.setReturningAttributes( new String[]{ this.getLdapDnAttribute(), "objectClass", groupNameAttribute} );

            String filter = "objectClass=" + getLdapGroupClass( );

            if ( !StringUtils.isEmpty( this.groupFilter ) )
            {
                filter = "(&(" + filter + ")(" + this.groupFilter + "))";
            }

            namingEnumeration = context.search( getGroupsDn( ), filter, searchControls );

            List<String> allGroups = new ArrayList<String>( );

            while ( namingEnumeration.hasMore( ) )
            {
                SearchResult searchResult = namingEnumeration.next( );
                String groupName = getGroupNameFromResult( searchResult );
                if ( StringUtils.isNotEmpty( groupName ) )
                {
                    log.debug( "Found groupName: '{}", groupName );
                    allGroups.add( groupName );
                }
            }

            return allGroups;
        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }

        finally
        {
            close( namingEnumeration );
        }
    }

    protected void closeNamingEnumeration( NamingEnumeration namingEnumeration )
    {
        if ( namingEnumeration != null )
        {
            try
            {
                namingEnumeration.close( );
            }
            catch ( NamingException e )
            {
                log.warn( "failed to close NamingEnumeration", e );
            }
        }
    }

    public boolean hasRole( DirContext context, String roleName )
        throws MappingException
    {
        String groupName = findGroupName( roleName );

        if ( groupName == null )
        {
            if ( this.useDefaultRoleName )
            {
                groupName = roleName;
            }
            else
            {
                log.warn( "skip group creation as no mapping for roleName:'{}'", roleName );
                return false;
            }
        }
        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {

            SearchControls searchControls = new SearchControls( );

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            String filter = "objectClass=" + getLdapGroupClass( );

            namingEnumeration = context.search( groupNameAttribute + "=" + groupName + "," + getGroupsDn( ), filter, searchControls );

            return namingEnumeration.hasMore( );
        }
        catch ( NameNotFoundException e )
        {
            log.debug( "group {} for role {} not found", groupName, roleName );
            return false;
        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }

        finally
        {
            close( namingEnumeration );
        }
    }

    public List<String> getAllRoles( DirContext context )
        throws MappingException
    {
        List<String> groups = getAllGroups( context );

        if ( groups.isEmpty( ) )
        {
            return Collections.emptyList( );
        }

        Set<String> roles = new HashSet<String>( groups.size( ) );

        Map<String, Collection<String>> mapping = ldapRoleMapperConfiguration.getLdapGroupMappings( );

        for ( String group : groups )
        {
            Collection<String> rolesPerGroup = mapping.get( group );
            if ( rolesPerGroup != null )
            {
                for ( String role : rolesPerGroup )
                {
                    roles.add( role );
                }
            }
        }

        return new ArrayList<String>( roles );
    }

    public List<String> getGroupsMember( String group, DirContext context )
        throws MappingException
    {

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {

            SearchControls searchControls = new SearchControls( );

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            String filter = "objectClass=" + getLdapGroupClass( );

            namingEnumeration = context.search( groupNameAttribute + "=" + group + "," + getGroupsDn( ), filter, searchControls );

            List<String> allMembers = new ArrayList<String>( );

            while ( namingEnumeration.hasMore( ) )
            {
                SearchResult searchResult = namingEnumeration.next( );

                Attribute uniqueMemberAttr = searchResult.getAttributes( ).get( getLdapGroupMemberAttribute( ) );

                if ( uniqueMemberAttr != null )
                {
                    NamingEnumeration<?> allMembersEnum = uniqueMemberAttr.getAll( );
                    while ( allMembersEnum.hasMore( ) )
                    {
                        String userName = allMembersEnum.next( ).toString( );
                        // uid=blabla we only want bla bla
                        userName = StringUtils.substringAfter( userName, "=" );
                        userName = StringUtils.substringBefore( userName, "," );
                        log.debug( "found userName for group {}: '{}", group, userName );

                        allMembers.add( userName );
                    }
                    close( allMembersEnum );
                }


            }

            return allMembers;
        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }

        finally
        {
            close( namingEnumeration );
        }
    }

    private String getUserDnFromId(String userKey) {
        return new StringBuilder().append( this.userIdAttribute ).append( "=" ).append( userKey ).append( "," ).append(
            getBaseDn( ) ).toString();
    }

    public List<String> getGroups( String username, DirContext context )
        throws MappingException
    {

        Set<String> userGroups = new HashSet<String>( );

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {

            SearchControls searchControls = new SearchControls( );

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );


            String groupEntry = null;
            try
            {
                //try to look the user up
                User user = userManager.findUser( username );
                if ( user != null && user instanceof LdapUser )
                {
                    LdapUser ldapUser = (LdapUser) user ;
                    Attribute dnAttribute = ldapUser.getOriginalAttributes( ).get( getLdapDnAttribute( ) );
                    if ( dnAttribute != null )
                    {
                        groupEntry = dnAttribute.get( ).toString();
                    }

                }
            }
            catch ( UserNotFoundException e )
            {
                log.warn( "Failed to look up user {}. Computing distinguished name manually", username, e );
            }
            catch ( UserManagerException e )
            {
                log.warn( "Failed to look up user {}. Computing distinguished name manually", username, e );
            }
            if ( groupEntry == null )
            {
                //failed to look up the user's groupEntry directly

                if ( this.useDnAsMemberValue )
                {
                    groupEntry = getUserDnFromId( username );
                }
                else
                {
                    groupEntry = username;
                }
            }

            String filter =
                new StringBuilder( ).append( "(&" ).append( "(objectClass=" + getLdapGroupClass( ) + ")" ).append(
                    "(" ).append( getLdapGroupMemberAttribute( ) ).append( "=" ).append( Rdn.escapeValue( groupEntry ) ).append( ")" ).append(
                    ")" ).toString( );

            log.debug( "filter: {}", filter );

            namingEnumeration = context.search( getGroupsDn( ), filter, searchControls );

            while ( namingEnumeration.hasMore( ) )
            {
                SearchResult groupSearchResult = namingEnumeration.next( );

                String groupName = getGroupNameFromResult( groupSearchResult );

                if (StringUtils.isNotEmpty( groupName )) {
                    userGroups.add( groupName );
                }


            }

            return new ArrayList( userGroups );
        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }
        finally
        {
            close( namingEnumeration );
        }
    }

    public List<String> getRoles( String username, DirContext context, Collection<String> realRoles )
        throws MappingException
    {
        List<String> groups = getGroups( username, context );

        Map<String, Collection<String>> rolesMapping = ldapRoleMapperConfiguration.getLdapGroupMappings( );

        Set<String> roles = new HashSet<String>( groups.size( ) );

        for ( String group : groups )
        {
            Collection<String> rolesPerGroup = rolesMapping.get( group );
            if ( rolesPerGroup != null )
            {
                roles.addAll( rolesPerGroup );
            }
            else
            {
                if ( this.useDefaultRoleName && realRoles != null && realRoles.contains( group ) )
                {
                    roles.add( group );
                }
            }
        }

        return new ArrayList<String>( roles );
    }

    private void close( NamingEnumeration namingEnumeration )
    {
        if ( namingEnumeration != null )
        {
            try
            {
                namingEnumeration.close( );
            }
            catch ( NamingException e )
            {
                log.warn( "fail to close namingEnumeration: {}", e.getMessage( ) );
            }
        }
    }

    public String getGroupsDn( )
    {
        return this.groupsDn;
    }

    public String getLdapGroupClass( )
    {
        return this.ldapGroupClass;
    }

    public String getLdapDnAttribute( )
    {
        return this.dnAttr;
    }

    public boolean saveRole( String roleName, DirContext context )
        throws MappingException
    {

        if ( hasRole( context, roleName ) )
        {
            return true;
        }

        String groupName = findGroupName( roleName );

        if ( groupName == null )
        {
            if ( this.useDefaultRoleName )
            {
                groupName = roleName;
            }
            else
            {
                log.warn( "skip group creation as no mapping for roleName:'{}'", roleName );
                return false;
            }
        }

        List<String> allGroups = getAllGroups( context );
        if ( allGroups.contains( groupName ) )
        {
            log.info( "group {} already exists for role.", groupName, roleName );
            return false;
        }

        Attributes attributes = new BasicAttributes( true );
        BasicAttribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "groupOfUniqueNames" );
        attributes.put( objectClass );
        attributes.put( this.groupNameAttribute, groupName );

        // attribute mandatory when created a group so add admin as default member
        BasicAttribute basicAttribute = new BasicAttribute( getLdapGroupMemberAttribute( ) );
        basicAttribute.add( this.userIdAttribute + "=admin," + getBaseDn( ) );
        attributes.put( basicAttribute );

        try
        {
            String dn = this.groupNameAttribute + "=" + groupName + "," + this.groupsDn;

            context.createSubcontext( dn, attributes );

            log.info( "created group with dn:'{}", dn );

            return true;
        }
        catch ( NameAlreadyBoundException e )
        {
            log.info( "skip group '{}' creation as already exists", groupName );
            return true;
        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage( ), e );

        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }
    }

    public boolean saveUserRole( String roleName, String username, DirContext context )
        throws MappingException
    {

        String groupName = findGroupName( roleName );

        if ( groupName == null )
        {
            log.warn( "no group found for role '{}", roleName );
            groupName = roleName;
        }

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {
            SearchControls searchControls = new SearchControls( );

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            String filter = "objectClass=" + getLdapGroupClass( );

            namingEnumeration = context.search( this.groupNameAttribute + "=" + groupName + "," + getGroupsDn( ), filter, searchControls );

            if ( namingEnumeration.hasMore( ) )
            {
                SearchResult searchResult = namingEnumeration.next( );
                Attribute attribute = searchResult.getAttributes( ).get( getLdapGroupMemberAttribute( ) );
                if ( attribute == null )
                {
                    BasicAttribute basicAttribute = new BasicAttribute( getLdapGroupMemberAttribute( ) );
                    basicAttribute.add( this.userIdAttribute + "=" + username + "," + getBaseDn( ) );
                    context.modifyAttributes( this.groupNameAttribute + "=" + groupName + "," + getGroupsDn( ), new ModificationItem[]{
                        new ModificationItem( DirContext.ADD_ATTRIBUTE, basicAttribute )} );
                }
                else
                {
                    attribute.add( this.userIdAttribute + "=" + username + "," + getBaseDn( ) );
                    context.modifyAttributes( this.groupNameAttribute + "=" + groupName + "," + getGroupsDn( ), new ModificationItem[]{
                        new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attribute )} );
                }
                return true;
            }

            return false;
        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }

        finally
        {
            if ( namingEnumeration != null )
            {
                try
                {
                    namingEnumeration.close( );
                }
                catch ( NamingException e )
                {
                    log.warn( "failed to close search results", e );
                }
            }
        }
    }

    public boolean removeUserRole( String roleName, String username, DirContext context )
        throws MappingException
    {
        String groupName = findGroupName( roleName );

        if ( groupName == null )
        {
            log.warn( "no group found for role '{}", roleName );
            return false;
        }

        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {

            SearchControls searchControls = new SearchControls( );

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );

            String filter = "objectClass=" + getLdapGroupClass( );

            namingEnumeration = context.search( groupNameAttribute + "=" + groupName + "," + getGroupsDn( ), filter, searchControls );

            if ( namingEnumeration.hasMore( ) )
            {
                SearchResult searchResult = namingEnumeration.next( );
                Attribute attribute = searchResult.getAttributes( ).get( getLdapGroupMemberAttribute( ) );
                if ( attribute != null )
                {
                    BasicAttribute basicAttribute = new BasicAttribute( getLdapGroupMemberAttribute( ) );
                    basicAttribute.add( this.userIdAttribute + "=" + username + "," + getGroupsDn( ) );
                    context.modifyAttributes( groupNameAttribute + "=" + groupName + "," + getGroupsDn( ), new ModificationItem[]{
                        new ModificationItem( DirContext.REMOVE_ATTRIBUTE, basicAttribute )} );
                }
                return true;
            }

            return false;
        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }

        finally
        {
            if ( namingEnumeration != null )
            {
                try
                {
                    namingEnumeration.close( );
                }
                catch ( NamingException e )
                {
                    log.warn( "failed to close search results", e );
                }
            }
        }
    }



    public void removeAllRoles( DirContext context )
        throws MappingException
    {
        //all mapped roles
        Collection<String> groups = ldapRoleMapperConfiguration.getLdapGroupMappings( ).keySet( );

        try
        {
            for ( String groupName : groups )
            {
                removeGroupByName( context, groupName );
            }

        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage( ), e );

        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }
    }

    private void removeGroupByName( DirContext context, String groupName ) throws NamingException
    {
        NamingEnumeration<SearchResult> namingEnumeration = null;
        try
        {
            SearchControls searchControls = new SearchControls( );

            searchControls.setDerefLinkFlag( true );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            String filter = "(&(objectClass=" + getLdapGroupClass( ) + ")(" + groupNameAttribute + "=" + Rdn.escapeValue( groupName ) + "))";
            // String filter = "(&(objectClass=" + getLdapGroupClass( ) + "))";
            namingEnumeration = context.search(  getGroupsDn( ), filter, searchControls );

            // We delete only the first found group
            if ( namingEnumeration != null && namingEnumeration.hasMore( ) )
            {
                SearchResult result = namingEnumeration.next( );
                String dn = result.getNameInNamespace( );
                context.unbind( new LdapName( dn ) );
                log.debug( "Deleted group with dn:'{}", dn );
            }
        }
        finally
        {
            closeNamingEnumeration( namingEnumeration );
        }
    }

    public void removeRole( String roleName, DirContext context )
        throws MappingException
    {

        String groupName = findGroupName( roleName );
        if (StringUtils.isEmpty( groupName )) {
            log.warn( "No group for the given role found: role={}", roleName );
            return;
        }
        try
        {

            removeGroupByName( context, groupName );

        }
        catch ( LdapException e )
        {
            throw new MappingException( e.getMessage( ), e );

        }
        catch ( NamingException e )
        {
            throw new MappingException( e.getMessage( ), e );
        }
    }

    //------------------------------------
    // Mapping part
    //------------------------------------

    //---------------------------------
    // setters for unit tests
    //---------------------------------


    public void setGroupsDn( String groupsDn )
    {
        this.groupsDn = groupsDn;
    }

    public void setLdapGroupClass( String ldapGroupClass )
    {
        this.ldapGroupClass = ldapGroupClass;
    }

    public void setUserConf( UserConfiguration userConf )
    {
        this.userConf = userConf;
    }

    public void setLdapConnectionFactory( LdapConnectionFactory ldapConnectionFactory )
    {
        this.ldapConnectionFactory = ldapConnectionFactory;
    }

    public String getBaseDn( )
    {
        return baseDn;
    }

    public void setBaseDn( String baseDn )
    {
        this.baseDn = baseDn;
    }

    public String getLdapGroupMemberAttribute( )
    {
        return ldapGroupMemberAttribute;
    }

    public void setLdapGroupMemberAttribute( String ldapGroupMemberAttribute )
    {
        this.ldapGroupMemberAttribute = ldapGroupMemberAttribute;
    }

    //-------------------
    // utils methods
    //-------------------

    protected String findGroupName( String role )
        throws MappingException
    {
        Map<String, Collection<String>> mapping = ldapRoleMapperConfiguration.getLdapGroupMappings( );

        for ( Map.Entry<String, Collection<String>> entry : mapping.entrySet( ) )
        {
            if ( entry.getValue( ).contains( role ) )
            {
                return entry.getKey( );
            }
        }
        return null;
    }


    public String getUserIdAttribute( )
    {
        return userIdAttribute;
    }

    public void setUserIdAttribute( String userIdAttribute )
    {
        this.userIdAttribute = userIdAttribute;
    }

    public boolean isUseDefaultRoleName( )
    {
        return useDefaultRoleName;
    }

    public void setUseDefaultRoleName( boolean useDefaultRoleName )
    {
        this.useDefaultRoleName = useDefaultRoleName;
    }
}
