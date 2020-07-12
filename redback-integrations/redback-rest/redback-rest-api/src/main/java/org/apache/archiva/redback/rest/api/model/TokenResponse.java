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

import org.apache.archiva.redback.authentication.Token;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.Duration;
import java.time.Instant;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="token")
public class TokenResponse
{
    String accessToken;
    String tokenType = "bearer";
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
    public String getAccessToken( )
    {
        return accessToken;
    }

    public void setAccessToken( String accessToken )
    {
        this.accessToken = accessToken;
    }

    @XmlElement(name="token_type")
    public String getTokenType( )
    {
        return tokenType;
    }

    public void setTokenType( String tokenType )
    {
        this.tokenType = tokenType;
    }

    @XmlElement(name="expires_in")
    public long getExpiresIn( )
    {
        return expiresIn;
    }

    public void setExpiresIn( long expiresIn )
    {
        this.expiresIn = expiresIn;
    }

    @XmlElement(name="refresh_token")
    public String getRefreshToken( )
    {
        return refreshToken;
    }

    public void setRefreshToken( String refreshToken )
    {
        this.refreshToken = refreshToken;
    }

    public String getScope( )
    {
        return scope;
    }

    public void setScope( String scope )
    {
        this.scope = scope;
    }

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
