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

import org.apache.archiva.redback.rbac.AbstractRole;
import org.apache.archiva.redback.rbac.Permission;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 25.09.16.
 */
@Entity
@Table(
        name="SECURITY_ROLES"
)
public class JpaRole extends AbstractRole implements Serializable {

    @Id
    @Column(name="NAME")
    private String name;
    @Column(name="DESCRIPTION")
    private String description;
    @Column(name="ASSIGNABLE")
    private boolean assignable;
    @Column(name="PERMANENT")
    private boolean permanent;
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(
            name="SECURITY_ROLE_PERMISSION_MAP",
            joinColumns={ @JoinColumn(name="NAME_OID", referencedColumnName="NAME") },
            inverseJoinColumns = {
                    @JoinColumn(name="NAME_EID",referencedColumnName = "NAME")
            }
    )
    List<JpaPermission> permissions = new ArrayList<JpaPermission>();

    @ElementCollection
    @CollectionTable(
            name="SECURITY_ROLE_CHILDROLE_MAP",
            joinColumns = {
                    @JoinColumn(name="NAME_OID",referencedColumnName = "NAME")
            }
    )
    List<String> childRoleNames = new ArrayList<String>();



    @Override
    public void addPermission(Permission permission) {
        if (permission instanceof JpaPermission) {
            this.permissions.add((JpaPermission) permission);
        }

    }

    @Override
    public void addChildRoleName(String name) {
        this.childRoleNames.add(name);
    }

    @Override
    public List<String> getChildRoleNames() {
        return childRoleNames;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Permission> getPermissions() {
        // Maybe better to create a new list?
        return (List<Permission>)(List<?>)permissions;
    }

    @Override
    public boolean isAssignable() {
        return assignable;
    }

    @Override
    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    @Override
    public void setAssignable(boolean assignable) {
        this.assignable=assignable;
    }

    @Override
    public void setChildRoleNames(List<String> names) {
        this.childRoleNames.clear();
        this.childRoleNames.addAll(names);
    }

    @Override
    public void setDescription(String description) {
        this.description=description;

    }

    @Override
    public void setName(String name) {
        this.name=name;

    }

    @Override
    public void setPermissions(List<Permission> permissions) {
        this.permissions.clear();
        for (Permission p : permissions) {
            if (p instanceof JpaPermission) {
                permissions.add(p);
            }
        }
    }

    @Override
    public boolean isPermanent() {
        return permanent;
    }

    @Override
    public void setPermanent(boolean permanent) {
        this.permanent=permanent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        JpaRole jpaRole = (JpaRole) o;

        return name.equals(jpaRole.name);

    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
