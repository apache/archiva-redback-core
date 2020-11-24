package org.apache.archiva.redback.rest.api.model.v2;/*
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used for role update. Contains only the role attributes, that can be updated.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema(name="Role",description="Role attributes that are used for updating a role")
public class Role implements Serializable
{
    private static final long serialVersionUID = 3238571295658509062L;

    protected String name;
    protected String id;
    protected String description;
    protected boolean permanent = false;
    /**
     * The ids of all the assigned users.
     */
    protected List<BaseUserInfo> assignedUsers = new ArrayList<>( 0 );

    @Schema(description = "The role name")
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Schema( description = "A description of the role" )
    public String getDescription( )
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @Schema( description = "True, if this role cannot be deleted.")
    public boolean isPermanent()
    {
        return permanent;
    }

    public void setPermanent( boolean permanent )
    {
        this.permanent = permanent;
    }

    @Schema(description = "The identifier of this role")
    public String getId( )
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @Schema( description = "List of user ids that are assigned to this role.")
    public List<BaseUserInfo> getAssignedUsers( )
    {
        return assignedUsers;
    }

    public void setAssignedUsers( List<BaseUserInfo> assignedUsers )
    {
        this.assignedUsers = assignedUsers;
    }

    public void addAssignedUser( BaseUserInfo id) {
        this.assignedUsers.add( id );
    }


}
