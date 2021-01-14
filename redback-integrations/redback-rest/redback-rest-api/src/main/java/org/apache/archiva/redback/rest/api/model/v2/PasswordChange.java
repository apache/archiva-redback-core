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
import java.io.Serializable;

/**
 * Data provided to the REST service for updating the password of the current logged in user
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
@XmlRootElement( name = "passwordChange" )
@Schema(name="PasswordChange", description = "Data for password change")
public class PasswordChange implements Serializable
{
    private static final long serialVersionUID = -1173796138433747226L;
    String currentPassword;
    String userId;
    String newPassword;
    String newPasswordConfirmation;

    @Schema(name="current_password", description = "The current password of the logged in user, or a initial registration key")
    public String getCurrentPassword( )
    {
        return currentPassword;
    }

    public void setCurrentPassword( String currentPassword )
    {
        this.currentPassword = currentPassword;
    }


    @Schema(name="user_id", description = "The User Id for the user to change the password. Must match the current logged in user.")
    public String getUserId( )
    {
        return userId;
    }

    public void setUserId( String userId )
    {
        this.userId = userId;
    }

    @Schema(name="new_password", description = "The new password to set")
    public String getNewPassword( )
    {
        return newPassword;
    }

    public void setNewPassword( String newPassword )
    {
        this.newPassword = newPassword;
    }

    @Schema(name="new_password_confirmation", description = "The new password to set as confirmation that it is typed correctly")
    public String getNewPasswordConfirmation( )
    {
        return newPasswordConfirmation;
    }

    public void setNewPasswordConfirmation( String newPasswordConfirmation )
    {
        this.newPasswordConfirmation = newPasswordConfirmation;
    }
}
