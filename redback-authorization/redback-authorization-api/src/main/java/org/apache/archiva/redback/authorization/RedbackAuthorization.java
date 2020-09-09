package org.apache.archiva.redback.authorization;

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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Authorization annotation. The annotation can be defined for methods and describes
 * the permissions necessary to execute the method.
 *
 * @author Olivier Lamy
 * @since 1.3
 */
@Target( ElementType.METHOD )
@Retention( RetentionPolicy.RUNTIME )
public @interface RedbackAuthorization
{

    /**
     * The list of permissions that are needed for executing the method.
     * The strings refer to defined permission ids.
     * The accessing user must have at least one of the given permissions to execute
     * the method.
     * @return the array of permission ids.
     */
    String[] permissions() default ( "" );

    /**
     * The resource is used to restrict access by using information from
     * the method parameters or call environment.
     * Resource annotations have to be in line with the defined permissions.
     * @return the redback ressource karma needed
     */
    String resource() default ( "" );

    /**
     * A description of the authorization definition.
     * @return the description string
     */
    String description() default ( "" );

    /**
     * @return <code>true</code> if doesn't need any special permission
     */
    boolean noRestriction() default false;

    /**
     * @return if this service need only authentication and not special karma
     */
    boolean noPermission() default false;
}
