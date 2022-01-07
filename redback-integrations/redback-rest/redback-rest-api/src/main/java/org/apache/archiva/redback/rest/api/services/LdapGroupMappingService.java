package org.apache.archiva.redback.rest.api.services;
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
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.model.ActionStatus;
import org.apache.archiva.redback.rest.api.model.LdapGroupMapping;
import org.apache.archiva.redback.rest.api.model.LdapGroupMappingUpdateRequest;
import org.apache.archiva.redback.rest.api.model.StringList;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 2.1
 */
@Path("/ldapGroupMappingService/")
@Tag( name = "v1" )
@Tag( name = "v1/LDAP" )
@SecurityScheme( scheme = "BasicAuth", type = SecuritySchemeType.HTTP )
@Deprecated
public interface LdapGroupMappingService
{

    @Operation( deprecated = true )
    @Path( "ldapGroups" )
    @GET
    @Produces( {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML} )
    @RedbackAuthorization( permissions = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION )
    StringList getLdapGroups( )
        throws RedbackServiceException;


    @Operation( deprecated = true )
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization(permissions = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION)
    List<LdapGroupMapping> getLdapGroupMappings()
        throws RedbackServiceException;


    @Operation( deprecated = true )
    @PUT
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization(permissions = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION)
    ActionStatus addLdapGroupMapping( LdapGroupMapping ldapGroupMapping )
        throws RedbackServiceException;

    @Operation( deprecated = true )
    @DELETE
    @Path("{group}")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization(permissions = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION)
    ActionStatus removeLdapGroupMapping( @PathParam("group") String group )
        throws RedbackServiceException;

    @Operation( deprecated = true )
    @POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @RedbackAuthorization(permissions = RedbackRoleConstants.CONFIGURATION_EDIT_OPERATION)
    ActionStatus updateLdapGroupMapping( LdapGroupMappingUpdateRequest ldapGroupMappingUpdateRequest )
        throws RedbackServiceException;

}
