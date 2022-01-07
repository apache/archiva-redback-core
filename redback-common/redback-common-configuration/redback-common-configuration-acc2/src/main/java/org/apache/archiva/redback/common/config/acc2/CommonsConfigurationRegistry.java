package org.apache.archiva.redback.common.config.acc2;

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

import org.apache.archiva.redback.common.config.api.ConfigRegistry;
import org.apache.archiva.redback.common.config.api.RegistryException;
import org.apache.archiva.redback.common.config.api.RegistryListener;
import org.apache.commons.configuration2.*;
import org.apache.commons.configuration2.builder.ConfigurationBuilder;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.combined.CombinedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.event.ConfigurationEvent;
import org.apache.commons.configuration2.event.EventSource;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.ex.ConfigurationRuntimeException;
import org.apache.commons.configuration2.interpol.ConfigurationInterpolator;
import org.apache.commons.configuration2.interpol.DefaultLookups;
import org.apache.commons.configuration2.interpol.InterpolatorSpecification;
import org.apache.commons.configuration2.io.ClasspathLocationStrategy;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.io.FileLocatorUtils;
import org.apache.commons.configuration2.io.FileSystem;
import org.apache.commons.configuration2.tree.DefaultExpressionEngine;
import org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols;
import org.apache.commons.configuration2.tree.NodeCombiner;
import org.apache.commons.configuration2.tree.UnionCombiner;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Implementation of the registry component using
 * <a href="https://commons.apache.org/proper/commons-configuration/index.html">Commons Configuration 2</a>.
 * The use of Commons Configuration enables a variety of sources to be used, including XML files, properties, JNDI, JDBC, etc.
 * <p/>
 * The component can be configured using the {@link #combinedConfigurationDefinition} configuration item, the content of which should take
 * the format of an input to the Commons Configuration
 * <a href="http://commons.apache.org/commons/configuration/howto_configurationbuilder.html">configuration
 * builder</a>.
 */
@Service( "acc2-configuration" )
public class CommonsConfigurationRegistry
    implements ConfigRegistry
{
    private static final Pattern DOT_NAME_PATTERN = Pattern.compile( "([^.]+)(\\..*)*" );

    /**
     * The combined configuration instance that houses the registry.
     */
    private Configuration configuration;


    private ConfigurationBuilder<? extends Configuration> configurationBuilder;
    private boolean combined = true;
    private final Map<String, ConfigurationBuilder<? extends Configuration>> builderMap = new HashMap<>( );

    private final Logger logger = LoggerFactory.getLogger( getClass( ) );

    private String propertyDelimiter = ".";

    private boolean addSystemProperties = false;

    final CfgListener listener = new CfgListener( this );

    /**
     * The configuration properties for the registry. This should take the format of an input to the Commons
     * Configuration
     * <a href="https://commons.apache.org/proper/commons-configuration/userguide/howto_combinedbuilder.html#The_configuration_definition_file">configuration
     * builder</a>.
     */
    private String combinedConfigurationDefinition;


    public CommonsConfigurationRegistry( )
    {
        // Default constructor
    }


    public CommonsConfigurationRegistry( CombinedConfiguration configuration, CombinedConfigurationBuilder configurationBuilder )
    {
        if ( configuration == null )
        {
            throw new NullPointerException( "configuration can not be null" );
        }
        if ( configurationBuilder == null )
        {
            throw new NullPointerException( "configuration builder cannot be null for a combined configuration" );
        }
        this.combined = true;
        setConfiguration(configuration);
        this.configurationBuilder = configurationBuilder;
    }

    @SuppressWarnings( "WeakerAccess" )
    public CommonsConfigurationRegistry( Configuration configuration, ConfigurationBuilder<? extends Configuration> configurationBuilder )
    {
        if ( configuration == null )
        {
            throw new NullPointerException( "configuration can not be null" );
        }
        setConfiguration(configuration);
        this.configurationBuilder = configurationBuilder;
        if (configuration instanceof CombinedConfiguration) {
            this.combined = true;
        }
    }

    public void setConfiguration( Configuration configuration )
    {
        this.configuration = configuration;
        if (configuration instanceof EventSource ) {
            EventSource evs = (EventSource) configuration;
            evs.removeEventListener( ConfigurationEvent.ANY, listener );
            evs.addEventListener( ConfigurationEvent.ANY, listener );
            evs.addEventListener( ConfigurationEvent.SUBNODE_CHANGED, listener );
        }
    }

    @Override
    public String dump( )
    {
        StringBuilder buffer = new StringBuilder( "Configuration Dump:\n");
        Iterable<String> it = () -> configuration.getKeys();
        return buffer.append(StreamSupport.stream( it.spliterator(), false ).map( k ->
            "\""+k+"\" = \""+configuration.getProperty( k ).toString() + "\"").collect(Collectors.joining( "\n" ) )).toString();
    }

    @Override
    public boolean isEmpty( )
    {
        return configuration.isEmpty( );
    }

    @Override
    public ConfigRegistry getSubset( String key ) throws RegistryException
    {
        if (configuration instanceof BaseHierarchicalConfiguration) {
            BaseHierarchicalConfiguration cfg = (BaseHierarchicalConfiguration) configuration;
            if (cfg.containsKey( key ))
            {
                try
                {
                    return new CommonsConfigurationRegistry( cfg.configurationAt( key, true ), null );
                } catch ( ConfigurationRuntimeException ex ) {
                    logger.error("There are multiple entries for the given key");
                    throw new RegistryException( "Subset for multiple key entries is not possible.");
                }
            } else {
                return new CommonsConfigurationRegistry( cfg.subset( key ), null );
            }
        }
        return new CommonsConfigurationRegistry( configuration.subset( key ), configurationBuilder );
    }

    @Override
    public List<String> getList( String key )
    {
        List<String> result = configuration.getList( String.class, key );
        return result == null ? new ArrayList<>( ) : result;
    }

    @Override
    public List<ConfigRegistry> getSubsetList( String key ) throws RegistryException
    {

        if (configuration instanceof BaseHierarchicalConfiguration) {
            BaseHierarchicalConfiguration cfg = (BaseHierarchicalConfiguration)configuration;
            return cfg.configurationsAt( key, true ).stream().map(c -> new CommonsConfigurationRegistry( c, null )).collect( Collectors.toList() );
        } else
        {
            List<ConfigRegistry> subsets = new ArrayList<>( );
            boolean done = false;
            do
            {
                ConfigRegistry registry = null;
                try
                {
                    registry = getSubset( key + "(" + subsets.size( ) + ")" );
                }
                catch ( RegistryException e )
                {
                    throw new RegistryException( "Could not retrieve subset from key "+key+": "+e.getMessage() );
                }
                if ( !registry.isEmpty( ) )
                {
                    subsets.add( registry );
                }
                else
                {
                    done = true;
                }
            }
            while ( !done );
            return subsets;
        }
    }

    @Override
    public ConfigRegistry getPartOfCombined( String name )
    {
        if ( combined )
        {
            CombinedConfiguration config = (CombinedConfiguration) configuration;
            Configuration newCfg = config.getConfiguration( name );
            ConfigurationBuilder<? extends Configuration> cfgBuilder = null;
            try
            {
                if ( builderMap.containsKey( name ) )
                {
                    cfgBuilder = builderMap.get( name );
                }
                else
                {
                    cfgBuilder = configurationBuilder == null ? null :
                        ( (CombinedConfigurationBuilder) configurationBuilder ).getNamedBuilder( name );
                    builderMap.put( name, cfgBuilder );
                }
            }
            catch ( ConfigurationException e )
            {
                logger.error( "Could not retrieve configuration builder: " + e.getMessage( ) );
            }
            return newCfg == null ? null : new CommonsConfigurationRegistry( newCfg, cfgBuilder );
        }
        return null;
    }

    @Override
    public Map<String, String> getProperties( String key )
    {
        Configuration configuration = this.configuration.subset( key );

        Map<String, String> properties = new TreeMap<>( );
        Iterator<String> cfgIter = configuration.getKeys( );
        String property;
        while ( cfgIter.hasNext( ) )
        {
            property = cfgIter.next( );
            List<String> l = configuration.getList( String.class, property );
            String value = String.join( ",", l );
            properties.put( property, value );
        }
        return properties;
    }

    @Override
    public void save( )
        throws RegistryException
    {
        if ( configuration instanceof FileBasedConfiguration )
        {
            FileBasedConfiguration fileConfiguration = (FileBasedConfiguration) configuration;
            FileHandler fileHandler;
            if ( configurationBuilder != null && configurationBuilder instanceof FileBasedConfigurationBuilder )
            {
                FileBasedConfigurationBuilder cfgBuilder = (FileBasedConfigurationBuilder) configurationBuilder;
                fileHandler = cfgBuilder.getFileHandler( );
            }
            else
            {
                fileHandler = new FileHandler( fileConfiguration );
            }
            try
            {
                fileHandler.save( );
            }
            catch ( ConfigurationException e )
            {
                throw new RegistryException( e.getMessage( ), e );
            }
        }
        else
        {
            throw new RegistryException( "Can only save file-based configurations" );
        }
    }

    @Override
    public void registerChangeListener( RegistryListener listener, String prefix)
    {
        this.listener.registerChangeListener(listener, prefix);
    }

    @Override
    public boolean unregisterChangeListener( RegistryListener listener )
    {
        return this.listener.unregisterChangeListener(listener);
    }

    @Override
    public Collection<String> getBaseKeys( )
    {
        Iterable<String> iterable = ( ) -> configuration.getKeys( );
        return StreamSupport.stream( iterable.spliterator( ), true )
            .map( DOT_NAME_PATTERN::matcher )
            .filter( Matcher::matches )
            .map( k -> k.group( 1 ) ).collect( Collectors.toSet( ) );

    }

    @Override
    public Collection<String> getKeys( )
    {
        Iterable<String> iterable = ( ) -> configuration.getKeys( );
        return StreamSupport.stream( iterable.spliterator( ), true ).collect( Collectors.toSet( ) );
    }

    @Override
    public Collection<String> getKeys( String prefix )
    {
        Iterable<String> iterable = ( ) -> configuration.getKeys( prefix );
        return StreamSupport.stream( iterable.spliterator( ), true ).collect( Collectors.toSet( ) );
    }

    @Override
    public void remove( String key )
    {
        configuration.clearProperty( key );
    }

    @Override
    public void removeSubset( String key )
    {
        getKeys( key ).forEach( k -> configuration.clearProperty( k ) );
    }

    @Override
    public Object getValue( String key ) {
        return configuration.getProperty(  key );
    }

    @Override
    public String getString( String key )
    {
        return configuration.getString( key );
    }

    @Override
    public String getString( String key, String defaultValue )
    {
        return configuration.getString( key, defaultValue );
    }

    @Override
    public void setString( String key, String value )
    {
        configuration.setProperty( key, value );
    }

    @Override
    public int getInt( String key )
    {
        return configuration.getInt( key );
    }

    @Override
    public int getInt( String key, int defaultValue )
    {
        return configuration.getInt( key, defaultValue );
    }

    @Override
    public void setInt( String key, int value )
    {
        configuration.setProperty( key, value );
    }

    @Override
    public boolean getBoolean( String key )
    {
        return configuration.getBoolean( key );
    }

    @Override
    public boolean getBoolean( String key, boolean defaultValue )
    {
        return configuration.getBoolean( key, defaultValue );
    }

    @Override
    public void setBoolean( String key, boolean value )
    {
        configuration.setProperty( key, value );
    }

    @Override
    public void addConfigurationFromResource( String name, String resource )
        throws RegistryException
    {
        addConfigurationFromResource( name, resource, null );
    }

    @Override
    public void addConfigurationFromResource( String name, String resource, String prefix )
        throws RegistryException
    {
        if ( configuration instanceof CombinedConfiguration )
        {
            String atPrefix = StringUtils.isEmpty( prefix ) ? null : prefix;
            CombinedConfiguration configuration = (CombinedConfiguration) this.configuration;
            if ( resource.endsWith( ".properties" ) )
            {
                try
                {
                    logger.debug( "Loading properties configuration from classloader resource: {}", resource );
                    FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>( PropertiesConfiguration.class )
                        .configure( new Parameters( ).properties( )
                            .setLocationStrategy( new ClasspathLocationStrategy( ) )
                            .setFileName( resource ) );
                    builderMap.put( name, builder );
                    configuration.addConfiguration( builder.getConfiguration( ), name, atPrefix );
                }
                catch ( ConfigurationException e )
                {
                    throw new RegistryException(
                        "Unable to add configuration from resource '" + resource + "': " + e.getMessage( ), e );
                }
            }
            else if ( resource.endsWith( ".xml" ) )
            {
                try
                {
                    logger.debug( "Loading XML configuration from classloader resource: {}", resource );
                    FileBasedConfigurationBuilder<XMLConfiguration> builder = new FileBasedConfigurationBuilder<>( XMLConfiguration.class )
                        .configure( new Parameters( ).xml( )
                            .setLocationStrategy( new ClasspathLocationStrategy( ) )
                            .setFileName( resource ) );
                    builderMap.put( name, builder );
                    configuration.addConfiguration( builder.getConfiguration( ), name, atPrefix );
                }
                catch ( ConfigurationException e )
                {
                    throw new RegistryException(
                        "Unable to add configuration from resource '" + resource + "': " + e.getMessage( ), e );
                }
            }
            else
            {
                throw new RegistryException(
                    "Unable to add configuration from resource '" + resource + "': unrecognised type" );
            }
        }
        else
        {
            throw new RegistryException( "The underlying configuration object is not a combined configuration " );
        }
    }

    @Override
    public void addConfigurationFromFile( String name, Path file ) throws RegistryException
    {
        addConfigurationFromFile( name, file, "" );
    }

    @Override
    public void addConfigurationFromFile( String name, Path file, String prefix )
        throws RegistryException
    {
        if ( this.configuration instanceof CombinedConfiguration )
        {
            String atPrefix = StringUtils.isEmpty( prefix ) ? null : prefix;
            CombinedConfiguration configuration = (CombinedConfiguration) this.configuration;
            String fileName = file.getFileName( ).toString( );
            if ( fileName.endsWith( ".properties" ) )
            {
                try
                {
                    logger.debug( "Loading properties configuration from file: {}", file );
                    FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>( PropertiesConfiguration.class )
                        .configure( new Parameters( ).properties( )
                            .setFileSystem( FileLocatorUtils.DEFAULT_FILE_SYSTEM )
                            .setLocationStrategy( FileLocatorUtils.DEFAULT_LOCATION_STRATEGY )
                            .setFile( file.toFile( ) ) );
                    // builder is needed for save
                    builderMap.put( name, builder );
                    configuration.addConfiguration( builder.getConfiguration( ), name, atPrefix );
                }
                catch ( ConfigurationException e )
                {
                    throw new RegistryException(
                        "Unable to add configuration from file '" + file.getFileName( ) + "': " + e.getMessage( ), e );
                }
            }
            else if ( fileName.endsWith( ".xml" ) )
            {
                try
                {
                    logger.debug( "Loading XML configuration from file: {}", file );
                    FileBasedConfigurationBuilder<XMLConfiguration> builder = new FileBasedConfigurationBuilder<>( XMLConfiguration.class )
                        .configure( new Parameters( ).xml( )
                            .setFileSystem( FileLocatorUtils.DEFAULT_FILE_SYSTEM )
                            .setLocationStrategy( FileLocatorUtils.DEFAULT_LOCATION_STRATEGY )
                            .setFile( file.toFile( ) ) );
                    builderMap.put( name, builder );
                    configuration.addConfiguration( builder.getConfiguration( ), name, atPrefix );
                }
                catch ( ConfigurationException e )
                {
                    throw new RegistryException(
                        "Unable to add configuration from file '" + file.getFileName( ) + "': " + e.getMessage( ), e );
                }
            }
            else
            {
                throw new RegistryException(
                    "Unable to add configuration from file '" + file.getFileName( ) + "': unrecognised type" );
            }
        }
        else
        {
            throw new RegistryException( "The underlying configuration is not a combined configuration object." );
        }
    }

    /**
     * This is a dummy FileSystem needed to load the CombinedConfiguration declaration from a String.
     */
    class StringFileSystem extends FileSystem
    {

        final String content;
        String encoding = "UTF-8";

        StringFileSystem( String content )
        {
            this.content = content;
        }


        @SuppressWarnings( "unused" )
        StringFileSystem( String encoding, String content )
        {
            this.encoding = encoding;
            this.content = content;
        }

        @Override
        public InputStream getInputStream( URL url ) throws ConfigurationException
        {
            try
            {
                return new ByteArrayInputStream( content.getBytes( encoding ) );
            }
            catch ( UnsupportedEncodingException e )
            {
                logger.error( "Bad encoding for FileSystem" );
                throw new ConfigurationException( "Bad encoding specified" );
            }
        }

        @Override
        public OutputStream getOutputStream( URL url )
        {
            return new ByteArrayOutputStream( 0 );
        }

        @Override
        public OutputStream getOutputStream( File file )
        {
            return new ByteArrayOutputStream( 0 );
        }

        @Override
        public String getPath( File file, URL url, String basePath, String fileName )
        {
            return basePath + "/" + fileName;
        }

        @Override
        public String getBasePath( String path )
        {
            return path;
        }

        @Override
        public String getFileName( String path )
        {
            return path;
        }

        @Override
        public URL locateFromURL( String basePath, String fileName )
        {
            try
            {
                return new URL( "file://" + getPath( null, null, basePath, fileName ) );
            }
            catch ( MalformedURLException e )
            {
                // ignore
                return null;
            }
        }

        @Override
        public URL getURL( String basePath, String fileName ) throws MalformedURLException
        {
            return new URL( "file://" + getPath( null, null, basePath, fileName ) );
        }

    }

    @Override
    @PostConstruct
    public void initialize( )
        throws RegistryException
    {
        try
        {
            CombinedConfiguration configuration;
            if ( StringUtils.isNotBlank( combinedConfigurationDefinition ) )
            {
                String interpolatedProps;
                Parameters params = new Parameters( );
                DefaultExpressionEngineSymbols symbols = new DefaultExpressionEngineSymbols.Builder( DefaultExpressionEngineSymbols.DEFAULT_SYMBOLS )
                    .setPropertyDelimiter( propertyDelimiter )
                    .setIndexStart( "(" )
                    .setIndexEnd( ")" )
                    .setEscapedDelimiter( "\\" + propertyDelimiter )
                    .create( );
                DefaultExpressionEngine expressionEngine = new DefaultExpressionEngine( symbols );

                // It allows to use system properties in the XML declaration.

                ConfigurationInterpolator interpolator = ConfigurationInterpolator.fromSpecification( new InterpolatorSpecification.Builder( ).withDefaultLookup( DefaultLookups.SYSTEM_PROPERTIES.getLookup( ) ).create( ) );
                interpolatedProps = interpolator.interpolate( combinedConfigurationDefinition ).toString( );
                logger.debug( "Loading configuration into commons-configuration, xml {}", interpolatedProps );
                // This is the builder configuration for the XML declaration, that contains the definition
                // for the sources that are used for the CombinedConfiguration.
                FileSystem fs = new StringFileSystem( interpolatedProps );
                FileBasedConfigurationBuilder<XMLConfiguration> cfgBuilder =
                    new FileBasedConfigurationBuilder<>(
                        XMLConfiguration.class )
                        .configure( params.xml( )
                            .setFileSystem( fs )
                            .setFileName( "config.xml" )
                            .setListDelimiterHandler(
                                new DefaultListDelimiterHandler( ',' ) )
                            .setExpressionEngine( expressionEngine )
                            .setThrowExceptionOnMissing( false ) );

                CombinedConfigurationBuilder builder = new CombinedConfigurationBuilder( ).
                    configure( params.combined( ).setDefinitionBuilder( cfgBuilder ) );
                // The builder is needed later for saving of the file parts in the combined configuration.
                this.configurationBuilder = builder;
                configuration = builder.getConfiguration( );


            }
            else
            {
                logger.debug( "Creating a default configuration - no configuration was provided" );
                NodeCombiner combiner = new UnionCombiner( );
                configuration = new CombinedConfiguration( combiner );
                this.configurationBuilder = null;
            }

            // In the end, we add the system properties to the combined configuration
            if ( addSystemProperties )
            {
                configuration.addConfiguration( new SystemConfiguration( ), "SystemProperties" );
            }

            this.configuration = configuration;
        }
        catch ( ConfigurationException e )
        {
            logger.error( "Fatal error, while reading the configuration definition: " + e.getMessage( ) );
            logger.error( "The definition was:" );
            logger.error( combinedConfigurationDefinition );
            throw new RegistryException( e.getMessage( ), e );
        }
    }

    public void setCombinedConfigurationDefinition( String combinedConfigurationDefinition )
    {
        this.combinedConfigurationDefinition = combinedConfigurationDefinition;
    }

    public String getPropertyDelimiter( )
    {
        return propertyDelimiter;
    }

    public void setPropertyDelimiter( String propertyDelimiter )
    {
        this.propertyDelimiter = propertyDelimiter;
    }


    public ConfigurationBuilder<? extends Configuration> getConfigurationBuilder( )
    {
        return configurationBuilder;
    }

    /**
     * Returns true, if the system properties are added to the base configuration. Otherwise system properties
     * can still be interpolated by ${sys:var} syntax.
     *
     * @return True, if system properties are added to the configuration root
     */
    public boolean isAddSystemProperties( )
    {
        return addSystemProperties;
    }

    /**
     * Set to true, if the system properties should be added to the base configuration.
     * If set to false, system properties are no direct part of the configuration.
     *
     * @param addSystemProperties True, or false.
     */
    public void setAddSystemProperties( boolean addSystemProperties )
    {
        this.addSystemProperties = addSystemProperties;
    }
}
