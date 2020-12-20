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
import org.apache.archiva.redback.rest.api.model.v2.Resource;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@XmlRootElement( name = "permission" )
@Schema(name="Permission", description = "Permission that allows operation on resources")
public class Permission
    implements Serializable
{
    private static final long serialVersionUID = 4243488525173718059L;
    private String name;

    private String description;

    private String descriptionKey;

    private Operation operation;

    private Resource resource;

    private boolean permanent;

    public Permission()
    {
        // no op
    }

    public Permission( org.apache.archiva.redback.rbac.Permission permission )
    {
        this.name = permission.getName();
        this.description = permission.getDescription();
        this.operation = permission.getOperation() == null ? null : new Operation( permission.getOperation() );
        this.resource = permission.getResource() == null ? null : new Resource( permission.getResource() );
        this.permanent = permission.isPermanent();
    }

    public static Permission of( org.apache.archiva.redback.rbac.Permission perm )  {
        return new Permission( perm );
    }

    @Schema(name="name", description = "The identifier of the permission")
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Schema(name="operation", description = "The operation that is assigned to this permission")
    public Operation getOperation()
    {
        return operation;
    }

    public void setOperation( Operation operation )
    {
        this.operation = operation;
    }

    @Schema(name="resource", description = "The resource this permission applies to")
    public Resource getResource()
    {
        return resource;
    }

    public void setResource( Resource resource )
    {
        this.resource = resource;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "Permission" );
        sb.append( "{name='" ).append( name ).append( '\'' );
        sb.append( ", description='" ).append( description ).append( '\'' );
        sb.append( ", operation=" ).append( operation );
        sb.append( ", resource=" ).append( resource );
        sb.append( ", permanent=" ).append( permanent );
        sb.append( '}' );
        return sb.toString();
    }

    public String getDescriptionKey( )
    {
        return descriptionKey;
    }

    public void setDescriptionKey( String descriptionKey )
    {
        this.descriptionKey = descriptionKey;
    }
}
