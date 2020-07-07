package org.apache.archiva.redback.rest.api.model;

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

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Base64;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="userLogin")
public class UserLogin extends User
{

    String authToken;
    String base64AuthToken;

    public UserLogin( )
    {
    }

    public UserLogin( String username, String fullName, String email, boolean validated, boolean locked, String authToken )
    {
        super( username, fullName, email, validated, locked );
        this.authToken = authToken;
        this.base64AuthToken = Base64.getEncoder( ).encodeToString( authToken.getBytes( ) );
    }

    public UserLogin( org.apache.archiva.redback.users.User user, String authToken )
    {
        super( user );
        this.authToken = authToken;
        this.base64AuthToken = Base64.getEncoder( ).encodeToString( authToken.getBytes( ) );
    }

    public String getAuthToken( )
    {
        return authToken;
    }

    public void setAuthToken( String authToken )
    {
        this.authToken = authToken;
    }

    public String getBase64AuthToken( )
    {
        return base64AuthToken;
    }

    public void setBase64AuthToken( String base64AuthToken )
    {
        this.base64AuthToken = base64AuthToken;
    }
}
