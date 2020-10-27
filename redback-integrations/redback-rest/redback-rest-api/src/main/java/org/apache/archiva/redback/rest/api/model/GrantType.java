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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.xml.bind.annotation.XmlEnumValue;

public enum GrantType
{
    @XmlEnumValue( "refresh_token" )
    REFRESH_TOKEN("refresh_token"),

    @XmlEnumValue( "authorization_code" )
    AUTHORIZATION_CODE("authorization_code"),

    @XmlEnumValue( "none" )
    NONE("none");

    private final String label;

    GrantType(final String label) {
        if (label==null) {
            throw new NullPointerException( "Label must not be null" );
        }
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    public static GrantType byLabel(String label) {
        for (GrantType value : values()) {
            if (value.getLabel().equals( label )) {
                return value;
            }
        }
        throw new IllegalArgumentException( "Label does not exist " + label );
    }

    @Override
    public String toString( )
    {
        final StringBuilder sb = new StringBuilder( "GrantType{" );
        sb.append( "label='" ).append( label ).append( '\'' );
        sb.append( '}' );
        return sb.toString( );
    }
}
