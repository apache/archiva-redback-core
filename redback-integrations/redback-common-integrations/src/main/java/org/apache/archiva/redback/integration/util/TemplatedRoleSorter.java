package org.apache.archiva.redback.integration.util;

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

import org.apache.archiva.redback.rbac.TemplatedRole;

import java.util.Comparator;

/**
 * TemplatedRoleSorter
 *
 * @author <a href="hisidro@exist.com">Henry Isidro</a>
 *
 */
public class TemplatedRoleSorter
    implements Comparator<TemplatedRole>
{
    public int compare( TemplatedRole r1, TemplatedRole r2 )
    {
        if ( ( r1 == null ) && ( r2 == null ) )
        {
            return 0;
        }

        if ( ( r1 == null ) && ( r2 != null ) )
        {
            return -1;
        }

        if ( ( r1 != null ) && ( r2 == null ) )
        {
            return 1;
        }

        if ( r1.getResource().equals( r2.getResource() ) )
        {
            return r1.getTemplateNamePrefix().compareTo( r2.getTemplateNamePrefix() );
        }
        else
        {
            return r1.getResource().compareToIgnoreCase( r2.getResource() );
        }
    }
}
