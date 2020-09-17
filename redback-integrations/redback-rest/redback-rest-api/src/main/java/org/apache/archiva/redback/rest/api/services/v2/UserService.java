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

import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.model.ActionStatus;
import org.apache.archiva.redback.rest.api.model.v2.AvailabilityStatus;
import org.apache.archiva.redback.rest.api.model.Operation;
import org.apache.archiva.redback.rest.api.model.v2.SelfUserData;
import org.apache.archiva.redback.rest.api.model.v2.PagedResult;
import org.apache.archiva.redback.rest.api.model.Permission;
import org.apache.archiva.redback.rest.api.model.v2.PingResult;
import org.apache.archiva.redback.rest.api.model.ResetPasswordRequest;
import org.apache.archiva.redback.rest.api.model.v2.RegistrationKey;
import org.apache.archiva.redback.rest.api.model.v2.User;
import org.apache.archiva.redback.rest.api.model.v2.UserRegistrationRequest;
import org.apache.archiva.redback.rest.api.model.VerificationStatus;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

import static org.apache.archiva.redback.rest.api.Constants.DEFAULT_PAGE_LIMIT;

@Path( "/users" )
@Tag(name = "v2")
@Tag(name = "v2/Users")
@SecurityRequirement(name = "BearerAuth")
public interface UserService
{
    @Path( "{userId}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION,
        resource = "{userId}" )
    @io.swagger.v3.oas.annotations.Operation( summary = "Returns information about a specific user",
        security = {
            @SecurityRequirement(
                name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If user was found in the database"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist" ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information" )
        }
    )
    User getUser( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;


    @Path( "" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION )
    @io.swagger.v3.oas.annotations.Operation( summary = "Returns all users defined. The result is paged.",
        security = {
            @SecurityRequirement(
                name = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be returned"
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information" )
        }
    )
    PagedResult<User> getUsers( @QueryParam( "offset" ) @DefaultValue( "0" ) Integer offset,
                                      @QueryParam( "limit" ) @DefaultValue( value = DEFAULT_PAGE_LIMIT ) Integer limit)
        throws RedbackServiceException;

    @Path( "" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @Consumes( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_CREATE_OPERATION )
    @io.swagger.v3.oas.annotations.Operation( summary = "Creates a user",
        security = {
            @SecurityRequirement(
                name = RedbackRoleConstants.USER_MANAGEMENT_USER_CREATE_OPERATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "201",
                description = "If user creation was successful",
                headers = {
                    @Header( name="Location", description = "The URL of the created mapping")
                }
            ),
            @ApiResponse( responseCode = "422", description = "Invalid input" ),
            @ApiResponse( responseCode = "303", description = "The user exists already",
                headers = {
                    @Header( name="Location", description = "The URL of existing user")
                }
            )
        }
    )
    User createUser( User user )
        throws RedbackServiceException;

    @Path( "{userId}" )
    @DELETE
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_DELETE_OPERATION )
    @io.swagger.v3.oas.annotations.Operation( summary = "Deletes a given user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_DELETE_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If user deletion was successful"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist" ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for deletion." )
        }
    )
    void deleteUser( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    @Path( "{userId}" )
    @PUT
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    @io.swagger.v3.oas.annotations.Operation( summary = "Updates an existing user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If update was successful"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist" ),
            @ApiResponse( responseCode = "422", description = "Update data was not valid. E.g. password violations." ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for update." )
        }
    )
    User updateUser( @PathParam( "userId" ) String userId, User user )
        throws RedbackServiceException;



    /**
     * will create admin user only if not exists !! if exists will return false
     */
    @Path( "admin" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @Consumes( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true )
    @io.swagger.v3.oas.annotations.Operation( summary = "Creates the admin user, if it does not exist",
        responses = {
            @ApiResponse( responseCode = "201",
                description = "If user creation was successful",
                headers = {
                    @Header( name="Location", description = "The URL of the created mapping")
                }
            ),
            @ApiResponse( responseCode = "422", description = "Invalid input" ),
            @ApiResponse( responseCode = "303", description = "The user exists already",
                headers = {
                    @Header( name="Location", description = "The URL of the existing admin user")
                }
            )
        }
    )
    User createAdminUser( User user )
        throws RedbackServiceException;

    @Path( "admin/status" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true )
    @io.swagger.v3.oas.annotations.Operation( summary = "Returns the availability status of the admin user. ",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If status can be retrieved"
            )
        }
    )
    AvailabilityStatus getAdminStatus()
        throws RedbackServiceException;



    /**
     */
    @Path( "{userId}/lock/set" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    @io.swagger.v3.oas.annotations.Operation( summary = "Creates a user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If locking was successful"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist" ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for locking." )
        }
    )
    void lockUser( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    /**
     */
    @Path( "{userId}/lock/clear" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    @io.swagger.v3.oas.annotations.Operation( summary = "Unlocks a user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If unlocking was successful"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist" ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for unlock." )
        }
    )
    void unlockUser( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;


    /**
     */
    @Path( "{userId}/password/require/set" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    @io.swagger.v3.oas.annotations.Operation( summary = "Sets the requirePassword flag for a given user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If password change require flag was set"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist" ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for editing." )

        }
    )
    void setRequirePasswordChangeFlag( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    /**
     */
    @Path( "{userId}/password/require/clear" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    @io.swagger.v3.oas.annotations.Operation( summary = "Clears the requirePassword flag for a given user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If password change require flag was unset"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist" ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for editing." )

        }
    )
    void clearRequirePasswordChangeFlag( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;


    /**
     * Update only the current logged in user and this fields: fullname, email, password.
     * The service verifies the current logged user with the one passed in the method
     * @return
     */
    @Path( "me" )
    @PUT
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noPermission = true )
    @io.swagger.v3.oas.annotations.Operation( summary = "Updates information of the current logged in user",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If user data has been updated"
            ),
            @ApiResponse( responseCode = "401", description = "User is not logged in" ),
            @ApiResponse( responseCode = "400", description = "Provided data is not valid" )
        }
    )
    User updateMe( SelfUserData user )
        throws RedbackServiceException;

    @Path( "me" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noPermission = true )
    @io.swagger.v3.oas.annotations.Operation( summary = "Gets information of the current logged in user",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If user data is returned"
            ),
            @ApiResponse( responseCode = "401", description = "User is not logged in" ),
        }
    )
    User getLoggedInUser( ) throws RedbackServiceException;

    @Path( "___ping___" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true )
    PingResult ping()
        throws RedbackServiceException;

    @Path( "{userId}/cache/clear" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION,
    resource = "{userId}")
    @io.swagger.v3.oas.annotations.Operation( summary = "Clears the cache for the user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the cache was cleared properly"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist" ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the required permission." )
        }
    )
    ActionStatus removeFromCache( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    /**
     * @return
     */
    @Path( "{userId}/register" )
    @POST
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @io.swagger.v3.oas.annotations.Operation( summary = "Registers a new user",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the registration was successful, a registration key is returned"
            ),
            @ApiResponse( responseCode = "422", description = "If the the provided user data is not valid" ),
        }
    )
    RegistrationKey registerUser( @PathParam( "userId" ) String userId, UserRegistrationRequest userRegistrationRequest )
        throws RedbackServiceException;

    /**
     *
     * @param resetPasswordRequest contains username for send a password reset email
     */
    @Path( "{userId}/password/reset" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON } )
    @Consumes( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @io.swagger.v3.oas.annotations.Operation( summary = "Asks for a password reset of the given user. This generates a reset email sent to the stored address of the given user.",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the password reset email was sent"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist" ),
        }
    )
    ActionStatus resetPassword( @PathParam( "userId" )String userId, ResetPasswordRequest resetPasswordRequest )
        throws RedbackServiceException;

    /**
     */
    @Path( "{userId}/permissions" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION,
        resource = "{userId}")
    @io.swagger.v3.oas.annotations.Operation( summary = "Returns a list of permissions assigned to the given user.",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be returned"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist" ),
            @ApiResponse( responseCode = "403", description = "Logged in user does not have the permission to get this information." ),
        }
    )
    Collection<Permission> getUserPermissions( @PathParam( "userId" ) String userName )
        throws RedbackServiceException;

    /**
     * @since 1.4
     */
    @Path( "{userId}/operations" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION )
    @io.swagger.v3.oas.annotations.Operation( summary = "Returns a list of privileged operations assigned to the given user.",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be returned"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist" ),
            @ApiResponse( responseCode = "403", description = "Logged in user does not have the permission to get this information." ),
        }
    )
    Collection<Operation> getUserOperations( @PathParam( "userId" ) String userName )
        throws RedbackServiceException;

    /**
     * @return  the current logged user permissions, if no logged user guest permissions are returned
     * @since 1.4
     */
    @Path( "me/permissions" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @io.swagger.v3.oas.annotations.Operation( summary = "Returns a list of permissions assigned to the logged in user.",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be returned"
            )
        }
    )
    Collection<Permission> getCurrentUserPermissions( )
        throws RedbackServiceException;

    /**
     * @return the current logged user operations, if no logged user guest operations are returned
     * @since 1.4
     */
    @Path( "me/operations" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @io.swagger.v3.oas.annotations.Operation( summary = "Returns a list of privileged operations assigned to the logged in user.",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be returned"
            )
        }
    )
    Collection<Operation> getCurrentUserOperations( )
        throws RedbackServiceException;


    @Path( "{userId}/register/{key}/validate" )
    @GET
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @io.swagger.v3.oas.annotations.Operation( summary = "Validate the user registration for the given userid by checking the provided key.",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the verification was successful"
            ),
            @ApiResponse( responseCode = "404", description = "No user registration was found for the given id and key" ),
        }
    )
    VerificationStatus validateUserRegistration( @PathParam( "userId" ) String userId, @PathParam( "key" ) String key )
        throws RedbackServiceException;
}
