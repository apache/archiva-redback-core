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

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 * @since 2.1
 */
public interface LdapRoleMapperConfiguration
{
    /**
     * add mapping ldap group to redback roles
     *
     * @param roles     list of Role names
     * @param ldapGroup ldap group
     */
    void addLdapMapping( String ldapGroup, List<String> roles )
        throws MappingException;

    /**
     * update an existing mapping
     * @param ldapGroup
     * @param roles
     * @throws MappingException
     */
    void updateLdapMapping( String ldapGroup, List<String> roles )
        throws MappingException;

    /**
     * remove a mapping
     *
     * @param group ldap group
     */
    void removeLdapMapping( String group )
        throws MappingException;

    /**
     * @return Map of corresponding LDAP group (key) and Redback roles (value)
     */
    Map<String, Collection<String>> getLdapGroupMappings()
        throws MappingException;

    /**
     * Returns the mapping for the given group
     * @param groupName the group name
     * @return the list of roles
     * @throws MappingException
     */
    Collection<String> getLdapGroupMapping(String groupName) throws MappingException;

    void setLdapGroupMappings( Map<String, List<String>> mappings )
        throws MappingException;
}
