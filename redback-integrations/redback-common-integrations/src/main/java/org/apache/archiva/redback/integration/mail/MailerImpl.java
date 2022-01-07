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

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.policy.UserValidationSettings;
import org.apache.archiva.redback.system.SecuritySystem;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Mailer
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
@Service( "mailer" )
public class MailerImpl
    implements Mailer
{
    protected Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named( value = "mailGenerator#default" )
    private MailGenerator generator;

    @Inject
    @Named( value = "mailSender" )
    private JavaMailSender javaMailSender;

    @Inject
    private SecuritySystem securitySystem;

    @Inject
    @Named( value = "userConfiguration#default" )
    private UserConfiguration config;

    public void sendAccountValidationEmail( Collection<String> recipients, AuthenticationKey authkey, String baseUrl )
    {
        String content = generator.generateMail( "newAccountValidationEmail", authkey, baseUrl );

        UserSecurityPolicy policy = securitySystem.getPolicy();
        UserValidationSettings validation = policy.getUserValidationSettings();
        sendMessage( recipients, validation.getEmailSubject(), content );
    }

    public void sendPasswordResetEmail( Collection<String> recipients, AuthenticationKey authkey, String baseUrl )
    {
        String content = generator.generateMail( "passwordResetEmail", authkey, baseUrl );

        UserSecurityPolicy policy = securitySystem.getPolicy();
        UserValidationSettings validation = policy.getUserValidationSettings();
        sendMessage( recipients, validation.getEmailSubject(), content );
    }

    public void sendMessage( Collection<String> recipients, String subject, String content )
    {
        if ( recipients.isEmpty() )
        {
            log.warn( "Mail Not Sent - No mail recipients for email. subject [{}]", subject );
            return;
        }

        String fromAddress = config.getString( UserConfigurationKeys.EMAIL_FROM_ADDRESS );
        String fromName = config.getString( UserConfigurationKeys.EMAIL_FROM_NAME );

        if ( StringUtils.isEmpty( fromAddress ) )
        {
            fromAddress = System.getProperty( "user.name" ) + "@localhost";
        }

        // TODO: Allow for configurable message headers.

        try
        {

            MimeMessage message = javaMailSender.createMimeMessage();

            message.setSubject( subject );
            message.setText( content );

            InternetAddress from = new InternetAddress( fromAddress, fromName );

            message.setFrom( from );

            List<Address> tos = new ArrayList<>( );

            for ( String mailbox : recipients )
            {
                InternetAddress to = new InternetAddress( mailbox.trim() );

                tos.add( to );
            }

            message.setRecipients( Message.RecipientType.TO, tos.toArray( new Address[tos.size()] ) );

            log.debug( "mail content {}", content );

            javaMailSender.send( message );
        }
        catch ( AddressException e )
        {
            log.error( "Unable to send message, subject [{}]", subject, e );
        }
        catch ( MessagingException e )
        {
            log.error( "Unable to send message, subject [{}]", subject, e );
        }
        catch ( UnsupportedEncodingException e )
        {
            log.error( "Unable to send message, subject [{}]", subject, e );
        }
    }
}
