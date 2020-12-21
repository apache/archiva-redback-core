package org.apache.archiva.redback.rest.api;/*
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

import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

/**
 * Central utility class that may be used by service implementations.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class Util
{
    /**
     * Returns <code>false</code>, if the given parameter is not present in the given uriInfo, or is present and set to 'false' or '0'.
     * In all other cases it returns <code>true</code>.
     *
     * This means you can activate a flag by setting '?param', '?param=true', '?param=1', ...
     * It is deactivated, if the parameter is absent, or '?param=false', or '?param=0'
     *
     * @param uriInfo the uriInfo context instance, that is used to check for the parameter
     * @param queryParameterName the query parameter name
     * @return
     */
    public static boolean isFlagSet( final UriInfo uriInfo, final String queryParameterName) {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters( );
        if (!params.containsKey( queryParameterName )) {
            return false;
        }
        // parameter is available
        String value = params.getFirst( queryParameterName );
        // if its available but without a value it is flagged as present
        if (StringUtils.isEmpty( value )) {
            return true;
        }
        // if it has a value, we check for false values:
        if ("false".equalsIgnoreCase( value ) || "0".equalsIgnoreCase( value )) {
            return false;
        }
        return true;
    }
}
