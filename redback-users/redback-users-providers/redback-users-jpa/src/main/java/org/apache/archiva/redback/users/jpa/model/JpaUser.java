package org.apache.archiva.redback.users.jpa.model;

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

import org.apache.openjpa.persistence.ExternalValues;
import org.apache.openjpa.persistence.Type;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by martin on 20.09.16.
 */
@Entity
@Table(name="JDOUSER")
public class JpaUser implements org.apache.archiva.redback.users.User {

    @Id
    @Column(name="USERNAME")
    private String username;
    @Column(name="FULL_NAME")
    private String fullName;
    @Column(name="EMAIL")
    private String email;
    @Column(name="ENCODED_PASSWORD")
    private String encodedPassword;
    @Column(name="LAST_PASSWORD_CHANGE")
    private Date lastPasswordChange;
    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn(name="INTEGER_IDX", nullable = false)
    @Column(name="STRING_ELE", nullable = false)
    @CollectionTable(name="JDOUSER_PREVIOUSENCODEDPASSWORDS",
            joinColumns = @JoinColumn(name = "USERNAME_OID", nullable = false, referencedColumnName = "USERNAME")
    )
    private List<String> previousEncodedPasswords = new ArrayList<String>();
    @Column(name="PERMANENT", nullable = false)
    private Boolean permanent = false;
    @Column(name="LOCKED", nullable = false)
    private Boolean locked = false;
    @Column(name="PASSWORD_CHANGE_REQUIRED", nullable = false)
    private Boolean passwordChangeRequired = false;
    @Column(name="VALIDATED", nullable = false)
    private Boolean validated = false;
    @Column(name="COUNT_FAILED_LOGIN_ATTEMPTS",nullable = false)
    private int countFailedLoginAttempts = 0;
    @Column(name="ACCOUNT_CREATION_DATE")
    private Date accountCreationDate;
    @Column(name="LAST_LOGIN_DATE")
    private Date lastLoginDate;
    @Column(name="USER_PASSWORD")
    private String rawPassword;


    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String name) {
        this.username = name;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public void setFullName(String name) {
        this.fullName = name;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String address) {
        this.email = address;
    }

    @Override
    public String getPassword() {
        return rawPassword;
    }

    @Override
    public void setPassword(String rawPassword) {
        this.rawPassword = rawPassword;
    }

    @Override
    public String getEncodedPassword() {
        return encodedPassword;
    }

    @Override
    public void setEncodedPassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    @Override
    public Date getLastPasswordChange() {
        return lastPasswordChange;
    }

    @Override
    public void setLastPasswordChange(Date passwordChangeDate) {
        this.lastPasswordChange = passwordChangeDate;
    }

    @Override
    public List<String> getPreviousEncodedPasswords() {
        if (previousEncodedPasswords==null) {
            setPreviousEncodedPasswords(new ArrayList<String>());
        }
        assert previousEncodedPasswords != null;
        return previousEncodedPasswords;
    }

    @Override
    public void setPreviousEncodedPasswords(List<String> encodedPasswordList) {
        if (previousEncodedPasswords==null) {
            previousEncodedPasswords = new ArrayList<String>();
        }
        previousEncodedPasswords.clear();
        previousEncodedPasswords.addAll(encodedPasswordList);
    }

    @Override
    public void addPreviousEncodedPassword(String encodedPassword) {
        if (previousEncodedPasswords==null) {
            previousEncodedPasswords = new ArrayList<String>();
        }
        previousEncodedPasswords.add(encodedPassword);
    }

    @Override
    public boolean isPermanent() {
        return permanent;
    }

    @Override
    public void setPermanent(boolean permanent) {
        this.permanent = permanent;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    @Override
    public void setPasswordChangeRequired(boolean changeRequired) {
        this.passwordChangeRequired = changeRequired;
    }

    @Override
    public boolean isValidated() {
        return validated;
    }

    @Override
    public void setValidated(boolean valid) {
        this.validated = valid;
    }

    @Override
    public int getCountFailedLoginAttempts() {
        return countFailedLoginAttempts;
    }

    @Override
    public void setCountFailedLoginAttempts(int count) {
        this.countFailedLoginAttempts = count;
    }

    @Override
    public Date getAccountCreationDate() {
        return accountCreationDate;
    }

    @Override
    public void setAccountCreationDate(Date date) {
        this.accountCreationDate = date;
    }

    @Override
    public Date getLastLoginDate() {
        return lastLoginDate;
    }

    @Override
    public void setLastLoginDate(Date date) {
        this.lastLoginDate = date;
    }

    @Override
    public String getUserManagerId() {
        return "jpa";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JpaUser jpaUser = (JpaUser) o;

        return username.equals(jpaUser.username);

    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }
}
