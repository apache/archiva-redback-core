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
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.archiva.redback.rest.api.model.ActionStatus;
import org.apache.archiva.redback.rest.api.model.AuthenticationKeyResult;
import org.apache.archiva.redback.rest.api.model.LoginRequest;
import org.apache.archiva.redback.rest.api.model.PingResult;
import org.apache.archiva.redback.rest.api.model.User;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Deprecated
@Path( "/loginService/" )
@Tag(name = "v1")
@Tag(name = "v1/Login")
@SecurityScheme( scheme = "BasicAuth", type = SecuritySchemeType.HTTP )
public interface LoginService
{

    @Operation( deprecated = true )
    @Path( "addAuthenticationKey" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true )
    String addAuthenticationKey( @QueryParam( "providerKey" ) String providedKey,
                                                  @QueryParam( "principal" ) String principal, @QueryParam( "purpose" ) String purpose,
                                                  @QueryParam( "expirationMinutes" ) int expirationMinutes )
        throws RedbackServiceException;


    @Operation( deprecated = true )
    @Path( "ping" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true )
    Boolean ping()
        throws RedbackServiceException;


    @Operation( deprecated = true )
    @Path( "pingWithAutz" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = false, noPermission = true )
    Boolean pingWithAutz()
        throws RedbackServiceException;

    /**
     * check username/password and create a http session.
     * So no more need of reuse username/password for all ajaxRequest
     */
    @Operation( deprecated = true )
    @Path( "logIn" )
    @POST
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    User logIn( LoginRequest loginRequest )
        throws RedbackServiceException;

    /**
     * simply check if current user has an http session opened with authz passed and return user data
     * @since 1.4
     */
    @Operation( deprecated = true )
    @Path( "isLogged" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noRestriction = true )
    User isLogged()
        throws RedbackServiceException;

    /**
     * clear user http session
     * @since 1.4
     */
    @Operation( deprecated = true )
    @Path( "logout" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    ActionStatus logout()
        throws RedbackServiceException;
}
