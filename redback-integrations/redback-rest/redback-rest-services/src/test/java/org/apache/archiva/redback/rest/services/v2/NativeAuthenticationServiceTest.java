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

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.core.annotation.MergedAnnotations.from;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@ExtendWith( SpringExtension.class )
@ContextConfiguration(
    locations = {"classpath:/ldap-spring-test.xml"} )
@TestInstance( TestInstance.Lifecycle.PER_CLASS )
@Tag( "rest-native" )
public class NativeAuthenticationServiceTest extends AbstractNativeRestServices
{

    @Override
    protected String getServicePath( )
    {
        return "/auth";
    }

    @BeforeAll
    void setup( ) throws Exception
    {
        setupNative( );
    }

    @AfterAll
    void shutdown( ) throws Exception
    {
        shutdownNative();
    }

    @Test
    void ping( )
    {
        Instant beforeCall = Instant.now( );
        Response response = given( ).spec( getRequestSpec() )
            .when( ).get( "/ping" )
            .then( ).assertThat( ).statusCode( 200 ).and( )
            .contentType( JSON ).
                body( "success", equalTo( true ) )
            .body( "requestTime", notNullValue( ) ).extract().response();
        OffsetDateTime dateTime = OffsetDateTime.parse( response.body( ).jsonPath( ).getString( "requestTime" ) );
        Instant afterCall = Instant.now( );
        assertTrue( dateTime.toInstant( ).isAfter( beforeCall ) );
        assertTrue( dateTime.toInstant( ).isBefore( afterCall ) );
    }

    @Test
    void tokenLogin() {
        Map<String, Object> jsonAsMap = new HashMap<>();
        jsonAsMap.put( "grant_type", "authorization_code" );
        jsonAsMap.put("user_id", getAdminUser());
        jsonAsMap.put("password", getAdminPwd() );
        Response result = given( ).spec( getRequestSpec( ) )
            .contentType( JSON )
            .body( jsonAsMap )
            .when( ).post( "/authenticate").then( ).statusCode( 200 )
            .extract( ).response( );
        assertNotNull( result.body( ).jsonPath( ).getString( "access_token" ) );
        assertNotNull( result.body( ).jsonPath( ).getString( "refresh_token" ) );
    }

    @Test
    void invalidGrantTypeLogin() {
        Map<String, Object> jsonAsMap = new HashMap<>();
        jsonAsMap.put( "grant_type", "bad_code" );
        jsonAsMap.put("user_id", getAdminUser());
        jsonAsMap.put("password", getAdminPwd() );
        Response result = given( ).spec( getRequestSpec( ) )
            .contentType( JSON )
            .body( jsonAsMap )
            .when( ).post( "/authenticate").then( ).statusCode( 403 )
            .extract( ).response( );
    }

    @Test
    void invalidPasswordLogin() {
        Map<String, Object> jsonAsMap = new HashMap<>();
        jsonAsMap.put( "grant_type", "authorization_code" );
        jsonAsMap.put("user_id", getAdminUser());
        jsonAsMap.put("password", "xxxx" );
        Response result = given( ).spec( getRequestSpec( ) )
            .contentType( JSON )
            .body( jsonAsMap )
            .when( ).post( "/authenticate").then( ).statusCode( 401 )
            .extract( ).response( );
    }

}
