package org.apache.archiva.redback.rest.api.model.v2;

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

    private boolean permanent;

    private String confirmPassword;

    // Display Only Fields.
    private OffsetDateTime timestampAccountCreation;

    private OffsetDateTime timestampLastLogin;

    private OffsetDateTime timestampLastPasswordChange;

    /**
     * for password change only
     *
     * @since 1.4
     */
    private String previousPassword;

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
        this.setPermanent( user.isPermanent() );
        this.setUserManagerId( user.getUserManagerId() );

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


    @XmlElement(name="user_id")
    public String getUserId()
    {
        return userId;
    }

    public void setUserId( String userId )
    {
        this.userId = userId;
    }

    public String getFullName()
    {
        return fullName;
    }

    public void setFullName( String fullName )
    {
        this.fullName = fullName;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    public boolean isValidated()
    {
        return validated;
    }

    public void setValidated( boolean validated )
    {
        this.validated = validated;
    }

    public boolean isLocked()
    {
        return locked;
    }

    public void setLocked( boolean isLocked )
    {
        this.locked = isLocked;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    public boolean isPasswordChangeRequired()
    {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired( boolean passwordChangeRequired )
    {
        this.passwordChangeRequired = passwordChangeRequired;
    }

    public boolean isPermanent()
    {
        return permanent;
    }

    public void setPermanent( boolean permanent )
    {
        this.permanent = permanent;
    }

    public String getConfirmPassword()
    {
        return confirmPassword;
    }

    public void setConfirmPassword( String confirmPassword )
    {
        this.confirmPassword = confirmPassword;
    }

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

    public String getPreviousPassword()
    {
        return previousPassword;
    }

    public void setPreviousPassword( String previousPassword )
    {
        this.previousPassword = previousPassword;
    }

    public List<String> getAssignedRoles()
    {
        return assignedRoles;
    }

    public void setAssignedRoles( List<String> assignedRoles )
    {
        this.assignedRoles = assignedRoles;
    }

    public boolean isReadOnly()
    {
        return readOnly;
    }

    public void setReadOnly( boolean readOnly )
    {
        this.readOnly = readOnly;
    }

    public String getUserManagerId()
    {
        return userManagerId;
    }

    public void setUserManagerId( String userManagerId )
    {
        this.userManagerId = userManagerId;
    }

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
            ", permanent=" + permanent +
            ", confirmPassword='" + confirmPassword + '\'' +
            ", timestampAccountCreation='" + timestampAccountCreation + '\'' +
            ", timestampLastLogin='" + timestampLastLogin + '\'' +
            ", timestampLastPasswordChange='" + timestampLastPasswordChange + '\'' +
            ", previousPassword='" + previousPassword + '\'' +
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
