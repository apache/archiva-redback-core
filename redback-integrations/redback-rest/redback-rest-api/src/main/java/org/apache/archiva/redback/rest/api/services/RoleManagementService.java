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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.integration.security.role.RedbackRoleConstants;
import org.apache.archiva.redback.rest.api.model.ActionStatus;
import org.apache.archiva.redback.rest.api.model.Application;
import org.apache.archiva.redback.rest.api.model.ApplicationRoles;
import org.apache.archiva.redback.rest.api.model.v2.AvailabilityStatus;
import org.apache.archiva.redback.rest.api.model.Role;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.model.VerificationStatus;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 *
 * @deprecated Use the new v2 service {@link org.apache.archiva.redback.rest.api.services.v2.RoleService}
 * @author Olivier Lamy
 */
@Deprecated
@Path( "/roleManagementService/" )
public interface RoleManagementService
{

    @Path( "createTemplatedRole" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    ActionStatus createTemplatedRole( @QueryParam( "templateId" ) String templateId,
                                      @QueryParam( "resource" ) String resource )
        throws RedbackServiceException;

    /**
     * removes a role corresponding to the role Id that was manufactured with the given resource
     *
     * it also removes any user assignments for that role
     *
     * @param templateId
     * @param resource
     */
    @Path( "removeTemplatedRole" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    ActionStatus removeTemplatedRole( @QueryParam( "templateId" ) String templateId,
                                 @QueryParam( "resource" ) String resource )
        throws RedbackServiceException;


    /**
     * allows for a role coming from a template to be renamed effectively swapping out the bits of it that
     * were labeled with the oldResource with the newResource
     *
     * it also manages any user assignments for that role
     *
     * @param templateId
     * @param oldResource
     * @param newResource
     */
    @Path( "updateRole" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    ActionStatus updateRole( @QueryParam( "templateId" ) String templateId, @QueryParam( "oldResource" ) String oldResource,
                        @QueryParam( "newResource" ) String newResource )
        throws RedbackServiceException;


    /**
     * Assigns the role indicated by the roleId to the given principal
     *
     * @param roleId
     * @param principal
     */
    @Path( "assignRole" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    ActionStatus assignRole( @QueryParam( "roleId" ) String roleId, @QueryParam( "principal" ) String principal )
        throws RedbackServiceException;

    /**
     * Assigns the role indicated by the roleName to the given principal
     *
     * @param roleName
     * @param principal
     * @throws RedbackServiceException
     */
    @Path( "assignRoleByName" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    ActionStatus assignRoleByName( @QueryParam( "roleName" ) String roleName, @QueryParam( "principal" ) String principal )
        throws RedbackServiceException;

    /**
     * Assigns the templated role indicated by the templateId
     *
     * fails if the templated role has not been created
     *
     * @param templateId
     * @param resource
     * @param principal
     */
    @Path( "assignTemplatedRole" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    ActionStatus assignTemplatedRole( @QueryParam( "templateId" ) String templateId,
                                 @QueryParam( "resource" ) String resource,
                                 @QueryParam( "principal" ) String principal )
        throws RedbackServiceException;

    /**
     * Unassigns the role indicated by the role id from the given principal
     *
     * @param roleId
     * @param principal
     * @throws RedbackServiceException
     */
    @Path( "unassignRole" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    ActionStatus unassignRole( @QueryParam( "roleId" ) String roleId, @QueryParam( "principal" ) String principal )
        throws RedbackServiceException;

    /**
     * Unassigns the role indicated by the role name from the given principal
     *
     * @param roleName
     * @param principal
     * @throws RedbackServiceException
     */
    @Path( "unassignRoleByName" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    ActionStatus unassignRoleByName( @QueryParam( "roleName" ) String roleName, @QueryParam( "principal" ) String principal )
        throws RedbackServiceException;

    /**
     * true of a role exists with the given roleId
     *
     * @param roleId
     * @return
     * @throws RedbackServiceException
     */
    @Path( "roleExists" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    Boolean roleExists( @QueryParam( "roleId" ) String roleId )
        throws RedbackServiceException;

    /**
     * true of a role exists with the given roleId
     *
     * @param templateId
     * @param resource
     * @return
     * @throws RedbackServiceException
     */
    @Path( "templatedRoleExists" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    Boolean templatedRoleExists( @QueryParam( "templateId" ) String templateId,
                                 @QueryParam( "resource" ) String resource )
        throws RedbackServiceException;


    /**
     * Check a role template is complete in the RBAC store.
     *
     * @param templateId the templated role
     * @param resource   the resource to verify
     * @throws RedbackServiceException
     */
    @Path( "verifyTemplatedRole" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    VerificationStatus verifyTemplatedRole( @QueryParam( "templateId" ) String templateId,
                                            @QueryParam( "resource" ) String resource )
        throws RedbackServiceException;

    /**
     * @since 1.4
     */
    @Path( "getEffectivelyAssignedRoles/{username}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    List<Role> getEffectivelyAssignedRoles( @PathParam( "username" ) String username )
        throws RedbackServiceException;


    /**
     * @since 2.0
     */
    @Path( "allRoles" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    List<Role> getAllRoles()
        throws RedbackServiceException;

    /**
     * @since 2.0
     */
    @Path( "detailledAllRoles" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    List<Role> getDetailedAllRoles()
        throws RedbackServiceException;


    /**
     * @since 2.0
     */
    @Path( "getApplications/{username}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    List<Application> getApplications( @PathParam( "username" ) String username )
        throws RedbackServiceException;


    /**
     * @since 2.0
     */
    @Path( "getRole/{roleName}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    Role getRole( @PathParam( "roleName" ) String roleName )
        throws RedbackServiceException;

    /**
     * @since 2.0
     */
    @Path( "updateRoleDescription" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    ActionStatus updateRoleDescription( @QueryParam( "roleName" ) String roleName,
                                   @QueryParam( "roleDescription" ) String description )
        throws RedbackServiceException;

    /**
     * update users assigned to a role
     * @since 2.0
     */
    @Path( "updateRoleUsers" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    ActionStatus updateRoleUsers( Role role )
        throws RedbackServiceException;

    /**
     * @since 2.0
     */
    @Path( "getApplicationRoles/{username}" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    List<ApplicationRoles> getApplicationRoles( @PathParam( "username" ) String username )
        throws RedbackServiceException;

    /**
     * update roles assigned to a user
     * @since 2.0
     */
    @Path( "updateUserRoles" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @Consumes( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( permissions = RedbackRoleConstants.USER_MANAGEMENT_RBAC_ADMIN_OPERATION )
    ActionStatus updateUserRoles( User user )
        throws RedbackServiceException;

}
