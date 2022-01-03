package org.apache.archiva.redback.rbac;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * Role
 *
 * A role is assignable to a user and effectively grants that user all of the
 * permissions that are present in that role.  A role can also contain other roles
 * which add the permissions in those roles to the available permissions for authorization.
 *
 * A role can contain any number of permissions
 * A role can contain any number of other roles
 * A role can be assigned to any number of users
 *
 * @author Jesse McConnell
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @author Martin Stockhammer <martin_s@apache.org>
 *
 */
public interface Role extends Serializable
{

    /**
     * Adds a permission to the list
     *
     * @param permission the permission to add to the list
     */
    void addPermission( Permission permission );

    /**
     * Adds a role to the list of child roles
     *
     * @param name the name of the child role.
     */
    void addChildRoleName( String name );

    /**
     * Returns the list of child roles
     */
    List<String> getChildRoleNames();

    /**
     * Adds a child role and sets the list of child names and child ids.
     * @param child the child role
     */
    void addChildRole( Role child );

    /**
     * Adds a child role id
     * @param id the id
     */
    void addChildRoleId( String id );

    /**
     * Returns the child role ids
     * @return the list of child role ids
     */
    List<String> getChildRoleIds();

    /**
     * Convenience method to see if Role has Child Roles.
     *
     * @return true if child roles exists and has any roles being tracked.
     */
    boolean hasChildRoles();

    /**
     * Long description of the role.
     * @return the role description
     */
    String getDescription();

    /**
     * Get the name. Must be unique.
     *
     * NOTE: This field is considered the Primary Key for this object.
     * @return the name of the role
     */
    String getName();

    /**
     * Returns the list of permissions assigned to this role.
     * @return the list of permissions assigned to this role
     */
    List<? extends Permission> getPermissions();

    /**
     * <code>True</code>, if this role is available to be assigned to a user, otherwise <code>false</code>.
     *
     * @return <code>true</code>, if this role can be assigned to users, otherwise <code>false</code>
     */
    boolean isAssignable();

    /**
     * Removes the given permission from the list. If the permission does not exist in the list of assigned
     * permissions, nothing happens.
     *
     * @param permission the permission to remove.
     */
    void removePermission( Permission permission );

    /**
     * Set to <code>true</code>, if this role should available to be assigned to a user
     *
     * @param assignable the assignable flag
     */
    void setAssignable( boolean assignable );

    /**
     * Sets the names of children roles. Children roles inherit the permissions of the parent role.
     *
     * @param names the list of names of child roles.
     */
    void setChildRoleNames( List<String> names );

    /**
     * Sets the list of child role ids
     * @param ids
     */
    void setChildRoleIds( List<String> ids );

    /**
     * Set the Description
     *
     * @param description the role description
     */
    void setDescription( String description );

    /**
     * Set the role name
     *
     * NOTE: This field is considered the Primary Key for this object.
     *
     * @param name the role name
     */
    void setName( String name );

    /**
     * Set role permissions. The list of assigned permissions is replaced by this list.
     *
     * @param permissions the permissions to set
     */
    void setPermissions( List<Permission> permissions );

    /**
     * Test to see if the object is a permanent object or not.
     *
     * @return <code>true</code>, if the object is permanent.
     */
    boolean isPermanent();

    /**
     * Set flag indicating if the object is a permanent object or not.
     *
     * @param permanent true if the object is permanent.
     */
    void setPermanent( boolean permanent );

    /**
     * The role identifier. Should be built from the modelId and the resource. And must be unique.
     *
     * @since 3.0
     * @return the role identifier
     */
    String getId();

    /**
     * Sets the role id
     *
     * @since 3.0
     * @param id the identifier of the role, should not be null or empty.
     */
    void setId(String id);

    /**
     * Returns the model the role is derived from.
     *
     * @since 3.0
     * @return The model id or empty string, if this role was not created from a model
     */
    default String getModelId( ) {
        return "";
    }

    /**
     * Sets the model id.
     *
     * @param modelId the identifier of the model, or empty string. Should not be null.
     */
    void setModelId(String modelId);

    /**
     * Returns <code>true</code>, if this role is a instance of a template role, otherwise <code>false</code>.
     * Templated roles are built from a template together with a resource identifier.
     *
     * @since 3.0
     * @return <code>true</code>, if this role is a templated role, otherwise <code>false</code>
     */
    default boolean isTemplateInstance( ) {
        return StringUtils.isEmpty( getResource() );
    }

    /**
     * Sets the template instance flag.
     *
     * @since 3.0
     * @param templateInstanceFlag Set to <code>true</code>, if this is a template instance, otherwise <code>false</code>
     */
    void setTemplateInstance(boolean templateInstanceFlag);

    /**
     * Returns the resource that is used to build this role from a template. If this is not a templated
     * role, a empty string should be returned.
     *
     * @since 3.0
     * @return the resource identifier, used to build this role or a empty string, if this role is not templated
     */
    default String getResource() {
        return "";
    }

    /**
     * Sets the resource, this template instance is attached to.
     *
     * @param resource the resource identifier. Must not be null.
     */
    void setResource( String resource );


}