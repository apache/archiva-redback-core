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

/**
 * Information about a group.
 *
 * @since 3.0
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema( name = "Group", description = "Group information" )
public class BaseGroupInfo implements Serializable
{
    private static final long serialVersionUID = 2945927911204165322L;
    private String id;
    private String groupName;
    private String description = "";

    public BaseGroupInfo( )
    {

    }

    public BaseGroupInfo( String id, String groupName )
    {
        this.id = id;
        this.groupName = groupName;
    }

    @Schema(description = "The name of the group")
    public String getGroupName( )
    {
        return groupName;
    }

    public void setGroupName( String groupName )
    {
        this.groupName = groupName;
    }

    @Schema( description = "The unique identifier of the group" )
    public String getId( )
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @Schema( description = "A description of the group" )
    public String getDescription( )
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }
}
