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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.xml.bind.annotation.XmlRootElement;

/**
 * JSON object for updating own user data.
 * Contains only the attributes, that a user is allowed to update. The user id is used from the logged in user principal.
 */
@XmlRootElement( name = "user" )
public class MeUser
{
    private String email;
    private String fullName;
    private String password;
    private String currentPassword;

    public String getEmail( )
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public String getFullName( )
    {
        return fullName;
    }

    public void setFullName( String fullName )
    {
        this.fullName = fullName;
    }

    public String getPassword( )
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public String getCurrentPassword( )
    {
        return currentPassword;
    }

    public void setCurrentPassword( String currentPassword )
    {
        this.currentPassword = currentPassword;
    }
}
