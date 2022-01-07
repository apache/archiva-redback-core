package org.apache.archiva.redback.rest.api.model.v2;/*
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
import java.io.Serializable;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema( name = "BaseUserInfo", description = "Basic user information" )
public class BaseUserInfo implements Serializable
{
    private static final long serialVersionUID = 4643187400578104895L;
    protected String userId;
    private String id;


    public BaseUserInfo( )
    {
    }

    public BaseUserInfo( String id , String userId )
    {
        this.userId = userId;
        this.id = id;
    }

    @Schema( name = "user_id", description = "The user id" )
    @XmlElement( name = "user_id" )
    public String getUserId( )
    {
        return userId;
    }

    public void setUserId( String userId )
    {
        this.userId = userId;
    }

    @Schema( description = "User id that is unique over all user managers" )
    public String getId( )
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "BaseUserInfo{" );
        sb.append( "userId='" ).append( userId ).append( '\'' );
        sb.append( ", id='" ).append( id ).append( '\'' );
        sb.append( '}' );
        return sb.toString( );
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        BaseUserInfo that = (BaseUserInfo) o;

        if ( !userId.equals( that.userId ) ) return false;
        return id.equals( that.id );
    }

    @Override
    public int hashCode( )
    {
        int result = userId.hashCode( );
        result = 31 * result + id.hashCode( );
        return result;
    }
}
