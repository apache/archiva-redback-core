package org.apache.archiva.redback.rest.api.model;

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

import org.apache.archiva.redback.keys.AuthenticationKey;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name = "authenticationKeyResult")
public class AuthenticationKeyResult
{
    String key = "";

    public AuthenticationKeyResult() {

    }

    public AuthenticationKeyResult(String key) {
        this.key = key;
    }

    public String getKey( )
    {
        return key;
    }

    public void setKey( String key )
    {
        this.key = key;
    }
}
