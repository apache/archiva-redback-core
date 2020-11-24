package org.apache.archiva.redback.rbac.jpa.model;/*
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

import java.io.Serializable;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class RoleId implements Serializable
{
    private static final long serialVersionUID = -3358026083136193536L;
    private String id;
    private String name;

    public RoleId( )
    {
    }

    public RoleId( String id, String name )
    {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;

        RoleId roleId = (RoleId) o;

        if ( !id.equals( roleId.id ) ) return false;
        return name.equals( roleId.name );
    }

    @Override
    public int hashCode( )
    {
        int result = id.hashCode( );
        result = 31 * result + name.hashCode( );
        return result;
    }
}
