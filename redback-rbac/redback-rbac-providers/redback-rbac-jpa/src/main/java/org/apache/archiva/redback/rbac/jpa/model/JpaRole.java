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
import org.apache.archiva.redback.rbac.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

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
@IdClass( RoleId.class )
public class JpaRole extends AbstractRole implements Serializable {

    private static final Logger log = LoggerFactory.getLogger( JpaRole.class );
    private static final long serialVersionUID = 4564608138465995665L;

    @Id
    @Column(name="NAME", unique = true)
    private String name;
    @Id
    @Column( name = "ID", unique = true )
    private String id;
    @Column(name="DESCRIPTION")
    private String description;
    @Column(name="ASSIGNABLE",nullable = false)
    private Boolean assignable = false;
    @Column(name="PERMANENT", nullable = false)
    private Boolean permanent = false;
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderColumn(name="INTEGER_IDX", nullable = false)
    @JoinTable(
            name="SECURITY_ROLE_PERMISSION_MAP",
            joinColumns={ @JoinColumn(name="NAME_OID", referencedColumnName="NAME", nullable = false) },
            inverseJoinColumns = {
                    @JoinColumn(name="NAME_EID",referencedColumnName = "NAME")
            }
    )
    List<JpaPermission> permissions = new ArrayList<JpaPermission>();

    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn(name="INTEGER_IDX",nullable = false)
    @Column(name="STRING_ELE")
    @CollectionTable(
            name="SECURITY_ROLE_CHILDROLE_MAP",
            joinColumns = {
                    @JoinColumn(name="NAME_OID",referencedColumnName = "NAME", nullable = false)
            }
    )
    List<String> childRoleNames = new ArrayList<String>();

    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn(name="INTEGER_IDX",nullable = false)
    @Column(name="CHILD_IDS")
    @CollectionTable(
        name="SECURITY_ROLE_CHILDROLE_ID_MAP",
        joinColumns = {
            @JoinColumn(name="ID_OID",referencedColumnName = "ID", nullable = false)
        }
    )
    List<String> childRoleIds = new ArrayList<String>();

    @Column(name="TEMPLATE_INSTANCE",nullable = false)
    private Boolean templateInstance = false;

    @Column(name="MODEL_ID",nullable = false)
    private String modelId = "";

    @Column(name="RESOURCE",nullable = false)
    private String resource = "";

    public JpaRole( )
    {
    }

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
    public void addChildRoleId( String id )
    {
        this.childRoleIds.add( id );
    }

    @Override
    public List<String> getChildRoleNames() {
        return childRoleNames;
    }

    @Override
    public List<String> getChildRoleIds( )
    {
        return childRoleIds;
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
    public List<? extends Permission> getPermissions() {
        return permissions;
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
    public void setChildRoleIds( List<String> childRoleIds )
    {
        this.childRoleIds.clear();
        this.childRoleIds.addAll( childRoleIds );
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
    public void setId( String id )
    {
        if (id==null)  {
            log.error( "Null value for role id" );
            throw new NullPointerException( "ID may not be null" );
        }
        this.id = id;
    }

    @Override
    public String getId( )
    {
        return id;
    }

    @Override
    public void setModelId( String modelId )
    {
        if (modelId==null) {
            this.modelId = "";
        } else
        {
            this.modelId = modelId;
        }
    }

    @Override
    public String getModelId( )
    {
        return modelId;
    }

    @Override
    public void setTemplateInstance( boolean templateInstanceFlag )
    {
        this.templateInstance = templateInstanceFlag;
    }

    @Override
    public boolean isTemplateInstance( )
    {
        return this.templateInstance;
    }

    @Override
    public void setResource( String resource )
    {
        if (resource==null) {
            this.resource = "";
        } else
        {
            this.resource = resource;
        }
    }

    @Override
    public String getResource( )
    {
        return resource;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o ) return true;
        if ( o == null || getClass( ) != o.getClass( ) ) return false;
        if ( !super.equals( o ) ) return false;

        JpaRole jpaRole = (JpaRole) o;

        if ( !name.equals( jpaRole.name ) ) return false;
        return id.equals( jpaRole.id );
    }

    @Override
    public int hashCode( )
    {
        int result = name.hashCode( );
        result = 31 * result + id.hashCode( );
        return result;
    }
}
