package org.apache.archiva.redback.keys.jpa.model;

/*
 * Copyright 2001-2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.archiva.redback.keys.AuthenticationKey;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

/**
 * Authentication Key implementation for JPA.
 *
 * The table names are set to match the legacy JDO tables.
 *
 * @author <a href="mailto:martin_s@apache.org">Martin Stockhammer</a>
 */
@javax.persistence.Entity
@Table(name="JDOAUTHENTICATIONKEY")
public class JpaAuthenticationKey implements AuthenticationKey {

    @Column(name="AUTHKEY")
    @Id
    private String key;

    @Column(name="FOR_PRINCIPAL")
    private String forPrincipal;

    @Column(name="PURPOSE")
    private String purpose;

    @Column(name="DATE_CREATED")
    private Date dateCreated;

    @Column(name="DATE_EXPIRES")
    private Date dateExpires;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getForPrincipal() {
        return forPrincipal;
    }

    public void setForPrincipal(String forPrincipal) {
        this.forPrincipal = forPrincipal;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public Date getDateExpires() {
        return dateExpires;
    }

    public void setDateExpires(Date dateExpires) {
        this.dateExpires = dateExpires;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JpaAuthenticationKey that = (JpaAuthenticationKey) o;

        return key.equals(that.key);

    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
