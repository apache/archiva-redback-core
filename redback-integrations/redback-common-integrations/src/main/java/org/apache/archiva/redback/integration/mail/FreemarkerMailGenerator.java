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

import freemarker.template.Configuration;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.inject.Inject;
import javax.inject.Named;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Mail generator that uses freemarker templates.
 *
 * This implementation sets the following model values that can be used in templates:
 * <ul>
 *     <li>applicationUrl</li>
 *     <li>urlPath</li>
 *     <li>authKey</li>
 *     <li>accountId</li>
 *     <li>requestedOn</li>
 *     <li>expiresOn</li>
 * </ul>
 *
 * The additional template data is added for interpolation, if not <code>null</code>.
 *
 * This implementation is location enabled. That means, it will try to find templates in the following order:
 * <ul>
 *     <li><i>templateName</i>_<i>language</i>_<i>country</i>.ftl</li>
 *     <li><i>templateName</i>_<i>language</i>.ftl</li>
 *     <li><i>templateName</i>.ftl</li>
 * </ul>
 *
 * The default encoding used for reading the template is UTF-8
 *
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Service( "mailGenerator#freemarker" )
public class FreemarkerMailGenerator implements MailGenerator
{
    private Logger log = LoggerFactory.getLogger( FreemarkerMailGenerator.class );

    public static final String DEFAULT_ENCODING = "UTF-8";

    @Inject
    @Named( value = "userConfiguration#default" )
    private UserConfiguration config;

    @Inject
    Configuration freemarkerConfiguration;

    private String encoding;

    private String getEncoding( )
    {
        if ( this.encoding == null )
        {
            this.encoding = config.getString( UserConfigurationKeys.MAIL_TEMPLATE_ENCODING, DEFAULT_ENCODING );
        }
        return this.encoding;
    }

    private Locale getMailLocale() {
        String localeString = config.getString( UserConfigurationKeys.MAIL_DEFAULT_LOCALE );
        if (localeString == null || "".equals(localeString)) {
            return Locale.getDefault( );
        } else {
            return Locale.forLanguageTag( localeString );
        }
    }

    /**
     *
     * @param templateName the template name without extension
     * @param locale the locale used to find the template file
     * @param authkey the authentication key
     * @param baseUrl the base url
     * @param templateData additional template data, may be <code>null</code>
     * @return the string generated from the template
     */
    @Override
    public String generateMail( String templateName, Locale locale, AuthenticationKey authkey, String baseUrl,
                                Map<String, Object> templateData )
    {
        Map<String, Object> context = createModel( authkey, baseUrl, templateData );
        StringBuffer content = new StringBuffer( );
        try
        {
            content.append( FreeMarkerTemplateUtils.processTemplateIntoString(
                freemarkerConfiguration.getTemplate( templateName + ".ftl", locale, getEncoding( ) ), context ) );
            return content.toString( );
        }
        catch ( Exception e )
        {
            log.error( "Could not parse the mail template {}: {}", templateName, e.getMessage( ), e );
        }
        return "";
    }

    @Override
    public String generateMail( String templateName, AuthenticationKey authenticationKey, String baseUrl )
    {
        return generateMail( templateName, getMailLocale(), authenticationKey, baseUrl );
    }

    @Override
    public String generateMail( String templateName, Locale locale, AuthenticationKey authenticationKey, String baseUrl )
    {
        return generateMail( templateName, locale, authenticationKey, baseUrl, null );
    }

    private Map<String, Object> createModel( AuthenticationKey authkey, String appUrl, Map<String, Object> templateData )
    {
        Map<String, Object> context = new HashMap<>( );
        context.put( "applicationUrl", config.getString( UserConfigurationKeys.APPLICATION_URL, appUrl ) );

        String feedback = config.getString( UserConfigurationKeys.EMAIL_FEEDBACK_PATH );

        if ( feedback != null )
        {
            if ( feedback.startsWith( "/" ) )
            {
                feedback = appUrl + feedback;
            }

            context.put( "feedback", feedback );
        }

        context.put( "urlPath",
            config.getString( UserConfigurationKeys.EMAIL_URL_PATH, "security/login!login.action" ) );

        context.put( "authkey", authkey.getKey( ) );

        context.put( "accountId", authkey.getForPrincipal( ) );

        SimpleDateFormat dateformatter =
            new SimpleDateFormat( config.getString( UserConfigurationKeys.APPLICATION_TIMESTAMP ), Locale.US );

        context.put( "requestedOn", dateformatter.format( authkey.getDateCreated( ) ) );

        if ( authkey.getDateExpires( ) != null )
        {
            context.put( "expiresOn", dateformatter.format( authkey.getDateExpires( ) ) );
        }
        else
        {
            context.put( "expiresOn", "(does not expire)" );
        }

        if (templateData!=null)
        {
            for ( Map.Entry<String, Object> entry : templateData.entrySet( ) )
            {
                context.put( entry.getKey( ), entry.getValue( ) );
            }
        }

        return context;
    }

    public Configuration getFreemarkerConfiguration( )
    {
        return freemarkerConfiguration;
    }

    public void setFreemarkerConfiguration( Configuration freemarkerConfiguration )
    {
        this.freemarkerConfiguration = freemarkerConfiguration;
    }

    public UserConfiguration getConfig( )
    {
        return config;
    }

    public void setConfig( UserConfiguration config )
    {
        this.config = config;
    }
}
