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

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement( name = "actionStatus" )
@Schema( name = "ActionStatus", description = "Status result of a updating action, like post, put, delete" )
public class ActionStatus
{
    private boolean success = false;
    private int modifiedNumber = 0;

    public static final ActionStatus SUCCESS = new ActionStatus( true );
    public static final ActionStatus FAIL = new ActionStatus( false );

    public static ActionStatus FROM( boolean status )
    {
        return status ? SUCCESS : FAIL;
    }

    public ActionStatus( )
    {

    }

    public ActionStatus( boolean success )
    {
        this.success = success;
    }

    public ActionStatus( boolean success, int modifiedNumber )
    {
        this.success = success;
        this.modifiedNumber = modifiedNumber;
    }

    public boolean isSuccess( )
    {
        return success;
    }

    public void setSuccess( boolean success )
    {
        this.success = success;
    }

    public int getModifiedNumber( )
    {
        return modifiedNumber;
    }

    public void setModifiedNumber( int modifiedNumber )
    {
        this.modifiedNumber = modifiedNumber;
    }

    @Override
    public String toString( )
    {
        return Boolean.toString( success );
    }
}
