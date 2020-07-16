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
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.rest.api.model.PingResult;
import org.apache.archiva.redback.rest.api.model.TokenRequest;
import org.apache.archiva.redback.rest.api.model.RequestTokenRequest;
import org.apache.archiva.redback.rest.api.model.TokenResponse;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Version 2 of authentication service
 *
 * @since 3.0
 */
@Path( "/auth" )
@Tag(name = "v2")
@Tag(name = "v2/Authentication")
public interface AuthenticationService
{

    /**
     * Just a ping request / response for checking availability of the server
     * @return the ping result
     * @throws RedbackServiceException
     */
    @Path( "ping" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true )
    PingResult ping()
        throws RedbackServiceException;


    /**
     * This ping request is only successful, if the provided Bearer token is valid and authenticates a existing user
     * @return the ping result or a failure message
     * @throws RedbackServiceException
     */
    @Path( "ping/authenticated" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = false, noPermission = true )
    @Operation( summary = "Ping request to restricted service. You have to provide a valid authentication token." )
    @SecurityRequirement( name="BearerAuth" )
    PingResult pingWithAutz()
        throws RedbackServiceException;

    /**
     * Check username/password and return a bearer token.
     * The bearer token can be added to the HTTP header on further requests to authenticate.
     *
     */
    @Path( "authenticate" )
    @POST
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    @Produces( { MediaType.APPLICATION_JSON } )
    @Operation( summary = "Authenticate by user/password login and return a bearer token, usable for further requests",
        responses = {
            @ApiResponse( description = "A access token, that has to be added to the Authorization header on authenticated requests. " +
                "And refresh token, used to refresh the access token. Each token as a lifetime. After expiration it cannot be used anymore." )
        }
    )
    TokenResponse logIn( RequestTokenRequest loginRequest )
        throws RedbackServiceException;

    /**
     * Request a new token.
     */
    @Path( "token" )
    @POST
    @RedbackAuthorization( noPermission = true )
    @Produces( { MediaType.APPLICATION_JSON } )
    @Operation( summary = "Creates a new access token based on the given payload. Currently only grant_type=refresh_token is "+
        "supported. You have to provide the refresh token in the payload. And you have to provide a valid Bearer access token in "+
        "the Authorization header.",
        responses = {
            @ApiResponse( description = "The new access token," )
        }
    )
    @SecurityRequirement( name="BearerAuth" )
    TokenResponse token( TokenRequest tokenRequest )
        throws RedbackServiceException;


    /**
     * Check, if the current request is authenticated and if so return the current user data
     */
    @Path( "authenticated" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    @RedbackAuthorization( noRestriction = true )
    @Operation(summary = "Checks the request for a valid access token, and returns the user object that corresponds to the " +
        "provided token.")
    User getAuthenticatedUser()
        throws RedbackServiceException;

}