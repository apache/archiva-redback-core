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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.model.ActionStatus;
import org.apache.archiva.redback.rest.api.model.Application;
import org.apache.archiva.redback.rest.api.model.RedbackRestError;
import org.apache.archiva.redback.rest.api.model.v2.PasswordChange;
import org.apache.archiva.redback.rest.api.model.v2.RoleTree;
import org.apache.archiva.redback.rest.api.model.v2.AvailabilityStatus;
import org.apache.archiva.redback.rest.api.model.v2.Permission;
import org.apache.archiva.redback.rest.api.model.v2.PingResult;
import org.apache.archiva.redback.rest.api.model.v2.RegistrationKey;
import org.apache.archiva.redback.rest.api.model.v2.RoleInfo;
import org.apache.archiva.redback.rest.api.model.v2.SelfUserData;
import org.apache.archiva.redback.rest.api.model.v2.User;
import org.apache.archiva.redback.rest.api.model.v2.UserInfo;
import org.apache.archiva.redback.rest.api.model.v2.UserRegistrationRequest;
import org.apache.archiva.redback.rest.api.model.v2.VerificationStatus;
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
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.archiva.redback.rest.api.Constants.DEFAULT_PAGE_LIMIT;
import static org.apache.archiva.redback.users.UserManager.GUEST_USERNAME;

/**
 * Service interface for user management
 */
@Path( "/users" )
@Tag(name = "v2")
@Tag(name = "v2/Users")
@SecurityRequirement(name = "BearerAuth")
public interface UserService
{
    @Path( "{userId}" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION,
        resource = "{userId}" )
    @Operation( summary = "Returns information about a specific user",
        security = {
            @SecurityRequirement(
                name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If user was found in the database",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = UserInfo.class))
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    UserInfo getUser( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;


    @Path( "" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION )
    @Operation( summary = "Returns all users defined. The result is paged.",
        parameters = {
            @Parameter(name = "q", description = "Search term"),
            @Parameter(name = "offset", description = "The offset of the first element returned"),
            @Parameter(name = "limit", description = "Maximum number of items to return in the response"),
            @Parameter(name = "orderBy", description = "List of attribute used for sorting (user_id, fullName, email, created"),
            @Parameter(name = "order", description = "The sort order. Either ascending (asc) or descending (desc)")
        },
        security = {
            @SecurityRequirement(
                name = RedbackRoleConstants.USER_MANAGEMENT_USER_LIST_OPERATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be returned",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = PagedResult.class))
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    PagedResult<UserInfo> getUsers( @QueryParam("q") @DefaultValue( "" ) String searchTerm,
                                    @QueryParam( "offset" ) @DefaultValue( "0" ) Integer offset,
                                    @QueryParam( "limit" ) @DefaultValue( value = DEFAULT_PAGE_LIMIT ) Integer limit,
                                    @QueryParam( "orderBy") @DefaultValue( "id" ) List<String> orderBy,
                                    @QueryParam("order") @DefaultValue( "asc" ) String order)
        throws RedbackServiceException;

    @Path( "" )
    @POST
    @Produces( { APPLICATION_JSON } )
    @Consumes( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_CREATE_OPERATION )
    @Operation( summary = "Creates a user",
        security = {
            @SecurityRequirement(
                name = RedbackRoleConstants.USER_MANAGEMENT_USER_CREATE_OPERATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "201",
                description = "If user creation was successful",
                headers = {
                    @Header( name="Location", description = "The URL of the created mapping", schema = @Schema(type="string"))
                },
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = UserInfo.class))
            ),
            @ApiResponse( responseCode = "422", description = "Invalid input",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "303", description = "The user exists already",
                headers = {
                    @Header( name="Location", description = "The URL of existing user", schema = @Schema(type="string"))
                }
            )
        }
    )
    UserInfo createUser( User user )
        throws RedbackServiceException;

    @Path( "{userId}" )
    @DELETE
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_DELETE_OPERATION )
    @Operation( summary = "Deletes a given user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_DELETE_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If user deletion was successful"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for deletion.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    void deleteUser( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    @Path( "{userId}" )
    @PUT
    @Produces( {APPLICATION_JSON} )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    @Operation( summary = "Updates an existing user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If update was successful",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = UserInfo.class))
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "422", description = "Update data was not valid. E.g. password violations.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for update." ,
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )))
        }
    )
    UserInfo updateUser( @PathParam( "userId" ) String userId, User user )
        throws RedbackServiceException;



    /**
     * will create admin user only if not exists !! if exists will return false
     */
    @Path( "admin" )
    @POST
    @Produces( { APPLICATION_JSON } )
    @Consumes( { APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true )
    @Operation( summary = "Creates the admin user, if it does not exist",
        responses = {
            @ApiResponse( responseCode = "201",
                description = "If user creation was successful",
                headers = {
                    @Header( name="Location", description = "The URL of the created mapping", schema = @Schema(type="string"))
                },
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = UserInfo.class))
            ),
            @ApiResponse( responseCode = "422", description = "Invalid input",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "303", description = "The user exists already",
                headers = {
                    @Header( name="Location", description = "The URL of the existing admin user", schema = @Schema(type="string"))
                }
            )
        }
    )
    UserInfo createAdminUser( User user )
        throws RedbackServiceException;

    @Path( "admin/status" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true )
    @Operation( summary = "Returns the availability status of the admin user. ",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If status can be retrieved",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = AvailabilityStatus.class))
            )
        }
    )
    AvailabilityStatus getAdminStatus()
        throws RedbackServiceException;



    /**
     */
    @Path( "{userId}/lock/set" )
    @POST
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    @Operation( summary = "Creates a user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If locking was successful"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for locking.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  )
        }
    )
    void lockUser( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    /**
     */
    @Path( "{userId}/lock/clear" )
    @POST
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    @Operation( summary = "Unlocks a user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If unlocking was successful"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for unlock.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  )
        }
    )
    void unlockUser( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;


    /**
     */
    @Path( "{userId}/password/require/set" )
    @POST
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    @Operation( summary = "Sets the requirePassword flag for a given user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If password change require flag was set"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for editing.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  )

        }
    )
    void setRequirePasswordChangeFlag( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    /**
     */
    @Path( "{userId}/password/require/clear" )
    @POST
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
    @Operation( summary = "Clears the requirePassword flag for a given user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If password change require flag was unset"
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for editing.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  )

        }
    )
    void clearRequirePasswordChangeFlag( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;


    /**
     * Update only the current logged in user and this fields: fullname, email, password.
     * The service verifies the current logged user with the one passed in the method
     * @return the user info object
     */
    @Path( "me" )
    @PUT
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( noPermission = true )
    @Operation( summary = "Updates information of the current logged in user",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "The updated user information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = UserInfo.class))
            ),
            @ApiResponse( responseCode = "401", description = "User is not logged in",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  ),
            @ApiResponse( responseCode = "400", description = "Provided data is not valid",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  )
        }
    )
    UserInfo updateMe( SelfUserData user )
        throws RedbackServiceException;

    @Path( "me" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( noPermission = true )
    @Operation( summary = "Gets information of the current logged in user",
             responses = {
            @ApiResponse( responseCode = "200",
                description = "The user information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = UserInfo.class))
            ),
            @ApiResponse( responseCode = "401", description = "User is not logged in" ,
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
        }
    )
    UserInfo getLoggedInUser( ) throws RedbackServiceException;

    @Path( "___ping___" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true )
    @Operation( summary = "Checks the service availability",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "Pong",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = PingResult.class))
            )}
    )
    PingResult ping()
        throws RedbackServiceException;

    @Path( "{userId}/cache/clear" )
    @POST
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION,
    resource = "{userId}")
    @Operation( summary = "Clears the cache for the user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_EDIT_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "Status of the clear operation",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ActionStatus.class))

            ),
            @ApiResponse( responseCode = "404", description = "User does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the required permission.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  )
        }
    )
    Response removeFromCache( @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    /**
     * @return the registration key
     */
    @Path( "{userId}/register" )
    @POST
    @Produces( {APPLICATION_JSON} )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @Operation( summary = "Registers a new user",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the registration was successful, a registration key is returned",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RegistrationKey.class))
            ),
            @ApiResponse( responseCode = "422", description = "If the the provided user data is not valid",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  ),
        }
    )
    RegistrationKey registerUser( @PathParam( "userId" ) String userId, UserRegistrationRequest userRegistrationRequest )
        throws RedbackServiceException;

    /**
     * Asks for a password reset of the given User. Normally this results in a password reset email sent to the
     * stored email address for the given user.
     */
    @Path( "{userId}/password/reset" )
    @POST
    @Produces( { APPLICATION_JSON } )
    @Consumes( { APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @Operation( summary = "Asks for a password reset of the given user. This generates a reset email sent to the stored address of the given user.",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "The result status of the password reset.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = ActionStatus.class))

            ),
            @ApiResponse( responseCode = "404", description = "User does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  ),
        }
    )
    Response resetPassword( @PathParam( "userId" )String userId )
        throws RedbackServiceException;

    /**
     */
    @Path( "{userId}/permissions" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_VIEW_OPERATION,
        resource = "{userId}")
    @Operation( summary = "Returns a list of permissions assigned to the given user.",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_VIEW_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be returned",
                content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema =
                    @Schema(implementation = Permission.class)))
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "Logged in user does not have the permission to get this information." ,
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))),
        }
    )
    Collection<Permission> getUserPermissions( @PathParam( "userId" ) String userName )
        throws RedbackServiceException;

    @Path( GUEST_USERNAME+"/permissions" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true )
    @Operation( summary = "Returns a list of permissions assigned to the guest user.",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be returned",
                content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema =
                @Schema(implementation = Permission.class)))
            )
        }
    )
    Collection<Permission> getGuestPermissions(  )
        throws RedbackServiceException;

    /**
     * @since 1.4
     */
    @Path( "{userId}/operations" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_USER_VIEW_OPERATION,
        resource = "{userId}")
    @Operation( summary = "Returns a list of privileged operations assigned to the given user.",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_USER_VIEW_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be returned",
                content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema =
                @Schema(implementation = org.apache.archiva.redback.rest.api.model.v2.Operation.class )))
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "Logged in user does not have the permission to get this information.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
        }
    )
    Collection<org.apache.archiva.redback.rest.api.model.v2.Operation> getUserOperations( @PathParam( "userId" ) String userName )
        throws RedbackServiceException;

    /**
     * @return  the current logged user permissions, if no logged user guest permissions are returned
     * @since 1.4
     */
    @Path( "me/permissions" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @Operation( summary = "Returns a list of permissions assigned to the logged in user.",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be returned",
                content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema =
            @Schema(implementation = Permission.class )))
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
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @Operation( summary = "Returns a list of privileged operations assigned to the logged in user.",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "The list of operations assigne to the current user",
                content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema =
                @Schema(implementation = org.apache.archiva.redback.rest.api.model.v2.Operation.class )))
            )
        }
    )
    Collection<org.apache.archiva.redback.rest.api.model.v2.Operation> getCurrentUserOperations( )
        throws RedbackServiceException;


    @Path( "{userId}/register/{key}/validate" )
    @POST
    @Produces( {APPLICATION_JSON} )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @Operation( summary = "Validate the user registration for the given userid by checking the provided key.",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "The status of the user registration",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = VerificationStatus.class))
            ),
            @ApiResponse( responseCode = "404", description = "No user registration was found for the given id and key",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )))
        }
    )
    VerificationStatus validateUserRegistration( @PathParam( "userId" ) String userId, @PathParam( "key" ) String key )
        throws RedbackServiceException;


    /**
     * Returns all roles for a given user id. Recurses all child roles.
     *
     * @since 3.0
     */
    @Path( "{userId}/roles" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Returns a list of all roles effectively assigned to the given user.",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "The list of roles assigned to the given user",
                content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema =
                @Schema(implementation = org.apache.archiva.redback.rest.api.model.v2.RoleInfo.class )))
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for retrieving the information.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    List<RoleInfo> getEffectivelyAssignedRoles( @PathParam( "userId" ) String username )
        throws RedbackServiceException;


    /**
     * @since 3.0
     */
    @Path( "{userId}/roletree" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Returns a list of all roles that are assigned, or can be assigned to the given user. "+
        "This method sets the 'assigned' flag on all returned role objects.",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "The list of roles separated by application that are assigned or assignable for the given user",
                content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema =
                @Schema(implementation = Application.class )))
            ),
            @ApiResponse( responseCode = "404", description = "User does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for retrieving the information.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    RoleTree getRoleTree( @PathParam( "userId" ) String username )
        throws RedbackServiceException;


    @Path( "me/password/update" )
    @POST
    @Consumes({APPLICATION_JSON})
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @Operation( summary = "Changes a user password",
        security = {
            @SecurityRequirement( name = "Authenticated" )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "The password change was successful"
            ),
            @ApiResponse( responseCode = "401", description = "User is not logged in",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  ),
            @ApiResponse( responseCode = "400", description = "Provided data is not valid",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  ),
            @ApiResponse( responseCode = "403", description = "If the given user_id does not match",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  )
        }
    )
    Response changePassword( PasswordChange passwordChange ) throws RedbackServiceException;

    @Path( "{userId}/password/update" )
    @POST
    @Consumes({APPLICATION_JSON})
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @Operation( summary = "Changes a user password",
        responses = {
            @ApiResponse( responseCode = "200",
                description = "The password change was successful"
            ),
            @ApiResponse( responseCode = "400", description = "Provided data is not valid",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  ),
            @ApiResponse( responseCode = "403", description = "If the given user_id does not match",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))  )
        }
    )
    Response changePasswordUnauthenticated( @PathParam( "userId" ) String userId,  PasswordChange passwordChange ) throws RedbackServiceException;

}
