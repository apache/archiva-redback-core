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
import org.apache.archiva.redback.authentication.Token;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="token")
@Schema(name="TokenData", description = "The token response data")
public class TokenResponse implements Serializable
{

    private static final long serialVersionUID = 2063260311211245209L;
    String accessToken;
    String tokenType = "Bearer";
    long expiresIn;
    String refreshToken;
    String scope;
    String state;

    public TokenResponse( )
    {
    }

    public TokenResponse( String accessToken, String tokenType, long expiresIn, String refreshToken, String scope )
    {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.scope = scope;
    }

    public TokenResponse( String accessToken, long expiresIn, String refreshToken, String scope )
    {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.scope = scope;
    }

    public TokenResponse( Token accessToken, Token refreshToken )
    {
        this.expiresIn = Duration.between( Instant.now( ), accessToken.getMetadata( ).validBefore( ) ).getSeconds();
        this.accessToken = accessToken.getData( );
        this.refreshToken = refreshToken.getData( );
        this.scope = "";
    }

    public TokenResponse( Token accessToken, Token refreshToken , String scope, String state)
    {
        this.expiresIn = Duration.between( Instant.now( ), accessToken.getMetadata( ).validBefore( ) ).getSeconds();
        this.accessToken = accessToken.getData( );
        this.refreshToken = refreshToken.getData( );
        this.scope = scope;
        this.state = state;
    }

    @XmlElement(name="access_token")
    @Schema(name = "access_token", description = "The access token that may be used as Bearer token in the Authorization header")
    public String getAccessToken( )
    {
        return accessToken;
    }

    public void setAccessToken( String accessToken )
    {
        this.accessToken = accessToken;
    }

    @XmlElement(name="token_type")
    @Schema(name="token_type", description = "The type of the token. Currently only Bearer Tokens are supported.")
    public String getTokenType( )
    {
        return tokenType;
    }

    public void setTokenType( String tokenType )
    {
        this.tokenType = tokenType;
    }

    @XmlElement(name="expires_in")
    @Schema(name="expires_in", description = "The time in seconds. After this time the token will expire and is not valid for authentication.")
    public long getExpiresIn( )
    {
        return expiresIn;
    }

    public void setExpiresIn( long expiresIn )
    {
        this.expiresIn = expiresIn;
    }

    @XmlElement(name="refresh_token")
    @Schema(name="refresh_token", description = "The refresh token, that can be used for getting a new access token.")
    public String getRefreshToken( )
    {
        return refreshToken;
    }

    public void setRefreshToken( String refreshToken )
    {
        this.refreshToken = refreshToken;
    }

    @Schema(description = "Scope of the token. Currently there are no scopes defined.")
    public String getScope( )
    {
        return scope;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

    @Schema(description = "The state value will be returned, if a state is provided in the request.")
    public String getState( )
    {
        return state;
    }

    public void setState( String state )
    {
        this.state = state;
    }

    public boolean hasState() {
        return state != null && state.length( ) > 0;
    }
}
