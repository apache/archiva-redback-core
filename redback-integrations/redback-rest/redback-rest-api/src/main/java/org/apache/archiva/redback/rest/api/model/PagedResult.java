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

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * A Paged result puts the data into an envelope
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="pagedResult")
public class PagedResult<T>
{
    PaginationInfo pagination;
    T data;

    public PagedResult() {

    }

    public PagedResult( long totalCount, long offset, long limit, T data ) {
        this.data = data;
        this.pagination = new PaginationInfo( totalCount, offset, limit );
    }

    public static final <T> PagedResult<T> of(long totalSize, long offset, long limit, T element) {
        return new PagedResult( totalSize, offset, limit, element);
    }

    public T getData( )
    {
        return data;
    }

    public void setData( T data )
    {
        this.data = data;
    }

    public PaginationInfo getPagination( )
    {
        return pagination;
    }

    public void setPagination( PaginationInfo pagination )
    {
        this.pagination = pagination;
    }
}
