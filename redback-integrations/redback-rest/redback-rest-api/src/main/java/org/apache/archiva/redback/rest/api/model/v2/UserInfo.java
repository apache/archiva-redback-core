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
public class UserInfo
    implements Serializable
{

    private static final long serialVersionUID = 822423853981984867L;
    private String id;

    private String userId;

    private String fullName;

    private String email;

    private boolean validated;

    private boolean locked;

    private boolean passwordChangeRequired;

    private boolean permanent;

    // Display Only Fields.
    private OffsetDateTime timestampAccountCreation;

    private OffsetDateTime timestampLastLogin;

    private OffsetDateTime timestampLastPasswordChange;

    /**
     * for roles update only <b>not return on user read</b>
     *
     * @since 2.0
     */
    private List<String> assignedRoles;

    /**
     * with some userManagerImpl it's not possible to edit users;
     * @since 2.1
     */
    private boolean readOnly;

    /**
     * as we can user multiple userManagers implementation we must track from which one this one comes.
     * @since 2.1
     */
    private String userManagerId;

    /**
     * for request validation
     *
     * @since 2.2
     */
    private String validationToken;


    public UserInfo()
    {
        // no op
    }

    public UserInfo( String userId, String fullName, String email, boolean validated, boolean locked )
    {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.validated = validated;
        this.locked = locked;
    }

    public UserInfo( org.apache.archiva.redback.users.User user )
    {
        setUserId( user.getUsername() );
        this.setEmail( user.getEmail() );
        this.setFullName( user.getFullName() );
        this.setLocked( user.isLocked() );
        this.setValidated( user.isValidated() );
        this.setPasswordChangeRequired( user.isPasswordChangeRequired() );
        this.setPermanent( user.isPermanent() );
        this.setUserManagerId( user.getUserManagerId() );
        this.setId( user.getId() );

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


    @Schema( name = "user_id", description = "The user id" )
    @XmlElement( name = "user_id" )
    public String getUserId( )
    {
        return userId;
    }

    public void setUserId( String userId )
    {
        this.userId = userId;
    }

    @Schema( description = "The full name of the user" )
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

    @Schema( description = "True, if user has to change his password" )
    public boolean isPasswordChangeRequired( )
    {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired( boolean passwordChangeRequired )
    {
        this.passwordChangeRequired = passwordChangeRequired;
    }

    @Schema(description = "True, if this is not a temporary user.")
    public boolean isPermanent()
    {
        return permanent;
    }

    public void setPermanent( boolean permanent )
    {
        this.permanent = permanent;
    }

    @Schema(description = "The date and time, when the account was first created.")
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

    @Schema(description = "Date and time of the last successful login")
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

    @Schema(description = "Date and time of the last password change")
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

    @Schema(description = "The roles assigned to the user")
    public List<String> getAssignedRoles()
    {
        return assignedRoles;
    }

    public void setAssignedRoles( List<String> assignedRoles )
    {
        this.assignedRoles = assignedRoles;
    }

    @Schema(description = "True, if this is user has readonly access")
    public boolean isReadOnly()
    {
        return readOnly;
    }

    public void setReadOnly( boolean readOnly )
    {
        this.readOnly = readOnly;
    }

    @Schema( description = "Id of the usermanager, where this user is registered")
    public String getUserManagerId()
    {
        return userManagerId;
    }

    public void setUserManagerId( String userManagerId )
    {
        this.userManagerId = userManagerId;
    }

    @Schema( description = "Current validation token of this user")
    public String getValidationToken() {
        return validationToken;
    }

    public void setValidationToken(String validationToken) {
        this.validationToken = validationToken;
    }

    @Schema( description = "User id that is unique over all user managers")
    public String getId( )
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
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
            ", permanent=" + permanent +
            ", timestampAccountCreation='" + timestampAccountCreation + '\'' +
            ", timestampLastLogin='" + timestampLastLogin + '\'' +
            ", timestampLastPasswordChange='" + timestampLastPasswordChange + '\'' +
            ", assignedRoles=" + assignedRoles +
            ", readOnly=" + readOnly +
            ", userManagerId='" + userManagerId + '\'' +
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
        if ( !( o instanceof UserInfo ) )
        {
            return false;
        }

        UserInfo user = (UserInfo) o;

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