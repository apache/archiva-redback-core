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

import net.sf.ehcache.CacheManager;
import org.apache.archiva.components.cache.Cache;
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
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.UserAssignment;
import org.apache.archiva.redback.rest.api.Constants;
import org.apache.archiva.redback.rest.api.model.ActionStatus;
import org.apache.archiva.redback.rest.api.model.v2.AvailabilityStatus;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.model.Operation;
import org.apache.archiva.redback.rest.api.model.PasswordStatus;
import org.apache.archiva.redback.rest.api.model.Permission;
import org.apache.archiva.redback.rest.api.model.v2.RegistrationKey;
import org.apache.archiva.redback.rest.api.model.ResetPasswordRequest;
import org.apache.archiva.redback.rest.api.model.Resource;
import org.apache.archiva.redback.rest.api.model.VerificationStatus;
import org.apache.archiva.redback.rest.api.model.v2.PagedResult;
import org.apache.archiva.redback.rest.api.model.v2.PingResult;
import org.apache.archiva.redback.rest.api.model.v2.User;
import org.apache.archiva.redback.rest.api.model.v2.UserRegistrationRequest;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;
import org.apache.archiva.redback.rest.api.services.v2.UserService;
import org.apache.archiva.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.apache.archiva.redback.rest.services.RedbackRequestInformation;
import org.apache.archiva.redback.rest.services.utils.PasswordValidator;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.role.RoleManagerException;
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
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.archiva.redback.rest.api.Constants.*;

@Service( "v2.userService#rest" )
public class DefaultUserService
    implements UserService
{

    private final Logger log = LoggerFactory.getLogger( getClass() );

    private static final String VALID_USERNAME_CHARS = "[a-zA-Z_0-9\\-.@]*";
    private static final String[] INVALID_USER_NAMES = { "me" };

    private UserManager userManager;

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
    @Named( value = "rbacManager#default" )
    private RBACManager rbacManager;

    private HttpAuthenticator httpAuthenticator;

    @Inject
    private PasswordValidator passwordValidator;

    @Context
    private HttpServletRequest httpServletRequest;

    @Context
    private HttpServletResponse httpServletResponse;

    @Context
    private UriInfo uriInfo;

    @Inject
    public DefaultUserService( @Named( value = "userManager#default" ) UserManager userManager,
                               SecuritySystem securitySystem )
    {
        this.userManager = userManager;
        this.securitySystem = securitySystem;
    }


    @Override
    public User createUser( User user )
        throws RedbackServiceException
    {
        User result;
        if ( Arrays.binarySearch( INVALID_USER_NAMES, user.getUserId( ) ) >=0 )
        {
            throw new RedbackServiceException( ErrorMessage.of( ERR_USER_ID_INVALID, user.getUserId() ), 405 );
        }

        try
        {
            org.apache.archiva.redback.users.User u = userManager.findUser( user.getUserId() );
            if ( u != null )
            {
                httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePathBuilder( ).path( u.getUsername( ) ).build( ).toString( ) );
                throw new RedbackServiceException(
                    ErrorMessage.of( ERR_USER_EXISTS, user.getUserId() ), 303 );
            }
        }
        catch ( UserNotFoundException e )
        {
            //ignore we just want to prevent non human readable error message from backend :-)
            log.debug( "user {} not exists", user.getUserId() );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( ERR_UNKNOWN, e.getMessage() ) );
        }

        // data validation
        if ( StringUtils.isEmpty( user.getUserId() ) )
        {
            throw new RedbackServiceException( ErrorMessage.of( ERR_USER_ID_EMPTY ), 405 );
        }

        if ( StringUtils.isEmpty( user.getFullName() ) )
        {
            throw new RedbackServiceException( ErrorMessage.of( ERR_USER_FULL_NAME_EMPTY ), 405 );
        }

        if ( StringUtils.isEmpty( user.getEmail() ) )
        {
            throw new RedbackServiceException( ErrorMessage.of( ERR_USER_EMAIL_EMPTY ), 405 );
        }

        try
        {

            org.apache.archiva.redback.users.User u =
                userManager.createUser( user.getUserId(), user.getFullName(), user.getEmail() );
            u.setPassword( user.getPassword() );
            u.setLocked( user.isLocked() );
            u.setPasswordChangeRequired( user.isPasswordChangeRequired() );
            u.setPermanent( user.isPermanent() );
            u.setValidated( user.isValidated() );
            u = userManager.addUser( u );
            if ( !user.isPasswordChangeRequired() )
            {
                u.setPasswordChangeRequired( false );
                try
                {
                    u = userManager.updateUser( u );
                    log.debug( "user {} created", u.getUsername() );
                }
                catch ( UserNotFoundException e )
                {
                    throw new RedbackServiceException( e.getMessage() );
                }
            }

            roleManager.assignRole( RedbackRoleConstants.REGISTERED_USER_ROLE_ID, u.getUsername() );
            result = getRestUser( u );
            httpServletResponse.setStatus( 201 );
            httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePathBuilder().path( user.getUserId() ).build(  ).toString() );
        }
        catch ( RoleManagerException rpe )
        {
            log.error( "RoleProfile Error: {}", rpe.getMessage(), rpe );
            throw new RedbackServiceException( ErrorMessage.of(ERR_USER_ASSIGN_ROLE ) );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of(ERR_UNKNOWN,  e.getMessage() ) );
        }
        return result;
    }

    @Override
    public void deleteUser( String userId )
        throws RedbackServiceException
    {

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
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( ErrorMessage.of( ERR_RBACMANAGER_FAIL, e.getMessage( ) ) );
        }
        try
        {
            userManager.deleteUser( userId );
        }
        catch ( UserNotFoundException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( ErrorMessage.of( ERR_USER_NOT_FOUND ), 404 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( ERR_USERMANAGER_FAIL, e.getMessage( ) ) );
        }
        finally
        {
            removeFromCache( userId );
        }
        httpServletResponse.setStatus( 200 );
    }


    @Override
    public User getUser( String userId )
        throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.users.User user = userManager.findUser( userId );
            return getRestUser( user );
        }
        catch ( UserNotFoundException e )
        {
            return null;
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }
    }

    @Override
    public PagedResult<User> getUsers(Integer offset,
                                      Integer limit)
        throws RedbackServiceException
    {
        try
        {
            List<? extends org.apache.archiva.redback.users.User> users = userManager.getUsers();
            if (offset>=users.size()) {
                return new PagedResult<>( users.size( ), offset, limit, Collections.emptyList( ) );
            }
            int endIndex = PagingHelper.getLastIndex( offset, limit, users.size( ) );
            List<? extends org.apache.archiva.redback.users.User> resultList = users.subList( offset, endIndex );
            List<User> simpleUsers = new ArrayList<>( resultList.size() );

            for ( org.apache.archiva.redback.users.User user : resultList )
            {
                simpleUsers.add( getRestUser( user ) );
            }
            return new PagedResult<>( users.size( ), offset, limit, simpleUsers );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }
    }

    @Override
    public ActionStatus updateMe( String userId, User user )
        throws RedbackServiceException
    {
        // check username == one in the session
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();
        if ( redbackRequestInformation == null || redbackRequestInformation.getUser() == null )
        {
            log.warn( "RedbackRequestInformation from ThreadLocal is null" );
            throw new RedbackServiceException( new ErrorMessage( "you must be logged to update your profile" ),
                                               Response.Status.FORBIDDEN.getStatusCode() );
        }
        if ( user == null )
        {
            throw new RedbackServiceException( new ErrorMessage( "user parameter is mandatory" ),
                                               Response.Status.BAD_REQUEST.getStatusCode() );
        }
        if ( !StringUtils.equals( redbackRequestInformation.getUser().getUsername(), user.getUserId() ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "you can update only your profile" ),
                                               Response.Status.FORBIDDEN.getStatusCode() );
        }

        if ( StringUtils.isEmpty( user.getPreviousPassword() ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "previous password is empty" ),
                                               Response.Status.BAD_REQUEST.getStatusCode() );
        }

        User realUser = getUser( user.getUserId() );
        try
        {
            String previousEncodedPassword =
                securitySystem.getUserManager().findUser( user.getUserId(), false ).getEncodedPassword();

            // check oldPassword with the current one

            PasswordEncoder encoder = securitySystem.getPolicy().getPasswordEncoder();

            if ( !encoder.isPasswordValid( previousEncodedPassword, user.getPreviousPassword() ) )
            {

                throw new RedbackServiceException( new ErrorMessage( "password.provided.does.not.match.existing" ),
                                                   Response.Status.BAD_REQUEST.getStatusCode() );
            }
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( new ErrorMessage( "user not found" ),
                                               Response.Status.BAD_REQUEST.getStatusCode() );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }
        // only 3 fields to update
        realUser.setFullName( user.getFullName() );
        realUser.setEmail( user.getEmail() );
        // ui can limit to not update password
        if ( StringUtils.isNotBlank( user.getPassword() ) )
        {
            passwordValidator.validatePassword( user.getPassword(), user.getUserId() );

            realUser.setPassword( user.getPassword() );
        }

        updateUser( realUser.getUserId(), realUser );

        return ActionStatus.SUCCESS;
    }

    @Override
    public User updateUser( String userId,  User user )
        throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.users.User rawUser = userManager.findUser( userId, false );
            if (user.getFullName()!=null)
                rawUser.setFullName( user.getFullName() );
            if (user.getEmail()!=null)
                rawUser.setEmail( user.getEmail() );
            rawUser.setValidated( user.isValidated() );
            rawUser.setLocked( user.isLocked() );
            if ( !StringUtils.isEmpty( user.getPassword( ) ) )
                rawUser.setPassword( user.getPassword() );
            rawUser.setPasswordChangeRequired( user.isPasswordChangeRequired() );
            rawUser.setPermanent( user.isPermanent() );

            org.apache.archiva.redback.users.User updatedUser = userManager.updateUser( rawUser );

            return getRestUser( updatedUser );
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( ERR_USER_NOT_FOUND ), 404 );
        } catch ( PasswordRuleViolationException e ) {
            List<ErrorMessage> messages = e.getViolations( ).getViolations( ).stream( ).map( m -> ErrorMessage.of( m.getKey( ), m.getArgs( ) ) ).collect( Collectors.toList() );
            throw new RedbackServiceException( messages, 422 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }
    }

    @Override
    public ActionStatus removeFromCache( String userId )
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

        CacheManager cacheManager = CacheManager.getInstance();
        String[] caches = cacheManager.getCacheNames();
        for ( String cacheName : caches )
        {
            if ( StringUtils.startsWith( cacheName, "org.apache.archiva.redback.rbac.jdo" ) )
            {
                cacheManager.getCache( cacheName ).removeAll();
            }
        }

        return ActionStatus.SUCCESS;
    }

    @Override
    public User getGuestUser()
        throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.users.User user = userManager.getGuestUser();
            return getRestUser( user );
        }
        catch ( Exception e )
        {
            return null;
        }
    }

    @Override
    public User createGuestUser()
        throws RedbackServiceException
    {
        User u = getGuestUser();
        if ( u != null )
        {
            return u;
        }
        // temporary disable policy during guest creation as no password !
        try
        {
            securitySystem.getPolicy().setEnabled( false );
            org.apache.archiva.redback.users.User user = userManager.createGuestUser();
            user.setPasswordChangeRequired( false );
            user = userManager.updateUser( user, false );
            roleManager.assignRole( config.getString( UserConfigurationKeys.DEFAULT_GUEST ), user.getUsername() );
            return getRestUser( user );
        }
        catch ( RoleManagerException | UserNotFoundException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }
        finally
        {

            if ( !securitySystem.getPolicy().isEnabled() )
            {
                securitySystem.getPolicy().setEnabled( true );
            }
        }
    }

    @Override
    public PingResult ping()
        throws RedbackServiceException
    {
        return new PingResult( true );
    }

    private User getRestUser( org.apache.archiva.redback.users.User user )
    {
        if ( user == null )
        {
            return null;
        }
        return new User( user );
    }

    @Override
    public User createAdminUser( User adminUser )
        throws RedbackServiceException
    {
        User result;
        if ( getAdminStatus().isExists() )
        {
            log.warn( "Admin user exists already" );
            httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePath().toString() );
            throw new RedbackServiceException( ErrorMessage.of( Constants.ERR_USER_ADMIN_EXISTS ), 303 );
        }
        log.debug("Creating admin admin user '{}'", adminUser.getUserId());
        if (!RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME.equals(adminUser.getUserId())) {
            log.error("Wrong admin user name {}", adminUser.getUserId());
            throw new RedbackServiceException(ErrorMessage.of(Constants.ERR_USER_ADMIN_BAD_NAME ), 405);
        }

        try
        {
            org.apache.archiva.redback.users.User user =
                userManager.createUser( RedbackRoleConstants.ADMINISTRATOR_ACCOUNT_NAME, adminUser.getFullName(),
                                        adminUser.getEmail() );
            user.setPassword( adminUser.getPassword() );

            user.setLocked( false );
            user.setPasswordChangeRequired( false );
            user.setPermanent( true );
            user.setValidated( true );

            userManager.addUser( user );
            result = getRestUser( user );
            roleManager.assignRole( "system-administrator", user.getUsername() );
        }
        catch ( RoleManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( ERR_ROLEMANAGER_FAIL, e.getMessage( ) ) );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( ERR_USERMANAGER_FAIL, e.getMessage() ) );
        }
        httpServletResponse.setStatus( 201 );
        httpServletResponse.setHeader( "Location", uriInfo.getAbsolutePath().toString() );
        return result;
    }

    @Override
    public AvailabilityStatus getAdminStatus()
        throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.users.User user = userManager.findUser( config.getString( UserConfigurationKeys.DEFAULT_ADMIN ) );
            if (user.getAccountCreationDate()!=null)
            {
                return new AvailabilityStatus( true, user.getAccountCreationDate( ).toInstant( ) );
            } else {
                return new AvailabilityStatus( true );
            }
        }
        catch ( UserNotFoundException e )
        {
            // ignore
        }
        catch ( UserManagerException e )
        {
            Throwable cause = e.getCause();

            if ( cause != null && cause instanceof UserNotFoundException )
            {
                return new AvailabilityStatus( false );
            }
            throw new RedbackServiceException( ErrorMessage.of( ERR_USERMANAGER_FAIL,  e.getMessage() ) );
        }
        return new AvailabilityStatus( false );
    }

    @Override
    public ActionStatus resetPassword( String userId, ResetPasswordRequest resetPasswordRequest )
        throws RedbackServiceException
    {
        String username = resetPasswordRequest.getUsername();
        if ( StringUtils.isEmpty( username ) )
        {
            throw new RedbackServiceException( new ErrorMessage( "username.cannot.be.empty" ) );
        }

        UserManager userManager = securitySystem.getUserManager();
        KeyManager keyManager = securitySystem.getKeyManager();
        UserSecurityPolicy policy = securitySystem.getPolicy();

        try
        {
            org.apache.archiva.redback.users.User user = userManager.findUser( username );

            AuthenticationKey authkey = keyManager.createKey( username, "Password Reset Request",
                                                              policy.getUserValidationSettings().getEmailValidationTimeout() );

            String applicationUrl = resetPasswordRequest.getApplicationUrl();
            if ( StringUtils.isBlank( applicationUrl ) )
            {
                applicationUrl = getBaseUrl();
            }

            mailer.sendPasswordResetEmail( Arrays.asList( user.getEmail() ), authkey, applicationUrl );
            log.info( "password reset request for username {}", username );
        }
        catch ( UserNotFoundException e )
        {
            log.info( "Password Reset on non-existant user [{}].", username );
            throw new RedbackServiceException( new ErrorMessage( "password.reset.failure" ) );
        }
        catch ( KeyManagerException e )
        {
            log.info( "Unable to issue password reset.", e );
            throw new RedbackServiceException( new ErrorMessage( "password.reset.email.generation.failure" ) );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }

        return ActionStatus.SUCCESS;
    }

    @Override
    public RegistrationKey registerUser( String userId, UserRegistrationRequest userRegistrationRequest )
        throws RedbackServiceException
    {
        User user = userRegistrationRequest.getUser();
        if ( user == null )
        {
            throw new RedbackServiceException( new ErrorMessage( "invalid.user.credentials", null ) );

        }

        UserSecurityPolicy securityPolicy = securitySystem.getPolicy();

        boolean emailValidationRequired = securityPolicy.getUserValidationSettings().isEmailValidationRequired();

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

            if ( userManager.userExists( user.getUserId() ) )
            {
                throw new RedbackServiceException(
                    new ErrorMessage( "user.already.exists", new String[]{ user.getUserId() } ) );
            }

            u = userManager.createUser( user.getUserId(), user.getFullName(), user.getEmail() );
            u.setPassword( user.getPassword() );
            u.setValidated( false );
            u.setLocked( false );

            roleManager.assignRole( RedbackRoleConstants.REGISTERED_USER_ROLE_ID, u.getUsername() );
        }
        catch ( RoleManagerException rpe )
        {
            log.error( "RoleProfile Error: {}", rpe.getMessage(), rpe );
            throw new RedbackServiceException( new ErrorMessage( "assign.role.failure", null ) );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }

        if ( emailValidationRequired )
        {
            u.setLocked( true );

            try
            {
                AuthenticationKey authkey =
                    securitySystem.getKeyManager().createKey( u.getUsername(), "New User Email Validation",
                                                              securityPolicy.getUserValidationSettings().getEmailValidationTimeout() );

                String baseUrl = userRegistrationRequest.getApplicationUrl();
                if ( StringUtils.isBlank( baseUrl ) )
                {
                    baseUrl = getBaseUrl();
                }

                log.debug( "register user {} with email {} and app url {}", u.getUsername(), u.getEmail(), baseUrl );

                mailer.sendAccountValidationEmail( Arrays.asList( u.getEmail() ), authkey, baseUrl );

                securityPolicy.setEnabled( false );
                userManager.addUser( u );
                return new RegistrationKey( authkey.getKey(), true );

            }
            catch ( KeyManagerException e )
            {
                log.error( "Unable to register a new user.", e );
                throw new RedbackServiceException( new ErrorMessage( "cannot.register.user", null ) );
            }
            catch ( UserManagerException e )
            {
                throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
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
                throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
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
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();
        String userName = UserManager.GUEST_USERNAME;
        if ( redbackRequestInformation != null && redbackRequestInformation.getUser() != null )
        {
            userName = redbackRequestInformation.getUser().getUsername();
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
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();
        String userName = UserManager.GUEST_USERNAME;
        if ( redbackRequestInformation != null && redbackRequestInformation.getUser() != null )
        {
            userName = redbackRequestInformation.getUser().getUsername();
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
            AuthenticationKey authkey = securitySystem.getKeyManager().findKey( key );

            org.apache.archiva.redback.users.User user =
                securitySystem.getUserManager().findUser( authkey.getForPrincipal() );

            user.setValidated( true );
            user.setLocked( false );
            user.setPasswordChangeRequired( true );
            user.setEncodedPassword( "" );

            principal = user.getUsername();

            TokenBasedAuthenticationDataSource authsource = new TokenBasedAuthenticationDataSource();
            authsource.setPrincipal( principal );
            authsource.setToken( authkey.getKey() );
            authsource.setEnforcePasswordChange( false );

            securitySystem.getUserManager().updateUser( user );

            VerificationStatus status = new VerificationStatus(false );
            SecuritySession authStatus = securitySystem.authenticate( authsource );
            if (authStatus.isAuthenticated()) {
                Token accessToken = jwtAuthenticator.generateToken( principal );
                status.setAccessToken( accessToken.getData() );
                status.setSuccess( true );
            }

            log.info( "account validated for user {}", user.getUsername() );

            return status;
        }
        catch ( MustChangePasswordException | AccountLockedException | AuthenticationException e )
        {
            throw new RedbackServiceException( e.getMessage(), Response.Status.FORBIDDEN.getStatusCode() );
        }
        catch ( KeyNotFoundException e )
        {
            log.info( "Invalid key requested: {}", key );
            throw new RedbackServiceException( new ErrorMessage( "cannot.find.key" ) );
        }
        catch ( KeyManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( "cannot.find.key.at.the.momment" ) );

        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( new ErrorMessage( "cannot.find.user", new String[]{ principal } ) );

        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
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
            if ( permission.getOperation() != null )
            {
                Operation operation = new Operation();
                operation.setName( permission.getOperation().getName() );
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
                Permission permission = new Permission();
                permission.setName( p.getName() );

                if ( p.getOperation() != null )
                {
                    Operation operation = new Operation();
                    operation.setName( p.getOperation().getName() );
                    permission.setOperation( operation );
                }

                if ( p.getResource() != null )
                {
                    Resource resource = new Resource();
                    resource.setIdentifier( p.getResource().getIdentifier() );
                    resource.setPattern( p.getResource().isPattern() );
                    permission.setResource( resource );
                }

                userPermissions.add( permission );
            }
            return userPermissions;
        }
        catch ( RbacManagerException e )
        {
            log.error( e.getMessage(), e );
            throw new RedbackServiceException( e.getMessage() );
        }
    }

    public void validateCredentialsLoose( User user )
        throws RedbackServiceException
    {
        RedbackServiceException redbackServiceException =
            new RedbackServiceException( "issues during validating user" );
        if ( StringUtils.isEmpty( user.getUserId() ) )
        {
            redbackServiceException.addErrorMessage( new ErrorMessage( "username.required", null ) );
        }
        else
        {
            if ( !user.getUserId().matches( VALID_USERNAME_CHARS ) )
            {
                redbackServiceException.addErrorMessage( new ErrorMessage( "username.invalid.characters", null ) );
            }
        }

        if ( StringUtils.isEmpty( user.getFullName() ) )
        {
            redbackServiceException.addErrorMessage( new ErrorMessage( "fullName.required", null ) );
        }

        if ( StringUtils.isEmpty( user.getEmail() ) )
        {
            redbackServiceException.addErrorMessage( new ErrorMessage( "email.required", null ) );
        }

        if ( !StringUtils.equals( user.getPassword(), user.getConfirmPassword() ) )
        {
            redbackServiceException.addErrorMessage( new ErrorMessage( "passwords.does.not.match", null ) );
        }

        try
        {
            if ( !StringUtils.isEmpty( user.getEmail() ) )
            {
                new InternetAddress( user.getEmail(), true );
            }
        }
        catch ( AddressException e )
        {
            redbackServiceException.addErrorMessage( new ErrorMessage( "email.invalid", null ) );
        }
        if ( !redbackServiceException.getErrorMessages().isEmpty() )
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
                userManager.createUser( user.getUserId(), user.getFullName(), user.getEmail() );

            user.setPassword( user.getPassword() );

            securitySystem.getPolicy().validatePassword( tmpuser );

            if ( ( StringUtils.isEmpty( user.getPassword() ) ) )
            {
                throw new RedbackServiceException( new ErrorMessage( "password.required", null ) );
            }
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }
    }

    private String getBaseUrl()
    {
        if ( httpServletRequest != null )
        {
            if ( httpServletRequest != null )
            {
                return httpServletRequest.getScheme() + "://" + httpServletRequest.getServerName() + (
                    httpServletRequest.getServerPort() == 80
                        ? ""
                        : ":" + httpServletRequest.getServerPort() ) + httpServletRequest.getContextPath();
            }
        }
        return null;
    }

    @Override
    public void unlockUser( String userId )
        throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.users.User rawUser = userManager.findUser( userId, false );
            if ( rawUser != null )
            {
                rawUser.setLocked( false );
                userManager.updateUser( rawUser, false );
            } else {
                throw new RedbackServiceException( ErrorMessage.of( ERR_USER_NOT_FOUND, userId ), 404 );
            }
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( ERR_USER_NOT_FOUND ), 404 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }
        httpServletResponse.setStatus( 200 );
    }

    @Override
    public void lockUser( String userId )
        throws RedbackServiceException
    {
        try
        {
            org.apache.archiva.redback.users.User rawUser = userManager.findUser( userId, false );
            if ( rawUser != null )
            {
                rawUser.setLocked( true );
                userManager.updateUser( rawUser, false );
            } else {
                throw new RedbackServiceException( ErrorMessage.of( ERR_USER_NOT_FOUND, userId ), 404 );
            }
        }
        catch ( UserNotFoundException e )
        {
            throw new RedbackServiceException( ErrorMessage.of( ERR_USER_NOT_FOUND ), 404 );
        }
        catch ( UserManagerException e )
        {
            throw new RedbackServiceException( new ErrorMessage( e.getMessage() ) );
        }
        httpServletResponse.setStatus( 200 );
    }

    @Override
    public PasswordStatus getPasswordStatus( String userId )
        throws RedbackServiceException
    {
        User user = getUser( userId );
        if ( user == null )
        {
            user.setPasswordChangeRequired( true );
            updateUser( user.getUserId(),  user );
            return new PasswordStatus( true );
        }
        return new PasswordStatus( false );
    }

}
