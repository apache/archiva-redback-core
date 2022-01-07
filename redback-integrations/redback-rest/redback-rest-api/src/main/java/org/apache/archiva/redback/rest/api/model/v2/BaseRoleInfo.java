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
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.role.model.ModelRole;

import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic role information. This class contains only the standard attributes used for displaying a role.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema(name="BaseRoleInfo", description = "Basic role attributes")
public class BaseRoleInfo implements Serializable
{
    private static final long serialVersionUID = -6725489773301720068L;

    protected String id;
    protected String name;
    protected String description = "";
    protected boolean permanent = false;
    protected String modelId = "";
    protected String resource = "";
    protected boolean isTemplateInstance = false;
    protected boolean assignable = true;
    private String applicationId = "";
    private boolean isChild = false;

    protected boolean assigned = false;
    private List<BaseRoleInfo> children = new ArrayList<>( 0 );

    public BaseRoleInfo() {

    }

    public static BaseRoleInfo ofName(String name) {
        BaseRoleInfo info = new BaseRoleInfo( );
        info.setName( name );
        return info;
    }

    public static BaseRoleInfo ofId(String id) {
        BaseRoleInfo info = new BaseRoleInfo( );
        info.setId( id );
        return info;
    }

    public static BaseRoleInfo of(Role rbacRole) {
        return of( rbacRole, new BaseRoleInfo( ) );
    }


    public static <T extends BaseRoleInfo>  T of( Role rbacRole, T role ) {
        role.id = rbacRole.getId( );
        role.name = rbacRole.getName( );
        role.description = rbacRole.getDescription( ) == null ?"": rbacRole.getDescription();
        role.permanent = rbacRole.isPermanent( );
        role.modelId = rbacRole.getModelId( );
        role.resource = rbacRole.getResource( );
        role.isTemplateInstance = rbacRole.isTemplateInstance( );
        role.assignable = rbacRole.isAssignable( );
        return role;
    }



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

    @Schema(name="model_id", description = "The model this role is derived from")
    public String getModelId( )
    {
        return modelId;
    }

    public void setModelId( String modelId )
    {
        this.modelId = modelId;
    }

    @Schema(description = "The resource this model is built for, if it is built by a template.")
    public String getResource( )
    {
        return resource;
    }

    public void setResource( String resource )
    {
        this.resource = resource;
    }

    @Schema(name="template_instance", description = "True, if this is a instance of a role template")
    public boolean isTemplateInstance( )
    {
        return isTemplateInstance;
    }

    public void setTemplateInstance( boolean templateInstance )
    {
        isTemplateInstance = templateInstance;
    }

    @Schema(description = "Roles that are children of this role. This field may not be populated, depending on the REST method call.")
    public List<BaseRoleInfo> getChildren( )
    {
        return children;
    }

    public void setChildren( List<BaseRoleInfo> children )
    {
        this.children = children;
    }

    public void addChild(BaseRoleInfo child) {
        if (!this.children.contains( child ))
        {
            this.children.add( child );
        }
    }

    @Schema(description = "This attribute is only set at specific REST calls, that return roles, that are either assigned or not assigned to a given user.")
    public boolean isAssigned( )
    {
        return assigned;
    }

    public void setAssigned( boolean assigned )
    {
        this.assigned = assigned;
    }


    @Schema( description = "If true, the role is assignable to users or roles. Otherwise, it can be used only as parent role.")
    public boolean isAssignable()
    {
        return assignable;
    }

    public void setAssignable( boolean assignable )
    {
        this.assignable = assignable;
    }

    @Override
    public int hashCode( )
    {
        return id.hashCode( );
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "BaseRoleInfo{" );
        sb.append( "id='" ).append( id ).append( '\'' );
        sb.append( ", name='" ).append( name ).append( '\'' );
        sb.append( ", description='" ).append( description ).append( '\'' );
        sb.append( ", permanent=" ).append( permanent );
        sb.append( ", modelId='" ).append( modelId ).append( '\'' );
        sb.append( ", resource='" ).append( resource ).append( '\'' );
        sb.append( ", isTemplateInstance=" ).append( isTemplateInstance );
        sb.append( '}' );
        return sb.toString( );
    }

    @Schema(name="application_id", description = "Application id, where this role belongs to. This is only filled by certain REST methods.")
    public String getApplicationId( )
    {
        return applicationId;
    }

    public void setApplicationId( String applicationId )
    {
        this.applicationId = applicationId;
    }

    public boolean isChild( )
    {
        return isChild;
    }

    public void setChild( boolean child )
    {
        isChild = child;
    }

    @XmlTransient
    public boolean isNotChild() {
        return !isChild;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        BaseRoleInfo that = (BaseRoleInfo) o;

        if ( permanent != that.permanent ) return false;
        if ( isTemplateInstance != that.isTemplateInstance ) return false;
        if ( assignable != that.assignable ) return false;
        if ( !id.equals( that.id ) ) return false;
        if ( !name.equals( that.name ) ) return false;
        if ( description != null ? !description.equals( that.description ) : that.description != null ) return false;
        if ( modelId != null ? !modelId.equals( that.modelId ) : that.modelId != null ) return false;
        if ( resource != null ? !resource.equals( that.resource ) : that.resource != null ) return false;
        return applicationId != null ? applicationId.equals( that.applicationId ) : that.applicationId == null;
    }
}
