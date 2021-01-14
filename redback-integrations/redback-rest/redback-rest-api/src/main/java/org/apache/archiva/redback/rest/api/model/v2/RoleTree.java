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
import java.util.List;
import java.util.Map;

/**
 * Information about roles of a application.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema(name="RoleTree",description = "Tree of roles defined. The root roles have no parent. Each role may have children recursively.")
public class RoleTree implements Serializable
{
    private static final long serialVersionUID = 6893397477073625729L;

    String userId;
    Map<String, Application> applications;
    List<BaseRoleInfo> rootRoles;

    @Schema(name="user_id", description = "The user id for which the assigned flags are set on the roles.")
    public String getUserId( )
    {
        return userId;
    }

    public void setUserId( String userId )
    {
        this.userId = userId;
    }

    @Schema(description = "Information about the applications that define roles. The keys of the map are the application ids.")
    public Map<String, Application> getApplications( )
    {
        return applications;
    }

    public void setApplications( Map<String, Application> applications )
    {
        this.applications = applications;
    }


    @Schema(name="root_roles", description = "The list of roles directly assigned to this application. Roles may contain children roles.")
    public List<BaseRoleInfo> getRootRoles( )
    {
        return rootRoles;
    }

    public void setRootRoles( List<BaseRoleInfo> rootRoles )
    {
        this.rootRoles = rootRoles;
    }


}
