package org.apache.archiva.redback.rest.api.model.v2;

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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * REST API Version 2 group element
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
@XmlRootElement(name="group")
@Schema(name="Group", description = "Group object")
public class Group implements Serializable
{
    private static final long serialVersionUID = -1842878251787304632L;
    String name;
    String uniqueName;
    String description;
    List<String> memberList;

    public Group() {

    }

    public Group( String name )
    {
        this.name = name;
    }

    @Schema(description = "The name of the group")
    public String getName( )
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    @Schema(name="unique_name", description = "The unique name of the group. Depends on the backend repository, e.g. the LDAP DN.")
    public String getUniqueName( )
    {
        return uniqueName;
    }

    public void setUniqueName( String uniqueName )
    {
        this.uniqueName = uniqueName;
    }

    @Schema( description = "The group description, if available" )
    public String getDescription( )
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    @Schema(name="member_list", description = "The list of members. The format of the member strings depends on the backend repository, e.g. for LDAP these may be the member DNs")
    public List<String> getMemberList( )
    {
        return memberList;
    }

    public void setMemberList( List<String> memberList )
    {
        this.memberList = memberList;
    }

    public void addMember(String member) {
        if (this.memberList==null) {
            this.memberList = new ArrayList<>( );
        }
        this.memberList.add( member );
    }
}
