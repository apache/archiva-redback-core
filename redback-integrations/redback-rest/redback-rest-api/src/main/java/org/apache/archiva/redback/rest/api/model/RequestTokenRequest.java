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

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="refreshToken")
@Schema(name="Request Token Data", description = "Schema used for requesting a Bearer token.")
public class RequestTokenRequest
{
    String grantType = "";
    String clientId;
    String clientSecret;
    String code;
    String scope = "";
    String state  = "";
    String userId;
    String password;
    String redirectUri;

    public RequestTokenRequest() {

    }

    public RequestTokenRequest( String userId, String password )
    {
        this.userId = userId;
        this.password = password;
    }

    public RequestTokenRequest( String userId, String password, String scope )
    {
        this.userId = userId;
        this.password = password;
        this.scope = scope;
    }

    @XmlElement(name = "grant_type", required = true, nillable = false)
    @Schema(description = "The grant type. Normally 'authorization_code'.")
    public String getGrantType( )
    {
        return grantType;
    }

    public void setGrantType( String grantType )
    {
        this.grantType = grantType;
    }

    @XmlElement(name="client_id", required = false, nillable = true)
    public String getClientId( )
    {
        return clientId;
    }

    public void setClientId( String clientId )
    {
        this.clientId = clientId;
    }

    @XmlElement(name="client_secret", required = false, nillable = true)
    public String getClientSecret( )
    {
        return clientSecret;
    }

    public void setClientSecret( String clientSecret )
    {
        this.clientSecret = clientSecret;
    }

    @XmlElement(name="scope", required = false, nillable = true)
    public String getScope( )
    {
        return scope;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    @XmlElement(name="user_id", required = true, nillable = false)
    @Schema(description = "The user identifier.")
    public String getUserId( )
    {
        return userId;
    }

    public void setUserId( String userId )
    {
        this.userId = userId;
    }

    @XmlElement(name="password", required = true, nillable = false)
    @Schema(description = "The user password")
    public String getPassword( )
    {
        return password;
    }

    @XmlElement(name="password", required = true, nillable = false)
    public void setPassword( String password )
    {
        this.password = password;
    }

    @XmlElement(name="code", required = false, nillable = false)
    public String getCode( )
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    @XmlElement(name="redirect_uri", required = false, nillable = false)
    public String getRedirectUri( )
    {
        return redirectUri;
    }

    public void setRedirectUri( String redirectUri )
    {
        this.redirectUri = redirectUri;
    }

    @XmlElement(name="state", required = false, nillable = false)
    public String getState( )
    {
        return state;
    }

    public void setState( String state )
    {
        this.state = state;
    }

}
