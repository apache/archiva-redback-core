package org.apache.archiva.redback.rest.services.mock;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created by Martin Stockhammer on 21.01.17.
 *
 * User configuration implementation to be used in unit tests.
 *
 */
public class MockUserConfiguration implements UserConfiguration {

    private Map<String, Object> values = new java.util.HashMap<String,Object>();

    @SuppressWarnings("SameParameterValue")
    public void addValue(String key, String value) {
        values.put(key,value);
    }

    public void addList(String key, List<String> listValue) {
        values.put(key, listValue);
    }

    @Override
    public void initialize() throws UserConfigurationException {

    }

    @Override
    public String getString(String key) {
        return values.get(key).toString();
    }

    @Override
    public String getString(String key, String defaultValue) {
        if (values.containsKey(key)) {
            return values.get(key).toString();
        } else {
            return defaultValue;
        }
    }

    @Override
    public int getInt(String key) {
        return getInt(key, -1);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        if (values.containsKey(key)) {
            return Integer.parseInt(values.get(key).toString());
        } else {
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        if (values.containsKey(key)) {
            return Boolean.parseBoolean(values.get(key).toString());
        } else {
            return defaultValue;
        }
    }

    @Override
    public List<String> getList(String key) {
        Object value = values.get(key);
        if (value!=null && value instanceof List) {
            return (List)value;
        } else {
            return null;
        }
    }

    @Override
    public String getConcatenatedList(String key, String defaultValue) {
        return null;
    }

    @Override
    public Collection<String> getKeys() {
        return values.keySet();
    }
}
