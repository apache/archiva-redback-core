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

import org.apache.archiva.redback.authorization.RedbackAuthorization;
import org.apache.archiva.redback.rest.api.model.User;
import org.apache.archiva.redback.rest.api.services.RedbackServiceException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Path( "/passwordService/" )
public interface PasswordService
{

    /**
     * used to change the password on first user connection after registration use.
     * the key is mandatory and a control will be done on the username provided.
     * <b>need to be logged by {@link UserService#validateUserFromKey(String)}</b>
     * @return username
     */
    @GET
    @Path( "changePasswordWithKey" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    User changePasswordWithKey( @QueryParam( "password" ) String password,
                                  @QueryParam( "passwordConfirmation" ) String passwordConfirmation,
                                  @QueryParam( "key" ) String key )
        throws RedbackServiceException;

    /**
     * used to change the password on passwordChangeRequired state.
     */
    @GET
    @Path( "changePassword" )
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noRestriction = true, noPermission = true )
    User changePassword( @QueryParam( "userName" ) String userName,
                            @QueryParam( "previousPassword" ) String previousPassword,
                            @QueryParam( "password" ) String password,
                            @QueryParam( "passwordConfirmation" ) String passwordConfirmation )
        throws RedbackServiceException;
}
