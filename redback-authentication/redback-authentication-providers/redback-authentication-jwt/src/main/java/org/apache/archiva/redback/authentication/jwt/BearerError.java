package org.apache.archiva.redback.authentication.jwt;

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
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public enum BearerError
{
    INVALID_REQUEST("invalid_request",1024),INVALID_TOKEN("invalid_token", 1025), INSUFFICIENT_SCOPE( "insufficient_scope", 1026 );

    protected String errorString;
    protected int id;
    BearerError(String errorString, int id) {
        this.errorString = errorString;
        this.id = id;
    }

    public String getError() {
        return this.errorString;
    }

    public int getId() {
        return this.id;
    }

    public static BearerError get(int id) {
        final BearerError[] values = BearerError.values();
        for ( int i = 0 ; i<values.length; i++) {
            if (values[i].id==id) {
                return values[i];
            }
        }
        return null;
    }
}
