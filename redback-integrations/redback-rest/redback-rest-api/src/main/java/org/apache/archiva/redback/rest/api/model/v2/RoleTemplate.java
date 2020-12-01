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
import org.apache.archiva.redback.role.model.ModelApplication;
import org.apache.archiva.redback.role.model.ModelTemplate;

import java.io.Serializable;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema(name="roleTemplate",description = "Information about role templates")
public class RoleTemplate implements Serializable
{
    private static final long serialVersionUID = 8639174144508127048L;

    private String id;
    private String name;
    private String description;
    private String applicationId;
    private boolean assignable;
    private boolean permanent;

    public static RoleTemplate of( ModelApplication application, ModelTemplate template ) {
        RoleTemplate tmpl = new RoleTemplate( );
        tmpl.setApplicationId( application.getId( ) );
        tmpl.setId( template.getId( ) );
        tmpl.setName( template.getNamePrefix() );
        tmpl.setAssignable( template.isAssignable( ) );
        tmpl.setPermanent( template.isPermanent() );
        tmpl.setDescription( template.getDescription( ) );
        return tmpl;
    }


    @Schema(description = "The template identifier")
    public String getId( )
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @Schema(description = "The name of the template")
    public String getName( )
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Schema(description = "The template description")
    public String getDescription( )
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @Schema(description = "Identifier of the application this template is part of")
    public String getApplicationId( )
    {
        return applicationId;
    }

    public void setApplicationId( String applicationId )
    {
        this.applicationId = applicationId;
    }

    @Schema(description = "If a template instance can be assigned")
    public boolean isAssignable( )
    {
        return assignable;
    }

    public void setAssignable( boolean assignable )
    {
        this.assignable = assignable;
    }

    @Schema(description = "If the template is permanent and cannot be deleted")
    public boolean isPermanent( )
    {
        return permanent;
    }

    public void setPermanent( boolean permanent )
    {
        this.permanent = permanent;
    }
}
