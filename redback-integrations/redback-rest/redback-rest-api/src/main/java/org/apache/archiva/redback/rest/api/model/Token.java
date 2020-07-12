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

import org.apache.archiva.redback.keys.AuthenticationKey;

import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Represents a authentication token.
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
@XmlRootElement( name = "token" )
public class Token
{
    String key;
    OffsetDateTime created;
    OffsetDateTime expires;
    String principal;
    String purpose;

    public Token( )
    {
    }

    public static Token of( AuthenticationKey key ) {
        Token token = new Token( );
        token.setKey( key.getKey() );
        token.setCreatedFromInstant( key.getDateCreated().toInstant() );
        token.setExpiresFromInstant( key.getDateExpires().toInstant() );
        token.setPrincipal( key.getForPrincipal() );
        token.setPurpose( key.getPurpose() );
        return token;
    }

    public static Token of( String key, Date created, Date expires, String principal, String purpose)
    {
        Token token = new Token( );
        token.setKey( key );
        token.setCreatedFromInstant( created.toInstant( ) );
        token.setExpiresFromInstant( expires.toInstant( ) );
        token.setPrincipal( principal );
        token.setPrincipal( purpose );
        return token;
    }

    public static Token of( String key, Instant created, Instant expires, String principal, String purpose )
    {
        Token token = new Token( );
        token.setKey( key );
        token.setCreatedFromInstant( created );
        token.setExpiresFromInstant( expires );
        token.setPrincipal( principal );
        token.setPrincipal( purpose );
        return token;
    }

    public String getKey( )
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    public OffsetDateTime getCreated( )
    {
        return created;
    }

    public void setCreatedFromInstant( Instant created )
    {
        this.created = OffsetDateTime.ofInstant( created, ZoneId.of( "UTC" ) );
    }

    public void setCreated( OffsetDateTime created )
    {
        this.created = created;
    }
    public OffsetDateTime getExpires( )
    {
        return expires;
    }

    public void setExpiresFromInstant( Instant expires )
    {
        this.expires = OffsetDateTime.ofInstant( expires, ZoneId.of( "UTC" ) );
    }

    public void setExpires( OffsetDateTime expires )
    {
        this.expires = expires;
    }

    public String getPrincipal( )
    {
        return principal;
    }

    public void setPrincipal( String principal )
    {
        this.principal = principal;
    }

    public String getPurpose( )
    {
        return purpose;
    }

    public void setPurpose( String purpose )
    {
        this.purpose = purpose;
    }
}
