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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Properties;


@Service("authenticator#jwt")
public class JwtAuthenticator extends AbstractAuthenticator implements Authenticator
{
    private static final Logger log = LoggerFactory.getLogger( JwtAuthenticator.class );

    public static final String ID = "JwtAuthenticator";
    public static final String PROP_PRIV_ALG = "privateAlgorithm";
    public static final String PROP_PRIV_FORMAT = "privateFormat";
    public static final String PROP_PUB_ALG = "publicAlgorithm";
    public static final String PROP_PUB_FORMAT = "publicFormat";
    public static final String PROP_PRIVATEKEY = "privateKey";
    public static final String PROP_PUBLICKEY = "publicKey";


    @Inject
    @Named( value = "userConfiguration#default" )
    UserConfiguration userConfiguration;

    boolean symmetricAlg = true;
    Key key;
    Key publicKey;
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
                KeyPair pair = createNewKeyPair( this.sigAlg );
                this.key = pair.getPrivate( );
                this.publicKey = pair.getPublic( );
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
            String algorithm = props.getProperty( PROP_PRIV_ALG ).trim( );
            String secretKey = props.getProperty( PROP_PRIVATEKEY ).trim( );
            byte[] keyData = Base64.getDecoder( ).decode( secretKey.getBytes() );
            return new SecretKeySpec(keyData, algorithm);
        } else {
            throw new RuntimeException( "Could not load keyfile from path " );
        }
    }


    private KeyPair loadPairFromFile(Path filePath) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
    {
        if (Files.exists( filePath )) {
            Properties props = new Properties( );
            try ( InputStream in = Files.newInputStream( filePath )) {
                props.loadFromXML( in );
            }
            String algorithm = props.getProperty( PROP_PRIV_ALG ).trim( );
            String secretKeyBase64 = props.getProperty( PROP_PRIVATEKEY ).trim( );
            String publicKeyBase64 = props.getProperty( PROP_PUBLICKEY ).trim( );
            byte[] privateBytes = Base64.getDecoder( ).decode( secretKeyBase64 );
            byte[] publicBytes = Base64.getDecoder( ).decode( publicKeyBase64 );

            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec( privateBytes );
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec( publicBytes );
            PrivateKey privateKey = KeyFactory.getInstance( algorithm ).generatePrivate( privateSpec );
            PublicKey publicKey = KeyFactory.getInstance( algorithm ).generatePublic( publicSpec );

            return new KeyPair( publicKey, privateKey );
        } else {
            throw new RuntimeException( "Could not load key file from " + filePath );
        }
    }

    private void writeSecretKey(Path filePath, SecretKey key) throws IOException
    {
        log.info( "Writing secret key algorithm=" + key.getAlgorithm( ) + ", format=" + key.getFormat( ) + " to file " + filePath );
        Properties props = new Properties( );
        props.setProperty( PROP_PRIV_ALG, key.getAlgorithm( ) );
        if ( key.getFormat( ) != null )
        {
            props.setProperty( PROP_PRIV_FORMAT, key.getFormat( ) );
        }
        props.setProperty( PROP_PRIVATEKEY, String.valueOf( Base64.getEncoder( ).encode( key.getEncoded( ) ) ) );
        try ( OutputStream out = Files.newOutputStream( filePath ) )
        {
            props.storeToXML( out, "Key for JWT signing" );
        }
        try
        {
            Files.setPosixFilePermissions( filePath, PosixFilePermissions.fromString( "600" ) );
        } catch (Exception e) {
            log.error( "Could not set file permissions for " + filePath );
        }
    }

    private void writeKeyPair(Path filePath, PrivateKey privateKey, PublicKey publicKey) {
        log.info( "Writing private key algorithm=" + privateKey.getAlgorithm( ) + ", format=" + privateKey.getFormat( ) + " to file " + filePath );
        log.info( "Writing public key algorithm=" + publicKey.getAlgorithm( ) + ", format=" + publicKey.getFormat( ) + " to file " + filePath );
        Properties props = new Properties( );
        props.setProperty( PROP_PRIV_ALG, privateKey.getAlgorithm( ) );
        if (privateKey.getFormat()!=null) {
            props.setProperty( PROP_PRIV_FORMAT, privateKey.getFormat( ) );
        }
        props.setProperty( PROP_PUB_ALG, publicKey.getAlgorithm( ) );
        if (publicKey.getFormat()!=null) {
            props.setProperty( PROP_PUB_FORMAT, publicKey.getFormat( ) );
        }
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec( privateKey.getEncoded( ) );
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec( publicKey.getEncoded( ) );
        props.setProperty( PROP_PRIVATEKEY, Base64.getEncoder( ).encodeToString( privateSpec.getEncoded( ) ) );
        props.setProperty( PROP_PUBLICKEY, Base64.getEncoder( ).encodeToString( publicSpec.getEncoded( ) ) );


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
