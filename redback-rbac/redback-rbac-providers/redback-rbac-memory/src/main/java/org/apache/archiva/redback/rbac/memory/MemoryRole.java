package org.apache.archiva.redback.rbac.memory;

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

import org.apache.archiva.redback.rbac.AbstractRole;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.Role;

import java.util.ArrayList;
import java.util.List;

/**
 * MemoryRole
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @author Martin Stockhammer <martin_s@apache.org>
 *
 */
public class MemoryRole
    extends AbstractRole
    implements Role, java.io.Serializable
{

    private static final long serialVersionUID = -2784061560950152088L;
    /**
     * Field name
     */
    private String name;

    private String id;

    /**
     * Field description
     */
    private String description;

    /**
     * Field assignable
     */
    private boolean assignable = false;

    /**
     * Field childRoleNames
     */
    private List<String> childRoleNames = new ArrayList<>( 0 );

    private List<String> childRoleIds = new ArrayList<>( 0 );

    /**
     * Field permissions
     */
    private List<Permission> permissions = new ArrayList<>( 0 );

    /**
     * Field permanent
     */
    private boolean permanent = false;

    private String modelId = "";
    private String resource = "";
    private boolean isTemplateInstance;

    @Override
    public void addPermission( Permission memoryPermission )
    {
        if ( !( memoryPermission instanceof MemoryPermission ) )
        {
            throw new ClassCastException( "MemoryRole.addPermissions(memoryPermission) parameter must be instanceof "
                                              + MemoryPermission.class.getName() );
        }
        getPermissions().add( memoryPermission );
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;
        if ( !super.equals( o ) ) return false;

        MemoryRole that = (MemoryRole) o;

        return name.equals( that.name );
    }

    @Override
    public int hashCode( )
    {
        return name.hashCode( );
    }

    @Override
    public List<String> getChildRoleNames()
    {
        return this.childRoleNames;
    }



    @Override
    public void addChildRoleId( String id )
    {
        this.childRoleIds.add( id );
    }

    @Override
    public List<String> getChildRoleIds( )
    {
        return this.childRoleIds;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public List<Permission> getPermissions()
    {
        return this.permissions;
    }

    @Override
    public boolean isAssignable()
    {
        return this.assignable;
    }

    @Override
    public void removePermission( Permission memoryPermission )
    {
        if ( !( memoryPermission instanceof MemoryPermission ) )
        {
            throw new ClassCastException( "MemoryRole.removePermissions(memoryPermission) parameter must be instanceof "
                                              + MemoryPermission.class.getName() );
        }
        getPermissions().remove( memoryPermission );
    }

    @Override
    public void setAssignable( boolean assignable )
    {
        this.assignable = assignable;
    }

    @Override
    public void setDescription( String description )
    {
        this.description = description;
    }

    @Override
    public void setName( String name )
    {
        this.name = name;
    }

    @Override
    public void setPermissions( List<Permission> permissions )
    {
        this.permissions = permissions;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "MemoryRole{" );
        sb.append( "name='" ).append( name ).append( '\'' );
        sb.append( ", id='" ).append( id ).append( '\'' );
        sb.append( '}' );
        return sb.toString( );
    }

    @Override
    public void addChildRoleName( String name )
    {
        this.childRoleNames.add( name );
    }

    @Override
    public void setChildRoleNames( List<String> names )
    {
        if ( names == null )
        {
            this.childRoleNames.clear();
        }
        else
        {
            this.childRoleNames = names;
        }
    }

    @Override
    public void setChildRoleIds( List<String> ids )
    {

    }

    @Override
    public boolean isPermanent()
    {
        return permanent;
    }

    @Override
    public void setPermanent( boolean permanent )
    {
        this.permanent = permanent;
    }


    @Override
    public String getId( )
    {
        return id;
    }

    @Override
    public void setId( String id )
    {
        if (id==null) {
            this.id = "";
        } else
        {
            this.id = id;
        }
    }

    @Override
    public String getModelId( )
    {
        return modelId;
    }

    @Override
    public void setModelId( String modelId )
    {
        if (modelId==null) {
            this.modelId = "";
        } else
        {
            this.modelId = modelId;
        }
    }

    @Override
    public String getResource( )
    {
        return resource;
    }

    @Override
    public void setResource( String resource )
    {
        if (resource==null) {
            this.resource = "";
        } else
        {
            this.resource = resource;
        }
    }

    @Override
    public boolean isTemplateInstance( )
    {
        return isTemplateInstance;
    }

    @Override
    public void setTemplateInstance( boolean templateInstance )
    {
        isTemplateInstance = templateInstance;
    }

}
