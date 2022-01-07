package org.apache.archiva.redback.integration.filter.authentication.digest;

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
import org.apache.archiva.redback.integration.filter.authentication.basic.HttpBasicAuthentication;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * HttpDigestAuthenticationFilter.
 *
 * Uses RFC 2617 and RFC 2069 to perform Digest authentication against the incoming client.
 *
 * <ul>
 * <li><a href="http://www.faqs.org/rfcs/rfc2617.html">RFC 2617</a> - HTTP Authentication: Basic and Digest Access Authentication</li>
 * <li><a href="http://www.faqs.org/rfcs/rfc2069.html">RFC 2069</a> - An Extension to HTTP : Digest Access Authentication</li>
 * </ul>
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 *
 */
public class HttpDigestAuthenticationFilter
    extends AbstractHttpAuthenticationFilter
{
    private HttpDigestAuthentication httpAuthentication;

    @Override
    public void init( FilterConfig filterConfig )
        throws ServletException
    {
        super.init( filterConfig );

        httpAuthentication =
            getApplicationContext().getBean( "httpAuthenticator#digest", HttpDigestAuthentication.class );

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
            httpAuthentication.setRealm( getRealmName() );
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
