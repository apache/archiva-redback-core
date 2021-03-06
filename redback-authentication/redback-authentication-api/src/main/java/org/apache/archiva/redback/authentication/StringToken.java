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
 * Simple token implementation. This implementation is immutable.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class StringToken implements Token
{
    final TokenData metadata;
    final String data;
    final String id;
    final TokenType type;

    public StringToken(String id, String tokenData, TokenData metadata) {
        this.id = id;
        this.data = tokenData;
        this.metadata = metadata;
        this.type = TokenType.ACCESS_TOKEN;
    }

    public StringToken(TokenType type, String id, String tokenData, TokenData metadata) {
        this.id = id;
        this.data = tokenData;
        this.metadata = metadata;
        this.type = type;
    }

    @Override
    public String getData( )
    {
        return data;
    }

    @Override
    public byte[] getBytes( )
    {
        return data.getBytes( );
    }

    @Override
    public TokenData getMetadata( )
    {
        return metadata;
    }

    @Override
    public String getId( )
    {
        return id;
    }

    @Override
    public TokenType getType( )
    {
        return type;
    }
}
