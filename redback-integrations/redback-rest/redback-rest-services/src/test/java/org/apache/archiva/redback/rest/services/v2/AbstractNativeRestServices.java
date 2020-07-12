package org.apache.archiva.redback.rest.services.v2;

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

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.services.FakeCreateAdminServiceImpl;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.port;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 * Native REST tests do not use the JAX-RS client and can be used with a remote
 * REST API service. The tests
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Tag("rest-native")
public abstract class AbstractNativeRestServices
{
    public static final String SYSPROP_START_SERVER = "archiva.rest.start.server";
    public static final String SYSPROP_SERVER_PORT = "archiva.rest.server.port";
    public static final String SYSPROP_SERVER_BASE_URI = "archiva.rest.server.baseuri";
    public static final int STOPPED = 0;
    public static final int STOPPING = 1;
    public static final int STARTING = 2;
    public static final int STARTED = 3;
    public static final int ERROR = 4;

    private RequestSpecification requestSpec;
    protected Logger log = LoggerFactory.getLogger( getClass() );

    private static AtomicReference<Server> server = new AtomicReference<>();
    private static AtomicReference<ServerConnector> serverConnector = new AtomicReference<>();
    private static AtomicInteger serverStarted = new AtomicInteger( STOPPED );
    private UserManager userManager;
    private RoleManager roleManager;


    protected abstract String getServicePath();

    protected String getSpringConfigLocation()
    {
        return "classpath*:spring-context.xml,classpath*:META-INF/spring-context.xml";
    }

    protected RequestSpecification getRequestSpec() {
        return this.requestSpec;
    }

    protected String getContextRoot()
    {
        return "/api";
    }


    private String getServiceBasePath( )
    {
        return "/v2/redback";
    }

    protected String getBasePath( )
    {
        return new StringBuilder(  )
            .append(getContextRoot( ))
            .append(getServiceBasePath( ))
            .append(getServicePath( )).toString();
    }

    /**
     * Returns the server that was started, or null if not initialized before.
     * @return
     */
    public Server getServer() {
        return this.server.get();
    }

    public int getServerPort() {
        ServerConnector connector = serverConnector.get();
        if (connector!=null) {
            return connector.getLocalPort();
        } else {
            return 0;
        }
    }

    /**
     * Returns true, if the server does exist and is running.
     * @return true, if server does exist and is running.
     */
    public boolean isServerRunning() {
        return serverStarted.get()==STARTED && this.server.get() != null && this.server.get().isRunning();
    }

    private UserManager getUserManager() {
        if (this.userManager==null) {
            UserManager userManager = ContextLoaderListener.getCurrentWebApplicationContext( )
                .getBean( "userManager#default", UserManager.class );
            assertNotNull( userManager );
            this.userManager = userManager;
        }
        return this.userManager;
    }

    private RoleManager getRoleManager() {
        if (this.roleManager==null) {
            RoleManager roleManager = ContextLoaderListener.getCurrentWebApplicationContext( )
                .getBean( "roleManager", RoleManager.class );
            assertNotNull( roleManager );
            this.roleManager = roleManager;
        }
        return this.roleManager;
    }

    private void setupAdminUser() throws UserManagerException, RoleManagerException
    {
        UserManager um = getUserManager( );

        User adminUser = null;
        try
        {
            adminUser = um.findUser( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME );
        } catch ( UserNotFoundException e ) {
            // ignore
        }
        if (adminUser==null)
        {
            adminUser = um.createUser( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME, "Administrator", "admin@local.home" );
            adminUser.setUsername( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME );
            adminUser.setPassword( FakeCreateAdminServiceImpl.ADMIN_TEST_PWD );
            adminUser.setFullName( "the admin user" );
            adminUser.setEmail( "toto@toto.fr" );
            adminUser.setPermanent( true );
            adminUser.setValidated( true );
            adminUser.setLocked( false );
            adminUser.setPasswordChangeRequired( false );
            um.addUser( adminUser );

            getRoleManager( ).assignRole( "system-administrator", adminUser.getUsername( ) );
        }
    }

    public void startServer()
        throws Exception
    {
        if (serverStarted.compareAndSet( STOPPED, STARTING ))
        {
            try
            {
                log.info( "Starting server" );
                Server myServer = new Server( );
                this.server.set( myServer );
                this.serverConnector.set( new ServerConnector( myServer, new HttpConnectionFactory( ) ) );
                myServer.addConnector( serverConnector.get( ) );

                ServletHolder servletHolder = new ServletHolder( new CXFServlet( ) );
                ServletContextHandler context = new ServletContextHandler( ServletContextHandler.SESSIONS );
                context.setResourceBase( SystemUtils.JAVA_IO_TMPDIR );
                context.setSessionHandler( new SessionHandler( ) );
                context.addServlet( servletHolder, getContextRoot( ) + "/*" );
                context.setInitParameter( "contextConfigLocation", getSpringConfigLocation( ) );
                context.addEventListener( new ContextLoaderListener( ) );

                getServer( ).setHandler( context );
                getServer( ).start( );

                if ( log.isDebugEnabled( ) )
                {
                    log.debug( "Jetty dump: {}", getServer( ).dump( ) );
                }

                setupAdminUser();
                log.info( "Started server on port {}", getServerPort( ) );
                serverStarted.set( STARTED );
            } finally {
                // In case, if the last statement was not reached
                serverStarted.compareAndSet( STARTING, ERROR );
            }
        }

    }

    public void stopServer()
        throws Exception
    {
        if ( this.serverStarted.compareAndSet( STARTED, STOPPING ) )
        {
            try
            {
                final Server myServer = getServer( );
                if ( myServer != null )
                {
                    log.info("Stopping server");
                    myServer.stop();
                }
                serverStarted.set( STOPPED );
            } finally {
                serverStarted.compareAndSet( STOPPING, ERROR );
            }
        } else {
            log.error( "Serer is not in STARTED state!" );
        }
    }


    protected void setupNative( ) throws Exception
    {
        String startServer = System.getProperty( SYSPROP_START_SERVER, "yes" ).toLowerCase( );
        String serverPort = System.getProperty( SYSPROP_SERVER_PORT, "" );
        String baseUri = System.getProperty( SYSPROP_SERVER_BASE_URI, "http://localhost" );

        if ( !"no".equals( startServer ) )
        {
            startServer( );
        }

        if ( StringUtils.isNotEmpty( serverPort ) )
        {
            RestAssured.port = Integer.parseInt( serverPort );
        }
        else
        {
            RestAssured.port = getServerPort( );
        }
        if ( StringUtils.isNotEmpty( baseUri ) )
        {
            RestAssured.baseURI = baseUri;
        }
        else
        {
            RestAssured.baseURI = "http://localhost";
        }
        String basePath = getBasePath( );
        RequestSpecBuilder builder = new RequestSpecBuilder( );
        builder.setBaseUri( baseURI )
            .setPort( port )
            .setBasePath( basePath )
            .addHeader( "Origin", RestAssured.baseURI + ":" + RestAssured.port );
        this.requestSpec = builder.build( );
        RestAssured.basePath = basePath;
    }

    protected void shutdownNative() throws Exception
    {
        stopServer();
    }
}
