package org.apache.archiva.redback.rest.api.model.v2;

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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="verificationStatus")
@Schema(name="VerificationStatus", description = "The verification status of the user registration")
public class VerificationStatus
{
    boolean success = false;
    String accessToken;

    public VerificationStatus() {

    }

    public VerificationStatus( boolean success ) {
        this.success = success;
    }

    @Schema(name="success",description = "True, if verification was successful")
    public boolean isSuccess( )
    {
        return success;
    }

    public void setSuccess( boolean success )
    {
        this.success = success;
    }

    @Schema(name="access_token", description = "The access token that is used for registration")
    public String getAccessToken( )
    {
        return accessToken;
    }

    public void setAccessToken( String accessToken )
    {
        this.accessToken = accessToken;
    }
}
