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

import org.apache.archiva.redback.rbac.Operation;
import org.apache.archiva.redback.rbac.Permission;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.rbac.Resource;
import org.apache.archiva.redback.rbac.Role;
import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.role.PermanentRoleDeletionInvalid;
import org.apache.archiva.redback.role.RoleExistsException;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.role.RoleNotFoundException;
import org.apache.archiva.redback.role.model.ModelApplication;
import org.apache.archiva.redback.role.model.ModelOperation;
import org.apache.archiva.redback.role.model.ModelPermission;
import org.apache.archiva.redback.role.model.ModelResource;
import org.apache.archiva.redback.role.model.ModelRole;
import org.apache.archiva.redback.role.model.ModelTemplate;
import org.apache.archiva.redback.role.model.RedbackRoleModel;
import org.apache.archiva.redback.role.util.RoleModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * DefaultRoleTemplateProcessor: inserts the components of a template into the rbac manager
 *
 * @author: Jesse McConnell
 */
@Service("roleTemplateProcessor")
public class DefaultRoleTemplateProcessor
    implements RoleTemplateProcessor
{
    private Logger log = LoggerFactory.getLogger( DefaultRoleTemplateProcessor.class );

    @Inject
    @Named(value = "rbacManager#default")
    private RBACManager rbacManager;

    @Override
    @SuppressWarnings("unchecked")
    public String create( final RedbackRoleModel model, final String templateId, final String resource )
        throws RoleManagerException
    {
        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelTemplate template : application.getTemplates() )
            {
                if ( templateId.equals( template.getId() ) )
                {
                    // resource can be special
                    processResource( template, resource );

                    // templates are roles that have yet to be paired with a resource for creation
                    return processTemplate( model, template, resource );

                }
            }
        }

        throw new RoleNotFoundException( "unknown template '" + templateId + "'" );
    }

    @Override
    @SuppressWarnings("unchecked")
    public void remove( RedbackRoleModel model, String templateId, String resource )
        throws RoleManagerException
    {
        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelTemplate template : application.getTemplates() )
            {
                if ( templateId.equals( template.getId() ) )
                {
                    removeTemplatedRole( model, template, resource );
                    return;
                }
            }
        }

        throw new RoleManagerException( "unknown template '" + templateId + "'" );
    }

    private void removeTemplatedRole( RedbackRoleModel model, ModelTemplate template, String resource )
        throws RoleManagerException
    {
        String roleId = getRoleId( template.getId( ), resource );

        try
        {
            Role role = rbacManager.getRoleById( roleId );

            if ( !role.isPermanent() )
            {
                // remove the role
                rbacManager.removeRole( role );

                // remove the permissions
                // todo, do this in a better way too, permissions can be shared across multiple roles and that could blow chunks here.
                //for ( Iterator i = template.getPermissions().iterator(); i.hasNext(); )
                //{
                //    ModelPermission permission = (ModelPermission) i.next();
                //    if ( !permission.isPermanent() )
                //    {                                                                        
                //            rbacManager.removePermission( permission.getName() + template.getDelimiter()
                //                       + resolvePermissionResource( model, permission, resolvePermissionResource( model, permission, resource ) ) );                     
                //   }
                //}

                // check if we want to remove the resources
                Resource rbacResource = rbacManager.getResource( resource );

                //if ( !rbacResource.isPermanent() )
                //{
                    //todo we need a better way of finding if a resource is unused anymore...probably a cleaning process in the db or something
                    //rbacManager.removeResource( rbacResource );
                //}

                // todo find dangling child role references and smoke
            }
            else
            {
                throw new PermanentRoleDeletionInvalid( "Unable to remove role, it is flagged permanent: "+roleId );
            }
        }
        catch ( RbacManagerException e )
        {
            throw new RoleManagerException( "Unable to remove templated role: " + roleId, e );
        }
        //catch ( RoleTemplateProcessorException e )
        //{
        //    throw new RoleManagerException( "unable to remove templated role, error resolving resource: Role:" + roleName + " Resource: " + resource, e );
        //}
    }

    private void processResource( ModelTemplate template, String resource )
        throws RoleManagerException
    {
        if ( !rbacManager.resourceExists( resource ) )
        {
            try
            {
                Resource res = rbacManager.createResource( resource );
                res.setPermanent( template.isPermanentResource() );
                rbacManager.saveResource( res );
            }
            catch ( RbacManagerException e )
            {
                throw new RoleManagerException( "error creating resource '" + resource + "'", e );
            }
        }
    }

    @Override
    public String getRoleId( String templateId, String resource) {
        return RoleModelUtils.getRoleId( templateId, resource );
    }

    @SuppressWarnings("unchecked")
    private String processTemplate( RedbackRoleModel model, ModelTemplate template, String resource )
        throws RoleManagerException
    {
        final String templateName = template.getNamePrefix() + template.getDelimiter() + resource;
        final String roleId = getRoleId( template.getId( ), resource );

        List<Permission> permissions = processPermissions( model, template, resource );

        boolean roleExists = false;

        try
        {
            roleExists = rbacManager.roleExists( templateName );
        }
        catch ( RbacManagerException e )
        {
            throw new RoleExistsException( e.getMessage(), e );
        }

        if ( !roleExists )
        {
            try
            {
                Role role = rbacManager.createRole( templateName );
                role.setId( roleId );
                role.setModelId( template.getId() );
                role.setResource( resource );
                role.setTemplateInstance( true );
                role.setDescription( template.getDescription() );
                role.setPermanent( template.isPermanent() );
                role.setAssignable( template.isAssignable() );

                // add any permissions associated with this role
                for ( Iterator<Permission> j = permissions.iterator(); j.hasNext(); )
                {
                    Permission permission = j.next();

                    role.addPermission( permission );
                }

                // add child roles to this role
                if ( template.getChildRoles() != null )
                {
                    for ( String childRoleId : template.getChildRoles() )
                    {
                        ModelRole childRoleProfile = RoleModelUtils.getModelRole( model, childRoleId );
                        role.addChildRoleName( childRoleProfile.getName() );
                        role.addChildRoleId( childRoleProfile.getId() );
                    }
                }

                // add child templates to this role, be nice and make them if they don't exist
                if ( template.getChildTemplates() != null )
                {
                    for ( String childTemplateId : template.getChildTemplates() )
                    {
                        ModelTemplate childModelTemplate = RoleModelUtils.getModelTemplate( model, childTemplateId );

                        if ( childModelTemplate == null )
                        {
                            throw new RoleManagerException(
                                "error obtaining child template from model: template " + templateName
                                    + " # child template: " + childTemplateId );
                        }

                        String childRoleName =
                            childModelTemplate.getNamePrefix() + childModelTemplate.getDelimiter() + resource;

                        // check if the role exists, if it does then add it as a child, otherwise make it and add it
                        // this should be safe since validation should protect us from template cycles
                        if ( rbacManager.roleExists( childRoleName ) )
                        {
                            role.addChildRoleName( childRoleName );
                            role.addChildRoleId( getRoleId( childTemplateId, resource ) );
                        }
                        else
                        {
                            processTemplate( model, childModelTemplate, resource );

                            role.addChildRoleName( childRoleName );
                            role.addChildRoleId( getRoleId( childTemplateId, resource ) );
                        }
                    }
                }

                // this role needs to be saved since it now needs to be added as a child role by 
                // another role
                if ( !rbacManager.roleExists( role.getName() ) )
                {
                    role = rbacManager.saveRole( role );
                }

                // add link from parent roles to this new role
                if ( template.getParentRoles() != null )
                {
                    for ( String parentRoleId : template.getParentRoles() )
                    {
                        ModelRole parentModelRole = RoleModelUtils.getModelRole( model, parentRoleId );
                        Role parentRole = rbacManager.getRole( parentModelRole.getName() );
                        parentRole.addChildRole( role );
                        rbacManager.saveRole( parentRole );
                    }
                }

                // add child templates to this role, be nice and make them if they don't exist
                if ( template.getParentTemplates() != null )
                {
                    for ( String parentTemplateId : template.getParentTemplates() )
                    {
                        ModelTemplate parentModelTemplate = RoleModelUtils.getModelTemplate( model, parentTemplateId );

                        if ( parentModelTemplate == null )
                        {
                            throw new RoleManagerException(
                                "error obtaining parent template from model: template " + templateName
                                    + " # child template: " + parentTemplateId );
                        }

                        String parentRoleName =
                            parentModelTemplate.getNamePrefix() + parentModelTemplate.getDelimiter() + resource;

                        // check if the role exists, if it does then add it as a child, otherwise make it and add it
                        // this should be safe since validation should protect us from template cycles
                        if ( rbacManager.roleExists( parentRoleName ) )
                        {
                            Role parentRole = rbacManager.getRole( parentRoleName );

                            parentRole.addChildRole( role );
                            rbacManager.saveRole( parentRole );
                        }
                        else
                        {
                            processTemplate( model, parentModelTemplate, resource );

                            Role parentRole = rbacManager.getRole( parentRoleName );

                            parentRole.addChildRole( role );
                            rbacManager.saveRole( parentRole );
                        }
                    }
                }

            }
            catch ( RbacManagerException e )
            {
                throw new RoleManagerException( "error creating role '" + templateName + "'", e );
            }
        }
        else
        {
            try
            {
                Role role = rbacManager.getRole( templateName );

                boolean changed = false;
                for ( Permission permission : permissions )
                {
                    if ( !role.getPermissions().contains( permission ) )
                    {
                        log.info( "Adding new permission '{}' to role '{}'",
                                  permission.getName(), role.getName() );
                        role.addPermission( permission );
                        changed = true;
                    }
                }

                // Copy list to avoid concurrent modifications
                List<Permission> oldPermissions = new ArrayList<Permission>( role.getPermissions() );
                for ( Permission permission : oldPermissions )
                {
                    if ( !permissions.contains( permission ) )
                    {
                        log.info( "Removing old permission '{}' from role '{}'", permission.getName(), role.getName() );
                        role.removePermission( permission );
                        changed = true;
                    }
                }
                if ( changed )
                {
                    rbacManager.saveRole( role );
                }
            }
            catch ( RbacManagerException e )
            {
                throw new RoleManagerException( "error updating role '" + templateName + "'", e );
            }
        }
        return roleId;
    }

    @SuppressWarnings("unchecked")
    private List<Permission> processPermissions( RedbackRoleModel model, ModelTemplate template, String resource )
        throws RoleManagerException
    {

        if ( template.getPermissions() != null )
        {
            // copy list to avoid concurrent modifications
            List<ModelPermission> templatePermissions = new ArrayList<ModelPermission>( template.getPermissions() );
            List<Permission> rbacPermissions = new ArrayList<Permission>( templatePermissions.size() );
            for ( ModelPermission profilePermission : templatePermissions )
            {
                try
                {
                    String permissionName =
                        profilePermission.getName() + template.getDelimiter() + resolvePermissionResource( model,
                                                                                                           profilePermission,
                                                                                                           resource );

                    if ( !rbacManager.permissionExists( permissionName ) )
                    {

                        Permission permission = rbacManager.createPermission( permissionName );

                        ModelOperation modelOperation =
                            RoleModelUtils.getModelOperation( model, profilePermission.getOperation() );
                        Operation rbacOperation = rbacManager.getOperation( modelOperation.getName() );

                        String permissionResource = resolvePermissionResource( model, profilePermission, resource );

                        Resource rbacResource = rbacManager.getResource( permissionResource );

                        permission.setOperation( rbacOperation );
                        permission.setResource( rbacResource );
                        permission.setPermanent( profilePermission.isPermanent() );
                        permission.setDescription( profilePermission.getDescription() );

                        permission = rbacManager.savePermission( permission );

                        rbacPermissions.add( permission );

                    }
                    else
                    {

                        rbacPermissions.add( rbacManager.getPermission( permissionName ) );

                    }
                }
                catch ( RbacManagerException e )
                {
                    throw new RoleManagerException( "unable to generate templated role: " + e.getMessage(), e );
                }
                catch ( RoleTemplateProcessorException e )
                {
                    throw new RoleManagerException( "unable to resolve resource: " + resource, e );
                }
            }
            return rbacPermissions;
        }

        return Collections.emptyList();
    }

    private String resolvePermissionResource( RedbackRoleModel model, ModelPermission permission, String resource )
        throws RoleTemplateProcessorException
    {
        String permissionResource = permission.getResource();

        // if permission's resource is ${resource}, return the resource passed in
        if ( permissionResource.startsWith( "${" ) )
        {
            String tempStr = permissionResource.substring( 2, permissionResource.indexOf( '}' ) );

            if ( "resource".equals( tempStr ) )
            {
                return resource;
            }
        }

        // check if the resource resolves to declared operation
        String declaredResource = resolveResource( model, permission.getResource() );
        if ( declaredResource != null )
        {
            return declaredResource;
        }
        else
        {
            // either niether of the above apply, then its the resource.
            return resource;
        }
    }

    private String resolveResource( RedbackRoleModel model, String resource )
        throws RoleTemplateProcessorException
    {
        ModelResource resolvedResource = RoleModelUtils.getModelResource( model, resource );

        if ( resolvedResource != null )
        {
            return resolvedResource.getName();
        }
        else
        {
            return null;
        }
    }

    public RBACManager getRbacManager()
    {
        return rbacManager;
    }

    public void setRbacManager( RBACManager rbacManager )
    {
        this.rbacManager = rbacManager;
    }
}
