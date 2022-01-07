package org.apache.archiva.redback.role.validator;

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
import org.apache.archiva.redback.role.model.RedbackRoleModel;
import org.apache.archiva.redback.role.model.io.stax.RedbackRoleModelStaxReader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import jakarta.inject.Inject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * RoleModelMergerTest:
 *
 * @author: Jesse McConnell
 *
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class RoleModelValidatorTest
    extends TestCase
{

    @Inject
    RoleModelValidator modelValidator;


    /**
     * Creates a new RbacStore which contains no data.
     */
    @Before
    public void setUp()
        throws Exception
    {
        super.setUp();
    }
    
    protected String getPlexusConfigLocation()
    {
        return "plexus.xml";
    }

    String getBasedir()
    {
        return System.getProperty( "basedir" );
    }

    @Test
    public void testGood() throws Exception 
    {
        Path resource = Paths.get(getBasedir() + "/src/test/validation-tests/redback-good.xml");
        
        assertNotNull( resource );
        
        RedbackRoleModelStaxReader modelReader = new RedbackRoleModelStaxReader();
        
        RedbackRoleModel redback = modelReader.read( resource.toAbsolutePath().toString() );
        
        assertNotNull( redback );

        assertTrue( modelValidator.validate( redback ) );
        
        assertNull( modelValidator.getValidationErrors() );
    }

    @Test
    public void testBad() throws Exception 
    {
        Path resource = Paths.get( getBasedir() + "/src/test/validation-tests/redback-bad.xml");
        
        assertNotNull( resource );
        
        RedbackRoleModelStaxReader modelReader = new RedbackRoleModelStaxReader();
        
        RedbackRoleModel redback = modelReader.read( resource.toAbsolutePath().toString() );
        
        assertNotNull( redback );

        assertFalse( modelValidator.validate( redback ) );
        
        assertNotNull( modelValidator.getValidationErrors() );
          
        assertTrue( checkForValidationError( modelValidator.getValidationErrors(), "missing application name" ) );
        
        assertTrue( checkForValidationError( modelValidator.getValidationErrors(), "eat-cornflakes-missing-operation-in-template" ) );
     
        assertTrue( checkForValidationError( modelValidator.getValidationErrors(), "can-drink-the-milk-missing-child-role" ) );
        
        assertTrue( checkForValidationError( modelValidator.getValidationErrors(), "test-template-missing-child-template" ) );
        
        assertTrue( checkForValidationError( modelValidator.getValidationErrors(), "Cycle detected" ) );
     
        assertTrue( checkForValidationError( modelValidator.getValidationErrors(), "Template cycle detected" ) );
        
    }

    @Test
    public void testCore() throws Exception 
    {
        Path resource = Paths.get( getBasedir() + "/src/test/validation-tests/redback-core.xml");
        
        assertNotNull( resource );
        
        RedbackRoleModelStaxReader modelReader = new RedbackRoleModelStaxReader();
        
        RedbackRoleModel redback = modelReader.read( resource.toAbsolutePath().toString() );
        
        assertNotNull( redback );

        assertTrue( modelValidator.validate( redback ) );
        
        assertNull( modelValidator.getValidationErrors() );
    }
    
    private boolean checkForValidationError( List<String> validationErrors, String errorText )    
    {
        for ( String error : validationErrors )
        {
            if ( error.indexOf( errorText ) != -1 )
            {
                return true;
            }
        }
        return false;        
    }
 
}