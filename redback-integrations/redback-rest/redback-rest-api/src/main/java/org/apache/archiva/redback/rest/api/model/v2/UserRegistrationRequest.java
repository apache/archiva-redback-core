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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @since 2.0
 */
@XmlRootElement( name = "userRegistrationRequest" )
@Schema(name="UserRegistrationRequest",description = "Registration request for a new user.")
public class UserRegistrationRequest
    implements Serializable
{
    private static final long serialVersionUID = 5516902373853039946L;
    private User user;

    private String applicationUrl;

    public UserRegistrationRequest()
    {
        // no op
    }

    public UserRegistrationRequest( User user, String applicationUrl )
    {
        this.user = user;
        this.applicationUrl = applicationUrl;
    }

    @Schema(name="user", description = "Information about the new user that wants to be registered.")
    public User getUser()
    {
        return user;
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    @Schema(name="application_url",description = "The URL of the application, used to verify the request.")
    public String getApplicationUrl()
    {
        return applicationUrl;
    }

    public void setApplicationUrl( String applicationUrl )
    {
        this.applicationUrl = applicationUrl;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "UserRegistrationRequest" );
        sb.append( "{user=" ).append( user );
        sb.append( ", applicationUrl='" ).append( applicationUrl ).append( '\'' );
        sb.append( '}' );
        return sb.toString();
    }
}
