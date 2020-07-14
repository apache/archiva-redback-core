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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.apache.archiva.redback.authentication.Token;
import org.apache.archiva.redback.authentication.jwt.JwtAuthenticator;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.services.v2.AuthenticationService;
import org.apache.archiva.redback.rest.services.BaseSetup;
import org.apache.archiva.redback.rest.services.FakeCreateAdminService;
import org.apache.archiva.redback.rest.services.FakeCreateAdminServiceImpl;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.commons.lang3.SystemUtils;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.ContextLoaderListener;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Olivier Lamy
 */
@ExtendWith( SpringExtension.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public abstract class AbstractRestServicesTestV2
{

    private JwtAuthenticator jwtAuthenticator;
    private UserManager userManager;

    protected Logger log = LoggerFactory.getLogger( getClass() );

    private static AtomicReference<Server> server = new AtomicReference<>();
    private static AtomicReference<ServerConnector> serverConnector = new AtomicReference<>();
    private RoleManager roleManager;

    protected void init() {
    }

    protected void destroy() {
        this.jwtAuthenticator = null;
        this.userManager = null;
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

    public JwtAuthenticator getJwtAuthenticator() {
        if (this.jwtAuthenticator == null) {
            JwtAuthenticator auth = ContextLoaderListener.getCurrentWebApplicationContext( )
                .getBean( JwtAuthenticator.class );
            assertNotNull( auth );
            this.jwtAuthenticator = auth;
        }
        return this.jwtAuthenticator;
    }

    public UserManager getUserManager() {
        if (this.userManager==null) {
            UserManager userManager = ContextLoaderListener.getCurrentWebApplicationContext( )
                .getBean( "userManager#default", UserManager.class );
            assertNotNull( userManager );
            this.userManager = userManager;
        }
        return this.userManager;
    }

    public RoleManager getRoleManager() {
        if (this.roleManager==null) {
            RoleManager roleManager = ContextLoaderListener.getCurrentWebApplicationContext( )
                .getBean( "roleManager", RoleManager.class );
            assertNotNull( roleManager );
            this.roleManager = roleManager;
        }
        return this.roleManager;
    }

    JacksonJaxbJsonProvider getJsonProvider() {
        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider( );
        ObjectMapper mapper = new ObjectMapper( );
        mapper.registerModule( new JavaTimeModule( ) );
        mapper.setAnnotationIntrospector( new JaxbAnnotationIntrospector( mapper.getTypeFactory() ) );
        mapper.setDateFormat( new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ) );
        provider.setMapper( mapper );
        return provider;
    }

    protected boolean exists( DirContext context, String dn )
    {
        Object result = null;
        try {
            result = context.lookup( dn );
        }
        catch ( NameNotFoundException e ) {
            return false;
        }
        catch ( NamingException e )
        {
            log.error( "Unknown error during lookup: {}", e.getMessage( ) );
        }
        return result != null;
    }

    protected void deleteUser(User user) {
        if (user!=null)
        {
            deleteUser( user.getUsername( ) );
        }
    }

    protected User addUser( String userId, String password, String fullName, String email ) throws UserManagerException
    {
        return addUser( userId, password, fullName, email, null );
    }
    protected User addUser( String userId, String password, String fullName, String email, Consumer<User> updateFunction ) throws UserManagerException
    {
        UserManager um = getUserManager( );
        User user = um.createUser( userId, fullName, email );
        user.setPassword( password );
        user.setPermanent( false );
        user.setPasswordChangeRequired( false );
        user.setLocked( false );
        user.setValidated( true );
        user = um.addUser( user );
        // We need this additional round, because new users have the password change flag set to true
        user.setPasswordChangeRequired( false );
        if (updateFunction!=null) {
            updateFunction.accept( user );
        }
        um.updateUser( user );
        return user;
    }

    protected void deleteUser(String userName) {
        if (userName!=null)
        {
            try
            {
                getUserManager( ).deleteUser( userName );
            }
            catch ( UserNotFoundException e )
            {
                // ignore
            }
            catch ( UserManagerException e )
            {
                log.error( "Could not delete user {}", userName );
            }
        }
    }

    /**
     * Returns true, if the server does exist and is running.
     * @return true, if server does exist and is running.
     */
    public boolean isServerRunning() {
        return this.server.get() != null && this.server.get().isRunning();
    }

    /**
     * Returns the timeout in ms for rest requests. The timeout can be set by
     * the system property <code>rest.test.timeout</code>.
     * @return The timeout value in ms.
     */
    public long getTimeout()
    {
        return Long.getLong( "rest.test.timeout", 1000000 );
    }

    public static String encode( String uid, String password )
    {
        return "Basic " + Base64Utility.encode( ( uid + ":" + password ).getBytes() );
    }

    public String getAdminAuthzHeader()
    {
        assertNotNull( getJwtAuthenticator());
        String adminUser = RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME;
        Token token = getJwtAuthenticator().generateToken( adminUser );
        return "Bearer " + token.getData( );
    }

    public String getAuthHeader(String userId) {
        assertNotNull( getJwtAuthenticator() );
        Token token = getJwtAuthenticator().generateToken( userId );
        return "Bearer " + token.getData( );
    }

    protected String getSpringConfigLocation()
    {
        return "classpath*:spring-context.xml,classpath*:META-INF/spring-context.xml";
    }


    protected String getRestServicesPath()
    {
        return "api";
    }

    public void startServer()
        throws Exception
    {
        log.info("Starting server");
        Server myServer = new Server();
        this.server.set(myServer);
        this.serverConnector.set(new ServerConnector( myServer, new HttpConnectionFactory()));
        myServer.addConnector(serverConnector.get());

        ServletHolder servletHolder = new ServletHolder( new CXFServlet() );
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setResourceBase( SystemUtils.JAVA_IO_TMPDIR );
        context.setSessionHandler( new SessionHandler(  ) );
        context.addServlet( servletHolder, "/" + getRestServicesPath() + "/*" );
        context.setInitParameter( "contextConfigLocation", getSpringConfigLocation() );
        context.addEventListener(new ContextLoaderListener());

        getServer().setHandler( context );
        getServer().start();

        if (log.isDebugEnabled())
        {
            log.debug( "Jetty dump: {}", getServer().dump() );
        }

        log.info( "Started server on port {}", getServerPort() );

        UserManager um = getUserManager( );

        User adminUser = null;
        try
        {
            adminUser = um.findUser( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME );
        } catch ( UserNotFoundException e ) {
            // ignore
        }
        adminUser = um.createUser( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME, "Administrator", "admin@local.home" );
        adminUser.setUsername( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME );
        adminUser.setPassword( BaseSetup.getAdminPwd() );
        adminUser.setFullName( "the admin user" );
        adminUser.setEmail( "toto@toto.fr" );
        adminUser.setPermanent( true );
        adminUser.setValidated( true );
        adminUser.setLocked( false );
        adminUser.setPasswordChangeRequired( false );
        if (adminUser==null)
        {
            um.addUser( adminUser );
            getRoleManager( ).assignRole( "system-administrator", adminUser.getUsername( ) );
        } else {
            um.updateUser( adminUser, false );
            getRoleManager( ).assignRole( "system-administrator", adminUser.getUsername( ) );
        }

        FakeCreateAdminService fakeCreateAdminService = getFakeCreateAdminService();
        this.jwtAuthenticator = null;

        //assertTrue( res.booleanValue() );

    }

    protected FakeCreateAdminService getFakeCreateAdminService()
    {
        return JAXRSClientFactory.create(
            "http://localhost:" + getServerPort()+ "/" + getRestServicesPath() + "/fakeCreateAdminService/",
            FakeCreateAdminService.class, Collections.singletonList( getJsonProvider() ) );
    }

    public void stopServer()
        throws Exception
    {
        if ( getServer() != null )
        {
            log.info("Stopping server");
            getServer().stop();
        }
    }

    protected AuthenticationService getLoginServiceV2( String authzHeader )
    {
        AuthenticationService service =
            JAXRSClientFactory.create( "http://localhost:" + getServerPort() + "/" + getRestServicesPath() + "/v2/redback/",
                AuthenticationService.class, Collections.singletonList( getJsonProvider() ) );

        // for debuging purpose
        WebClient.getConfig( service ).getHttpConduit().getClient().setReceiveTimeout( getTimeout() );

        if ( authzHeader != null )
        {
            WebClient.client( service ).header( "Authorization", authzHeader );
        }
        WebClient.client(service).header("Referer","http://localhost:"+getServerPort());

        WebClient.client( service ).accept( MediaType.APPLICATION_JSON_TYPE );
        WebClient.client( service ).type( MediaType.APPLICATION_JSON_TYPE );

        return service;
    }



}
