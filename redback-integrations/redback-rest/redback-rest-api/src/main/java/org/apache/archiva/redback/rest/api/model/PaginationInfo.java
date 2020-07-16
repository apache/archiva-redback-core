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

import io.swagger.v3.oas.annotations.media.Schema;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * Informational attributes for pagination.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="pagination")
@Schema(name="PaginationInfo", description = "Contains paging information (limit, offset, totalCount)")
public class PaginationInfo
{
    long totalCount;
    long offset;
    long limit;

    public PaginationInfo() {

    }

    public PaginationInfo( long totalCount, long offset, long limit )
    {
        this.totalCount = totalCount;
        this.offset = offset;
        this.limit = limit;
    }

    @Schema(description = "The total number of data available.")
    public long getTotalCount( )
    {
        return totalCount;
    }

    public void setTotalCount( long totalCount )
    {
        this.totalCount = totalCount;
    }

    @Schema(description = "The offset of the first element of the returned dataset.")
    public long getOffset( )
    {
        return offset;
    }

    public void setOffset( long offset )
    {
        this.offset = offset;
    }

    @Schema(description = "The maximum number of elements returned per page.")
    public long getLimit( )
    {
        return limit;
    }

    public void setLimit( long limit )
    {
        this.limit = limit;
    }
}
