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
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang3.StringUtils;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class LdapInfo
{
    String URL;
    String baseDN;
    String bindDN;
    String bindPassword;
    boolean remote;

    public LdapInfo( )
    {
    }

    public LdapInfo( String URL, String baseDN, String bindDN, String bindPassword )
    {
        this.URL = URL;
        this.baseDN = baseDN;
        this.bindDN = bindDN;
        this.bindPassword = bindPassword;
    }

    public String getURL( )
    {
        return URL;
    }

    public void setURL( String URL )
    {
        this.URL = URL;
    }

    public String getBaseDN( )
    {
        return baseDN;
    }

    public void setBaseDN( String baseDN )
    {
        this.baseDN = baseDN;
    }

    public String getBindDN( )
    {
        return bindDN;
    }

    public void setBindDN( String bindDN )
    {
        this.bindDN = bindDN;
    }

    public String getBindPassword( )
    {
        return bindPassword;
    }

    public void setBindPassword( String bindPassword )
    {
        this.bindPassword = bindPassword;
    }

    public boolean isRemote() {
        return StringUtils.isNotEmpty( URL ) && StringUtils.isNotEmpty( baseDN ) && StringUtils.isNotEmpty( bindDN );
    }
}
