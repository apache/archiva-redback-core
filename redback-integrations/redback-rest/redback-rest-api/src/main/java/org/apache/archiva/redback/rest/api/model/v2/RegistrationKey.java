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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * @author Olivier Lamy
 * @author Martin Stockhammer
 */
@XmlRootElement( name = "registrationKey" )
public class RegistrationKey
    implements Serializable
{
    private String key;
    boolean emailValidationRequired=true;

    public RegistrationKey()
    {
        // nope
    }

    public RegistrationKey( String key, boolean emailValidationRequired )
    {
        this.key = key;
        this.emailValidationRequired = emailValidationRequired;
    }

    @Schema(description = "The key sent after registration, which is used to verify")
    public String getKey()
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }

    @Schema(name="email_validation_required",description = "If true, email validation is required for registration.")
    public boolean isEmailValidationRequired( )
    {
        return emailValidationRequired;
    }

    public void setEmailValidationRequired( boolean emailValidationRequired )
    {
        this.emailValidationRequired = emailValidationRequired;
    }
}
