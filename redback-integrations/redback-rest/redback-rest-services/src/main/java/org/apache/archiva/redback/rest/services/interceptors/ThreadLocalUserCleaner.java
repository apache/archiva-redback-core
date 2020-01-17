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


import org.apache.archiva.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * @author Olivier Lamy
 * @since 1.4
 */
@Service( "threadLocalUserCleaner#rest" )
@Provider
@PreMatching
public class ThreadLocalUserCleaner
    implements ContainerResponseFilter
{
    private final Logger log = LoggerFactory.getLogger( getClass() );


    public ThreadLocalUserCleaner()
    {
    }

    private void cleanup()
    {
        RedbackAuthenticationThreadLocal.set( null );
    }

    @Override
    public void filter( ContainerRequestContext requestContext, ContainerResponseContext responseContext ) throws IOException
    {
        log.debug( "ThreadLocalUserCleaner cleanup" );
        cleanup();
    }
}
