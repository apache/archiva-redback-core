package org.apache.archiva.redback.role.template;

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

import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.role.model.RedbackRoleModel;

/**
 * RoleModelValidator:
 *
 * @author: Jesse McConnell
 */
public interface RoleTemplateProcessor
{

    /**
     * Creates a role instance from a template for the given resource and returns the id of the new role.
     * @param model the model
     * @param templateId the template identifier
     * @param resource the resource to which the role is applied
     * @return the id of the role
     * @throws RoleManagerException if the access to the backend datastore failed
     */
    String create( RedbackRoleModel model, String templateId, String resource )
        throws RoleManagerException;

    /**
     * Removes the role instance that belongs to the template from the datastore
     * @param model the model
     * @param templateId the template identifier
     * @param resource the resource to which the role is applied
     * @throws RoleManagerException if the access to the backend datastore failed
     */
    void remove( RedbackRoleModel model, String templateId, String resource )
        throws RoleManagerException;


    /**
     * Returns the role id that identifies the role that is a instance of the given template for the given resource.
     * @param templateId the template identifier
     * @param resource the resource
     * @return the role identifier
     */
    String getRoleId( String templateId, String resource );
}
