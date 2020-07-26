package org.apache.archiva.redback.rest.services.v2;

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

import org.apache.archiva.redback.rest.api.model.v2.PagedResult;

import java.util.Collections;
import java.util.List;

/**
 * Helper class for creating paged results.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class PagingHelper
{
    public static <T> PagedResult<T> getResultFromList( int offset, int limit, List<T> data) {
        if (offset>=data.size()) {
            return new PagedResult<>( data.size( ), offset, limit, Collections.emptyList( ) );
        }
        int lastIndex = getLastIndex( offset, limit, data.size( ) );
        return new PagedResult<>( data.size(), offset, limit, data.subList( offset, lastIndex ) );
    }

    public static int getLastIndex(int offset, int limit, int listSize) {
        return Math.min( Math.max( 0, offset + limit ), listSize );
    }


}
