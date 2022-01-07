package org.apache.archiva.redback.integration.filter.authentication;

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

import org.apache.archiva.redback.integration.filter.SpringServletFilter;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;

/**
 * AbstractHttpAuthenticationFilter
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public abstract class AbstractHttpAuthenticationFilter
    extends SpringServletFilter
{
    private String realmName;

    public void init( FilterConfig filterConfig )
        throws ServletException
    {
        realmName = filterConfig.getInitParameter( "realm-name" );
    }

    public String getRealmName()
    {
        return realmName;
    }

    public void setRealmName( String realmName )
    {
        this.realmName = realmName;
    }
}
