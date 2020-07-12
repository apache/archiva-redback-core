package org.apache.archiva.redback.authentication;

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

/**
 * Datasource used for authentication by Bearer token (JWT)
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
public class BearerTokenAuthenticationDataSource implements AuthenticationDataSource
{
    private String tokenData;
    private String principal;

    public BearerTokenAuthenticationDataSource( )
    {
    }

    public BearerTokenAuthenticationDataSource( String principal, String tokenData )
    {
        this.tokenData = tokenData;
        this.principal = principal;
    }

    @Override
    public String getUsername( )
    {
        return principal;
    }

    @Override
    public boolean isEnforcePasswordChange( )
    {
        return false;
    }

    public String getTokenData( )
    {
        return tokenData;
    }

    public void setTokenData( String tokenData )
    {
        this.tokenData = tokenData;
    }

}
