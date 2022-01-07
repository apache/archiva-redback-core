package org.apache.archiva.redback.rest.api.model.v2;

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

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="groupMapping")
@Schema(name="GroupMap", description = "Mapping of a group to roles")
public class GroupMapping implements Serializable
{
    private static final long serialVersionUID = 8327221676510149313L;

    String groupName;
    String uniqueGroupName;
    List<String> roles;

    public GroupMapping( )
    {
    }

    public GroupMapping( String groupName, String uniqueGroupName, List<String> roles )
    {
        this.groupName = groupName;
        this.uniqueGroupName = uniqueGroupName;
        this.roles = roles;
    }

    @Schema(name="group_name", description = "The name of the mapped group")
    public String getGroupName( )
    {
        return groupName;
    }

    public void setGroupName( String groupName )
    {
        this.groupName = groupName;
    }

    @Schema(name="unique_group_name", description = "The unique name of the mapped group. Dependent on the used repository backend.")
    public String getUniqueGroupName( )
    {
        return uniqueGroupName;
    }

    public void setUniqueGroupName( String uniqueGroupName )
    {
        this.uniqueGroupName = uniqueGroupName;
    }

    @Schema(description = "The list of role ids mapped to this group")
    public List<String> getRoles( )
    {
        return roles;
    }

    public void setRoles( List<String> roles )
    {
        this.roles = roles;
    }

    public void addRole(String role) {
        if (roles==null) {
            this.roles = new ArrayList<>( );
        }
        if (!this.roles.contains(role)) {
            this.roles.add( role );
        }
    }
}
