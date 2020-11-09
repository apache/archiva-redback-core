package org.apache.archiva.redback.rest.services.interceptors;
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.eclipse.jetty.util.annotation.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.SimpleDateFormat;

/**
 * to setup some ObjectMapper configuration
 *
 * @author Olivier Lamy
 * @since 2.0
 */
@Service("redbackJacksonJsonConfigurator")
public class JacksonJsonConfigurator
{
    private Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    public JacksonJsonConfigurator( @Named( "redbackJacksonJsonMapper" ) ObjectMapper objectMapper,
                                    @Name( "redbackJacksonXMLMapper" ) XmlMapper xmlMapper, @Named( "v2.redbackJacksonJsonMapper" ) ObjectMapper objectMapperV2 )
    {

        log.info( "configure jackson ObjectMapper" );
        objectMapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        objectMapper.enable( DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL );
        objectMapper.enable( DeserializationFeature.USE_LONG_FOR_INTS );
        objectMapper.setAnnotationIntrospector( new JaxbAnnotationIntrospector( objectMapper.getTypeFactory( ) ) );
        objectMapper.findAndRegisterModules( );
        objectMapper.registerModule( new JavaTimeModule( ) );
        objectMapper.setDateFormat( new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ) );

        objectMapperV2.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        objectMapperV2.enable( DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL );
        objectMapperV2.enable( DeserializationFeature.USE_LONG_FOR_INTS );
        objectMapperV2.setAnnotationIntrospector( new JaxbAnnotationIntrospector( objectMapper.getTypeFactory( ) ) );
        objectMapperV2.findAndRegisterModules( );
        objectMapperV2.registerModule( new JavaTimeModule( ) );
        objectMapperV2.setDateFormat( new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ) );
        objectMapperV2.setPropertyNamingStrategy( PropertyNamingStrategy.SNAKE_CASE );


        xmlMapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        xmlMapper.setAnnotationIntrospector( new JaxbAnnotationIntrospector( xmlMapper.getTypeFactory( ) ) );

    }
}
