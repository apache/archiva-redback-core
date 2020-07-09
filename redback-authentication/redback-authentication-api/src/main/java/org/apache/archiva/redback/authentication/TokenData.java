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

import java.time.Instant;

/**
 *
 * This contains the token payload that is used for verification of tokens.
 *
 * Created by Martin Stockhammer on 11.02.17.
 */
public interface TokenData {

    /**
     * Returns the user name.
     *
     * @return The username property.
     */
    String getUser();

    /**
     * The date the token was created.
     *
     * @return The creation date.
     */
    Instant created();

    /**
     * The date after that the token is invalid.
     *
     * @return The invalidation date.
     */
    Instant validBefore();

    /**
     * The nonce that is stored in the token.
     *
     * @return The nonce.
     */
    long getNonce();

    /**
     * Returns true, if the token is valid.
     *
     * @return True, if valid, otherwise false.
     */
    boolean isValid();
}
