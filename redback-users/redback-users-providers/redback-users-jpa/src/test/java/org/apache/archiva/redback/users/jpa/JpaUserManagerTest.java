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

import org.apache.archiva.redback.users.UserManager;
import org.apache.archiva.redback.users.provider.test.AbstractUserManagerTestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 *
 * Test for the JPA User Manager
 *
 * @author <a href="mailto:martin_s@apache.org">Martin Stockhammer</a>
 */
@Transactional
public class JpaUserManagerTest extends AbstractUserManagerTestCase {

    Log log = LogFactory.getLog(JpaUserManagerTest.class);

    @Inject
    @Named("userManager#jpa")
    UserManager jpaUserManager;


    @Before
    @Override
    public void setUp() throws Exception {

        super.setUp();
        assertNotNull(jpaUserManager);
        super.setUserManager(jpaUserManager);
        log.info("injected usermanager "+jpaUserManager);

    }

    @Test
    public void testInit() {
        assertNotNull(jpaUserManager);
        jpaUserManager.initialize();
    }





}
