package org.apache.archiva.redback.rest.api.model;
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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 2.1
 */
@XmlRootElement(name = "groupMapping")
public class GroupMapping
    implements Serializable
{
    private String group;

    private List<String> roleNames;

    public GroupMapping()
    {
        // no op
    }

    public GroupMapping( String group, List<String> roleNames )
    {
        this.group = group;
        this.roleNames = roleNames;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup( String group )
    {
        this.group = group;
    }

    public Collection<String> getRoleNames()
    {
        return roleNames;
    }

    public void setRoleNames( List<String> roleNames )
    {
        this.roleNames = roleNames;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "LdapGroupMapping" );
        sb.append( "{group='" ).append( group ).append( '\'' );
        sb.append( ", roleNames=" ).append( roleNames );
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

        GroupMapping that = (GroupMapping) o;

        if ( !group.equals( that.group ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return group.hashCode();
    }
}
