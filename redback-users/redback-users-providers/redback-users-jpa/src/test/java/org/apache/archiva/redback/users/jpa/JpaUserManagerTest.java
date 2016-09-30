package org.apache.archiva.redback.users.jpa;

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

import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.provider.test.AbstractUserManagerTestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by martin on 21.09.16.
 */

public class JpaUserManagerTest extends AbstractUserManagerTestCase {

    Log log = LogFactory.getLog(JpaUserManagerTest.class);

    @Inject
    @Named("userManager#jpa")
    JpaUserManager jpaUserManager;


    @Inject
    private UserSecurityPolicy securityPolicy;

    @Before
    @Override
    public void setUp() throws Exception {

        super.setUp();
        Properties props = new Properties();
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("test.properties");
        assert is!=null;
        props.load(is);
        is.close();
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("redback-jpa",props);

        jpaUserManager.setEntityManager(emf.createEntityManager());
        super.setUserManager(jpaUserManager);
        assertNotNull(jpaUserManager);
        log.info("injected usermanager "+jpaUserManager);

    // create the factory defined by the "openjpa" entity-manager entry
  
    }

    @Test
    public void testInit() {
        jpaUserManager.initialize();
    }




}
