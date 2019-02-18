package org.apache.archiva.redback.common.config.api;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *
 * A annotation that marks a event listener method as asynchronous. That means the listener event methods
 * are run in a separated thread. How tasks are executed is dependent on the implementation.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */

@Target(value={METHOD, TYPE})
@Retention(value=RUNTIME)
@Documented
public @interface AsyncListener
{
    /**
     * May be set to set the executor. The meaning of this value is implementation specific.
     * @return The value.
     */
    String value() default "";
}
