package org.apache.archiva.redback.users.jpa;

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

import org.apache.archiva.redback.users.UserQuery;

import java.util.Arrays;

/**
 * Created by martin on 23.09.16.
 */
public class JpaUserQuery implements UserQuery {

    private String username;
    private String email;
    private String fullName;
    private long firstResult=0;
    private long maxResults=Integer.MAX_VALUE;
    private boolean ascending=true;

    private String orderBy="username";

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    public boolean hasUsername() {
        return username != null && !"".equals(username);
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public void setEmail(String email) {
        this.email = email;
    }

    public boolean hasEmail() {
        return email!=null && !"".equals(email);
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean hasFullName() {
        return fullName!=null && !"".equals(fullName);
    }

    @Override
    public long getFirstResult() {
        return firstResult;
    }

    public void setFirstResult(int firstResult) {
        this.firstResult = firstResult;
    }

    @Override
    public long getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    @Override
    public boolean isAscending() {
        return ascending;
    }

    @Override
    public void setAscending(boolean ascending) {
        this.ascending = ascending;
    }


    @Override
    public String getOrderBy() {
        return orderBy;
    }

    @Override
    public void setOrderBy(String orderBy) {
        if (!UserQuery.ALLOWED_ORDER_FIELDS.contains(orderBy)) {
            throw new IllegalArgumentException("Order attribute not allowed: "+orderBy);
        }
        this.orderBy = orderBy;
    }
}
