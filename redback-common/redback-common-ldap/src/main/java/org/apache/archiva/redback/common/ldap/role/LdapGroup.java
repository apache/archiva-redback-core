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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Representation of a LDAP group
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
public class LdapGroup
{
    String dn = "";
    String name = "";
    String description = "";
    List<String> memberList;

    public LdapGroup( )
    {
    }

    public LdapGroup( String dn )
    {
        this.dn = dn;
    }

    public LdapGroup( String dn, String name, String displayName, String description )
    {
        this.dn = dn;
        this.name = name;
        this.description = description;
    }

    public String getDn( )
    {
        return dn;
    }

    public void setDn( String dn )
    {
        this.dn = dn;
    }

    public String getName( )
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getDescription( )
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public void addMember(String member) {
        if (this.memberList==null) {
            this.memberList = new ArrayList<>( );
        }
        this.memberList.add( member );
    }

    public void setMemberList( Collection<String> memberList) {
        this.memberList = new ArrayList<>( memberList );
    }

    public List<String> getMemberList() {
        if (memberList==null) {
            return Collections.EMPTY_LIST;
        }
        return memberList;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        LdapGroup ldapGroup = (LdapGroup) o;

        return dn.equals( ldapGroup.dn );
    }

    @Override
    public int hashCode( )
    {
        return dn.hashCode( );
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "LdapGroup{" );
        sb.append( "dn='" ).append( dn ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( '}' );
        return sb.toString( );
    }
}
