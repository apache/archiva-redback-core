package org.apache.archiva.redback.authentication;

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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Gives a priority and what to do, if the authentication succeeds.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 * @since 3.0
 */
public class AuthenticatorControl implements Comparable<AuthenticatorControl>
{
    final String name;
    final int priority;
    final boolean active;
    final AuthenticationControl control;

    public AuthenticatorControl( String name, int priority, AuthenticationControl control )
    {
        this.name = name;
        this.priority = priority;
        this.control = control;
        this.active = true;
    }

    public AuthenticatorControl( String name, int priority, AuthenticationControl control, boolean active)
    {
        this.name = name;
        this.priority = priority;
        this.control = control;
        this.active = active;
    }


    public String getName( )
    {
        return name;
    }

    public int getPriority( )
    {
        return priority;
    }

    public AuthenticationControl getControl( )
    {
        return control;
    }

    @Override
    public int compareTo( AuthenticatorControl o )
    {
        return this.getPriority()-o.getPriority();
    }

    public boolean isActive( )
    {
        return active;
    }
}
