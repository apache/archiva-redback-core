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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="refreshToken")
@Schema(name="Request Token Data", description = "Schema used for requesting a Bearer token.")
public class TokenRequest implements Serializable
{
    private static final long serialVersionUID = -2420082541650525792L;
    GrantType grantType = GrantType.NONE;
    String clientId;
    String clientSecret;
    String code;
    String scope = "";
    String state  = "";
    String userId;
    String password;
    String redirectUri;

    public TokenRequest() {

    }

    public TokenRequest( String userId, String password )
    {
        this.userId = userId;
        this.password = password;
    }

    public TokenRequest( String userId, String password, String scope )
    {
        this.userId = userId;
        this.password = password;
        this.scope = scope;
    }

    public TokenRequest( String userId, String password, GrantType grantType )
    {
        this.userId = userId;
        this.password = password;
        this.grantType = grantType;
    }

    @XmlElement(name = "grant_type", required = true )
    @Schema(
        name = "grant_type",
        description = "The grant type. Currently only 'authorization_code' is supported.",
        allowableValues = {"authorization_code","access_token"},
        defaultValue = "authorization_code",
        example = "authorization_code")
    public GrantType getGrantType( )
    {
        return grantType;
    }

    public void setGrantType( GrantType grantType )
    {
        this.grantType = grantType;
    }

    @XmlElement(name="client_id", nillable = true)
    @Schema(
        name = "client_id",
        description = "The client identifier.")
    public String getClientId( )
    {
        return clientId;
    }

    public void setClientId( String clientId )
    {
        this.clientId = clientId;
    }

    @XmlElement(name="client_secret", nillable = true)
    @Schema(
        name = "client_secret",
        description = "The client application secret.")
    public String getClientSecret( )
    {
        return clientSecret;
    }

    public void setClientSecret( String clientSecret )
    {
        this.clientSecret = clientSecret;
    }

    @XmlElement(name="scope", nillable = true)
    public String getScope( )
    {
        return scope;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    @XmlElement(name="user_id", required = true )
    @Schema(name="user_id", description = "The user identifier.")
    public String getUserId( )
    {
        return userId;
    }

    public void setUserId( String userId )
    {
        this.userId = userId;
    }

    @XmlElement(name="password", required = true )
    @Schema(description = "The user password")
    public String getPassword( )
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    @XmlElement(name="code" )
    public String getCode( )
    {
        return code;
    }

    public void setCode( String code )
    {
        this.code = code;
    }

    @XmlElement(name="redirect_uri" )
    @Schema(
        name = "redirect_uri",
        description = "The URL to redirect to.")
    public String getRedirectUri( )
    {
        return redirectUri;
    }

    public void setRedirectUri( String redirectUri )
    {
        this.redirectUri = redirectUri;
    }

    @XmlElement(name="state" )
    public String getState( )
    {
        return state;
    }

    public void setState( String state )
    {
        this.state = state;
    }


    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "TokenRequest{" );
        sb.append( "grantType=" ).append( grantType );
        sb.append( ", clientId='" ).append( clientId ).append( '\'' );
        sb.append( ", clientSecret='" ).append( clientSecret ).append( '\'' );
        sb.append( ", code='" ).append( code ).append( '\'' );
        sb.append( ", scope='" ).append( scope ).append( '\'' );
        sb.append( ", state='" ).append( state ).append( '\'' );
        sb.append( ", userId='" ).append( userId ).append( '\'' );
        sb.append( ", password='*******'" );
        sb.append( ", redirectUri='" ).append( redirectUri ).append( '\'' );
        sb.append( '}' );
        return sb.toString( );
    }
}
