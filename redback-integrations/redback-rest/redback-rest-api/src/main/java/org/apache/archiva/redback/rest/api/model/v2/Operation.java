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

import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@XmlRootElement( name = "operation" )
@Schema(name="Operation", description = "Operation assigned to a permission")
public class Operation
    implements Serializable
{
    private static final long serialVersionUID = 3666638961610656624L;

    private String name;

    private String description;

    private String descriptionKey;

    private boolean permanent;

    public Operation()
    {
        // no op
    }

    public Operation( org.apache.archiva.redback.rbac.Operation operation )
    {
        this.name = operation.getName();
        this.description = operation.getDescription();
        this.permanent = operation.isPermanent();
    }

    @Schema(description = "The operation name")
    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Schema(description = "The description of the operation")
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @Schema(description = "True, if this operation is permanent")
    public boolean isPermanent()
    {
        return permanent;
    }

    public void setPermanent( boolean permanent )
    {
        this.permanent = permanent;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "Operation" );
        sb.append( "{name='" ).append( name ).append( '\'' );
        sb.append( ", description='" ).append( description ).append( '\'' );
        sb.append( ", permanent=" ).append( permanent );
        sb.append( '}' );
        return sb.toString();
    }

    @Schema(name="description_key",description = "The language key for the description")
    public String getDescriptionKey( )
    {
        return descriptionKey;
    }

    public void setDescriptionKey( String descriptionKey )
    {
        this.descriptionKey = descriptionKey;
    }
}
