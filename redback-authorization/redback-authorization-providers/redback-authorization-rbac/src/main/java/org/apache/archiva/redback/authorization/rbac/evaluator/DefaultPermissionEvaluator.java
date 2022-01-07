package org.apache.archiva.redback.authorization.rbac.evaluator;

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

import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * DefaultPermissionEvaluator:
 *
 * Currently only one expression is available for evaluation, ${username} will be replaced with the username
 * of the person making the authorization check
 *
 * @author Jesse McConnell
 */
@Service("permissionEvaluator")
public class DefaultPermissionEvaluator
    implements PermissionEvaluator
{
    @Inject
    @Named(value = "userManager#default")
    private UserManager userManager;

    public boolean evaluate( Permission permission, String operation, String resource, String principal )
        throws PermissionEvaluationException
    {
        String permissionResource = permission.getResource().getIdentifier();

        // expression evaluation checking
        if ( permissionResource.startsWith( "${" ) )
        {
            String tempStr = permissionResource.substring( 2, permissionResource.indexOf( '}' ) );

            if ( "username".equals( tempStr ) )
            {
                try
                {
                    permissionResource = userManager.findUser( principal ).getUsername();
                }
                catch ( UserNotFoundException e )
                {
                    throw new PermissionEvaluationException( "unable to locate user to retrieve username", e );
                }
                catch ( UserManagerException e )
                {
                    throw new PermissionEvaluationException( "trouble finding user: " + e.getMessage(), e );
                }
            }
        }

        // check if this permission applies to the operation at all
        if ( permission.getOperation().getName().equals( operation ) )
        {
            // check if it is a global resource, if it is then since the operations match we return true
            if ( Resource.GLOBAL.equals( permission.getResource().getIdentifier() ) )
            {
                return true;
            }

            // Resource settings on the permission object and on the annotation
            // should be in line. If not, we use the least privilege, which means
            // if one of both is set, we will check for equality.
            if ( StringUtils.isEmpty( permissionResource ) && resource == null )
            {
                return true;
            }

            // check if the resource identifier of the permission matches the resource we are checking against
            // if it does then return true
            if ( permissionResource.equals( resource ) )
            {
                return true;
            }
        }

        return false;
    }

    public UserManager getUserManager()
    {
        return userManager;
    }

    public void setUserManager( UserManager userManager )
    {
        this.userManager = userManager;
    }
}
