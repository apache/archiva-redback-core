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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;

import java.util.List;

/**
 * AuthenticationManager:
 *
 * @author Jesse McConnell
 * @author Martin Stockhammer
 */
public interface AuthenticationManager
{
    /**
     * Returns the identifier of this authentication manager
     * @return the identifier string
     */
    String getId();

    /**
     * Returns the list of authenticators in the same order as they are called for authentication
     * @return the list of authenticators.
     */
    List<Authenticator> getAuthenticators();

    /**
     * Authenticates by calling all authenticators in the defined order.
     *
     * @param source the authentication data
     * @return the result that gives information, if the authentication was successful
     * @throws AccountLockedException if the account is locked
     * @throws AuthenticationException if something unexpected happend during authentication
     * @throws MustChangePasswordException if the user has to change his password
     */
    AuthenticationResult authenticate( AuthenticationDataSource source )
        throws AccountLockedException, AuthenticationException, MustChangePasswordException;

    /**
     * Returns the authenticator controls that are used to control the order and actions during authentication.
     * @return the list of controls
     */
    List<AuthenticatorControl> getControls();

    /**
     * Sets the list of authenticator controls
     * @param controlList the list of control instances
     */
    void setControls( List<AuthenticatorControl> controlList);

    /**
     * Modifies the control for a single authenticator
     * @param control the authenticator control
     */
    void modifyControl(AuthenticatorControl control);
}