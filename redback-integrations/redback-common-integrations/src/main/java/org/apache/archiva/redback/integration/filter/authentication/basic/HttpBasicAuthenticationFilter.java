package org.apache.archiva.redback.integration.filter.authentication.basic;

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

import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.integration.filter.authentication.AbstractHttpAuthenticationFilter;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HttpBasicAuthenticationFilter
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public class HttpBasicAuthenticationFilter
    extends AbstractHttpAuthenticationFilter
{
    private HttpAuthenticator httpAuthentication;

    @Override
    public void init( FilterConfig filterConfig )
        throws ServletException
    {
        super.init( filterConfig );

        httpAuthentication = getApplicationContext().getBean( "httpAuthenticator#basic", HttpAuthenticator.class );
    }

    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
        throws IOException, ServletException
    {
        if ( !( request instanceof HttpServletRequest ) )
        {
            throw new ServletException( "Can only process HttpServletRequest" );
        }

        if ( !( response instanceof HttpServletResponse ) )
        {
            throw new ServletException( "Can only process HttpServletResponse" );
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try
        {
            httpAuthentication.authenticate( httpRequest, httpResponse );
        }
        catch ( AuthenticationException e )
        {
            HttpAuthenticator httpauthn = new HttpBasicAuthentication();
            httpauthn.challenge( httpRequest, httpResponse, getRealmName(), e );
            return;
        }

        chain.doFilter( request, response );
    }
}
