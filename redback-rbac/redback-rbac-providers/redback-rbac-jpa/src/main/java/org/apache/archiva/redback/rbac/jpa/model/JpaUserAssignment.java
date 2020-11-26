package org.apache.archiva.redback.rbac.jpa.model;

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

import org.apache.archiva.redback.rbac.AbstractUserAssignment;
import org.apache.archiva.redback.rbac.UserAssignment;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Martin Stockhammer <martin_s@apache.org> on 26.09.16.
 */
@Entity
@Table(name="SECURITY_USER_ASSIGNMENTS")
public class JpaUserAssignment extends AbstractUserAssignment implements UserAssignment,Serializable {


    @Id
    @Column(name="PRINCIPAL")
    private String principal;
    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name="STRING_ELE")
    @OrderColumn(name="INTEGER_IDX", nullable = false)
    @CollectionTable(
            name="SECURITY_USERASSIGNMENT_ROLENAMES",
            joinColumns = {
                    @JoinColumn(name = "PRINCIPAL_OID", referencedColumnName = "PRINCIPAL", nullable = false)
            }
    )
    private List<String> roleIds = new ArrayList<>( );

    @Column(name="PERMANENT", nullable = false)
    private Boolean permanent = false;

    @Column(name="LAST_UPDATED")
    private Date timestamp;

    @Override
    public String getPrincipal() {
        return principal;
    }

    @Override
    public List<String> getRoleNames( )
    {
        return roleIds;
    }

    @Override
    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    @Override
    public void setRoleNames( List<String> roles )
    {
        this.roleIds = roles;
    }

    @Override
    public List<String> getRoleIds() {
        return roleIds;
    }

    @Override
    public void setRoleIds( List<String> roleIds ) {
        this.roleIds = roleIds;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JpaUserAssignment that = (JpaUserAssignment) o;

        return principal.equals(that.principal);

    }

    @Override
    public int hashCode() {
        return principal.hashCode();
    }


    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

}
