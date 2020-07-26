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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.Constants;
import org.apache.archiva.redback.rest.api.model.ActionStatus;
import org.apache.archiva.redback.rest.api.model.Group;
import org.apache.archiva.redback.rest.api.model.v2.PagedResult;
import org.apache.archiva.redback.rest.api.model.v2.GroupMapping;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 2.1
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
        responses = {
            @ApiResponse( description = "List of group objects. The number of returned results depend on the pagination parameters offset and limit." )
        }
    )
    PagedResult<Group> getGroups( @QueryParam( "offset" ) @DefaultValue( "0" ) Integer offset,
                                  @QueryParam( "limit" ) @DefaultValue( value = Constants.DEFAULT_PAGE_LIMIT ) Integer limit)
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
                    @Header( name="Location", description = "The URL of the created mapping")
                }
            ),
            @ApiResponse( responseCode = "405", description = "Invalid input" )
        }
    )
    ActionStatus addGroupMapping( @Parameter( description = "The data of the group mapping", required = true )
                                      GroupMapping groupMapping, @Context UriInfo uriInfo )
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
    ActionStatus removeGroupMapping( @Parameter( description = "The group name", required = true )
                                         @PathParam( "group" ) String group )
        throws RedbackServiceException;

    @Path( "mappings/{group}" )
    @PUT
    @Consumes( {MediaType.APPLICATION_JSON} )
    @Produces( {MediaType.APPLICATION_JSON} )
    @RedbackAuthorization( permissions = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION )
    @Operation( summary = "Updates a group mapping",
        responses = {
            @ApiResponse( description = "If the update was successful" ),
            @ApiResponse( responseCode = "404", description = "Group mapping not found" )
        }
    )
    ActionStatus updateGroupMapping( @Parameter( description = "The group name", required = true )
                                         @PathParam( "group" ) String groupName,
                                     @Parameter( description = "The updated role list of the group mapping", required = true )
                                         List<String> roles )
        throws RedbackServiceException;


}
