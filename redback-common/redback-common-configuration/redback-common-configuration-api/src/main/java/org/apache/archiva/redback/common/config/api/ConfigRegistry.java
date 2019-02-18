package org.apache.archiva.redback.common.config.api;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

/**
 * The configuration registry is a single source of external configuration.
 *
 * Each configuration entry is accessible by a unique key. The keys may be hierarchical so, that
 * you can build subsets of the configuration.
 *
 * A configuration may be combined by multiple sources. How the multiple sources are combined, is
 * part of the implementation classes.
 *
 * You can register listeners that are notified, if the configuration changes. The syntax for filter
 * of notifications is implementation specific.
 *
 */
public interface ConfigRegistry
{

    /**
     * Dump the entire registry to a string, for debugging purposes.
     *
     * @return the registry contents
     */
    String dump( );

    /**
     * Get the original value stored in the registry. If not found, <code>null</code> is returned.
     *
     * @param key The key in the registry.
     * @return The value.
     */
    Object getValue( String key);

    /**
     * Get a string value from the registry. If not found, <code>null</code> is returned.
     *
     * @param key the key in the registry
     * @return the value
     */
    String getString( String key );

    /**
     * Get a string value from the registry. If not found, the default value is used.
     *
     * @param key          the key in the registry
     * @param defaultValue the default value
     * @return the value
     */
    String getString( String key, String defaultValue );

    /**
     * Set a string value in the registry.
     *
     * @param key   the key in the registry
     * @param value the value to set
     */
    void setString( String key, String value );

    /**
     * Get an integer value from the registry. If not found, an exception is thrown.
     *
     * @param key the key in the registry
     * @return the value
     * @throws java.util.NoSuchElementException
     *          if the key is not found
     */
    int getInt( String key ) throws NoSuchElementException;

    /**
     * Get an integer value from the registry. If not found, the default value is used.
     *
     * @param key          the key in the registry
     * @param defaultValue the default value
     * @return the value
     */
    int getInt( String key, int defaultValue );

    /**
     * Set an integer value in the registry.
     *
     * @param key   the key in the registry
     * @param value the value to set
     */
    void setInt( String key, int value );

    /**
     * Get a boolean value from the registry. If not found, an exception is thrown.
     *
     * @param key the key in the registry
     * @return the value
     * @throws java.util.NoSuchElementException
     *          if the key is not found
     */
    boolean getBoolean( String key ) throws NoSuchElementException;

    /**
     * Get a boolean value from the registry. If not found, the default value is used.
     *
     * @param key          the key in the registry
     * @param defaultValue the default value
     * @return the value
     */
    boolean getBoolean( String key, boolean defaultValue );

    /**
     * Set a boolean value in the registry.
     *
     * @param key   the key in the registry
     * @param value the value to set
     */
    void setBoolean( String key, boolean value );

    /**
     * Load configuration from the given classloader resource.
     *
     * @param name the unique name that identifies this configuration in the combined one
     * @param resource the location to load the configuration from
     * @throws RegistryException if a problem occurred reading the resource to add to the registry
     */
    void addConfigurationFromResource( String name, String resource )
        throws RegistryException;

    /**
     * Load configuration from the given classloader resource.
     *
     * @param name the unique name that identifies this configuration in the combined one
     * @param resource the location to load the configuration from
     * @param prefix   the location to add the configuration at in the registry
     * @throws RegistryException if a problem occurred reading the resource to add to the registry
     */
    void addConfigurationFromResource( String name, String resource, String prefix )
        throws RegistryException;

    /**
     * Load configuration from the given file.
     *
     * @param name the unique name that identifies this configuration in the combined one
     * @param file the location to load the configuration from
     * @throws RegistryException if a problem occurred reading the resource to add to the registry
     */
    void addConfigurationFromFile( String name, Path file )
        throws RegistryException;

    /**
     * Load configuration from the given file.
     *
     * @param name the unique name that identifies this configuration in the combined one
     * @param file   the location to load the configuration from
     * @param prefix the location to add the configuration at in the registry
     * @throws RegistryException if a problem occurred reading the resource to add to the registry
     */
    void addConfigurationFromFile( String name, Path file, String prefix )
        throws RegistryException;

    /**
     * Determine if the registry contains any elements.
     *
     * @return whether the registry contains any elements
     */
    boolean isEmpty( );

    /**
     * Get a list of strings at the given key in the registry. If not found a empty list will be returned.
     *
     * @param key the key to lookup
     * @return the list of strings
     */
    List<String> getList( String key );

    /**
     * Get the properties at the given key in the registry.
     *
     * @param key the key to lookup
     * @return the properties
     */
    Map<String, String> getProperties( String key );

    /**
     * Get a subset of the registry, for all keys descended from the given key.
     *
     * @param key the key to take the subset from
     * @return the registry subset
     */
    ConfigRegistry getSubset( String key ) throws RegistryException;

    /**
     * Get a list of subsets of the registry, for all keys descended from the given key.
     *
     * @param key the key to take the subsets from
     * @return the registry subsets
     */
    List<ConfigRegistry> getSubsetList( String key ) throws RegistryException;

    /**
     * Get a configuration source part of the registry, identified by the given name. If it doesn't exist, <code>null</code> will be
     * returned.
     *
     * Configurations can be combined from different sources. This returns the configuration of a specific source.
     * The names are the ones given by the addConfiguration* methods or defined during the initialization process.
     *
     * @param name The source name of the configuration source.
     * @return the The config registry object that represents this source part.
     */
    ConfigRegistry getPartOfCombined(String name );

    /**
     * Save any changes to the registry since it was loaded.
     *
     * @throws RegistryException             if there was a problem saving the registry
     * @throws UnsupportedOperationException if the registry is not writable
     */
    void save( )
        throws RegistryException, UnsupportedOperationException;

    /**
     * Register a change listener for configuration keys that match the given patterns.
     *
     * @param listener the listener
     */
    void registerChangeListener( RegistryListener listener, String prefix );

    /**
     * Unregister the change listener for all events.
     *
     * @param listener
     * @return <code>true</code> if has been removed
     */
    boolean unregisterChangeListener( RegistryListener listener );

    /**
     * Get all keys on the base level in this registry. Keys are only retrieved at a depth of 1.
     *
     * @return the set of keys
     */
    Collection<String> getBaseKeys( );

    /**
     * Get all the keys in this registry.
     * @return the set of keys
     */
    Collection<String> getKeys( );

    /**
     * Return the keys that match the given prefix.
     *
     * @param prefix The prefix
     * @return A collection of keys
     */
    Collection<String> getKeys( String prefix);

    /**
     * Remove a keyed element from the registry.
     *
     * @param key the key to remove
     */
    void remove( String key );

    /**
     * Remove a keyed subset of the registry.
     *
     * @param key the subset to remove
     */
    void removeSubset( String key );

    /**
     * Initializes the given registry. The definition for the configuration sources is
     * implementation specific. Implementations should provide methods and/or constructors
     * that allows to define the configuration source.
     *
     * @throws RegistryException if the initialization was not successful
     */
    void initialize( ) throws RegistryException;
}
