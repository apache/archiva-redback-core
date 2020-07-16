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

import javax.naming.directory.DirContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * will map ldap group to redback role
 *
 * @author Olivier Lamy
 * @since 2.1
 */
public interface LdapRoleMapper
{

    /**
     * read all groups from ldap
     *
     * @return all LDAP groups
     */
    List<String> getAllGroups( DirContext context )
        throws MappingException;

    /**
     * Read all groups from LDAP and return the list of group objects.
     *
     * @return all LDAP groups found in the LDAP directory
     */
    List<LdapGroup> getAllGroupObjects( DirContext context )
        throws MappingException;

    LdapGroup getGroupForName( DirContext context, String groupName )
        throws MappingException;

    /**
     * read all ldap groups then map to corresponding role (if no mapping found group is ignored)
     *
     * @return all roles
     * @throws MappingException
     */
    List<String> getAllRoles( DirContext context )
        throws MappingException;

    boolean hasRole( DirContext context, String role )
        throws MappingException;


    /**
     * @return the base dn which contains all ldap groups
     */
    String getGroupsDn();

    /**
     * @return the class used for group usually groupOfUniqueNames
     */
    String getLdapGroupClass();

    /**
     * @param group ldap group
     * @return uids of group members
     * @throws MappingException
     */
    List<String> getGroupsMember( String group, DirContext context )
        throws MappingException;

    List<String> getGroups( String username, DirContext context )
        throws MappingException;

    List<LdapGroup> getGroupObjects( String username, DirContext context )
        throws MappingException;

    List<String> getRoles( String username, DirContext context, Collection<String> realRoles )
        throws MappingException;



    /**
     * will save a ldap group corresponding to the mapping.
     * <b>will do nothing in group already exists.</b>
     *
     * @param roleName
     * @return <code>true</code> if role was added, <code>false</code> if role already exists
     * @throws MappingException
     */
    boolean saveRole( String roleName, DirContext context )
        throws MappingException;

    /**
     * associate role to user in ldap
     *
     * @param roleName
     * @param username
     * @return <code>true</code> if role was added to user, <code>false</code> if role already exists for the user
     * @throws MappingException
     */
    boolean saveUserRole( String roleName, String username, DirContext context )
        throws MappingException;

    boolean removeUserRole( String roleName, String username, DirContext context )
        throws MappingException;

    void removeAllRoles( DirContext context )
        throws MappingException;

    void removeRole( String roleName, DirContext context )
        throws MappingException;

    String getUserIdAttribute();

    boolean isUseDefaultRoleName();

}
