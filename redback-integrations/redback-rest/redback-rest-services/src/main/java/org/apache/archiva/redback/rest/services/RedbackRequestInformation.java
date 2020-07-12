package org.apache.archiva.redback.rest.services;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.system.SecuritySession;
import org.apache.archiva.redback.users.User;

/**
 * @author Olivier Lamy
 * @author Martin Stockhammer
 * @since 1.4
 *
 */
public class RedbackRequestInformation
{
    private SecuritySession securitySession;

    private User user;

    private String remoteAddr;

    public RedbackRequestInformation( User user, String remoteAddr )
    {
        this.user = user;
        this.remoteAddr = remoteAddr;
    }

    public RedbackRequestInformation( SecuritySession securitySession, User user, String remoteAddr )
    {
        this.securitySession = securitySession;
        this.user = user;
        this.remoteAddr = remoteAddr;
    }

    public User getUser()
    {
        return user;
    }

    public void setUser( User user )
    {
        this.user = user;
    }

    public String getRemoteAddr()
    {
        return remoteAddr;
    }

    public void setRemoteAddr( String remoteAddr )
    {
        this.remoteAddr = remoteAddr;
    }

    public SecuritySession getSecuritySession( )
    {
        return securitySession;
    }

    public void setSecuritySession( SecuritySession securitySession )
    {
        this.securitySession = securitySession;
    }
}
