package org.apache.archiva.redback.rest.api.model.v2;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

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

@XmlRootElement( name = "user" )
@Schema(name="User", description = "User information data")
public class User
    implements Serializable
{


    private static final long serialVersionUID = 7457798933140993643L;

    private String userId;

    private String fullName;

    private String email;

    private boolean validated;

    private boolean locked;

    private String password;

    private boolean passwordChangeRequired;

    private String confirmPassword;

    // Display Only Fields.
    private OffsetDateTime timestampAccountCreation;

    private OffsetDateTime timestampLastLogin;

    private OffsetDateTime timestampLastPasswordChange;

    /**
     * for password change only
     *
     */
    private String currentPassword;

    /**
     * for roles update only <b>not return on user read</b>
     *
     * @since 2.0
     */
    private List<String> assignedRoles;

    /**
     * for request validation
     *
     * @since 2.2
     */
    private String validationToken;


    public User()
    {
        // no op
    }

    public User( String userId, String fullName, String email, boolean validated, boolean locked )
    {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.validated = validated;
        this.locked = locked;
    }

    public User( org.apache.archiva.redback.users.User user )
    {
        setUserId( user.getUsername() );
        this.setEmail( user.getEmail() );
        this.setFullName( user.getFullName() );
        this.setLocked( user.isLocked() );
        this.setPassword( user.getPassword() );
        this.setValidated( user.isValidated() );
        this.setPasswordChangeRequired( user.isPasswordChangeRequired() );

        if (user.getAccountCreationDate()==null) {
            setTimestampAccountCreationByInstant( Instant.EPOCH );
        } else {
            setTimestampAccountCreationByInstant( user.getAccountCreationDate().toInstant() );
        }
        if (user.getLastLoginDate()==null) {
            setTimestampLastLoginByInstant( Instant.EPOCH );
        } else
        {
            setTimestampLastLoginByInstant( user.getLastLoginDate( ).toInstant( ) );
        }
        if (user.getLastPasswordChange()==null) {
            setTimestampLastLoginByInstant( Instant.EPOCH );
        } else
        {
            setTimestampLastPasswordChangeByInstant( user.getLastPasswordChange( ).toInstant( ) );
        }
    }


    @Schema( name = "user_id", description = "The user id", required = true )
    @XmlElement( name = "user_id" )
    public String getUserId( )
    {
        return userId;
    }

    public void setUserId( String userId )
    {
        this.userId = userId;
    }

    @Schema( name="full_name", description = "The full name of the user" )
    public String getFullName( )
    {
        return fullName;
    }

    public void setFullName( String fullName )
    {
        this.fullName = fullName;
    }

    @Schema( description = "Email address" )
    public String getEmail( )
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    @Schema( description = "True, if user is validated, or False, if user is still in register phase.")
    public boolean isValidated()
    {
        return validated;
    }

    public void setValidated( boolean validated )
    {
        this.validated = validated;
    }

    @Schema(description = "True, if user is locked.")
    public boolean isLocked()
    {
        return locked;
    }

    public void setLocked( boolean isLocked )
    {
        this.locked = isLocked;
    }

    @Schema(description = "The password. This is required for creating new users." )
    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    @Schema(name="password_change_required", description = "True, if user has to change password")
    public boolean isPasswordChangeRequired()
    {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired( boolean passwordChangeRequired )
    {
        this.passwordChangeRequired = passwordChangeRequired;
    }

    @Schema(name="confirm_password",description = "The password confirmation, must be identical to the password.")
    public String getConfirmPassword()
    {
        return confirmPassword;
    }

    public void setConfirmPassword( String confirmPassword )
    {
        this.confirmPassword = confirmPassword;
    }

    @Schema(name="timestamp_account_creation",description = "The time, when the account was created")
    public OffsetDateTime getTimestampAccountCreation()
    {
        return timestampAccountCreation;
    }

    public void setTimestampAccountCreation( OffsetDateTime timestampAccountCreation )
    {
        this.timestampAccountCreation = timestampAccountCreation;
    }

    public void setTimestampAccountCreationByInstant( Instant timestampAccountCreation )
    {
        this.timestampAccountCreation = OffsetDateTime.ofInstant( timestampAccountCreation, ZoneId.systemDefault() );
    }

    @Schema(name="timestamp_last_login",description = "The time of the last user login (password based login).")
    public OffsetDateTime getTimestampLastLogin()
    {
        return timestampLastLogin;
    }

    public void setTimestampLastLogin( OffsetDateTime timestampLastLogin )
    {
        this.timestampLastLogin = timestampLastLogin;
    }

    public void setTimestampLastLoginByInstant( Instant timestampLastLogin )
    {
        this.timestampLastLogin = OffsetDateTime.ofInstant( timestampLastLogin, ZoneId.systemDefault( ) );
    }

    @Schema(name="timestamp_last_password_change",description = "The time of the last password change of this account.")
    public OffsetDateTime getTimestampLastPasswordChange()
    {
        return timestampLastPasswordChange;
    }

    public void setTimestampLastPasswordChange( OffsetDateTime timestampLastPasswordChange )
    {
        this.timestampLastPasswordChange = timestampLastPasswordChange;
    }

    public void setTimestampLastPasswordChangeByInstant( Instant timestampLastPasswordChange )
    {
        this.timestampLastPasswordChange = OffsetDateTime.ofInstant( timestampLastPasswordChange, ZoneId.systemDefault() );
    }

    @Schema(name="current_password",description = "The current password")
    public String getCurrentPassword()
    {
        return currentPassword;
    }

    public void setCurrentPassword( String currentPassword )
    {
        this.currentPassword = currentPassword;
    }

    @Schema(name="assigned_roles",description = "List of role ids assigned to this user")
    public List<String> getAssignedRoles()
    {
        return assignedRoles;
    }

    public void setAssignedRoles( List<String> assignedRoles )
    {
        this.assignedRoles = assignedRoles;
    }

    @Schema(name="validation_token",description = "The token for request validation.")
    public String getValidationToken() {
        return validationToken;
    }

    public void setValidationToken(String validationToken) {
        this.validationToken = validationToken;
    }

    @Override
    public String toString()
    {
        return "User{" +
            "username='" + userId + '\'' +
            ", fullName='" + fullName + '\'' +
            ", email='" + email + '\'' +
            ", validated=" + validated +
            ", locked=" + locked +
            //", password='" + password + '\'' +
            ", passwordChangeRequired=" + passwordChangeRequired +
            ", confirmPassword='" + confirmPassword + '\'' +
            ", timestampAccountCreation='" + timestampAccountCreation + '\'' +
            ", timestampLastLogin='" + timestampLastLogin + '\'' +
            ", timestampLastPasswordChange='" + timestampLastPasswordChange + '\'' +
            ", previousPassword='" + currentPassword + '\'' +
            ", assignedRoles=" + assignedRoles +
            ", validationToken='" + validationToken + '\'' +
            '}';
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof User ) )
        {
            return false;
        }

        User user = (User) o;

        if ( !userId.equals( user.userId ) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return userId.hashCode();
    }
}
