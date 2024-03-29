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

import org.apache.archiva.components.cache.Cache;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.Token;
import org.apache.archiva.redback.authentication.TokenBasedAuthenticationDataSource;
import org.apache.archiva.redback.authentication.jwt.JwtAuthenticator;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.integration.filter.authentication.HttpAuthenticator;
import org.apache.archiva.redback.integration.mail.Mailer;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.archiva.redback.keys.KeyManager;
import org.apache.archiva.redback.keys.KeyManagerException;
import org.apache.archiva.redback.keys.KeyNotFoundException;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.apache.archiva.redback.policy.PasswordEncoder;
import org.apache.archiva.redback.policy.PasswordRuleViolationException;
import org.apache.archiva.redback.policy.PasswordRuleViolations;
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.rest.api.MessageKeys;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.model.v2.Application;
import org.apache.archiva.redback.rest.api.model.v2.AvailabilityStatus;
import org.apache.archiva.redback.rest.api.model.v2.BaseRoleInfo;
import org.apache.archiva.redback.rest.api.model.v2.Operation;
import org.apache.archiva.redback.rest.api.model.v2.PasswordChange;
import org.apache.archiva.redback.rest.api.model.v2.Permission;
import org.apache.archiva.redback.rest.api.model.v2.PingResult;
import org.apache.archiva.redback.rest.api.model.v2.RegistrationKey;
import org.apache.archiva.redback.rest.api.model.v2.Resource;
import org.apache.archiva.redback.rest.api.model.v2.RoleInfo;
import org.apache.archiva.redback.rest.api.model.v2.RoleTree;
import org.apache.archiva.redback.rest.api.model.v2.SelfUserData;
import org.apache.archiva.redback.rest.api.model.v2.User;
import org.apache.archiva.redback.rest.api.model.v2.UserInfo;
import org.apache.archiva.redback.rest.api.model.v2.UserRegistrationRequest;
import org.apache.archiva.redback.rest.api.model.v2.VerificationStatus;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.api.services.v2.UserService;
import org.apache.archiva.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.apache.archiva.redback.rest.services.RedbackRequestInformation;
import org.apache.archiva.redback.rest.services.interceptors.RedbackPrincipal;
import org.apache.archiva.redback.rest.services.utils.PasswordValidator;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.role.model.ModelApplication;
import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service( "v2.userService#rest" )
public class DefaultUserService extends BaseRedbackService
    implements UserService
{

    private final Logger log = LoggerFactory.getLogger( getClass( ) );

    private static final String VALID_USERNAME_CHARS = "[a-zA-Z_0-9\\-.@]*";
    private static final String[] INVALID_CREATE_USER_NAMES = {"admin", "guest", "me"};


    private SecuritySystem securitySystem;

    @Inject
    @Named( value = "userConfiguration#default" )
    private UserConfiguration config;

    @Inject
    private JwtAuthenticator jwtAuthenticator;

    @Inject
    private RoleManager roleManager;

    /**
     * cache used for user assignments
     */
    @Inject
    @Named( value = "cache#userAssignments" )
    private Cache<String, ? extends UserAssignment> userAssignmentsCache;

    /**
     * cache used for user permissions
     */
    @Inject
    @Named( value = "cache#userPermissions" )
    private Cache<String, ? extends Permission> userPermissionsCache;

    /**
     * Cache used for users
     */
    @Inject
    @Named( value = "cache#users" )
    private Cache<String, ? extends User> usersCache;

    @Inject
    private Mailer mailer;


    @Inject
    @Named( value = "v2.roleService#rest" )
    private DefaultRoleService roleManagementService;

    private HttpAuthenticator httpAuthenticator;

    @Inject
    private PasswordValidator passwordValidator;

    @Context
    private HttpServletRequest httpServletRequest;

    @Context
    private HttpServletResponse httpServletResponse;

    @Context
    private UriInfo uriInfo;

    @Context
    private SecurityContext securityContext;

    @Inject
    public DefaultUserService(@Named( value = "rbacManager#default" ) RBACManager rbacManager,
                              @Named( value = "userManager#default" ) UserManager userManager ,
                               SecuritySystem securitySystem )
    {
        super( rbacManager, userManager );
        this.securitySystem = securitySystem;
    }

    RedbackPrincipal getPrincipal( )
    {
        if ( this.securityContext != null )
        {
            Principal pri = this.securityContext.getUserPrincipal( );
            if ( pri != null && pri instanceof RedbackPrincipal )
            {
                return (RedbackPrincipal) pri;
            }
        }
        return null;
    }

    @Override
    public UserInfo createUser( User user )
        throws RedbackServiceException
    {
        if ( user == null )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_ID_EMPTY ), 422 );
        }
        UserInfo result;
        if ( Arrays.binarySearch( INVALID_CREATE_USER_NAMES, user.getUserId( ) ) >= 0 )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_ID_INVALID, user.getUserId( ) ), 422 );
        }

        try
        {
            org.apache.archiva.redback.users.User u = userManager.findUser( user.getUserId( ) );
            if ( u != null )
            {
                httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePathBuilder( ).path( u.getUsername( ) ).build( ).toString( ) );
                throw new RedbackServiceException(
                    ErrorMessage.of( MessageKeys.ERR_USER_EXISTS, user.getUserId( ) ), 303 );
            }
        }
        catch ( UserNotFoundException e )
        {
            //ignore we just want to prevent non human readable error message from backend :-)
            log.debug( "User {} does not exist", user.getUserId( ) );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_UNKNOWN, e.getMessage( ) ) );
        }

        // data validation
        if ( StringUtils.isEmpty( user.getUserId( ) ) )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_ID_EMPTY ), 422 );
        }

        if ( StringUtils.isEmpty( user.getFullName( ) ) )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_FULL_NAME_EMPTY ), 422 );
        }

        if ( StringUtils.isEmpty( user.getEmail( ) ) )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_EMAIL_EMPTY ), 422 );
        }

        try
        {

            org.apache.archiva.redback.users.User u =
                userManager.createUser( user.getUserId( ), user.getFullName( ), user.getEmail( ) );
            u.setPassword( user.getPassword( ) );
            u.setLocked( user.isLocked( ) );
            u.setPasswordChangeRequired( user.isPasswordChangeRequired( ) );
            u.setValidated( user.isValidated( ) );
            u = userManager.addUser( u );
            if ( !user.isPasswordChangeRequired( ) )
            {
                u.setPasswordChangeRequired( false );
                try
                {
                    u = userManager.updateUser( u );
                    log.debug( "User {} created", u.getUsername( ) );
                }
                catch ( UserNotFoundException e )
                {
                    throw new RedbackServiceException( e.getMessage( ) );
                }
            }

            roleManager.assignRole( RedbackRoleConstants.REGISTERED_USER_ROLE_ID, u.getUsername( ) );
            result = getRestUser( u );
            httpServletResponse.setStatus( 201 );
            httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePathBuilder( ).path( user.getUserId( ) ).build( ).toString( ) );
        }
        catch ( RoleManagerException rpe )
        {
            log.error( "RoleProfile Error: {}", rpe.getMessage( ), rpe );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_ASSIGN_ROLE ) );
        }
        catch ( UserManagerException e )
        {
            log.error( "UserManagerException: {}", e.getMessage( ), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_UNKNOWN, e.getMessage( ) ) );
        }
        return result;
    }

    @Override
    public void deleteUser( String userId )
        throws RedbackServiceException
    {
        if ( StringUtils.isEmpty( userId ) )
        {
            throw new RedbackServiceException( MessageKeys.ERR_USER_ID_EMPTY, 404 );
        }

        try
        {

            if ( rbacManager.userAssignmentExists( userId ) )
            {
                UserAssignment assignment = rbacManager.getUserAssignment( userId );
                rbacManager.removeUserAssignment( assignment );
            }

        }
        catch ( RbacManagerException e )
        {
            log.error( e.getMessage( ), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
        try
        {
            userManager.deleteUser( userId );
        }
        catch ( UserNotFoundException e )
        {
            log.error( e.getMessage( ), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_FOUND ), 404 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ) );
        }
        finally
        {
            removeFromCache( userId );
        }
        httpServletResponse.setStatus( 200 );
    }


    @Override
    public UserInfo getUser( String userId )
        throws RedbackServiceException
    {
        if ( StringUtils.isEmpty( userId ) )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_ID_EMPTY ), 404 );
        }
        try
        {
            if ( "guest".equals( userId ) )
            {
                return getRestUser( userManager.getGuestUser( ) );
            }
            org.apache.archiva.redback.users.User user = userManager.findUser( userId );
            return getRestUser( user );
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_FOUND ), 404 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage( ) ) );
        }
    }

    @Override
    public PagedResult<UserInfo> getUsers( String q, Integer offset,
                                           Integer limit, List<String> orderBy, String order )
        throws RedbackServiceException
    {
        boolean ascending = isAscending( order );
        try
        {
            // UserQuery does not work here, because the configurable user manager does only return the query for
            // the first user manager in the list. So we have to fetch the whole user list
            List<? extends org.apache.archiva.redback.users.User> rawUsers = userManager.getUsers( );
            return getUserInfoPagedResult( rawUsers, q, offset, limit, orderBy, ascending );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage( ) ) );
        }
    }


    @Override
    public UserInfo updateMe( SelfUserData user )
        throws RedbackServiceException
    {
        RedbackPrincipal principal = getPrincipal( );
        if ( principal == null )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_UNAUTHORIZED_REQUEST ), 401 );
        }

        // check oldPassword with the current one
        // only 3 fields to update
        // ui can limit to not update password
        org.apache.archiva.redback.users.User foundUser = updateUser( principal.getName( ), realUser -> {
            try
            {
                // current password is only needed, if password change is requested
                if ( StringUtils.isNotBlank( user.getPassword( ) ) )
                {
                    String previousEncodedPassword =
                        securitySystem.getUserManager( ).findUser( principal.getName( ), false ).getEncodedPassword( );

                    // check oldPassword with the current one

                    PasswordEncoder encoder = securitySystem.getPolicy( ).getPasswordEncoder( );

                    if ( !encoder.isPasswordValid( previousEncodedPassword, user.getCurrentPassword( ) ) )
                    {

                        return new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_BAD_PASSWORD ),
                            Response.Status.BAD_REQUEST.getStatusCode( ) );
                    }
                }
            }
            catch ( UserNotFoundException e )
            {
                return new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_FOUND ),
                    Response.Status.BAD_REQUEST.getStatusCode( ) );
            }
            catch ( UserManagerException e )
            {
                return new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ) );
            }
            // only 3 fields to update
            if ( StringUtils.isNotBlank( user.getFullName( ) ) )
            {
                realUser.setFullName( user.getFullName( ) );
            }
            if ( StringUtils.isNotBlank( user.getEmail( ) ) )
            {
                realUser.setEmail( user.getEmail( ) );
            }
            // ui can limit to not update password
            if ( StringUtils.isNotBlank( user.getPassword( ) ) )
            {
                realUser.setPassword( user.getPassword( ) );
            }
            return null;
        } );

        return getRestUser( foundUser );
    }

    @Override
    public UserInfo getLoggedInUser( )
        throws RedbackServiceException
    {
        RedbackPrincipal principal = getPrincipal( );
        if ( principal == null || principal.getUser()==null )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_UNAUTHORIZED_REQUEST ), 401 );
        }

        try
        {
            org.apache.archiva.redback.users.User foundUser = userManager.findUser( principal.getUser( ).getUsername( ), false );
            return getRestUser( foundUser );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ), 400 );
        }
    }

    @Override
    public UserInfo updateUser( String userId, User user )
        throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.users.User rawUser = userManager.findUser( userId, false );
            if ( user.getFullName( ) != null )
                rawUser.setFullName( user.getFullName( ) );
            if ( user.getEmail( ) != null )
                rawUser.setEmail( user.getEmail( ) );
            rawUser.setValidated( user.isValidated( ) );
            rawUser.setLocked( user.isLocked( ) );
            if ( !StringUtils.isEmpty( user.getPassword( ) ) )
                rawUser.setPassword( user.getPassword( ) );
            rawUser.setPasswordChangeRequired( user.isPasswordChangeRequired( ) );

            org.apache.archiva.redback.users.User updatedUser = userManager.updateUser( rawUser );

            return getRestUser( updatedUser );
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_FOUND ), 404 );
        }
        catch ( PasswordRuleViolationException e )
        {
            List<ErrorMessage> messages = e.getViolations( ).getViolations( ).stream( ).map( m -> ErrorMessage.of( m.getKey( ), m.getArgs( ) ) ).collect( Collectors.toList( ) );
            throw new RedbackServiceException( messages, 422 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ), 400 );
        }
    }

    @Override
    public Response removeFromCache( String userId )
        throws RedbackServiceException
    {
        if ( userAssignmentsCache != null )
        {
            userAssignmentsCache.remove( userId );
        }
        if ( userPermissionsCache != null )
        {
            userPermissionsCache.remove( userId );
        }
        if ( usersCache != null )
        {
            usersCache.remove( userId );
        }

        return Response.ok( ).build( );
    }

    @Override
    public PingResult ping( )
        throws RedbackServiceException
    {
        return new PingResult( true );
    }

    @Override
    public UserInfo createAdminUser( User adminUser )
        throws RedbackServiceException
    {
        UserInfo result;
        if ( getAdminStatus( ).isExists( ) )
        {
            log.warn( "Admin user exists already" );
            httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePath( ).toString( ) );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_ADMIN_EXISTS ), 303 );
        }
        log.debug( "Creating admin admin user '{}'", adminUser.getUserId( ) );
        if ( !RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME.equals( adminUser.getUserId( ) ) )
        {
            log.error( "Wrong admin user name {}", adminUser.getUserId( ) );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_ADMIN_BAD_NAME ), 422 );
        }

        try
        {
            org.apache.archiva.redback.users.User user =
                userManager.createUser( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME, adminUser.getFullName( ),
                    adminUser.getEmail( ) );
            user.setPassword( adminUser.getPassword( ) );

            user.setLocked( false );
            user.setPasswordChangeRequired( false );
            user.setPermanent( true );
            user.setValidated( true );

            userManager.addUser( user );
            result = getRestUser( user );
            roleManager.assignRole( "system-administrator", user.getUsername( ) );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_ROLEMANAGER_FAIL, e.getMessage( ) ), 400 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ), 400 );
        }
        httpServletResponse.setStatus( 201 );
        httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePath( ).toString( ) );
        return result;
    }

    @Override
    public AvailabilityStatus getAdminStatus( )
        throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.users.User user = userManager.findUser( config.getString( UserConfigurationKeys.DEFAULT_ADMIN ) );
            if ( user.getAccountCreationDate( ) != null )
            {
                return new AvailabilityStatus( true, user.getAccountCreationDate( ).toInstant( ) );
            }
            else
            {
                return new AvailabilityStatus( true );
            }
        }
        catch ( UserNotFoundException e )
        {
            // ignore
        }
        catch ( UserManagerException e )
        {
            Throwable cause = e.getCause( );

            if ( cause != null && cause instanceof UserNotFoundException )
            {
                return new AvailabilityStatus( false );
            }
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ), 400 );
        }
        return new AvailabilityStatus( false );
    }

    @Override
    public Response resetPassword( String userId )
        throws RedbackServiceException
    {
        String username = userId;
        if ( StringUtils.isEmpty( username ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "username.cannot.be.empty" ) );
        }

        UserManager userManager = securitySystem.getUserManager( );
        KeyManager keyManager = securitySystem.getKeyManager( );
        UserSecurityPolicy policy = securitySystem.getPolicy( );

        try
        {
            org.apache.archiva.redback.users.User user = userManager.findUser( username );

            AuthenticationKey authkey = keyManager.createKey( username, "Password Reset Request",
                policy.getUserValidationSettings( ).getEmailValidationTimeout( ) );

            String applicationUrl = getBaseUrl( );

            mailer.sendPasswordResetEmail( Arrays.asList( user.getEmail( ) ), authkey, applicationUrl );
            log.info( "password reset request for username {}", username );
        }
        catch ( UserNotFoundException e )
        {
            log.info( "Password Reset on non-existant user [{}].", username );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_FOUND, userId ), 404 );
        }
        catch ( KeyManagerException e )
        {
            log.info( "Unable to issue password reset.", e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_PASSWD_RESET_FAILED, e.getMessage( ) ), 400 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ), 400 );
        }

        return Response.ok( ).build( );
    }

    @Override
    public RegistrationKey registerUser( String userId, UserRegistrationRequest userRegistrationRequest )
        throws RedbackServiceException
    {
        User user = userRegistrationRequest.getUser( );
        if ( user == null )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_FOUND, userId ), 404 );

        }

        UserSecurityPolicy securityPolicy = securitySystem.getPolicy( );

        boolean emailValidationRequired = securityPolicy.getUserValidationSettings( ).isEmailValidationRequired( );

        if ( emailValidationRequired )
        {
            validateCredentialsLoose( user );
        }
        else
        {
            validateCredentialsStrict( user );
        }

        org.apache.archiva.redback.users.User u = null;

        try
        {

            // NOTE: Do not perform Password Rules Validation Here.

            if ( userManager.userExists( user.getUserId( ) ) )
            {
                throw new RedbackServiceException(
                    ErrorMessage.of( MessageKeys.ERR_USER_EXISTS, user.getUserId() ));
            }

            u = userManager.createUser( user.getUserId( ), user.getFullName( ), user.getEmail( ) );
            u.setPassword( user.getPassword( ) );
            u.setValidated( false );
            u.setLocked( false );

            roleManager.assignRole( RedbackRoleConstants.REGISTERED_USER_ROLE_ID, u.getUsername( ) );
        }
        catch ( RoleManagerException rpe )
        {
            log.error( "RoleProfile Error: {}", rpe.getMessage( ), rpe );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_REGISTRATION_ROLE_ASSIGNMENT_FAILED, rpe.getMessage( ) ), 400 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ), 400 );
        }

        if ( emailValidationRequired )
        {
            u.setLocked( true );

            try
            {
                AuthenticationKey authkey =
                    securitySystem.getKeyManager( ).createKey( u.getUsername( ), "New User Email Validation",
                        securityPolicy.getUserValidationSettings( ).getEmailValidationTimeout( ) );

                String baseUrl = userRegistrationRequest.getApplicationUrl( );
                if ( StringUtils.isBlank( baseUrl ) )
                {
                    baseUrl = getBaseUrl( );
                }

                log.debug( "register user {} with email {} and app url {}", u.getUsername( ), u.getEmail( ), baseUrl );

                mailer.sendAccountValidationEmail( Arrays.asList( u.getEmail( ) ), authkey, baseUrl );

                securityPolicy.setEnabled( false );
                userManager.addUser( u );
                return new RegistrationKey( authkey.getKey( ), true );

            }
            catch ( KeyManagerException e )
            {
                log.error( "Unable to register a new user.", e );
                throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_KEYMANAGER_FAIL, e.getMessage( ) ), 400 );
            }
            catch ( UserManagerException e )
            {
                throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ), 400 );
            }
            finally
            {
                securityPolicy.setEnabled( true );
            }
        }
        else
        {
            try
            {
                userManager.addUser( u );
                return new RegistrationKey( "-1", false );
            }
            catch ( UserManagerException e )
            {
                throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ), 400 );
            }
        }

        // FIXME log this event
        /*
        AuditEvent event = new AuditEvent( getText( "log.account.create" ) );
        event.setAffectedUser( username );
        event.log();
        */

    }


    @Override
    public Collection<Permission> getCurrentUserPermissions( )
        throws RedbackServiceException
    {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get( );
        String userName = UserManager.GUEST_USERNAME;
        if ( redbackRequestInformation != null && redbackRequestInformation.getUser( ) != null )
        {
            userName = redbackRequestInformation.getUser( ).getUsername( );
        }
        else
        {
            log.warn( "RedbackRequestInformation from ThreadLocal is null" );
        }

        return getUserPermissions( userName );
    }

    @Override
    public Collection<Operation> getCurrentUserOperations( )
        throws RedbackServiceException
    {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get( );
        String userName = UserManager.GUEST_USERNAME;
        if ( redbackRequestInformation != null && redbackRequestInformation.getUser( ) != null )
        {
            userName = redbackRequestInformation.getUser( ).getUsername( );
        }
        else
        {
            log.warn( "RedbackRequestInformation from ThreadLocal is null" );
        }

        return getUserOperations( userName );
    }

    @Override
    public VerificationStatus validateUserRegistration( String userId, String key ) throws RedbackServiceException
    {
        String principal = null;
        try
        {
            AuthenticationKey authkey = securitySystem.getKeyManager( ).findKey( key );

            org.apache.archiva.redback.users.User user =
                securitySystem.getUserManager( ).findUser( authkey.getForPrincipal( ) );

            if ( user.isValidated( ) )
            {
                throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_REGISTRATION_USER_VALIDATED ), 404 );
            }
            user.setValidated( true );
            user.setLocked( false );
            user.setPasswordChangeRequired( true );
            user.setEncodedPassword( "" );
            securitySystem.getUserManager( ).updateUser( user );
            principal = user.getUsername( );

            TokenBasedAuthenticationDataSource authsource = new TokenBasedAuthenticationDataSource( );
            authsource.setPrincipal( principal );
            authsource.setToken( authkey.getKey( ) );
            authsource.setEnforcePasswordChange( false );

            VerificationStatus status = new VerificationStatus( false );
            SecuritySession authStatus = securitySystem.authenticate( authsource );
            if ( authStatus.isAuthenticated( ) )
            {
                Token accessToken = jwtAuthenticator.generateToken( principal );
                status.setAccessToken( accessToken.getData( ) );
                status.setSuccess( true );
            }
            else
            {
                user.setValidated( false );
                user.setLocked( true );
                user.setPasswordChangeRequired( false );
                securitySystem.getUserManager( ).updateUser( user );
            }

            log.info( "account validated for user {}", user.getUsername( ) );

            return status;
        }
        catch ( MustChangePasswordException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_PASSWORD_CHANGE_REQUIRED ), Response.Status.FORBIDDEN.getStatusCode( ) );
        }
        catch ( AccountLockedException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_ACCOUNT_LOCKED ), Response.Status.FORBIDDEN.getStatusCode( ) );
        }
        catch ( AuthenticationException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_INVALID_CREDENTIALS ), Response.Status.FORBIDDEN.getStatusCode( ) );
        }
        catch ( KeyNotFoundException e )
        {
            log.info( "Invalid key requested: {}", key );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_REGISTRATION_KEY_INVALID ), 404 );
        }
        catch ( KeyManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_KEYMANAGER_FAIL, e.getMessage( ) ), 400 );

        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_FOUND, principal ), 404 );

        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ), 400 );
        }
    }

    @Override
    public List<RoleInfo> getEffectivelyAssignedRoles( String username ) throws RedbackServiceException
    {
        try
        {
            return rbacManager.getEffectivelyAssignedRoles( username ).stream( )
                .filter( org.apache.archiva.redback.rbac.Role::isAssignable )
                .map( this::getRoleInfoOptional )
                .filter( Optional::isPresent )
                .map(Optional::get).collect( Collectors.toList());
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
    }

    private static final Application toApplication( ModelApplication app )
    {
        Application application = new Application( );
        application.setId( app.getId( ) );
        application.setVersion( app.getVersion( ) );
        application.setDescription( app.getDescription( ) == null ? "" :app.getDescription() );
        application.setLongDescription( app.getLongDescription( ) == null ? "" : app.getLongDescription( ) );
        return application;
    }

    private List<Application> getAllApplications( )
    {
        return roleManager.getModel( ).getApplications( ).stream( ).map( DefaultUserService::toApplication ).collect( Collectors.toList( ) );
    }


    @Override
    public RoleTree getRoleTree( final String username ) throws RedbackServiceException
    {
        final Map<String, String> roleApplicationMap = roleManager.getModel( ).getApplications( ).stream( )
            .flatMap( modelApplication -> modelApplication.getRoles( ).stream( ).map( role -> {
                BaseRoleInfo roleInfo = new BaseRoleInfo( );
                roleInfo.setId( role.getId( ) );
                roleInfo.setApplicationId( modelApplication.getId( ) );
                return roleInfo;
            } ) ).collect( Collectors.toMap( BaseRoleInfo::getId, BaseRoleInfo::getApplicationId ) );

        try
        {
            final Set<String> assignedRoleIds = new HashSet( rbacManager.getUserAssignment( username ).getRoleIds( ) );
            // We have to reuse the BaseRoleInfo objects, because the roles are not returned starting from the roots
            final Map<String, BaseRoleInfo> roleIdCache = new HashMap<>( );
            List<BaseRoleInfo> roleList = rbacManager.getAllRoles( ).stream( ).flatMap( this::flattenRole ).map( role ->
            {
                BaseRoleInfo roleInfo = roleIdCache.computeIfAbsent( role.getId( ), s -> new BaseRoleInfo( ) );
                // Setting the role data, as there may be child role objects that are not completely initialized
                roleInfo = BaseRoleInfo.of( role, roleInfo );
                roleInfo.setApplicationId( roleApplicationMap.get( role.getId( ) ) );
                roleInfo.setAssigned( assignedRoleIds.contains( role.getId( ) ) );
                roleInfo.setChildren( role.getChildRoleIds( ).stream( )
                    .map( roleId ->
                    {
                        BaseRoleInfo childRoleInfo = roleIdCache.computeIfAbsent( roleId, s -> BaseRoleInfo.ofId( roleId ) );
                        childRoleInfo.setChild( true );
                        return childRoleInfo;
                    } )
                    .collect( Collectors.toList( ) ) );
                return roleInfo;
            } ).collect( Collectors.toList( ) );
            RoleTree roleTree = new RoleTree( );
            roleTree.setApplications( getAllApplications( ).stream( ).collect( Collectors.toMap( Application::getId, Function.identity( ) ) ) );
            roleTree.setRootRoles( roleList.stream( ).filter( BaseRoleInfo::isNotChild ).collect( Collectors.toList( ) ) );
            roleTree.setUserId( username );
            return roleTree;
        }
        catch ( RbacManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
    }

    @Override
    public Response changePasswordUnauthenticated( String userId, PasswordChange passwordChange ) throws RedbackServiceException
    {
        changeUserPassword( userId, passwordChange );
        return Response.ok( ).build( );
    }

    @Override
    public Response changePassword( PasswordChange passwordChange ) throws RedbackServiceException
    {
        RedbackPrincipal principal = getPrincipal( );
        if ( principal == null )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_UNAUTHORIZED_REQUEST ), 401 );
        }
        String userId = principal.getUser( ).getUsername( );
        changeUserPassword( userId, passwordChange );
        return Response.ok( ).build( );
    }

    private List<ErrorMessage> getPasswordViolationMessages( PasswordRuleViolationException e )
    {
        PasswordRuleViolations violations = e.getViolations( );
        List<ErrorMessage> errorMessages = new ArrayList<>( violations.getViolations( ).size( ) );
        if ( violations != null )
        {
            for ( PasswordRuleViolations.MessageReference violation : violations.getViolations() )
            {
                errorMessages.add( new ErrorMessage( violation.getKey(), violation.getArgs() ) );
            }
        }
        return errorMessages;
    }

    private void changeUserPassword(final String userId, final PasswordChange passwordChange) throws RedbackServiceException
    {
        if ( StringUtils.isEmpty( passwordChange.getCurrentPassword() ) )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_PASSWORDCHANGE_CURRENT_EMPTY ), 400 );
        }
        if ( passwordChange.getUserId( ) == null || ( !passwordChange.getUserId( ).equals( userId ) ) )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_ID_INVALID ), 403 );
        }

        if ( StringUtils.isEmpty( passwordChange.getNewPassword() ) )
        {
            throw new RedbackServiceException( ErrorMessage.of(MessageKeys.ERR_PASSWORDCHANGE_NEW_EMPTY), 400 );
        }
        if ( StringUtils.isEmpty( passwordChange.getNewPasswordConfirmation() ) )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_PASSWORDCHANGE_CONFIRMATION_EMPTY ),
                400 );
        }
        if ( !StringUtils.equals( passwordChange.getNewPassword(), passwordChange.getNewPasswordConfirmation() ) )
        {
            throw new RedbackServiceException(ErrorMessage.of( MessageKeys.ERR_PASSWORDCHANGE_BAD_CONFIRMATION ),
                403 );
        }

        try
        {
            org.apache.archiva.redback.users.User u = securitySystem.getUserManager().findUser( userId );

            String previousEncodedPassword = u.getEncodedPassword();

            // check oldPassword with the current one

            PasswordEncoder encoder = securitySystem.getPolicy().getPasswordEncoder();

            if ( !encoder.isPasswordValid( previousEncodedPassword, passwordChange.getCurrentPassword() ) )
            {

                throw new RedbackServiceException( MessageKeys.ERR_AUTH_INVALID_CREDENTIALS,
                    401 );
            }

            u.setPassword( passwordChange.getNewPassword() );
            securitySystem.getUserManager().updateUser( u );
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_FOUND ),
                400 );
        }
        catch ( UserManagerException e )
        {
            log.info( "UserManagerException: {}", e.getMessage() );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage() ) );
        }
        catch ( PasswordRuleViolationException e )
        {
            throw new RedbackServiceException( getPasswordViolationMessages( e ), 401 );
        }


    }

    private Stream<Role> flattenRole( Role role )
    {
        return Stream.concat( Stream.of( role ), this.getChildren( role ).flatMap( this::flattenRole ) ).distinct( );
    }

    private Stream<? extends Role> getChildren( Role role )
    {
        try
        {
            return rbacManager.getChildRoleNames( role ).values( ).stream( );
        }
        catch ( RbacManagerException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public Collection<Operation> getUserOperations( String userName )
        throws RedbackServiceException
    {
        Collection<Permission> permissions = getUserPermissions( userName );
        List<Operation> operations = new ArrayList<>( permissions.size( ) );
        for ( Permission permission : permissions )
        {
            if ( permission.getOperation( ) != null )
            {
                Operation operation = new Operation( );
                operation.setName( permission.getOperation( ).getName( ) );
                operations.add( operation );
            }
        }
        return operations;
    }

    @Override
    public Collection<Permission> getUserPermissions( String userName )
        throws RedbackServiceException
    {
        try
        {
            Set<? extends org.apache.archiva.redback.rbac.Permission> permissions =
                rbacManager.getAssignedPermissions( userName );
            // FIXME return guest permissions !!
            List<Permission> userPermissions = new ArrayList<>( permissions.size( ) );
            for ( org.apache.archiva.redback.rbac.Permission p : permissions )
            {
                Permission permission = new Permission( );
                permission.setName( p.getName( ) );

                if ( p.getOperation( ) != null )
                {
                    Operation operation = new Operation( );
                    operation.setName( p.getOperation( ).getName( ) );
                    permission.setOperation( operation );
                }

                if ( p.getResource( ) != null )
                {
                    Resource resource = new Resource( );
                    resource.setIdentifier( p.getResource( ).getIdentifier( ) );
                    resource.setPattern( p.getResource( ).isPattern( ) );
                    permission.setResource( resource );
                }

                userPermissions.add( permission );
            }
            return userPermissions;
        }
        catch ( RbacManagerException e )
        {
            log.error( e.getMessage( ), e );
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_RBACMANAGER_FAIL, e.getMessage( ) ), 400 );
        }
    }

    @Override
    public Collection<Permission> getGuestPermissions( ) throws RedbackServiceException
    {
        return getUserPermissions( UserManager.GUEST_USERNAME );
    }

    public void validateCredentialsLoose( User user )
        throws RedbackServiceException
    {
        RedbackServiceException redbackServiceException =
            new RedbackServiceException( "issues during validating user", 422 );
        if ( StringUtils.isEmpty( user.getUserId( ) ) )
        {
            redbackServiceException.addErrorMessage( ErrorMessage.of( MessageKeys.ERR_USER_ID_EMPTY ) );
        }
        else
        {
            if ( !user.getUserId( ).matches( VALID_USERNAME_CHARS ) )
            {
                redbackServiceException.addErrorMessage( ErrorMessage.of( MessageKeys.ERR_USER_ID_INVALID ) );
            }
        }

        if ( StringUtils.isEmpty( user.getFullName( ) ) )
        {
            redbackServiceException.addErrorMessage( ErrorMessage.of( MessageKeys.ERR_USER_FULL_NAME_EMPTY ) );
        }

        if ( StringUtils.isEmpty( user.getEmail( ) ) )
        {
            redbackServiceException.addErrorMessage( ErrorMessage.of( MessageKeys.ERR_USER_EMAIL_EMPTY ) );
        }

        if ( !StringUtils.equals( user.getPassword( ), user.getConfirmPassword( ) ) )
        {
            redbackServiceException.addErrorMessage( ErrorMessage.of( MessageKeys.ERR_AUTH_INVALID_CREDENTIALS, "nomatch" ) );
        }

        try
        {
            if ( !StringUtils.isEmpty( user.getEmail( ) ) )
            {
                new InternetAddress( user.getEmail( ), true );
            }
        }
        catch ( AddressException e )
        {
            redbackServiceException.addErrorMessage( ErrorMessage.of( MessageKeys.ERR_USER_EMAIL_INVALID ) );
        }
        if ( !redbackServiceException.getErrorMessages( ).isEmpty( ) )
        {
            throw redbackServiceException;
        }
    }

    public void validateCredentialsStrict( User user )
        throws RedbackServiceException
    {
        validateCredentialsLoose( user );
        try
        {
            org.apache.archiva.redback.users.User tmpuser =
                userManager.createUser( user.getUserId( ), user.getFullName( ), user.getEmail( ) );

            user.setPassword( user.getPassword( ) );

            securitySystem.getPolicy( ).validatePassword( tmpuser );

            if ( ( StringUtils.isEmpty( user.getPassword( ) ) ) )
            {
                throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_INVALID_CREDENTIALS, "empty" ) );
            }
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_AUTH_INVALID_CREDENTIALS, e.getMessage( ) ) );
        }
    }

    private String getBaseUrl( )
    {
        if ( httpServletRequest != null )
        {
            if ( httpServletRequest != null )
            {
                return httpServletRequest.getScheme( ) + "://" + httpServletRequest.getServerName( ) + (
                    httpServletRequest.getServerPort( ) == 80
                        ? ""
                        : ":" + httpServletRequest.getServerPort( ) ) + httpServletRequest.getContextPath( );
            }
        }
        return null;
    }


    private org.apache.archiva.redback.users.User updateUser( String userId, Function<org.apache.archiva.redback.users.User, RedbackServiceException> updateFunction ) throws RedbackServiceException
    {

        try
        {
            org.apache.archiva.redback.users.User rawUser = userManager.findUser( userId, false );
            if ( rawUser != null )
            {
                RedbackServiceException result = updateFunction.apply( rawUser );
                if ( result != null )
                {
                    throw result;
                }
                userManager.updateUser( rawUser, false );
            }
            else
            {
                throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_FOUND, userId ), 404 );
            }
            return rawUser;
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USER_NOT_FOUND ), 404 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( MessageKeys.ERR_USERMANAGER_FAIL, e.getMessage( ) ) );
        }
    }

    @Override
    public void unlockUser( String userId )
        throws RedbackServiceException
    {
        updateUser( userId, user -> {
            user.setLocked( false );
            return null;
        } );
        httpServletResponse.setStatus( 200 );
    }

    @Override
    public void lockUser( String userId )
        throws RedbackServiceException
    {
        updateUser( userId, user -> {
            user.setLocked( true );
            return null;
        } );
        httpServletResponse.setStatus( 200 );
    }

    @Override
    public void setRequirePasswordChangeFlag( String userId )
        throws RedbackServiceException
    {
        updateUser( userId, user -> {
            user.setPasswordChangeRequired( true );
            return null;
        } );
        httpServletResponse.setStatus( 200 );
    }

    @Override
    public void clearRequirePasswordChangeFlag( String userId )
        throws RedbackServiceException
    {
        updateUser( userId, user -> {
            user.setPasswordChangeRequired( false );
            return null;
        } );
        httpServletResponse.setStatus( 200 );
    }

}
