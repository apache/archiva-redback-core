package org.apache.archiva.redback.rest.services.interceptors;

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

import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.users.User;

import javax.ws.rs.core.UriInfo;
import java.security.Principal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple security context for JAX-RS to forward data from the Authentication filter to the service implementations
 *
 * @since 3.0
 */
public class RedbackSecurityContext implements javax.ws.rs.core.SecurityContext
{
    SecuritySession securitySession;
    Principal principal;
    User user;
    String authenticationScheme = "Bearer";
    Set<String> roles;
    boolean isSecure;


    RedbackSecurityContext( UriInfo uriInfo, User user, SecuritySession securitySession) {
        this.isSecure = uriInfo.getAbsolutePath().toString().toLowerCase().startsWith("https");
        setPrincipal( user );
        this.securitySession = securitySession;
    }

    @Override
    public Principal getUserPrincipal( )
    {
        return principal;
    }

    @Override
    public boolean isUserInRole( String s )
    {
        return roles == null ? false : roles.contains( s );
    }

    @Override
    public boolean isSecure( )
    {
        return isSecure;
    }

    @Override
    public String getAuthenticationScheme( )
    {
        return authenticationScheme;
    }

    public SecuritySession getSecuritySession() {
        return this.securitySession;
    }

    public void setPrincipal( User user)
    {
        this.user = user;
        this.principal = new RedbackPrincipal( user );
    }

    public void setSession( SecuritySession securitySession )
    {
        this.securitySession = securitySession;
    }

    public void setRoles( Collection<String> roles) {
        this.roles = new HashSet<>( roles );
    }

    public User getUser( )
    {
        return user;
    }
}
