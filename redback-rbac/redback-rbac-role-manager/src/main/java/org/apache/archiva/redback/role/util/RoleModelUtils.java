package org.apache.archiva.redback.role.util;

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

import org.apache.archiva.components.graph.api.Category;
import org.apache.archiva.components.graph.api.RelationType;
import org.apache.archiva.components.graph.base.SimpleGraph;
import org.apache.archiva.components.graph.base.SimpleNode;
import org.apache.archiva.components.graph.util.Traversal;
import org.apache.archiva.redback.role.model.ModelApplication;
import org.apache.archiva.redback.role.model.ModelOperation;
import org.apache.archiva.redback.role.model.ModelResource;
import org.apache.archiva.redback.role.model.ModelRole;
import org.apache.archiva.redback.role.model.ModelTemplate;
import org.apache.archiva.redback.role.model.RedbackRoleModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RoleModelUtils:
 *
 * @author: Jesse McConnell
 *
 */
public class RoleModelUtils
{
    public enum RoleType implements Category {
        ROLE,TEMPLATE
    }

    public enum RoleRelation implements RelationType {
        ROLE_TO_ROLE,ROLE_TO_TEMPLATE,TEMPLATE_TO_ROLE,TEMPLATE_TO_TEMPLATE;
    }

    public static final String ROOT = ":archiva:node:root";

    private static final Logger log = LoggerFactory.getLogger(RoleModelUtils.class);

    public static List<ModelRole> getRoles( RedbackRoleModel model )
    {
        List<ModelRole> roleList = new ArrayList<ModelRole>( );

        for ( ModelApplication application : model.getApplications() )
        {
            roleList.addAll( application.getRoles() );
        }

        return roleList;
    }

    public static List<ModelTemplate> getTemplates( RedbackRoleModel model )
    {
        List<ModelTemplate> templateList = new ArrayList<ModelTemplate>();

        for ( ModelApplication application : model.getApplications() )
        {
            templateList.addAll( application.getTemplates() );
        }

        return templateList;
    }

    @SuppressWarnings( "unchecked" )
    public static List<String> getOperationIdList( RedbackRoleModel model )
    {
        List<String> operationsIdList = new ArrayList<String>();

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelOperation operation : application.getOperations() )
            {
                operationsIdList.add( operation.getId() );
            }
        }

        return operationsIdList;
    }

    @SuppressWarnings( "unchecked" )
    public static List<String> getResourceIdList( RedbackRoleModel model )
    {
        List<String> resourceIdList = new ArrayList<String>();

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelResource resource : application.getResources() )
            {
                resourceIdList.add( resource.getId() );
            }
        }

        return resourceIdList;
    }

    public static List<String> getRoleIdList( RedbackRoleModel model )
    {
        List<String> roleIdList = new ArrayList<String>();

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelRole role : application.getRoles() )
            {
                roleIdList.add( role.getId() );
            }
        }

        return roleIdList;
    }


    public static List<String> getTemplateIdList( RedbackRoleModel model )
    {
        List<String> templateIdList = new ArrayList<String>();

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelTemplate template : application.getTemplates() )
            {
                templateIdList.add( template.getId() );
            }
        }

        return templateIdList;

    }

    /**
     * WARNING: can return null
     *
     * @param model
     * @param roleId
     * @return
     */
    @SuppressWarnings( "unchecked" )
    public static ModelRole getModelRole( RedbackRoleModel model, String roleId )
    {
        ModelRole mrole = null;

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelRole role : application.getRoles() )
            {
                if ( roleId.equals( role.getId() ) )
                {
                    mrole = role;
                }
            }
        }

        return mrole;
    }

    /**
     * WARNING: can return null
     *
     * @param model
     * @param templateId
     * @return
     */
    @SuppressWarnings( "unchecked" )
    public static ModelTemplate getModelTemplate( RedbackRoleModel model, String templateId )
    {
        ModelTemplate mtemplate = null;

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelTemplate template : application.getTemplates() )
            {
                if ( templateId.equals( template.getId() ) )
                {
                    mtemplate = template;
                }
            }
        }

        return mtemplate;
    }

    /**
     * WARNING: can return null
     *
     * @param model
     * @param operationId
     * @return
     */
    @SuppressWarnings( "unchecked" )
    public static ModelOperation getModelOperation( RedbackRoleModel model, String operationId )
    {
        ModelOperation moperation = null;

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelOperation operation : application.getOperations() )
            {
                if ( operationId.equals( operation.getId() ) )
                {
                    moperation = operation;
                }
            }
        }

        return moperation;
    }

    @SuppressWarnings( "unchecked" )
    public static ModelResource getModelResource( RedbackRoleModel model, String resourceId )
    {
        ModelResource mresource = null;

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelResource resource : application.getResources() )
            {
                if ( resourceId.equals( resource.getId() ) )
                {
                    mresource = resource;
                }
            }
        }

        return mresource;
    }

    @SuppressWarnings( "unchecked" )
    public static SimpleGraph generateRoleGraph(RedbackRoleModel model )

    {
        SimpleGraph roleGraph = new SimpleGraph();
        SimpleNode rootNode = roleGraph.addNode(ROOT, ROOT);

        log.debug("Created graph with root {}", rootNode);

        for ( ModelApplication application : model.getApplications() )
        {
            log.debug("Application {}", application.getId());
            for ( ModelRole role : application.getRoles() )
            {
                final String roleId = role.getId();
                SimpleNode roleNode = roleGraph.addNode(roleId, roleId);
                roleNode.addCategory(RoleType.ROLE);
                if (role.getParentRoles()==null || role.getParentRoles().size()==0) {
                    // We add it to the root node only, if it has no parent roles
                    roleGraph.addEdge("root:" + roleId, "root -> " + roleId, rootNode, roleNode);
                }

                if ( role.getChildRoles() != null )
                {
                    for ( String childRole : role.getChildRoles() )
                    {
                        SimpleNode childNode = roleGraph.addNode(childRole, childRole);
                        childNode.addCategory(RoleType.ROLE);
                        roleGraph.addEdge( RoleRelation.ROLE_TO_ROLE, roleId+":"+childRole,
                                roleId+" -> "+childRole, roleNode, childNode );

                    }
                }

                if ( role.getParentRoles() != null )
                {
                    for ( String parentRole : role.getParentRoles() )
                    {
                        SimpleNode parentNode = roleGraph.addNode( parentRole, parentRole );
                        parentNode.addCategory(RoleType.ROLE);
                        roleGraph.addEdge( RoleRelation.ROLE_TO_ROLE, parentRole+":"+roleId,
                                parentRole + " -> "+ roleId, parentNode, roleNode);
                    }
                }
            }
        }

        return roleGraph;
    }

    @SuppressWarnings( "unchecked" )
    public static SimpleGraph generateTemplateGraph( RedbackRoleModel model )

    {
        SimpleGraph templateGraph = generateRoleGraph( model );
        SimpleNode rootNode = templateGraph.getNode(ROOT);

        for ( ModelApplication application : model.getApplications() )
        {
            for ( ModelTemplate template : application.getTemplates() )
            {
                final String templId = template.getId();
                SimpleNode templateNode = templateGraph.addNode(templId, templId);
                templateNode.addCategory(RoleType.TEMPLATE);
                if ((template.getParentRoles() == null || template.getParentRoles().size()==0)
                && ( template.getParentTemplates() == null || template.getParentTemplates().size()==0) ) {
                    templateGraph.addEdge("root:" + templId, "root -> " + templId, rootNode, templateNode);
                }

                if ( template.getChildRoles() != null )
                {
                    for ( String childRole : template.getChildRoles() )
                    {
                        SimpleNode childNode = templateGraph.addNode(childRole, childRole);
                        childNode.addCategory(RoleType.ROLE);
                        templateGraph.addEdge( RoleRelation.TEMPLATE_TO_ROLE, templId+":"+childNode, templId+" -> "+childNode, templateNode, childNode );
                    }
                }

                if ( template.getParentRoles() != null )
                {
                    for ( String parentRole : template.getParentRoles() )
                    {
                        SimpleNode parentNode = templateGraph.addNode(parentRole, parentRole);
                        parentNode.addCategory(RoleType.ROLE);
                        templateGraph.addEdge( RoleRelation.ROLE_TO_TEMPLATE, parentRole+":"+templId,
                                parentRole+" -> "+templId, parentNode, templateNode);
                    }
                }

                if ( template.getChildTemplates() != null )
                {
                    for ( String childTemplate : template.getChildTemplates() )
                    {
                        SimpleNode childTemplNode = templateGraph.addNode(childTemplate, childTemplate);
                        childTemplNode.addCategory(RoleType.TEMPLATE);
                        templateGraph.addEdge( RoleRelation.TEMPLATE_TO_TEMPLATE, templId+":"+childTemplate,
                                templId+" -> "+childTemplate, templateNode, childTemplNode);
                    }
                }

                if ( template.getParentTemplates() != null )
                {
                    for ( String parentTemplate : template.getParentTemplates() )
                    {
                        SimpleNode parentTemplNode = templateGraph.addNode( parentTemplate, parentTemplate );
                        parentTemplNode.addCategory(RoleType.TEMPLATE);
                        templateGraph.addEdge( RoleRelation.TEMPLATE_TO_TEMPLATE,
                                parentTemplate+":"+templId, parentTemplate+" -> "+templId,
                                parentTemplNode, templateNode);
                    }
                }
            }
        }

        return templateGraph;
    }

    @SuppressWarnings( "unchecked" )
    public static List<String> reverseTopologicalSortedRoleList( RedbackRoleModel model )
    {
        SimpleGraph graph = generateRoleGraph(model);
        List<String> sortedGraph = Traversal.topologialSort(graph.getNode(ROOT)).stream().map(n -> n.getId())
                .filter(id -> !ROOT.equals(id)).collect(Collectors.toList());
        Collections.reverse(sortedGraph);
        return sortedGraph;
    }

}
