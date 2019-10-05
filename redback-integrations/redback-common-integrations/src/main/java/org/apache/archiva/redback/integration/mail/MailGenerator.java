package org.apache.archiva.redback.integration.mail;

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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.redback.keys.AuthenticationKey;

import java.util.Locale;
import java.util.Map;

/**
 * Mail generator component.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 *
 */
public interface MailGenerator
{


    /**
     * Generates a mail string from a template. How the template will be located depends on the underlying
     * implementation.
     * It uses a default locale.
     *
     * @param templateName the template name without extension
     * @param authkey the authentication key of the current user
     * @param baseUrl  the base url
     * @return A string for the mail body generated from the template
     */
    String generateMail( String templateName, AuthenticationKey authkey, String baseUrl );

    /**
     * Generates a mail string from a template. The given locale is used for retrieving the template.
     * How the template will be located depends on the underlying implementation.
     *
     * @param templateName the template name without extension
     * @param locale the locale used to find the template file
     * @param authenticationKey the authentication key of the current user
     * @param baseUrl the base url
     * @return a string for the mail body generated from the template
     */
    String generateMail( String templateName, Locale locale, AuthenticationKey authenticationKey, String baseUrl );

    /**
     * Generates a mail string from a template. The given locale is used for retrieving the template.
     * How the template will be located depends on the underlying implementation.
     * The templateData is used as model data that is interpolated from the template.
     *
     * @param templateName the template name without extension
     * @param locale the locale used to find the template file
     * @param authenticationKey the authentication key of the current user
     * @param baseUrl the base url
     * @param templateData additional data used for interpolation in the template
     * @return a string for the mail body generated from the template
     */
    String generateMail( String templateName, Locale locale, AuthenticationKey authenticationKey, String baseUrl,
                         Map<String, Object> templateData );

}
