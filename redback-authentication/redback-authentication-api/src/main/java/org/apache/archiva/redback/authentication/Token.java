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
 * This interface represents a token including its metadata.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public interface Token
{

    /**
     * The token id, if it exists, otherwise a empty string.
     * @return
     */
    String getId();

    /**
     * Returns the token type (access or refresh token)
     * @return the token type
     */
    TokenType getType();

    /**
     * The string representation of the token data. It depends on the token algorithm,
     * what kind of string conversion is used (e.g. Base64)
     * @return the token string
     */
    String getData();

    /**
     * The token as byte array
     * @return
     */
    byte[] getBytes();

    /**
     * The token meta data, like expiration time.
     * @return the metadata
     */
    TokenData getMetadata();
}
