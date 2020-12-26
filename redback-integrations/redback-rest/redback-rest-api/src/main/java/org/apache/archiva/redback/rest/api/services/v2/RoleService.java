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
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.model.RedbackRestError;
import org.apache.archiva.redback.rest.api.model.v2.PagedResult;
import org.apache.archiva.redback.rest.api.model.v2.Role;
import org.apache.archiva.redback.rest.api.model.v2.RoleInfo;
import org.apache.archiva.redback.rest.api.model.v2.RoleTemplate;
import org.apache.archiva.redback.rest.api.model.v2.UserInfo;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.archiva.redback.rest.api.Constants.DEFAULT_PAGE_LIMIT;

/**
 *
 * Service interface for role management.
 *
 * @author Olivier Lamy
 * @author Martin Stockhammer
 * @since 3.0
 */
@Path( "/roles" )
@Tag(name = "v2")
@Tag(name = "v2/Roles")
@SecurityRequirement(name = "BearerAuth")
public interface RoleService
{

    @Path( "" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Returns all roles defined. The result is paged.",
        parameters = {
            @Parameter(name = "q", description = "Search term"),
            @Parameter(name = "offset", description = "The offset of the first element returned"),
            @Parameter(name = "limit", description = "Maximum number of items to return in the response"),
            @Parameter(name = "orderBy", description = "List of attribute used for sorting (user_id, fullName, email, created"),
            @Parameter(name = "order", description = "The sort order. Either ascending (asc) or descending (desc)")
        },
        security = {
            @SecurityRequirement(
                name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION
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
    PagedResult<RoleInfo> getAllRoles( @QueryParam("q") @DefaultValue( "" ) String searchTerm,
                                       @QueryParam( "offset" ) @DefaultValue( "0" ) Integer offset,
                                       @QueryParam( "limit" ) @DefaultValue( value = DEFAULT_PAGE_LIMIT ) Integer limit,
                                       @QueryParam( "orderBy") @DefaultValue( "id" ) List<String> orderBy,
                                       @QueryParam("order") @DefaultValue( "asc" ) String order)
        throws RedbackServiceException;

    @Path( "{roleId}" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Returns the definition about a specific role.",
        security = {
            @SecurityRequirement(
                name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If role was found in the database",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RoleInfo.class))
            ),
            @ApiResponse( responseCode = "404", description = "Role does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    RoleInfo getRole( @PathParam( "roleId" ) String roleId )
        throws RedbackServiceException;

    @Path( "{roleId}" )
    @HEAD
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Checks, if the role with the given id exists.",
        security = {
            @SecurityRequirement(
                name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If role was found in the database"
            ),
            @ApiResponse( responseCode = "404", description = "Role does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    Response checkRole( @PathParam( "roleId" ) String roleId )
        throws RedbackServiceException;


    /**
     * Moves a templated role from one resource to another resource
     * @TODO: Not sure, if it makes sense to keep the child template at the source. Shouldn't we move the childs too?
     *
     * @param templateId the template identifier
     * @param oldResource the resource of the current role
     * @param newResource the resource of the new role
     */
    @Path( "templates/{templateId}/{oldResource}/moveto/{newResource}" )
    @POST
    @Produces( {APPLICATION_JSON} )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Moves a templated role from one resource to another resource. If the template has child templates," +
        " then child instances will be created on for the destination resource. But the child instances on the source are not deleted.",
        security = {
            @SecurityRequirement(
                name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "201",
                description = "If user creation was successful",
                headers = {
                    @Header( name="Location", description = "The URL of the moved role", schema = @Schema(type="string"))
                },
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RoleInfo.class))
            ),
            @ApiResponse( responseCode = "404", description = "The source role does not exist" ),
            @ApiResponse( responseCode = "303", description = "The destination role exists already" ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission to move the role.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )

        }
    )
    RoleInfo moveTemplatedRole( @PathParam( "templateId" ) String templateId, @PathParam( "oldResource" ) String oldResource,
                                @PathParam( "newResource" ) String newResource )
        throws RedbackServiceException;

    @Path( "templates/{templateId}/{resource}" )
    @HEAD
    @Produces( { APPLICATION_JSON} )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Checks, if a instance of the role template exists for the given resource",
        security = {
            @SecurityRequirement(
                name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the role instance exists"
            ),
            @ApiResponse( responseCode = "404", description = "Role does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class ))
            ),
            @ApiResponse( responseCode = "403", description = "Authenticated user is not permitted to gather the information",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    Response checkTemplateRole( @PathParam( "templateId" ) String templateId,
                                @PathParam( "resource" ) String resource )
        throws RedbackServiceException;

    @Path( "templates/{templateId}/{resource}" )
    @PUT
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Creates a role instance from a template for the given resource",
        security = {
            @SecurityRequirement(
                name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION
            )
        },
        responses = {
            @ApiResponse( responseCode = "201",
                description = "If user creation was successful",
                headers = {
                    @Header( name = "Location", description = "The URL of the created role", schema = @Schema( type = "string" ) )
                },
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = RoleInfo.class ) )
            ),
            @ApiResponse( responseCode = "200",
                description = "If the role instance existed before and was updated",
                headers = {
                    @Header( name = "Location", description = "The URL of the updated role", schema = @Schema( type = "string" ) )
                },
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = RoleInfo.class ) )
            ),
            @ApiResponse( responseCode = "404", description = "The template does not exist" ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for role creation.",
                content = @Content( mediaType = APPLICATION_JSON, schema = @Schema( implementation = RedbackRestError.class ) ) )

        }
    )
    RoleInfo createTemplatedRole( @PathParam( "templateId" ) String templateId,
                                      @PathParam( "resource" ) String resource )
        throws RedbackServiceException;

    /**
     * Removes a role corresponding to the role Id that was manufactured with the given resource
     * it also removes any user assignments for that role
     *
     * @param templateId
     * @param resource
     */
    @Path( "templates/{templateId}/{resource}" )
    @DELETE
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Deletes a role template instance",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If role deletion was successful"
            ),
            @ApiResponse( responseCode = "404", description = "Role does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for deletion.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    Response removeTemplatedRole( @PathParam(  "templateId" ) String templateId,
                                 @PathParam( "resource" ) String resource )
        throws RedbackServiceException;



    /**
     * Assigns the templated role indicated by the templateId
     *
     * fails if the templated role has not been created
     *
     * @param templateId
     * @param resource
     * @param userId
     */
    @Path( "templates/{templateId}/{resource}/user/{userId}" )
    @PUT
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Assigns a template role instance to a given user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the role instance was assigned"
            ),
            @ApiResponse( responseCode = "404", description = "Role instance does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for role assignment.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    RoleInfo assignTemplatedRole( @PathParam( "templateId" ) String templateId,
                                 @PathParam( "resource" ) String resource,
                                 @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    /**
     * Assigns the role indicated by the roleId to the given principal
     *
     * @param roleId
     * @param userId
     */
    @Path( "{roleId}/user/{userId}" )
    @PUT
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Assigns a role to a given user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the role was assigned"
            ),
            @ApiResponse( responseCode = "404", description = "Role does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for role assignment.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    RoleInfo assignRole( @PathParam( "roleId" ) String roleId, @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    /**
     * Deletes the assignment of a role to a user.
     *
     * @param roleId the role id
     * @param userId the user id
     * @throws RedbackServiceException
     */
    @Path( "{roleId}/user/{userId}" )
    @DELETE
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Removes a role assignment for the given role and user",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the role assignment was removed"
            ),
            @ApiResponse( responseCode = "404", description = "Role instance does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for role assignment.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    RoleInfo deleteRoleAssignment( @PathParam( "roleId" ) String roleId, @PathParam( "userId" ) String userId )
        throws RedbackServiceException;

    /**
     * This returns the list of assigned users to a given role. The flag "recurse" is a query parameter.
     * If the query parameter exists and is not set to 'false', or '0', it will recurse all parent roles and return a list of users
     * assigned to the current role and the parent roles up to the root.
     * If the query parameter does not exist or is set to 'false' or '0', it will return only the users assigned directly
     * to the given role.
     *
     * @param roleId the role identifier, for which the assigned users are returned
     * @param recurse if the parameter does not exist or is set to 'false' or '0', only directly assigned users are returned.
     *                If the parameter value is set to 'parentsOnly', the users assigned to all parent roles up to the root excluding the
     *                given role are returned.
     *                Otherwise all users assigned to the given role and all parent roles up to the root are returned.
     * @param searchTerm the substring query term to search for in the user ids and names
     * @param offset the offset index in the user list for paging
     * @param limit the maximum number of users returned
     * @param orderBy the order attributes for ordering
     * @param order the order direction 'asc' (ascending), or 'desc' (descending)
     * @return the list of user objects
     * @throws RedbackServiceException
     */
    @Path("{roleId}/user")
    @GET
    @Produces({APPLICATION_JSON})
    @RedbackAuthorization(permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION)
    @Operation( summary = "Returns the users assigned to the given role",
        parameters = {
            @Parameter(name = "q", description = "Search term"),
            @Parameter(name = "offset", description = "The offset of the first element returned"),
            @Parameter(name = "limit", description = "Maximum number of items to return in the response"),
            @Parameter(name = "orderBy", description = "List of attribute used for sorting (user_id, fullName, email, created"),
            @Parameter(name = "order", description = "The sort order. Either ascending (asc) or descending (desc)"),
            @Parameter(name = "recurse", description = "If not present, or set to 'false' or '0', only users assigned directly to this role are returned."+
            " If present and set to 'parentsOnly', the list of users assigned to all parents of the given role up to the root."+
                " If present and set to any other value than 'parentsOnly', 'false' or '0', the users assigned to this role or any parent role in the hierarchy"+
                " up to the root are returned.")
        },
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the users could be retrieved"
            ),
            @ApiResponse( responseCode = "404", description = "Role instance does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for role assignment.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    PagedResult<UserInfo> getRoleUsers(@PathParam( "roleId" ) String roleId,
                                       @QueryParam("recurse") String recurse,
                                       @QueryParam("q") @DefaultValue( "" ) String searchTerm,
                                       @QueryParam( "offset" ) @DefaultValue( "0" ) Integer offset,
                                       @QueryParam( "limit" ) @DefaultValue( value = DEFAULT_PAGE_LIMIT ) Integer limit,
                                       @QueryParam( "orderBy") @DefaultValue( "id" ) List<String> orderBy,
                                       @QueryParam("order") @DefaultValue( "asc" ) String order
                                       ) throws RedbackServiceException;


    @Path("{roleId}/unassigned")
    @GET
    @Produces({APPLICATION_JSON})
    @RedbackAuthorization(permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION)
    @Operation( summary = "Returns the users not assigned to the given role",
        parameters = {
            @Parameter(name = "q", description = "Search term"),
            @Parameter(name = "offset", description = "The offset of the first element returned"),
            @Parameter(name = "limit", description = "Maximum number of items to return in the response"),
            @Parameter(name = "orderBy", description = "List of attribute used for sorting (user_id, fullName, email, created"),
            @Parameter(name = "order", description = "The sort order. Either ascending (asc) or descending (desc)"),
            @Parameter(name = "recurse", description = "If not present, or set to 'false' or '0', only users assigned directly to this role are returned."+
                " If present and set to 'parentsOnly', the list of users assigned to all parents of the given role up to the root."+
                " If present and set to any other value than 'parentsOnly', 'false' or '0', the users assigned to this role or any parent role in the hierarchy"+
                " up to the root are returned.")
        },
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the users could be retrieved"
            ),
            @ApiResponse( responseCode = "404", description = "Role instance does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for role assignment.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    PagedResult<UserInfo> getUnassignedUsers(@PathParam( "roleId" ) String roleId,
                                       @QueryParam("recurse") String recurse,
                                       @QueryParam("q") @DefaultValue( "" ) String searchTerm,
                                       @QueryParam( "offset" ) @DefaultValue( "0" ) Integer offset,
                                       @QueryParam( "limit" ) @DefaultValue( value = DEFAULT_PAGE_LIMIT ) Integer limit,
                                       @QueryParam( "orderBy") @DefaultValue( "id" ) List<String> orderBy,
                                       @QueryParam("order") @DefaultValue( "asc" ) String order
    ) throws RedbackServiceException;


    /**
     * Updates a role. Attributes that are empty or null will be ignored.
     *
     * @since 3.0
     */
    @Path( "{roleId}" )
    @PATCH
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Creates or updates the given role",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the update was successful"
            ),
            @ApiResponse( responseCode = "404", description = "Role does not exist",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "422", description = "Role id does not match the id of the request path",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for role assignment.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    RoleInfo updateRole( @PathParam("roleId") String roleId, Role role )
    throws RedbackServiceException;

    @Path( "templates" )
    @GET
    @Produces( { APPLICATION_JSON } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    @Operation( summary = "Returns all role templates",
        security = {
            @SecurityRequirement( name = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
        },
        responses = {
            @ApiResponse( responseCode = "200",
                description = "If the list could be retrieved"
            ),
            @ApiResponse( responseCode = "403", description = "The authenticated user has not the permission for role assignment.",
                content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = RedbackRestError.class )) )
        }
    )
    List<RoleTemplate> getTemplates( ) throws RedbackServiceException;

}
