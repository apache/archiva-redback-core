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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.archiva.redback.rbac.Role;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Result object for role information.
 *
 * @author Martin Stockhammer
 * @since 3.0
 */
@XmlRootElement( name = "role" )
@Schema(name="RoleInfo",description = "Information about role.")
public class RoleInfo extends BaseRoleInfo
    implements Serializable
{
    private static final long serialVersionUID = -3506615158923923845L;

    /**
     * Field childRoleNames
     */
    private List<String> childRoleIds = new ArrayList<>(0);

    /**
     * Field permissions
     */
    private List<Permission> permissions = new ArrayList<>(0);

    /**
     * The names of all parent roles
     */
    private List<String> parentRoleIds = new ArrayList<>(0);

    public RoleInfo()
    {
        // no op
    }


    public static RoleInfo of( Role rbacRole) {
        RoleInfo role = BaseRoleInfo.of( rbacRole, new RoleInfo( ) );
        if(rbacRole.getPermissions()!=null)
        {
            role.permissions = rbacRole.getPermissions( ).stream( ).map( rbacPerm ->
                Permission.of( rbacPerm )
            ).collect( Collectors.toList( ) );
        }
        return role;
    }

    @XmlTransient
    @Override
    public List<BaseRoleInfo> getChildren( )
    {
        return super.getChildren( );
    }

    @Schema( name="child_role_ids", description = "List of names of children roles")
    public List<String> getChildRoleIds()
    {
        return childRoleIds;
    }

    public void setChildRoleIds( List<String> childRoleIds )
    {
        this.childRoleIds = childRoleIds;
    }

    @Schema( description = "List of permissions assigned to this role.")
    public List<Permission> getPermissions()
    {
        return permissions;
    }

    public void setPermissions( List<Permission> permissions )
    {
        this.permissions = permissions;
    }

    @Schema(name="parent_role_ids", description = "List of names of roles that are parents of this role.")
    public List<String> getParentRoleIds()
    {
        return parentRoleIds;
    }

    public void setParentRoleIds( List<String> parentRoleIds )
    {
        this.parentRoleIds = parentRoleIds;
    }

    @Override
    public boolean isChild( )
    {
        return getParentRoleIds( ).size( ) > 0;
    }

    @Override
    public int hashCode()
    {
        return getName( ) != null ? getName( ).hashCode() : 0;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "RoleInfo{" );
        sb.append( "name='" ).append( getName( ) ).append( '\'' );
        sb.append( ", id='" ).append( getId( ) ).append( '\'' );
        sb.append( ", description='" ).append( getDescription( ) ).append( '\'' );
        sb.append( ", assignable=" ).append( assignable );
        sb.append( ", childRoleNames=" ).append( childRoleIds );
        sb.append( ", permissions=" ).append( permissions );
        sb.append( ", parentRoleNames=" ).append( parentRoleIds );
        sb.append( ", permanent=" ).append( isPermanent( ) );
        sb.append( ", modelId='" ).append( modelId ).append( '\'' );
        sb.append( ", resource='" ).append( resource ).append( '\'' );
        sb.append( ", isTemplateInstance=" ).append( isTemplateInstance );
        sb.append( '}' );
        return sb.toString( );
    }




}
