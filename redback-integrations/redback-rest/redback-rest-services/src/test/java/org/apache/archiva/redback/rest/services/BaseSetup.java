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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicReference;

public class BaseSetup
{
    public static final String SYSPROP_START_SERVER = "archiva.rest.start.server";
    public static final String SYSPROP_SERVER_PORT = "archiva.rest.server.port";
    public static final String SYSPROP_SERVER_BASE_URI = "archiva.rest.server.baseuri";
    public static final String SYSPROP_SERVER_ADMIN_PWD = "rest.admin.pwd";
    public static final String SYSPROP_LDAP_URL = "archiva.rest.ldap.url";
    public static final String SYSPROP_LDAP_BASEDN = "archiva.rest.ldap.baseDn";
    public static final String SYSPROP_LDAP_BINDDN = "archiva.rest.ldap.bindDn";
    public static final String SYSPROP_LDAP_BINPWD = "archiva.rest.ldap.bindPwd";

    public static String DEFAULT_ADMIN_PWD = "Ackd245aer9sdfan";

    public static AtomicReference<String> adminPwd = new AtomicReference<>( null );

    public static String getAdminPwd() {
        final String result = adminPwd.get( );
        if (StringUtils.isEmpty(result)) {
            String pwd = System.getProperty( SYSPROP_SERVER_ADMIN_PWD, DEFAULT_ADMIN_PWD );
            if ( StringUtils.isEmpty( pwd ) )
            {
                pwd = DEFAULT_ADMIN_PWD;
            }
            adminPwd.compareAndSet(null,  pwd );
            return pwd;
        } else {
            return result;
        }
    }

    public static boolean startServer()
    {
        return !"no".equals( System.getProperty( SYSPROP_START_SERVER, "yes" ).toLowerCase( ) );
    }
    public static String getServerPort()
    {
        return System.getProperty( SYSPROP_SERVER_PORT, "" );
    }

    public static String getBaseUri() {
        return System.getProperty( SYSPROP_SERVER_BASE_URI, "http://localhost" );
    }

    public static LdapInfo getLdapInfo() {
        String url = System.getProperty( SYSPROP_LDAP_URL, "" );
        String baseDn = System.getProperty( SYSPROP_LDAP_BASEDN, "" );
        String bindDn = System.getProperty( SYSPROP_LDAP_BINDDN, "" );
        String bindPwd = System.getProperty( SYSPROP_LDAP_BINPWD, "" );

        return new LdapInfo( url, baseDn, bindDn, bindPwd );
    }
}
