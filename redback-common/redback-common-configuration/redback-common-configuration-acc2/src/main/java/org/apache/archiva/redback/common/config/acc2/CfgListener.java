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

import org.apache.archiva.redback.common.config.api.AsyncListener;
import org.apache.archiva.redback.common.config.api.ConfigRegistry;
import org.apache.archiva.redback.common.config.api.EventType;
import org.apache.archiva.redback.common.config.api.RegistryListener;
import org.apache.commons.configuration2.event.ConfigurationEvent;
import org.apache.commons.configuration2.event.Event;
import org.apache.commons.configuration2.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * This class maps apache commons configuration events into redback configuration events.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */

public class CfgListener implements EventListener
{

    ConfigRegistry registry;

    Map<String, ListenerInfo> listeners = new LinkedHashMap<>( );
    WeakHashMap<EventInfo, Object> oldValueStore = new WeakHashMap<>( );

    Logger logger = LoggerFactory.getLogger( CfgListener.class );

    CfgListener( ConfigRegistry registry )
    {
        this.registry = registry;
    }

    TaskExecutor defaultExecutor;

    ApplicationContext applicationContext;

    private final class ListenerInfo
    {
        final String prefix;
        final RegistryListener listener;
        final boolean async;
        final TaskExecutor executor;

        public ListenerInfo( String prefix, RegistryListener listener )
        {
            this.prefix = prefix;
            this.listener = listener;
            Class<? extends RegistryListener> clazz = listener.getClass( );
            boolean async = clazz.isAnnotationPresent( AsyncListener.class );
            try
            {
                AsyncListener classAnnotation = clazz.getAnnotation( AsyncListener.class );
                AsyncListener methodAnnotation = clazz.getMethod( "handleConfigurationChangeEvent", ConfigRegistry.class, EventType.class, String.class, Object.class, Object.class ).getAnnotation( AsyncListener.class );
                this.async = methodAnnotation != null || classAnnotation != null;
                String executorString = methodAnnotation != null ? methodAnnotation.value( ) : ( classAnnotation != null ? classAnnotation.value( ) : null );
                TaskExecutor newExec;
                if ( executorString == null )
                {
                    newExec = defaultExecutor;
                }
                else
                {
                    newExec = applicationContext.getBean( executorString, TaskExecutor.class );
                    if ( newExec == null )
                    {
                        newExec = defaultExecutor;
                    }
                }
                this.executor = newExec;
            }
            catch ( NoSuchMethodException e )
            {
                throw new RuntimeException( "Fatal error! EventListener methods not found. Maybe you have the wrong version of EventLister in your classpath." );
            }
        }

    }


    private final class EventInfo
    {
        final org.apache.commons.configuration2.event.EventType type;
        final String name;
        final Object value;

        EventInfo( org.apache.commons.configuration2.event.EventType type, String name, Object value )
        {
            this.type = type;
            this.name = name;
            this.value = value;
        }

        @Override
        public int hashCode( )
        {
            return Objects.hash( type, name, value );
        }

        public boolean equals( EventInfo obj )
        {
            return Objects.equals( this.type, obj.type ) && Objects.equals( this.name, obj.name ) && Objects.equals( this.value, obj.value );
        }

    }

    /**
     * This method stores old values in the
     * @param event
     */
    public void onEvent( org.apache.commons.configuration2.event.ConfigurationEvent event )
    {
        logger.debug( "Event " + event.getClass( ).getName( ) + " Source Class: " + event.getSource( ).getClass( ).getName( ) );
        logger.debug( "EventType " + event.getEventType( ) + ", EventProperty: " + event.getPropertyName( ) + ", EventValue: " + event.getPropertyValue( ) );
        if ( event.isBeforeUpdate( ) )
        {
            logger.debug( "Event before update" );
            Object oldValue = registry.getValue( event.getPropertyName( ) );
            oldValueStore.put( new EventInfo( event.getEventType( ), event.getPropertyName( ), event.getPropertyValue( ) ), oldValue );
        }
        else
        {
            logger.debug( "Event after update" );
            final EventType type = transformEventType( event.getEventType( ) );
            final Object oldValue = oldValueStore.remove( new EventInfo( event.getEventType( ), event.getPropertyName( ), event.getPropertyValue( ) ) );
            final String propertyName = event.getPropertyName();
            final Object newValue = event.getPropertyValue();
            listeners.entrySet( ).stream( ).filter( entry -> event.getPropertyName( ).startsWith( entry.getKey( ) ) ).forEach(
                entry ->
                    callListener( entry.getValue(), type, propertyName, newValue, oldValue )

            );
        }

    }

    private void callListener(ListenerInfo li, EventType type, String propertyName, Object newValue, Object oldValue) {
        try
        {
            if ( li.async )
            {
                li.executor.execute( ( ) -> li.listener.handleConfigurationChangeEvent( registry, type, propertyName, newValue, oldValue ) );
            }
            else
            {
                li.listener.handleConfigurationChangeEvent( registry, type, propertyName, newValue, oldValue );
            }
        } catch (Throwable ex) {
            logger.error( "Listener exception occured: "+ex.getMessage(), ex);
            // Exception is catched allow to call the other listeners.
        }
    }

    private EventType transformEventType( org.apache.commons.configuration2.event.EventType<? extends Event> type )
    {

        if ( type.equals( ConfigurationEvent.ADD_PROPERTY ) )
        {
            return EventType.PROPERTY_ADDED;
        }
        else if ( type.equals( ConfigurationEvent.CLEAR_PROPERTY ) )
        {
            return EventType.PROPERTY_CLEARED;
        }
        else if ( type.equals( ConfigurationEvent.SET_PROPERTY ) )
        {
            return EventType.PROPERTY_SET;
        }
        else
        {
            return EventType.UNDEFINED;
        }
    }

    @Override
    public void onEvent( Event event )
    {
        if ( event instanceof ConfigurationEvent )
        {
            onEvent( (ConfigurationEvent) event );
        }
        else
        {
            logger.debug( "Event " + event.getClass( ).getName( ) + " Source Class: " + event.getSource( ).getClass( ).getName( ) );
            logger.debug( "EventType " + event.getEventType( ) );
        }
    }

    public void registerChangeListener( RegistryListener listener, String prefix )
    {
        listeners.put( prefix, new ListenerInfo( prefix, listener ) );
    }

    public boolean unregisterChangeListener( RegistryListener listener )
    {
        boolean found = false;
        Iterator<Map.Entry<String, ListenerInfo>> it = listeners.entrySet( ).iterator( );
        while ( it.hasNext( ) )
        {
            Map.Entry<String, ListenerInfo> e = it.next( );
            if ( e.getValue( ).listener == listener )
            {
                it.remove( );
                found = true;
            }
        }
        return found;
    }

    public TaskExecutor getDefaultExecutor( )
    {
        return defaultExecutor;
    }

    public void setDefaultExecutor( TaskExecutor defaultExecutor )
    {
        this.defaultExecutor = defaultExecutor;
    }


    public ApplicationContext getApplicationContext( )
    {
        return applicationContext;
    }

    public void setApplicationContext( ApplicationContext applicationContext )
    {
        this.applicationContext = applicationContext;
    }
}
