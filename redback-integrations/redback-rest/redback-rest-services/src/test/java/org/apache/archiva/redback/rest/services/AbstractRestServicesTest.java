package org.apache.archiva.redback.rest.services;

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

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import junit.framework.TestCase;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.services.LdapGroupMappingService;
import org.apache.archiva.redback.rest.api.services.LoginService;
import org.apache.archiva.redback.rest.api.services.RoleManagementService;
import org.apache.archiva.redback.rest.api.services.UserService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;

import javax.ws.rs.core.MediaType;
import java.util.Collections;

/**
 * @author Olivier Lamy
 */
@RunWith(JUnit4.class)
public abstract class AbstractRestServicesTest
    extends TestCase
{
    protected Logger log = LoggerFactory.getLogger( getClass() );

    Server server;
    ServerConnector serverConnector;
    public int port;

    public String authorizationHeader = getAdminAuthzHeader();


    public long getTimeout()
    {
        return Long.getLong( "rest.test.timeout", 1000000 );
    }

    public static String encode( String uid, String password )
    {
        return "Basic " + Base64Utility.encode( ( uid + ":" + password ).getBytes() );
    }

    public static String getAdminAuthzHeader()
    {
        String adminPwdSysProps = System.getProperty( "rest.admin.pwd" );
        if ( StringUtils.isBlank( adminPwdSysProps ) )
        {
            return encode( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME, FakeCreateAdminService.ADMIN_TEST_PWD );
        }
        return encode( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME, adminPwdSysProps );
    }

    protected String getSpringConfigLocation()
    {
        return "classpath*:spring-context.xml,classpath*:META-INF/spring-context.xml";
    }


    protected String getRestServicesPath()
    {
        return "restServices";
    }

    @Before
    public void startServer()
        throws Exception
    {
        server = new Server();
        serverConnector = new ServerConnector( server, new HttpConnectionFactory());
        server.addConnector(serverConnector);

        ServletHolder servletHolder = new ServletHolder( new CXFServlet() );
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setResourceBase( SystemUtils.JAVA_IO_TMPDIR );
        context.setSessionHandler( new SessionHandler(  ) );
        context.addServlet( servletHolder, "/" + getRestServicesPath() + "/*" );
        context.setInitParameter( "contextConfigLocation", getSpringConfigLocation() );
        context.addEventListener(new ContextLoaderListener());

        server.setHandler( context );
        server.start();

        if (log.isDebugEnabled())
        {
            log.debug( "jetty dump: {}", server.dump() );
        }

        this.port = serverConnector.getLocalPort();

        log.info( "start server on port {}", this.port );

        UserService userService = getUserService();

        User adminUser = new User();
        adminUser.setUsername( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME );
        adminUser.setPassword( FakeCreateAdminServiceImpl.ADMIN_TEST_PWD );
        adminUser.setFullName( "the admin user" );
        adminUser.setEmail( "toto@toto.fr" );
        Boolean res = userService.createAdminUser( adminUser );

        FakeCreateAdminService fakeCreateAdminService = getFakeCreateAdminService();
        //assertTrue( res.booleanValue() );

    }

    protected FakeCreateAdminService getFakeCreateAdminService()
    {
        return JAXRSClientFactory.create(
            "http://localhost:" + port + "/" + getRestServicesPath() + "/fakeCreateAdminService/",
            FakeCreateAdminService.class, Collections.singletonList( new JacksonJaxbJsonProvider() ) );
    }

    @After
    public void stopServer()
        throws Exception
    {
        if ( this.server != null )
        {
            this.server.stop();
        }
    }

    protected UserService getUserService()
    {

        return getUserService( null );
    }

    // START SNIPPET: get-user-service
    protected UserService getUserService( String authzHeader )
    {
        UserService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/redbackServices/",
                                       UserService.class, Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        // time out for debuging purpose
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( getTimeout() );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.client(service).header("Referer","http://localhost:"+port);
        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );

        return service;
    }
    // END SNIPPET: get-user-service

    protected RoleManagementService getRoleManagementService( String authzHeader )
    {
        RoleManagementService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/redbackServices/",
                                       RoleManagementService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        // for debuging purpose
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( getTimeout() );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.client(service).header("Referer","http://localhost:"+port);

        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );

        return service;
    }

    protected LoginService getLoginService( String authzHeader )
    {
        LoginService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/redbackServices/",
                                       LoginService.class, Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        // for debuging purpose
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( getTimeout() );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.client(service).header("Referer","http://localhost:"+port);

        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );

        return service;
    }


    protected LdapGroupMappingService getLdapGroupMappingService( String authzHeader )
    {
        LdapGroupMappingService service =
            JAXRSClientFactory.create( "http://localhost:" + port + "/" + getRestServicesPath() + "/redbackServices/",
                                       LdapGroupMappingService.class,
                                       Collections.singletonList( new JacksonJaxbJsonProvider() ) );

        // for debuging purpose
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( getTimeout() );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.client(service).header("Referer","http://localhost:"+port);

        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );

        return service;
    }


}
