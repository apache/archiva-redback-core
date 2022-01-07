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
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.archiva.components.rest.model.PagedResult;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.Constants;
import org.apache.archiva.redback.rest.api.model.v2.Group;
import org.apache.archiva.redback.rest.api.model.v2.GroupMapping;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;

import static org.apache.archiva.redback.rest.api.Constants.DEFAULT_PAGE_LIMIT;

/**
 * @author Olivier Lamy
 * @author Martin Stockhammer
 * @since 3.0
 */
@Path( "/groups" )
@Tag(name = "v2")
@Tag(name = "v2/Groups")
@SecurityRequirement(name = "BearerAuth")
public interface GroupService
{

    @Path( "" )
    @GET
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization( permissions = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION )
    @Operation( summary = "Get list of group objects",
        parameters = {
            @Parameter(name = "q", description = "Search term"),
            @Parameter(name = "offset", description = "The offset of the first element returned"),
            @Parameter(name = "limit", description = "Maximum number of items to return in the response"),
            @Parameter(name = "orderBy", description = "List of attribute used for sorting (id, name, description, assignable)"),
            @Parameter(name = "order", description = "The sort order. Either ascending (asc) or descending (desc)")
        },

        responses = {
            @ApiResponse( description = "List of group objects. The number of returned results depend on the pagination parameters offset and limit." )
        }
    )
    PagedResult<Group> getGroups( @QueryParam("q") @DefaultValue( "" ) String searchTerm,
                                  @QueryParam( "offset" ) @DefaultValue( "0" ) Integer offset,
                                  @QueryParam( "limit" ) @DefaultValue( value = DEFAULT_PAGE_LIMIT ) Integer limit,
                                  @QueryParam( "orderBy") @DefaultValue( "name" ) List<String> orderBy,
                                  @QueryParam("order") @DefaultValue( "asc" ) String order)
        throws RedbackServiceException;


    @Path( "mappings" )
    @GET
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization( permissions = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION )
    @Operation( summary = "Get list of group mappings",
        responses = {
            @ApiResponse( description = "List of group mappings" )
        }
    )
    List<GroupMapping> getGroupMappings( )
        throws RedbackServiceException;


    @Path( "mappings" )
    @POST
    @Consumes( {MediaType.APPLICATION_JSON} )
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization( permissions = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION )
    @Operation( summary = "Adds a group mapping",
        responses = {
            @ApiResponse( responseCode = "201",
                description = "If the group addition was successful",
                headers = {
                    @Header( name="Location", description = "The URL of the created mapping", schema = @Schema(type="string"))
                }
            ),
            @ApiResponse( responseCode = "405", description = "Invalid input" )
        }
    )
    Response addGroupMapping( @Parameter( description = "The data of the group mapping", required = true )
                                      GroupMapping groupMapping, @Context UriInfo uriInfo )
        throws RedbackServiceException;

    @Path( "mappings/{group}" )
    @GET
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization( permissions = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION )
    @Operation( summary = "Returns the list of roles of a given group mapping",
        responses = {
            @ApiResponse( responseCode = "200", description = "If the list could be returned" ),
            @ApiResponse( responseCode = "404", description = "Group mapping not found" )
        }
    )
    List<String> getGroupMapping( @Parameter( description = "The group name", required = true )
                             @PathParam( "group" ) String group )
        throws RedbackServiceException;

    @Path( "mappings/{group}" )
    @DELETE
    @Consumes( {MediaType.APPLICATION_JSON} )
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization( permissions = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION )
    @Operation( summary = "Deletes a group mapping",
        responses = {
            @ApiResponse( responseCode = "200", description = "If the status of the delete action was successful" ),
            @ApiResponse( responseCode = "404", description = "Group mapping not found" )
        }
    )
    void removeGroupMapping( @Parameter( description = "The group name", required = true )
                                         @PathParam( "group" ) String group )
        throws RedbackServiceException;

    @Path( "mappings/{group}" )
    @PUT
    @Consumes( {MediaType.APPLICATION_JSON} )
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization( permissions = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION )
    @Operation( summary = "Updates a group mapping",
        responses = {
            @ApiResponse( responseCode = "200", description = "If the update was successful" ),
            @ApiResponse( responseCode = "404", description = "Group mapping not found" )
        }
    )
    Response updateGroupMapping( @Parameter( description = "The group name", required = true )
                                         @PathParam( "group" ) String groupName,
                                     @Parameter( description = "The updated role list of the group mapping", required = true )
                                         List<String> roles )
        throws RedbackServiceException;

    @Path( "mappings/{group}/roles/{roleId}" )
    @PUT
    @Consumes( {MediaType.APPLICATION_JSON} )
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization( permissions = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION )
    @Operation( summary = "Adds a role to a given group mapping.",
        responses = {
            @ApiResponse( responseCode = "200", description = "If the update was successful" ),
        }
    )
    Response addRolesToGroupMapping( @Parameter( description = "The group name", required = true )
                                 @PathParam( "group" ) String groupName,
                                 @PathParam( "roleId" )
                                 @Parameter( description = "The id of the role to add to the mapping", required = true )
                                     String roleId )
        throws RedbackServiceException;
}
