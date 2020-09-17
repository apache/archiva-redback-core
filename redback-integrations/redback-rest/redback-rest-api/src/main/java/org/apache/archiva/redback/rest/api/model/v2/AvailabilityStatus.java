package org.apache.archiva.redback.rest.api.model.v2;

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

import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Returns a status of availability (does exist, or does not exist) of a given object.
 * If the object exists, the creation date of the object may be returned.
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@XmlRootElement(name="availabilityStatus")
public class AvailabilityStatus
{
    boolean exists = false;
    OffsetDateTime since;

    public AvailabilityStatus(boolean exists, OffsetDateTime since) {
        this.exists = exists;
        this.since = since;
    }

    public AvailabilityStatus(boolean exists, Instant since) {
        this.exists = exists;
        setSinceByInstant( since );
    }

    public AvailabilityStatus(boolean exists) {
        this.exists = exists;
        this.since = OffsetDateTime.ofInstant( Instant.EPOCH, ZoneId.systemDefault() );
    }

    public boolean isExists( )
    {
        return exists;
    }

    public void setExists( boolean exists )
    {
        this.exists = exists;
    }

    public OffsetDateTime getSince( )
    {
        return since;
    }

    public void setSince( OffsetDateTime since )
    {
        this.since = since;
    }

    public void setSinceByInstant( Instant since ) {
        this.since = OffsetDateTime.ofInstant( since, ZoneId.systemDefault( ) );
    }
}
