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
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <b>This implementation is only in memory you must use a different one if you need to save datas.</b>
 *
 * @author Olivier Lamy
 * @since 2.1
 */
@Service("ldapRoleMapperConfiguration#default")
public class DefaultLdapRoleMapperConfiguration
    implements LdapRoleMapperConfiguration
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    private Map<String, List<String>> ldapMappings = new HashMap<String, List<String>>();

    @Inject
    @Named(value = "userConfiguration#default")
    private UserConfiguration userConf;

    public void addLdapMapping( String ldapGroup, List<String> roles )
        throws MappingException
    {
        ldapMappings.put( ldapGroup, roles );
        log.warn( "addLdapMapping implemented but only in memory save: group '{}' roles '{}'", ldapGroup, roles );
    }

    public void removeLdapMapping( String group )
    {
        ldapMappings.remove( group );
    }

    public void updateLdapMapping( String ldapGroup, List<String> roles )
        throws MappingException
    {
        ldapMappings.put( ldapGroup, roles );
        log.warn( "updateLdapMapping implemented but only in memory save: group '{}' roles '{}'", ldapGroup, roles );
    }

    public void setLdapGroupMappings( Map<String, List<String>> mappings )
        throws MappingException
    {
        log.warn( "setLdapGroupMappings implemented but only in memory save" );
        this.ldapMappings = mappings;
    }

    public Map<String, Collection<String>> getLdapGroupMappings()
    {
        MultiValuedMap<String, String> map = new ArrayListValuedHashMap<>( );

        Collection<String> keys = userConf.getKeys();

        for ( String key : keys )
        {
            if ( key.startsWith( UserConfigurationKeys.LDAP_GROUPS_ROLE_START_KEY ) )
            {
                String val = userConf.getString( key );
                String[] roles = StringUtils.split( val, ',' );
                for ( String role : roles )
                {
                    map.put( StringUtils.substringAfter( key, UserConfigurationKeys.LDAP_GROUPS_ROLE_START_KEY ),
                             role );
                }
            }
        }

        for ( Map.Entry<String, List<String>> entry : this.ldapMappings.entrySet() )
        {
            map.putAll( entry.getKey(), entry.getValue() );
        }

        Map<String, Collection<String>> mappings = map.asMap();
        return mappings;
    }

    @Override
    public Collection<String> getLdapGroupMapping( String groupName ) throws MappingException
    {
        if (this.ldapMappings.containsKey( groupName )) {
            return this.ldapMappings.get( groupName );
        } else {
            String value = userConf.getString( UserConfigurationKeys.LDAP_GROUPS_ROLE_START_KEY + groupName );
            if ( value != null) {
                return Arrays.asList( StringUtils.split( "," ) );
            }
        }
        throw new MappingException( "Mapping for group " + groupName + " not found" );
    }
}
