package org.apache.archiva.redback.common.ldap.connection;

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

import junit.framework.TestCase;
import org.apache.archiva.redback.common.ldap.connection.ConfigurableLdapConnectionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class ConfigurableLdapConnectionFactoryTest
    extends TestCase
{

    @Inject
    @Named( value = "ldapConnectionFactory#configurable" )
    ConfigurableLdapConnectionFactory factory;

    @Test
    public void testConfiguration()
    {
        assertEquals( "dc=codehaus,dc=org", factory.getLdapConnectionConfiguration().getBaseDn().toString() );
        assertEquals( "uid=user,dc=codehaus,dc=org", factory.getLdapConnectionConfiguration().getBindDn().toString() );
    }
}
