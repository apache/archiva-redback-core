package org.apache.archiva.redback.rbac.jpa;

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

import org.apache.archiva.redback.rbac.RBACManager;
import org.apache.archiva.redback.rbac.RbacManagerException;
import org.apache.archiva.redback.tests.AbstractRbacManagerTestCase;
import org.junit.Before;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.io.InputStream;
import java.util.Properties;

/**
 * JpaRbacManagerTest:
 *
 * @author Jesse McConnell
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
public class JpaRbacManagerTest
    extends AbstractRbacManagerTestCase
{

    @Inject
    @Named(value = "rbacManager#jpa")
    RBACManager rbacManager;


    public static int EVENTCOUNT = 2;

    @Override
    public void assertEventCount()
    {
        assertEquals( EVENTCOUNT, eventTracker.initCount );
    }

    /**
     * Creates a new RbacStore which contains no data.
     */
    @Before
    public void setUp()
        throws Exception
    {

        super.setUp();
//        Properties props = new Properties();
//        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("test.properties");
//        assert is!=null;
//        props.load(is);
//        is.close();
//        EntityManagerFactory emf = Persistence.createEntityManagerFactory("redback-jpa",props);
//
        log.info("test setup");
        // rbacManager.setEntityManager(emf.createEntityManager());
        super.setRbacManager(rbacManager);
        assertNotNull(rbacManager);
        log.info("injected rbac manager "+rbacManager);

    }


    @Override
    public void testGetAssignedRoles()
        throws RbacManagerException
    {
        super.testGetAssignedRoles();
    }

    @Override
    public void testGetAssignedPermissionsDeep()
        throws RbacManagerException
    {
        super.testGetAssignedPermissionsDeep();
    }

    @Override
    protected void afterSetup()
    {
        super.afterSetup();
    }

    @Override
    public void testLargeApplicationInit()
        throws RbacManagerException
    {
        super.testLargeApplicationInit();
    }

    @Override
    public void testGetRolesDeep()
        throws RbacManagerException
    {
        super.testGetRolesDeep();
    }


    @Override
    public void testStoreInitialization()
        throws Exception
    {
        rbacManager.eraseDatabase();
        eventTracker.rbacInit( true );
        super.testStoreInitialization();
        assertEquals( EVENTCOUNT, eventTracker.initCount );
    }


}
