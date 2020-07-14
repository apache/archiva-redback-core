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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.InvalidTokenException;
import org.apache.archiva.redback.authentication.TokenData;
import org.apache.archiva.redback.authentication.TokenManager;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.integration.filter.authentication.basic.HttpBasicAuthentication;
import org.apache.archiva.redback.users.User;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Stockhammer on 19.01.17.
 * <p>
 * This interceptor tries to check if requests come from a valid origin and
 * are not generated by another site on behalf of the real client.
 * <p>
 * We are using some of the techniques mentioned in
 * https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)_Prevention_Cheat_Sheet
 * <p>
 * Try to find Origin and Referer of the request.
 * Match them to the target address, that may be either statically configured or is determined
 * by the Host/X-Forwarded-For Header.
 */
@Provider
@Service( "requestValidationInterceptor#rest" )
@Priority( Priorities.PRECHECK )
public class RequestValidationInterceptor
    extends AbstractInterceptor
    implements ContainerRequestFilter
{


    private static final String X_FORWARDED_PROTO = "X-Forwarded-Proto";

    private static final String X_FORWARDED_HOST = "X-Forwarded-Host";

    private static final String X_XSRF_TOKEN = "X-XSRF-TOKEN";

    private static final String ORIGIN = "Origin";

    private static final String REFERER = "Referer";

    private static final int DEFAULT_HTTP = 80;

    private static final int DEFAULT_HTTPS = 443;

    private final Logger log = LoggerFactory.getLogger( getClass() );

    private boolean enabled = true;

    private boolean checkToken = true;

    private boolean useStaticUrl = false;

    private boolean denyAbsentHeaders = true;

    private List<URL> baseUrl = new ArrayList<URL>();

    private HttpServletRequest httpRequest = null;

    @Inject
    @Named( value = "httpAuthenticator#basic" )
    private HttpBasicAuthentication httpAuthenticator;

    @Inject
    @Named( value = "tokenManager#default" )
    TokenManager tokenManager;

    @Context
    private ResourceInfo resourceInfo;

    private UserConfiguration config;

    private class HeaderValidationInfo
    {

        final static int UNKNOWN = -1;

        final static int OK = 0;

        final static int F_REFERER_HOST = 1;

        final static int F_REFERER_PORT = 2;

        final static int F_ORIGIN_HOST = 8;

        final static int F_ORIGIN_PORT = 16;

        final static int F_ORIGIN_PROTOCOL = 32;

        boolean headerFound = false;

        URL targetUrl;

        URL originUrl;

        URL refererUrl;

        String targetHost;

        String originHost;

        String refererHost;

        int targetPort;

        int originPort;

        int refererPort;

        int status = UNKNOWN;

        public HeaderValidationInfo( URL targetUrl )
        {
            setTargetUrl( targetUrl );
        }

        public URL getTargetUrl()
        {
            return targetUrl;
        }

        public void setTargetUrl( URL targetUrl )
        {
            this.targetUrl = targetUrl;
            this.targetHost = getHost( targetUrl );
            this.targetPort = getPort( targetUrl );
        }

        public URL getOriginUrl()
        {
            return originUrl;
        }

        public void setOriginUrl( URL originUrl )
        {
            this.originUrl = originUrl;
            this.originHost = getHost( originUrl );
            this.originPort = getPort( originUrl );
            checkOrigin();
            this.headerFound = true;
        }

        public URL getRefererUrl()
        {
            return refererUrl;
        }

        public void setRefererUrl( URL refererUrl )
        {
            this.refererUrl = refererUrl;
            this.refererHost = getHost( refererUrl );
            this.refererPort = getPort( refererUrl );
            checkReferer();
            this.headerFound = true;
        }

        public String getTargetHost()
        {
            return targetHost;
        }

        public void setTargetHost( String targetHost )
        {
            this.targetHost = targetHost;
        }

        public String getOriginHost()
        {
            return originHost;
        }

        public void setOriginHost( String originHost )
        {
            this.originHost = originHost;
        }

        public String getRefererHost()
        {
            return refererHost;
        }

        public void setRefererHost( String refererHost )
        {
            this.refererHost = refererHost;
        }

        public int getTargetPort()
        {
            return targetPort;
        }

        public void setTargetPort( int targetPort )
        {
            this.targetPort = targetPort;
        }

        public int getOriginPort()
        {
            return originPort;
        }

        public void setOriginPort( int originPort )
        {
            this.originPort = originPort;
        }

        public int getRefererPort()
        {
            return refererPort;
        }

        public void setRefererPort( int refererPort )
        {
            this.refererPort = refererPort;
        }

        public void setStatus( int status )
        {
            this.status |= status;
        }

        public int getStatus()
        {
            return this.status;
        }

        // Origin check for Protocol, Host, Port
        public void checkOrigin()
        {
            if ( this.getStatus() == UNKNOWN )
            {
                this.status = OK;
            }
            if ( !targetUrl.getProtocol().equals( originUrl.getProtocol() ) )
            {
                setStatus( F_ORIGIN_PROTOCOL );
            }
            if ( !targetHost.equals( originHost ) )
            {
                setStatus( F_ORIGIN_HOST );
            }
            if ( targetPort != originPort )
            {
                setStatus( F_ORIGIN_PORT );
            }
        }

        // Referer check only for Host, Port
        public void checkReferer()
        {
            if ( this.getStatus() == UNKNOWN )
            {
                this.status = OK;
            }
            if ( !targetHost.equals( refererHost ) )
            {
                setStatus( F_REFERER_HOST );
            }
            if ( targetPort != refererPort )
            {
                setStatus( F_REFERER_PORT );
            }
        }

        public boolean hasOriginError()
        {
            return ( status & ( F_ORIGIN_PROTOCOL | F_ORIGIN_HOST | F_ORIGIN_PORT ) ) > 0;
        }

        public boolean hasRefererError()
        {
            return ( status & ( F_REFERER_HOST | F_REFERER_PORT ) ) > 0;
        }

        @Override
        public String toString()
        {
            return "Stat=" + status + ", target=" + targetUrl + ", origin=" + originUrl + ", referer=" + refererUrl;
        }
    }

    @Inject
    public RequestValidationInterceptor( @Named( value = "userConfiguration#default" ) UserConfiguration config )
    {
        this.config = config;
    }

    @PostConstruct
    public void init()
    {
        List<String> baseUrlList = config.getList( UserConfigurationKeys.REST_BASE_URL );
        if ( baseUrlList != null )
        {
            for ( String baseUrlStr : baseUrlList )
            {
                if ( !"".equals( baseUrlStr.trim() ) )
                {
                    try
                    {
                        baseUrl.add( new URL( baseUrlStr ) );
                        useStaticUrl = true;
                    }
                    catch ( MalformedURLException ex )
                    {
                        log.error( "Configured baseUrl (rest.baseUrl={}) is invalid. Message: {}", baseUrlStr,
                            ex.getMessage() );
                    }
                }
            }
        }
        denyAbsentHeaders = config.getBoolean( UserConfigurationKeys.REST_CSRF_ABSENTORIGIN_DENY, true );
        enabled = config.getBoolean( UserConfigurationKeys.REST_CSRF_ENABLED, true );
        if ( !enabled )
        {
            log.info( "CSRF Filter is disabled by configuration" );
        }
        else
        {
            log.info( "CSRF Filter is enable" );
        }
        checkToken = !config.getBoolean( UserConfigurationKeys.REST_CSRF_DISABLE_TOKEN_VALIDATION, false );
        if ( !checkToken )
        {
            log.info( "CSRF Token validation is disabled by configuration" );
        }
        else
        {
            log.info( "CSRF Token validation is enable" );
        }
    }

    @Override
    public void filter( ContainerRequestContext containerRequestContext )
        throws IOException
    {

        if ( enabled )
        {

            final String requestPath = containerRequestContext.getUriInfo( ).getPath( );
            if (ignoreAuth( requestPath )) {
                return;
            }

            HttpServletRequest request = getRequest();
            List<URL> targetUrls = getTargetUrl( request );
            if ( targetUrls == null )
            {
                log.error( "Could not verify target URL." );
                containerRequestContext.abortWith( Response.status( Response.Status.FORBIDDEN ).build() );
                return;
            }
            List<HeaderValidationInfo> validationInfos = new ArrayList<HeaderValidationInfo>();
            boolean targetMatch = false;
            boolean noHeader = true;
            for ( URL targetUrl : targetUrls )
            {
                log.trace( "Checking against target URL: {}", targetUrl );
                HeaderValidationInfo info = checkSourceRequestHeader( new HeaderValidationInfo( targetUrl ), request );
                // We need only one match
                noHeader = noHeader && info.getStatus() == info.UNKNOWN;
                if ( info.getStatus() == info.OK )
                {
                    targetMatch = true;
                    break;
                }
                else
                {
                    validationInfos.add( info );
                }
            }
            if ( noHeader && denyAbsentHeaders )
            {
                log.warn( "Request denied. No Origin or Referer header found and {}=true",
                    UserConfigurationKeys.REST_CSRF_ABSENTORIGIN_DENY );
                containerRequestContext.abortWith( Response.status( Response.Status.FORBIDDEN ).build() );
                return;
            }
            if ( !targetMatch )
            {
                log.warn( "HTTP Header check failed. Assuming CSRF attack." );
                for ( HeaderValidationInfo info : validationInfos )
                {
                    if ( info.hasOriginError() )
                    {
                        log.warn(
                            "Origin Header does not match: originUrl={}, targetUrl={}. Matches: Host={}, Port={}, Protocol={}",
                            info.originUrl, info.targetUrl, ( info.getStatus() & info.F_ORIGIN_HOST ) == 0,
                            ( info.getStatus() & info.F_ORIGIN_PORT ) == 0,
                            ( info.getStatus() & info.F_ORIGIN_PROTOCOL ) == 0 );
                    }
                    if ( info.hasRefererError() )
                    {
                        log.warn(
                            "Referer Header does not match: refererUrl={}, targetUrl={}. Matches: Host={}, Port={}",
                            info.refererUrl, info.targetUrl, ( info.getStatus() & info.F_REFERER_HOST ) == 0,
                            ( info.getStatus() & info.F_REFERER_PORT ) == 0 );
                    }
                }
                containerRequestContext.abortWith( Response.status( Response.Status.FORBIDDEN ).build() );
                return;
            }
            if ( checkToken )
            {
                checkValidationToken( containerRequestContext, request );
            }
        }
    }

    /**
     * Checks the request for a validation token header. It takes the encrypted token, decrypts it
     * and compares the user information from the token to the logged in user.
     *
     * @param containerRequestContext
     * @param request
     */
    private void checkValidationToken( ContainerRequestContext containerRequestContext, HttpServletRequest request )
    {
        RedbackAuthorization redbackAuthorization = getRedbackAuthorization( resourceInfo );
        // We check only services that are restricted
        if ( !redbackAuthorization.noRestriction() )
        {
            String tokenString = request.getHeader( X_XSRF_TOKEN );
            if ( tokenString == null || tokenString.length() == 0 )
            {
                log.warn( "No validation token header found: {}", X_XSRF_TOKEN );
                containerRequestContext.abortWith( Response.status( Response.Status.FORBIDDEN ).build() );
                return;
            }

            try
            {
                TokenData td = tokenManager.decryptToken( tokenString );
                AuthenticationResult auth = getAuthenticationResult( containerRequestContext, httpAuthenticator, request );
                if ( auth == null )
                {
                    log.error( "Not authentication data found" );
                    containerRequestContext.abortWith( Response.status( Response.Status.FORBIDDEN ).build() );
                    return;
                }
                User loggedIn = auth.getUser();
                if ( loggedIn == null )
                {
                    log.error( "User not logged in" );
                    containerRequestContext.abortWith( Response.status( Response.Status.FORBIDDEN ).build() );
                    return;
                }
                String username = loggedIn.getUsername();
                if ( !td.isValid() || !td.getUser().equals( username ) )
                {
                    log.error( "Invalid data in validation token header {} for user {}: isValid={}, username={}",
                        X_XSRF_TOKEN, username, td.isValid(), td.getUser() );
                    containerRequestContext.abortWith( Response.status( Response.Status.FORBIDDEN ).build() );
                }
            }
            catch ( InvalidTokenException e )
            {
                log.error( "Token validation failed {}", e.getMessage() );
                containerRequestContext.abortWith( Response.status( Response.Status.FORBIDDEN ).build() );
            }
        }
        log.debug( "Token validated" );
    }

    private HttpServletRequest getRequest()
    {
        if ( httpRequest != null )
        {
            return httpRequest;
        }
        else
        {
            return getHttpServletRequest( );
        }
    }

    private List<URL> getTargetUrl( HttpServletRequest request )
    {
        if ( useStaticUrl )
        {
            return baseUrl;
        }
        else
        {
            List<URL> urls = new ArrayList<URL>();
            URL requestUrl;
            try
            {
                requestUrl = new URL( request.getRequestURL().toString() );
                urls.add( requestUrl );
            }
            catch ( MalformedURLException ex )
            {
                log.error( "Bad Request URL {}, Message: {}", request.getRequestURL(), ex.getMessage() );
                return null;
            }
            String xforwarded = request.getHeader( X_FORWARDED_HOST );
            String xforwardedProto = request.getHeader( X_FORWARDED_PROTO );
            if ( xforwardedProto == null )
            {
                xforwardedProto = requestUrl.getProtocol();
            }

            if ( xforwarded != null && !StringUtils.isEmpty( xforwarded ) )
            {
                // X-Forwarded-Host header may contain multiple hosts if there is
                // more than one proxy between the client and the server
                String[] forwardedList = xforwarded.split( "\\s*,\\s*" );
                for ( String hostname : forwardedList )
                {
                    try
                    {
                        urls.add( new URL( xforwardedProto + "://" + hostname ) );
                    }
                    catch ( MalformedURLException ex )
                    {
                        log.warn( "X-Forwarded-Host Header is malformed: {}", ex.getMessage() );
                    }
                }
            }
            return urls;
        }
    }

    private int getPort( final URL url )
    {
        return url.getPort() > 0
            ? url.getPort()
            : ( "https".equals( url.getProtocol() ) ? DEFAULT_HTTPS : DEFAULT_HTTP );
    }

    private String getHost( final URL url )
    {
        return url.getHost().trim().toLowerCase();
    }

    /**
     * Checks the validation headers. First the Origin header is checked, if this fails
     * or is absent, the referer header is checked.
     *
     * @param info    The info object that must be populated with the targetURL
     * @param request The HTTP request object
     * @return A info object with updated status information
     */
    private HeaderValidationInfo checkSourceRequestHeader( final HeaderValidationInfo info,
                                                           final HttpServletRequest request )
    {
        String origin = request.getHeader( ORIGIN );
        if ( origin != null )
        {
            try
            {
                info.setOriginUrl( new URL( origin ) );
            }
            catch ( MalformedURLException e )
            {
                log.warn( "Bad origin header found: {}", origin );
            }
        }
        // Check referer if Origin header dos not match or is not available
        if ( info.getStatus() != info.OK )
        {
            String referer = request.getHeader( REFERER );
            if ( referer != null )
            {
                try
                {
                    info.setRefererUrl( new URL( referer ) );
                }
                catch ( MalformedURLException ex )
                {
                    log.warn( "Bad URL in Referer HTTP-Header: {}, Message: {}", referer, ex.getMessage() );
                }
            }
        }
        return info;
    }

    public void setHttpRequest( HttpServletRequest request )
    {
        this.httpRequest = request;
    }

}
