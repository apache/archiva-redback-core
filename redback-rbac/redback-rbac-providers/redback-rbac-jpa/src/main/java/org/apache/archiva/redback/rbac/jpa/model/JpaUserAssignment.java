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

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Martin Stockhammer <martin_s@apache.org> on 26.09.16.
 */
@Entity
@Table(name="SECURITY_USER_ASSIGNMENTS")
public class JpaUserAssignment extends AbstractUserAssignment implements Serializable {


    @Id
    @Column(name="PRINCIPAL")
    private String principal;
    @ElementCollection
    @Column(name="STRING_ELE")
    @CollectionTable(
            name="SECURITY_USERASSIGNMENT_MAP",
            joinColumns = {
                    @JoinColumn(name = "PRINCIPAL_OID", referencedColumnName = "PRINCIPAL")
            }
    )
    private List<String> roleNames = new ArrayList<String>();
    @Column(name="PERMANENT")
    private boolean permanent = false;

    @Override
    public String getPrincipal() {
        return principal;
    }

    @Override
    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    @Override
    public List<String> getRoleNames() {
        return roleNames;
    }

    @Override
    public void setRoleNames(List<String> roleNames) {
        this.roleNames = roleNames;
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
}
