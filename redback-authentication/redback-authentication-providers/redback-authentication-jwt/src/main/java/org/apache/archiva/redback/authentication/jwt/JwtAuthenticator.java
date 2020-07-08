package org.apache.archiva.redback.authentication.jwt;

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

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.apache.archiva.redback.authentication.AbstractAuthenticator;
import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.Authenticator;
import org.apache.archiva.redback.authentication.TokenBasedAuthenticationDataSource;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.archiva.redback.policy.AccountLockedException;
import org.apache.archiva.redback.policy.MustChangePasswordException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Properties;


@Service("authenticator#jwt")
public class JwtAuthenticator extends AbstractAuthenticator implements Authenticator
{
    public static final String ID = "JwtAuthenticator";
    public static final String PROP_ALG = "algorithm";
    public static final String PROP_PRIVATEKEY = "privateKey";
    public static final String PROP_PUBLICKEY = "publicKey";


    @Inject
    @Named( value = "userConfiguration#default" )
    UserConfiguration userConfiguration;

    boolean symmetricAlg = true;
    Key key;
    KeyPair keyPair;
    String sigAlg;
    String keystoreType;
    Path keystoreFilePath;


    @Override
    public String getId( )
    {
        return ID;
    }

    @PostConstruct
    public void init() {
        this.keystoreType = userConfiguration.getString( UserConfigurationKeys.AUTHENTICATION_JWT_KEYSTORETYPE );
        this.sigAlg = userConfiguration.getString( UserConfigurationKeys.AUTHENTICATION_JWT_SIGALG );
        if ( this.sigAlg.startsWith( "HS" ) ) {
            this.symmetricAlg = true;
        } else {
            this.symmetricAlg = false;
        }
        if (this.keystoreType.equals(UserConfigurationKeys.AUTHENTICATION_JWT_KEYSTORETYPE_MEMORY))
        {
            if ( this.symmetricAlg )
            {
                this.key = createNewSecretKey( this.sigAlg );
            } else {
                this.keyPair = createNewKeyPair( this.sigAlg );
                this.keyPair.getPublic();
            }
        }
    }

    private SecretKey createNewSecretKey( String sigAlg) {
        return Keys.secretKeyFor( SignatureAlgorithm.forName( sigAlg ));
    }

    private KeyPair createNewKeyPair(String sigAlg) {
        return Keys.keyPairFor( SignatureAlgorithm.forName( sigAlg ));
    }

    private SecretKey loadKeyFromFile(Path filePath) throws IOException
    {
        if ( Files.exists( filePath )) {
            Properties props = new Properties( );
            try ( InputStream in = Files.newInputStream( filePath )) {
                props.loadFromXML( in );
            }
            String algorithm = props.getProperty( PROP_ALG ).trim( );
            String secretKey = props.getProperty( PROP_PRIVATEKEY ).trim( );
            byte[] keyData = Base64.getDecoder( ).decode( secretKey.getBytes() );
            return new SecretKeySpec(keyData, algorithm);
        } else {
            throw new RuntimeException( "Could not load keyfile from path " );
        }
    }

    private KeyPair loadPairFromFile(Path filePath) throws IOException
    {
        return null;
    }

    @Override
    public boolean supportsDataSource( AuthenticationDataSource source )
    {
        return (source instanceof TokenBasedAuthenticationDataSource);
    }

    @Override
    public AuthenticationResult authenticate( AuthenticationDataSource source ) throws AccountLockedException, AuthenticationException, MustChangePasswordException
    {
        if (source instanceof TokenBasedAuthenticationDataSource ) {
            TokenBasedAuthenticationDataSource tSource = (TokenBasedAuthenticationDataSource) source;
            return null;
        } else {
            throw new AuthenticationException( "The provided authentication source is not suitable for this authenticator" );
        }
    }
}
