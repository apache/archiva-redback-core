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

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 *
 * Simple Token information class that contains a username and a lifetime.
 *
 * The class is not able to detect time manipulations. It is assumed that the
 * current time of the system is correct.
 *
 * This class is immutable.
 *
 * Created by Martin Stockhammer on 03.02.17.
 */
public final class SimpleTokenData implements Serializable, TokenData {


    private static final long serialVersionUID = 5907745449771921813L;

    private final String user;
    private final Instant created;
    private final Instant validBefore;
    private final long nonce;

    /**
     * Creates a new token info instance for the given user.
     * The lifetime in milliseconds defines the invalidation date by
     * adding the lifetime to the current time of instantiation.
     *
     * @param user The user name
     * @param lifetime The number of milliseconds after that the token is invalid
     * @param nonce Should be a random number and different for each instance.
     */
    public SimpleTokenData(final String user, final long lifetime, final long nonce) {
        this.user=user;
        this.created = Instant.now( );
        this.validBefore = created.plus( Duration.ofMillis( lifetime ) );
        this.nonce = nonce;
    }

    /**
     * Creates a new token info instance for the given user.
     * The lifetime in milliseconds defines the invalidation date by
     * adding the lifetime to the current time of instantiation.
     *
     * @param user The user name
     * @param lifetime The number of milliseconds after that the token is invalid
     * @param nonce Should be a random number and different for each instance.
     */
    public SimpleTokenData(final String user, final Duration lifetime, final long nonce) {
        this.user=user;
        this.created = Instant.now( );
        this.validBefore = created.plus( lifetime );
        this.nonce = nonce;
    }

    @Override
    public final String getUser() {
        return user;
    }

    @Override
    public final Instant created() {
        return created;
    }

    @Override
    public final Instant validBefore() {
        return validBefore;
    }

    @Override
    public final long getNonce()  {
        return nonce;
    }

    @Override
    public boolean isValid() {
        return Instant.now( ).isBefore( validBefore );
    }

}
