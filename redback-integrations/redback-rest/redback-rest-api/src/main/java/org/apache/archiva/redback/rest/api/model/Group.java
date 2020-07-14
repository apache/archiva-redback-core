package org.apache.archiva.redback.rest.api.model;

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

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="group")
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

    public String getName( )
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getUniqueName( )
    {
        return uniqueName;
    }

    public void setUniqueName( String uniqueName )
    {
        this.uniqueName = uniqueName;
    }

    public String getDescription( )
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

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
