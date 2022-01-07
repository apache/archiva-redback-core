package org.apache.archiva.redback.integration.checks.security;

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

import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.redback.system.check.EnvironmentCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.inject.Inject;
import java.util.List;

/**
 * RequiredRolesEnvironmentCheck: this environment check will check that the
 * required roles of the redback-xwork-integration artifact exist to be
 * assigned.
 *
 * @author: Jesse McConnell
 *
 */
@Service("environmentCheck#required-roles")
public class RequiredRolesEnvironmentCheck
    implements EnvironmentCheck
{

    protected Logger log = LoggerFactory.getLogger( getClass() );
    
    @Inject
    private RoleManager roleManager;

    /**
     * boolean detailing if this environment check has been executed
     */
    private boolean checked = false;

    /**
     * @param violations
     */
    public void validateEnvironment( List<String> violations )
    {
        if ( !checked )
        {
            log.info( "Checking the existence of required roles." );

            try
            {
                if ( !roleManager.roleExists( "registered-user" ) )
                {
                    violations.add( "unable to validate existence of the registered-user role" );
                }

                if ( !roleManager.roleExists( "user-administrator" ) )
                {
                    violations.add( "unable to validate existence of the user-administator role" );
                }

                if ( !roleManager.roleExists( "system-administrator" ) )
                {
                    violations.add( "unable to validate existence of the system-administrator role" );
                }
            }
            catch ( RoleManagerException e )
            {
                violations.add( "unable to check required roles: " + e.getMessage() );
            }

            checked = true;
        }
    }
}
