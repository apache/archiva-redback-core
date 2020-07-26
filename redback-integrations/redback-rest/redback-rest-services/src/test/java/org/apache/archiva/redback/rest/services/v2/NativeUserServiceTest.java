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

import io.restassured.response.Response;
import org.apache.archiva.redback.rest.api.model.Group;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
public class NativeUserServiceTest extends AbstractNativeRestServices
{
    @Override
    protected String getServicePath( )
    {
        return "/users";
    }

    @BeforeAll
    void setup() throws Exception
    {
        super.setupNative();
    }

    @AfterAll
    void destroy() throws Exception
    {
        super.shutdownNative();
    }

//    @Test
//    void getGroups() {
//        String token = getAdminToken( );
//        Response response = given( ).spec( getRequestSpec( token ) ).contentType( JSON ).when( )
//            .get( ).then( ).statusCode( 200 ).extract( ).response( );
//        assertNotNull( response );
//        List<Group> data = response.body( ).jsonPath( ).getList(  "data", Group.class );
//        assertNotNull( data );
//        assertEquals( Integer.valueOf( 0 ), response.body( ).jsonPath( ).get( "pagination.offset" ) );
//        assertEquals( Integer.valueOf( 1000 ), response.body( ).jsonPath( ).get( "pagination.limit" ) );
//        assertEquals( Integer.valueOf( 6 ), response.body( ).jsonPath( ).get( "pagination.totalCount" ) );
//        assertEquals( 6, data.size( ) );
//        String[] values = data.stream( ).map( ldapInfo -> ldapInfo.getName( ) ).sorted( ).collect( Collectors.toList( ) ).toArray( new String[0] );
//        assertArrayEquals( getTestGroupList( ).toArray( new String[0] ), values );
//        assertEquals( "uid=admin," + this.peopleSuffix, data.get( 0 ).getMemberList( ).get( 0 ) );
//    }

}
