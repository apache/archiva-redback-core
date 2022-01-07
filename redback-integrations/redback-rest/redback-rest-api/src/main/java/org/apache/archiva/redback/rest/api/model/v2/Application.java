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
 */
@XmlRootElement( name = "application" )
@Schema(name="Application", description = "A single application that is used for defining roles")
public class Application
    implements Serializable
{
    private static final long serialVersionUID = -4738856943947960583L;

    private String version;
    private String id;
    private String description;
    private String longDescription;

    public Application()
    {
        // no op
    }

    @Schema(description = "The application version. Used to separate different sets of roles.")
    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    @Schema(description = "The identifier of the application")
    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @Schema(description = "A short description.")
    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @Schema(name="long_description", description = "May be a longer explanation, of the application purpose and its defined roles.")
    public String getLongDescription()
    {
        return longDescription;
    }

    public void setLongDescription( String longDescription )
    {
        this.longDescription = longDescription;
    }
}
