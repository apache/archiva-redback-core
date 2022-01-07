package org.apache.archiva.redback.rest.services.interceptors.v2;
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

import org.apache.archiva.redback.policy.PasswordRuleViolationException;
import org.apache.archiva.redback.policy.PasswordRuleViolations;
import org.apache.archiva.redback.rest.api.model.ErrorMessage;
import org.apache.archiva.redback.rest.api.model.RedbackRestError;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.ArrayList;
import java.util.List;

/**
 * This implementation returns a 422 status code.
 * 
 * @author Olivier Lamy
 * @since 3.0
 */
@Provider
@Service( "v2.passwordRuleViolationExceptionMapper" )
public class PasswordRuleViolationExceptionMapper
    implements ExceptionMapper<PasswordRuleViolationException>
{
    public Response toResponse( PasswordRuleViolationException e )
    {
        RedbackRestError restError = new RedbackRestError();

        List<ErrorMessage> errorMessages = new ArrayList<ErrorMessage>( e.getViolations().getViolations().size() );
        for ( PasswordRuleViolations.MessageReference messageReference : e.getViolations().getViolations() )
        {
            errorMessages.add( new ErrorMessage( messageReference.getKey(), messageReference.getArgs() ) );
        }
        restError.setErrorMessages( errorMessages );
        Response.ResponseBuilder responseBuilder = Response.status( 422 ).entity( restError );
        return responseBuilder.build();
    }
}
