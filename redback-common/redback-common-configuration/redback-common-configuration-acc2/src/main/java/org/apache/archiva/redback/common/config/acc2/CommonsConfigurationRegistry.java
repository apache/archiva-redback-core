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
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.combined.CombinedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.interpol.ConfigurationInterpolator;
import org.apache.commons.configuration2.interpol.DefaultLookups;
import org.apache.commons.configuration2.interpol.InterpolatorSpecification;
import org.apache.commons.configuration2.io.ClasspathLocationStrategy;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.io.FileSystem;
import org.apache.commons.configuration2.io.FileSystemLocationStrategy;
import org.apache.commons.configuration2.tree.DefaultExpressionEngine;
import org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols;
import org.apache.commons.configuration2.tree.NodeCombiner;
import org.apache.commons.configuration2.tree.UnionCombiner;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Implementation of the registry component using
 * <a href="http://commons.apache.org/commons/configuration">Commons Configuration</a>. The use of Commons Configuration
 * enables a variety of sources to be used, including XML files, properties, JNDI, JDBC, etc.
 * <p/>
 * The component can be configured using the {@link #properties} configuration item, the content of which should take
 * the format of an input to the Commons Configuration
 * <a href="http://commons.apache.org/commons/configuration/howto_configurationbuilder.html">configuration
 * builder</a>.
 */
@Service( "acc2-configuration" )
public class CommonsConfigurationRegistry
    implements ConfigRegistry
{
    /**
     * The combined configuration instance that houses the registry.
     */
    private Configuration configuration;

    private Logger logger = LoggerFactory.getLogger( getClass() );

    private String propertyDelimiter = ".";

    /**
     * The configuration properties for the registry. This should take the format of an input to the Commons
     * Configuration
     * <a href="http://commons.apache.org/configuration/howto_configurationbuilder.html">configuration
     * builder</a>.
     */
    private String properties;


    public CommonsConfigurationRegistry()
    {
        // default constructor
        logger.debug( "empty constructor" );
    }

    public CommonsConfigurationRegistry( Configuration configuration )
    {
        if ( configuration == null )
        {
            throw new NullPointerException( "configuration can not be null" );
        }

        this.configuration = configuration;
    }

    public String dump()
    {
        StringBuilder buffer = new StringBuilder();
        buffer.append( "Configuration Dump." );
        for ( Iterator i = configuration.getKeys(); i.hasNext(); )
        {
            String key = (String) i.next();
            Object value = configuration.getProperty( key );
            buffer.append( "\n\"" ).append( key ).append( "\" = \"" ).append( value ).append( "\"" );
        }
        return buffer.toString();
    }

    public boolean isEmpty()
    {
        return configuration.isEmpty();
    }

    public ConfigRegistry getSubset( String key )
    {
        return new CommonsConfigurationRegistry( configuration.subset( key ) );
    }

    public List<String> getList( String key )
    {
        return configuration.getList(String.class,  key );
    }

    public List<ConfigRegistry> getSubsetList( String key )
    {
        List<ConfigRegistry> subsets = new ArrayList<>();

        boolean done = false;
        do
        {
            ConfigRegistry registry = getSubset( key + "(" + subsets.size() + ")" );
            if ( !registry.isEmpty() )
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

    @Override
    public ConfigRegistry getSource( String name )
    {
        return null;
    }

    public Map<String,String> getProperties( String key )
    {
        Configuration configuration = this.configuration.subset( key );

        Map<String,String> properties = new TreeMap<>();
        Iterator<String> cfgIter = configuration.getKeys( );
        String property;
        while ( cfgIter.hasNext() )
        {
            property = cfgIter.next();
            List<String> l = configuration.getList( String.class, property );
            String value = String.join(",", l);
            properties.put( property, value );
        }
        return properties;
    }

    public void save()
        throws RegistryException
    {
        if ( configuration instanceof FileBasedConfiguration  )
        {
            FileBasedConfiguration fileConfiguration = (FileBasedConfiguration) configuration;
            try
            {
                new FileHandler( fileConfiguration ).save();
            }
            catch ( ConfigurationException e )
            {
                throw new RegistryException( e.getMessage(), e );
            }
        }
        else
        {
            throw new RegistryException( "Can only save file-based configurations" );
        }
    }

    @Override
    public void registerChangeListener( RegistryListener listener, Pattern... filter )
    {

    }

    @Override
    public boolean unregisterChangeListener( RegistryListener listener )
    {
        return false;
    }


    public Collection<String> getKeys()
    {
        Set<String> keys = new HashSet<String>();

        for ( Iterator<String> i = configuration.getKeys(); i.hasNext(); )
        {
            String key = i.next();

            int index = key.indexOf( '.' );
            if ( index < 0 )
            {
                keys.add( key );
            }
            else
            {
                keys.add( key.substring( 0, index ) );
            }
        }

        return keys;
    }

    public Collection getFullKeys()
    {
        Set<String> keys = new HashSet<String>();

        for ( Iterator<String> i = configuration.getKeys(); i.hasNext(); )
        {
            keys.add( i.next() );
        }

        return keys;
    }

    public void remove( String key )
    {
        configuration.clearProperty( key );
    }

    public void removeSubset( String key )
    {
        // create temporary list since removing a key will modify the iterator from configuration
        List keys = new ArrayList();
        for ( Iterator i = configuration.getKeys( key ); i.hasNext(); )
        {
            keys.add( i.next() );
        }

        for ( Iterator i = keys.iterator(); i.hasNext(); )
        {
            configuration.clearProperty( (String) i.next() );
        }
    }

    public String getString( String key )
    {
        return configuration.getString( key );
    }

    public String getString( String key, String defaultValue )
    {
        return configuration.getString( key, defaultValue );
    }

    public void setString( String key, String value )
    {
        configuration.setProperty( key, value );
    }

    public int getInt( String key )
    {
        return configuration.getInt( key );
    }

    public int getInt( String key, int defaultValue )
    {
        return configuration.getInt( key, defaultValue );
    }

    public void setInt( String key, int value )
    {
        configuration.setProperty( key, Integer.valueOf( value ) );
    }

    public boolean getBoolean( String key )
    {
        return configuration.getBoolean( key );
    }

    public boolean getBoolean( String key, boolean defaultValue )
    {
        return configuration.getBoolean( key, defaultValue );
    }

    public void setBoolean( String key, boolean value )
    {
        configuration.setProperty( key, Boolean.valueOf( value ) );
    }

    public void addConfigurationFromResource( String name, String resource )
        throws RegistryException
    {
        addConfigurationFromResource( name, resource, null );
    }

    public void addConfigurationFromResource( String name, String resource, String prefix )
        throws RegistryException
    {
        if (configuration instanceof CombinedConfiguration)
        {
            String atPrefix = StringUtils.isEmpty( prefix ) ? null : prefix;
            CombinedConfiguration configuration = (CombinedConfiguration) this.configuration;
            if ( resource.endsWith( ".properties" ) )
            {
                try
                {
                    logger.debug( "Loading properties configuration from classloader resource: {}", resource );
                    FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>( PropertiesConfiguration.class )
                        .configure( new Parameters().properties()
                            .setLocationStrategy( new ClasspathLocationStrategy() )
                            .setFileName( resource ) );
                    configuration.addConfiguration( builder.getConfiguration() , name, atPrefix );
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
                        .configure( new Parameters().xml()
                            .setLocationStrategy( new ClasspathLocationStrategy() )
                            .setFileName( resource ) );
                    configuration.addConfiguration( builder.getConfiguration(), name, atPrefix );
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
        } else {
            throw new RegistryException( "The underlying configuration object is not a combined configuration " );
        }
    }

    @Override
    public void addConfigurationFromFile( String name, Path file ) throws RegistryException
    {
        addConfigurationFromFile( name, file, "" );
    }

    public void addConfigurationFromFile( String name, Path file, String prefix )
        throws RegistryException
    {
        if (this.configuration instanceof CombinedConfiguration)
        {
            String atPrefix = StringUtils.isEmpty( prefix ) ? null : prefix;
            CombinedConfiguration configuration = (CombinedConfiguration) this.configuration;
            String fileName = file.getFileName().toString();
            if ( fileName.endsWith( ".properties" ) )
            {
                try
                {
                    logger.debug( "Loading properties configuration from file: {}", file );
                    FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>( PropertiesConfiguration.class )
                        .configure( new Parameters().properties()
                            .setLocationStrategy( new FileSystemLocationStrategy() )
                            .setFile( file.toFile() ) );
                    configuration.addConfiguration( builder.getConfiguration() , name, atPrefix );
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
                        .configure( new Parameters().xml()
                            .setLocationStrategy( new ClasspathLocationStrategy() )
                            .setFile( file.toFile()) );
                    configuration.addConfiguration( builder.getConfiguration(), name, atPrefix );
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
        } else {
            throw new RegistryException( "The underlying configuration is not a combined configuration object." );
        }
    }

    class StringFileSystem extends FileSystem {

        String content;
        String encoding = "UTF-8";

        StringFileSystem(String content) {
            this.content = content;
        }

        @Override
        public InputStream getInputStream( URL url ) throws ConfigurationException
        {
            try
            {
                return new ByteArrayInputStream( content.getBytes(encoding) );
            }
            catch ( UnsupportedEncodingException e )
            {
                logger.error("Bad encoding for FileSystem");
                throw new ConfigurationException( "Bad encoding specified" );
            }
        }

        @Override
        public OutputStream getOutputStream( URL url ) throws ConfigurationException
        {
            return new ByteArrayOutputStream( 0 );
        }

        @Override
        public OutputStream getOutputStream( File file ) throws ConfigurationException
        {
            return new ByteArrayOutputStream( 0 );
        }

        @Override
        public String getPath( File file, URL url, String basePath, String fileName )
        {
            return basePath+"/"+fileName;
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
                return new URL("file://"+getPath(null, null, basePath, fileName));
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
            try
            {
                return new URL("file://"+getPath(null, null, basePath, fileName));
            }
            catch ( MalformedURLException e )
            {
                // ignore
                return null;
            }
        }

    }

    @PostConstruct
    public void initialize()
        throws RegistryException
    {
        try
        {
            CombinedConfiguration configuration;
            if ( StringUtils.isNotBlank( properties ) )
            {
                System.out.println("Configuration");
                System.out.println(properties);
                    Parameters params = new Parameters();
                    DefaultExpressionEngineSymbols symbols = new DefaultExpressionEngineSymbols.Builder(DefaultExpressionEngineSymbols.DEFAULT_SYMBOLS )
                        .setPropertyDelimiter( propertyDelimiter )
                        .setIndexStart( "(" )
                        .setIndexEnd( ")" )
                        .setEscapedDelimiter( "\\"+propertyDelimiter )
                        . create( );
                    DefaultExpressionEngine expressionEngine = new DefaultExpressionEngine( symbols );
                    ConfigurationInterpolator interpolator = ConfigurationInterpolator.fromSpecification( new InterpolatorSpecification.Builder().withDefaultLookup( DefaultLookups.SYSTEM_PROPERTIES.getLookup() ).create() );
                    System.out.println(interpolator.getDefaultLookups().stream( ).map(p -> p.toString()).collect( Collectors.joining( ",")));
                    String interpolatedProps = interpolator.interpolate( properties ).toString();
                    logger.debug( "Loading configuration into commons-configuration, xml {}", interpolatedProps );
                    FileSystem fs = new StringFileSystem( interpolatedProps );
                    FileBasedConfigurationBuilder<XMLConfiguration> cfgBuilder =
                        new FileBasedConfigurationBuilder<>(
                            XMLConfiguration.class)
                            .configure(params.xml()
                                .setInterpolator( interpolator )
                                .setFileSystem( fs )
                                .setFileName( "config.xml")
                                .setListDelimiterHandler(
                                    new DefaultListDelimiterHandler(','))
                                .setExpressionEngine( expressionEngine )
                                .setThrowExceptionOnMissing(false));

                    CombinedConfigurationBuilder builder = new CombinedConfigurationBuilder().
                        configure(params.combined().setDefinitionBuilder( cfgBuilder ));

                    configuration = builder.getConfiguration();

                    // interpolation as plexus did it before

                    //configuration.set
            }
            else
            {
                logger.debug( "Creating a default configuration - no configuration was provided" );
                NodeCombiner combiner = new UnionCombiner();
                configuration = new CombinedConfiguration(combiner);
            }

            configuration.addConfiguration( new SystemConfiguration(), "SystemProperties" );

            this.configuration = configuration;
        }
        catch ( ConfigurationException e )
        {
            throw new RuntimeException( e.getMessage(), e );
        }
    }

    public void setProperties( String properties )
    {
        this.properties = properties;
    }

    public ConfigRegistry getSection( String name )
    {
        CombinedConfiguration combinedConfiguration = (CombinedConfiguration) configuration;
        Configuration configuration = combinedConfiguration.getConfiguration( name );
        return configuration == null ? null : new CommonsConfigurationRegistry( configuration );
    }

    public String getPropertyDelimiter()
    {
        return propertyDelimiter;
    }

    public void setPropertyDelimiter( String propertyDelimiter )
    {
        this.propertyDelimiter = propertyDelimiter;
    }
}
