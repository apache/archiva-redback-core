package org.apache.archiva.redback.rest.api.services.v2;

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

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.model.ActionStatus;
import org.apache.archiva.redback.rest.api.model.AvailabilityStatus;
import org.apache.archiva.redback.rest.api.model.Operation;
import org.apache.archiva.redback.rest.api.model.PasswordStatus;
import org.apache.archiva.redback.rest.api.model.Permission;
import org.apache.archiva.redback.rest.api.model.PingResult;
import org.apache.archiva.redback.rest.api.model.RegistrationKey;
import org.apache.archiva.redback.rest.api.model.ResetPasswordRequest;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.model.UserRegistrationRequest;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;

@Path( "/users" )
@Tag(name = "v2")
@Tag(name = "v2/Users")
@SecurityRequirement(name = "BearerAuth")
public interface UserService
{
    @Path( "{userId}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    User getUser( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;


    @Path( "" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION )
    List<User> getUsers()
        throws RedbackServiceException;

    @Path( "" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @Consumes( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_CREATE_OPERATION )
    ActionStatus createUser( User user )
        throws RedbackServiceException;


    /**
     * will create admin user only if not exists !! if exists will return false
     */
    @Path( "admin" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @Consumes( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true )
    ActionStatus createAdminUser( User user )
        throws RedbackServiceException;

    @Path( "admin/exists" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true )
    AvailabilityStatus isAdminUserExists()
        throws RedbackServiceException;


    @Path( "{userId}" )
    @DELETE
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_DELETE_OPERATION )
    ActionStatus deleteUser( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    @Path( "{userId}" )
    @PUT
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    ActionStatus updateUser( @PathParam( "userId" ) String userId, User user )
        throws RedbackServiceException;

    /**
     * @since 2.0
     */
    @Path( "{userId}/lock" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    ActionStatus lockUser( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    /**
     * @since 2.0
     */
    @Path( "{userId}/unlock" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    ActionStatus unlockUser( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;


    /**
     * @since 2.0
     */
    @Path( "{userId}/passwordStatus" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    PasswordStatus passwordChangeRequired( @PathParam( "userId" ) String username )
        throws RedbackServiceException;

    /**
     * update only the current user and this fields: fullname, email, password.
     * the service verify the curent logged user with the one passed in the method
     * @since 1.4
     */
    @Path( "{userId}" )
    @PUT
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noPermission = true )
    ActionStatus updateMe( User user )
        throws RedbackServiceException;

    @Path( "___ping___" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true )
    PingResult ping()
        throws RedbackServiceException;

    @Path( "{userId}/clearCache" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    ActionStatus removeFromCache( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    @Path( "guest" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    User getGuestUser()
        throws RedbackServiceException;

    @Path( "guest" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    User createGuestUser()
        throws RedbackServiceException;

    /**
     * if redback is not configured for email validation is required, -1 is returned as key
     * @since 1.4
     */
    @Path( "{userId}/register" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    RegistrationKey registerUser( @PathParam( "userId" ) String userId,  UserRegistrationRequest userRegistrationRequest )
        throws RedbackServiceException;

    /**
     *
     * @param resetPasswordRequest contains username for send a password reset email
     * @since 1.4
     */
    @Path( "{userId}/resetPassword" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @Consumes( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    ActionStatus resetPassword( @PathParam( "userId" )String userId, ResetPasswordRequest resetPasswordRequest )
        throws RedbackServiceException;

    /**
     * @since 1.4
     */
    @Path( "{userId}/permissions" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION )
    Collection<Permission> getUserPermissions( @PathParam( "userId" ) String userName )
        throws RedbackServiceException;

    /**
     * @since 1.4
     */
    @Path( "{userId}/operations" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION )
    Collection<Operation> getUserOperations( @PathParam( "userId" ) String userName )
        throws RedbackServiceException;

    /**
     * @return  the current logged user permissions, if no logged user guest permissions are returned
     * @since 1.4
     */
    @Path( "{userId}/self/permissions" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    Collection<Permission> getCurrentUserPermissions(@PathParam( "userId" ) String userId)
        throws RedbackServiceException;

    /**
     * @return the current logged user operations, if no logged user guest operations are returned
     * @since 1.4
     */
    @Path( "{userId}/self/operations" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    Collection<Operation> getCurrentUserOperations(@PathParam( "userId" ) String userId)
        throws RedbackServiceException;

}
