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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.apache.archiva.redback.authentication.AbstractAuthenticator;
import org.apache.archiva.redback.authentication.AuthenticationDataSource;
import org.apache.archiva.redback.authentication.AuthenticationException;
import org.apache.archiva.redback.authentication.AuthenticationFailureCause;
import org.apache.archiva.redback.authentication.AuthenticationResult;
import org.apache.archiva.redback.authentication.Authenticator;
import org.apache.archiva.redback.authentication.BearerTokenAuthenticationDataSource;
import org.apache.archiva.redback.authentication.SimpleTokenData;
import org.apache.archiva.redback.authentication.StringToken;
import org.apache.archiva.redback.authentication.Token;
import org.apache.archiva.redback.authentication.TokenBasedAuthenticationDataSource;
import org.apache.archiva.redback.authentication.TokenData;
import org.apache.archiva.redback.configuration.UserConfiguration;
import org.apache.archiva.redback.configuration.UserConfigurationKeys;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.archiva.redback.configuration.UserConfigurationKeys.*;

/**
 * Authenticator for JWT tokens. This authenticator needs a secret key or keypair depending
 * on the used algorithm for signing and verification.
 * The key can be either volatile in memory, which means a new one is created, with each
 * start of the service. Or it can be stored in a file.
 * If this service is running in a cluster, you need a shared filesystem (NFS) for storing
 * the key file otherwise different keys will be used in each instance.
 * <p>
 * You can renew the used key ({@link #renewSigningKey()}). The authenticator keeps a fixed
 * sized list of the last keys used and stores the key identifier in the JWT header.
 * <p>
 * The default algorithm used for the JWT is currently {@link org.apache.archiva.redback.configuration.UserConfigurationKeys#AUTHENTICATION_JWT_SIGALG_ES384}
 *
 * If the <code>plainfile</code> keystore is used, only the most recent key is saved to the file. Not the
 * complete list.
 *
 * The JWT tokens have a lifetime set (14400 seconds - 4 hours).
 *
 * The following configuration keys are used to setup this authenticator:
 * <dl>
 *     <dt>{@value UserConfigurationKeys#AUTHENTICATION_JWT_KEYSTORETYPE}</dt>
 *     <dd>The type of the keystore, either <code>{@value UserConfigurationKeys#AUTHENTICATION_JWT_KEYSTORETYPE_MEMORY}</code>
 *     (key is lost, if the jvm stops) or <code>{@value UserConfigurationKeys#AUTHENTICATION_JWT_KEYSTORETYPE_PLAINFILE}</code></dd>
 *     <dt>{@value UserConfigurationKeys#AUTHENTICATION_JWT_SIGALG}</dt>
 *     <dd>The signature algorithm for the JWT.
 *     <ul>
 *           <li>HS256: HMAC using SHA-256</li>
 *           <li>HS384: HMAC using SHA-384</li>
 *           <li>HS512: HMAC using SHA-512</li>
 *           <li>ES256: ECDSA using P-256 and SHA-256</li>
 *           <li>ES384: ECDSA using P-384 and SHA-384</li>
 *           <li>ES512: ECDSA using P-521 and SHA-512</li>
 *           <li>RS256: RSASSA-PKCS-v1_5 using SHA-256</li>
 *           <li>RS384: RSASSA-PKCS-v1_5 using SHA-384</li>
 *           <li>RS512: RSASSA-PKCS-v1_5 using SHA-512</li>
 *           <li>PS256: RSASSA-PSS using SHA-256 and MGF1 with SHA-256</li>
 *           <li>PS384: RSASSA-PSS using SHA-384 and MGF1 with SHA-384</li>
 *           <li>PS512: RSASSA-PSS using SHA-512 and MGF1 with SHA-512</li>
 *     </ul>
 *     </dd>
 *     <dt>{@value UserConfigurationKeys#AUTHENTICATION_JWT_MAX_KEYS}</dt>
 *     <dd>The maximum number of signature keys to keep in memory for verification</dd>
 *     <dt>{@value UserConfigurationKeys#AUTHENTICATION_JWT_KEYFILE}</dt>
 *     <dd>The key file. Either a full path to the file, or a single filename, which means it is stored in the working directory</dd>
 *     <dt>{@value UserConfigurationKeys#AUTHENTICATION_JWT_LIFETIME_MS}</dt>
 *     <dd>The default token lifetime in milliseconds</dd>
 * </dl>
 */
@Service( "authenticator#jwt" )
public class JwtAuthenticator extends AbstractAuthenticator implements Authenticator
{
    private static final Logger log = LoggerFactory.getLogger( JwtAuthenticator.class );

    public static final String DEFAULT_LIFETIME = "14400000";
    public static final String DEFAULT_KEYFILE = "jwt-key.xml";
    public static final String ID = "JwtAuthenticator";
    public static final String PROP_PRIV_ALG = "privateAlgorithm";
    public static final String PROP_PRIV_FORMAT = "privateFormat";
    public static final String PROP_PUB_ALG = "publicAlgorithm";
    public static final String PROP_PUB_FORMAT = "publicFormat";
    public static final String PROP_PRIVATEKEY = "privateKey";
    public static final String PROP_PUBLICKEY = "publicKey";
    public static final String PROP_KEYID = "keyId";
    private static final String ISSUER = "archiva.apache.org/redback";


    @Inject
    @Named( value = "userConfiguration#default" )
    UserConfiguration userConfiguration;

    boolean symmetricAlgorithm = true;
    boolean fileStore = false;
    LinkedHashMap<Long, SecretKey> secretKey;
    LinkedHashMap<Long, KeyPair> keyPair;
    String signatureAlgorithm;
    String keystoreType;
    Path keystoreFilePath;
    int maxInMemoryKeys = 5;
    AtomicLong keyCounter;
    final SigningKeyResolver resolver = new SigningKeyResolver( );
    final ReadWriteLock lock = new ReentrantReadWriteLock( );
    private JwtParser parser;
    private Duration lifetime;

    public class SigningKeyResolver extends SigningKeyResolverAdapter
    {

        @Override
        public Key resolveSigningKey( JwsHeader jwsHeader, Claims claims )
        {
            Long keyId = Long.valueOf( jwsHeader.get( JwsHeader.KEY_ID ).toString() );
            Key key;
            if (symmetricAlgorithm) {
                key = getSecretKey( keyId );
            } else
            {
                KeyPair pair = getKeyPair( keyId );
                if (pair == null) {
                    throw new JwtKeyIdNotFoundException( "Key ID not found in current list. Verification failed." );
                }
                key = pair.getPublic( );
            }
            if (key==null) {
                throw new JwtKeyIdNotFoundException( "Key ID not found in current list. Verification failed." );
            }
            return key;
        }
    }

    @Override
    public String getId( )
    {
        return ID;
    }

    @PostConstruct
    public void init( ) throws AuthenticationException
    {
        super.initialize();
        this.keyCounter = new AtomicLong( System.currentTimeMillis( ) );
        this.keystoreType = userConfiguration.getString( AUTHENTICATION_JWT_KEYSTORETYPE, AUTHENTICATION_JWT_KEYSTORETYPE_MEMORY );
        this.fileStore = this.keystoreType.equals( AUTHENTICATION_JWT_KEYSTORETYPE_PLAINFILE );
        this.signatureAlgorithm = userConfiguration.getString( AUTHENTICATION_JWT_SIGALG, AUTHENTICATION_JWT_SIGALG_HS384 );
        this.maxInMemoryKeys = userConfiguration.getInt( AUTHENTICATION_JWT_MAX_KEYS, 5 );
        secretKey = new LinkedHashMap<Long, SecretKey>( )
        {
            @Override
            protected boolean removeEldestEntry( Map.Entry eldest )
            {
                return size( ) > maxInMemoryKeys;
            }
        };
        keyPair = new LinkedHashMap<Long, KeyPair>( )
        {
            @Override
            protected boolean removeEldestEntry( Map.Entry eldest )
            {
                return size( ) > maxInMemoryKeys;
            }
        };


        this.symmetricAlgorithm = this.signatureAlgorithm.startsWith( "HS" );

        if ( this.fileStore )
        {
            String file = userConfiguration.getString( AUTHENTICATION_JWT_KEYFILE, DEFAULT_KEYFILE );
            this.keystoreFilePath = Paths.get( file ).toAbsolutePath( );
            handleKeyfile( );
        }
        else
        {
            // In memory key store is the default
            addNewKey( );
        }
        this.parser = Jwts.parserBuilder( )
            .setSigningKeyResolver( getResolver( ) )
            .requireIssuer( ISSUER )
            .build( );

        lifetime = Duration.ofMillis( Long.parseLong( userConfiguration.getString( AUTHENTICATION_JWT_LIFETIME_MS, DEFAULT_LIFETIME ) ) );
    }

    private void addNewSecretKey( Long id, SecretKey key )
    {
        lock.writeLock( ).lock( );
        try
        {
            this.secretKey.put( id, key );
        }
        finally
        {
            lock.writeLock( ).unlock( );
        }
    }

    private void addNewKeyPair( Long id, KeyPair pair )
    {
        lock.writeLock( ).lock( );
        try
        {
            this.keyPair.put( id, pair );
        }
        finally
        {
            lock.writeLock( ).unlock( );
        }
    }

    private Long addNewKey( )
    {
        final Long id = keyCounter.incrementAndGet( );
        if ( this.symmetricAlgorithm )
        {
            addNewSecretKey( id, createNewSecretKey( this.signatureAlgorithm ) );
        }
        else
        {
            addNewKeyPair( id, createNewKeyPair( this.signatureAlgorithm ) );
        }
        return id;
    }

    private SecretKey getSecretKey( Long id )
    {
        lock.readLock( ).lock( );
        try
        {
            return this.secretKey.get( id );
        }
        finally
        {
            lock.readLock( ).unlock( );
        }
    }

    private KeyPair getKeyPair( Long id )
    {
        lock.readLock( ).lock( );
        try
        {
            return this.keyPair.get( id );
        }
        finally
        {
            lock.readLock( ).unlock( );
        }
    }

    private void handleKeyfile( )
    {
        if ( !Files.exists( this.keystoreFilePath ) )
        {
            final Long keyId = addNewKey( );
            if ( this.symmetricAlgorithm )
            {
                try
                {
                    writeSecretKey( this.keystoreFilePath, keyId, getSecretKey( keyId ) );
                }
                catch ( IOException e )
                {
                    log.error( "Could not write Jwt key file {}: {}", this.keystoreFilePath, e.getMessage( ), e );
                    log.warn( "Switching to in memory key handling " );
                    this.fileStore = false;
                }
            }
            else
            {
                try
                {
                    writeKeyPair( this.keystoreFilePath, keyId, getKeyPair( keyId ) );
                }
                catch ( IOException e )
                {
                    log.error( "Could not write Jwt key file {}: {}", this.keystoreFilePath, e.getMessage( ), e );
                    log.warn( "Switching to in memory key handling " );
                    this.fileStore = false;
                }
            }
        }
        else
        {
            if ( this.symmetricAlgorithm )
            {
                try
                {
                    final KeyHolder key = loadKeyFromFile( this.keystoreFilePath );
                    keyCounter.set( key.getId() );
                    addNewSecretKey( key.getId(), key.getSecretKey() );
                }
                catch ( IOException e )
                {
                    log.error( "Could not read Jwt key file {}: {}", this.keystoreFilePath, e.getMessage( ), e );
                    log.warn( "Switching to in memory key handling " );
                    this.fileStore = false;
                    addNewKey( );
                }
            }
            else
            {
                try
                {
                    final KeyHolder pair = loadPairFromFile( this.keystoreFilePath );
                    keyCounter.set( pair.getId() );
                    addNewKeyPair( pair.getId(), pair.getKeyPair() );
                }
                catch ( Exception e )
                {
                    log.error( "Could not read Jwt key file {}: {}", this.keystoreFilePath, e.getMessage( ), e );
                    log.warn( "Switching to in memory key handling " );
                    this.fileStore = false;
                    addNewKey( );
                }
            }
        }
    }

    private SecretKey createNewSecretKey( String sigAlg )
    {
        return Keys.secretKeyFor( SignatureAlgorithm.forName( sigAlg ) );
    }

    private KeyPair createNewKeyPair( String sigAlg )
    {
        return Keys.keyPairFor( SignatureAlgorithm.forName( sigAlg ) );
    }

    private KeyHolder loadKeyFromFile( Path filePath ) throws IOException
    {
        if ( Files.exists( filePath ) )
        {
            log.info( "Loading secret key from file storage {}", filePath );
            Properties props = new Properties( );
            try ( InputStream in = Files.newInputStream( filePath ) )
            {
                props.loadFromXML( in );
            }
            String algorithm = props.getProperty( PROP_PRIV_ALG ).trim( );
            String secretKey = props.getProperty( PROP_PRIVATEKEY ).trim( );
            Long keyId;
            try {
                keyId = Long.valueOf( props.getProperty( PROP_KEYID ) );
            } catch (NumberFormatException e) {
                keyId = keyCounter.incrementAndGet( );
            }
            byte[] keyData = Base64.getDecoder( ).decode( secretKey.getBytes( ) );
            return new KeyHolder( keyId, new SecretKeySpec( keyData, algorithm ) );
        }
        else
        {
            throw new FileNotFoundException( "Keyfile does not exist " + filePath );
        }
    }


    private KeyHolder loadPairFromFile( Path filePath ) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException
    {
        if ( Files.exists( filePath ) )
        {
            log.info( "Loading key pair from file storage {}", filePath );
            Properties props = new Properties( );
            try ( InputStream in = Files.newInputStream( filePath ) )
            {
                props.loadFromXML( in );
            }
            String algorithm = props.getProperty( PROP_PRIV_ALG ).trim( );
            String secretKeyBase64 = props.getProperty( PROP_PRIVATEKEY ).trim( );
            String publicKeyBase64 = props.getProperty( PROP_PUBLICKEY ).trim( );
            Long keyId;
            try {
                keyId = Long.valueOf( props.getProperty( PROP_KEYID ) );
            } catch (NumberFormatException e) {
                keyId = keyCounter.incrementAndGet( );
            }
            byte[] privateBytes = Base64.getDecoder( ).decode( secretKeyBase64 );
            byte[] publicBytes = Base64.getDecoder( ).decode( publicKeyBase64 );

            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec( privateBytes );
            X509EncodedKeySpec publicSpec = new X509EncodedKeySpec( publicBytes );
            PrivateKey privateKey = KeyFactory.getInstance( algorithm ).generatePrivate( privateSpec );
            PublicKey publicKey = KeyFactory.getInstance( algorithm ).generatePublic( publicSpec );

            return new KeyHolder( keyId, new KeyPair( publicKey, privateKey ) );
        }
        else
        {
            throw new FileNotFoundException( "Keyfile does not exist " + filePath );
        }
    }

    private void writeSecretKey( Path filePath, Long id, Key key ) throws IOException
    {
        log.info( "Writing secret key algorithm=" + key.getAlgorithm( ) + ", format=" + key.getFormat( ) + " to file " + filePath );
        Properties props = new Properties( );
        props.setProperty( PROP_PRIV_ALG, key.getAlgorithm( ) );
        if ( key.getFormat( ) != null )
        {
            props.setProperty( PROP_PRIV_FORMAT, key.getFormat( ) );
        }
        props.setProperty( PROP_KEYID, id.toString() );
        props.setProperty( PROP_PRIVATEKEY, Base64.getEncoder( ).encodeToString( key.getEncoded( ) ) );
        try ( OutputStream out = Files.newOutputStream( filePath ) )
        {
            props.storeToXML( out, "Key for JWT signing" );
        }
        try
        {
            Files.setPosixFilePermissions( filePath, PosixFilePermissions.fromString( "rw-------" ) );
        }
        catch ( Exception e )
        {
            log.error( "Could not set file permissions for {}: {}", filePath, e.getMessage( ), e );
        }
    }

    private void writeKeyPair( Path filePath, Long id, KeyPair keyPair ) throws IOException
    {
        PrivateKey privateKey = keyPair.getPrivate( );
        PublicKey publicKey = keyPair.getPublic( );

        log.info( "Writing private key algorithm=" + privateKey.getAlgorithm( ) + ", format=" + privateKey.getFormat( ) + " to file " + filePath );
        log.info( "Writing public key algorithm=" + publicKey.getAlgorithm( ) + ", format=" + publicKey.getFormat( ) + " to file " + filePath );
        Properties props = new Properties( );
        props.setProperty( PROP_PRIV_ALG, privateKey.getAlgorithm( ) );
        if ( privateKey.getFormat( ) != null )
        {
            props.setProperty( PROP_PRIV_FORMAT, privateKey.getFormat( ) );
        }
        props.setProperty( PROP_KEYID, id.toString( ) );
        props.setProperty( PROP_PUB_ALG, publicKey.getAlgorithm( ) );
        if ( publicKey.getFormat( ) != null )
        {
            props.setProperty( PROP_PUB_FORMAT, publicKey.getFormat( ) );
        }
        PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec( privateKey.getEncoded( ) );
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec( publicKey.getEncoded( ) );
        props.setProperty( PROP_PRIVATEKEY, Base64.getEncoder( ).encodeToString( privateSpec.getEncoded( ) ) );
        props.setProperty( PROP_PUBLICKEY, Base64.getEncoder( ).encodeToString( publicSpec.getEncoded( ) ) );

        try ( OutputStream out = Files.newOutputStream( filePath ) )
        {
            props.storeToXML( out, "Key pair for JWT signing" );
        }
        try
        {
            Files.setPosixFilePermissions( filePath, PosixFilePermissions.fromString( "rw-------" ) );
        }
        catch ( Exception e )
        {
            log.error( "Could not set file permissions for {}: {}", filePath, e.getMessage( ), e );
        }

    }

    /**
     * Returns <code>true</code>, if the source is a instance of {@link TokenBasedAuthenticationDataSource}
     * @param source the source to check
     * @return <code>true</code>, if the given source is a instance of {@link TokenBasedAuthenticationDataSource}
     */
    @Override
    public boolean supportsDataSource( AuthenticationDataSource source )
    {
        return ( source instanceof BearerTokenAuthenticationDataSource );
    }

    /**
     * Tries to verify the represented token and returns the result
     * @param source the authentication source, which must be a {@link TokenBasedAuthenticationDataSource}
     * @return the authentication result
     * @throws AuthenticationException if the source is no {@link TokenBasedAuthenticationDataSource}
     */
    public AuthenticationResult authenticate( BearerTokenAuthenticationDataSource source ) throws AuthenticationException
    {
            String jwt = source.getTokenData( );
            AuthenticationResult result;
            try
            {
                String subject = verify( jwt );
                result = new AuthenticationResult( true, subject, null );
            } catch ( TokenAuthenticationException e) {
                AuthenticationFailureCause cause = new AuthenticationFailureCause(e.getError().getId(), e.getMessage() );
                result = new AuthenticationResult( false, source.getUsername(), e, Arrays.asList( cause ) );
            }
            return result;
    }

    @Override
    public AuthenticationResult authenticate(AuthenticationDataSource dataSource) throws AuthenticationException
    {
        if (dataSource instanceof BearerTokenAuthenticationDataSource) {
            return this.authenticate( (BearerTokenAuthenticationDataSource) dataSource );
        }
        throw new AuthenticationException( "Authentication datasource not supported by this JwtAuthenticator" );
    }

    /**
     * Creates a new signing key and uses this for new tokens. It will keep {@link #maxInMemoryKeys} keys in the
     * list for jwt verification.
     */
    public Long renewSigningKey( )
    {
        final Long id = addNewKey( );
        if (this.fileStore)
        {
            if ( this.symmetricAlgorithm )
            {
                try
                {
                    writeSecretKey( this.keystoreFilePath, id, getSecretKey( id ) );
                }
                catch ( IOException e )
                {
                    log.error( "Could not write to keyfile {}: {}", this.keystoreFilePath, e.getMessage( ), e );
                }
            }
            else
            {
                try
                {
                    writeKeyPair( this.keystoreFilePath, id, getKeyPair( id ) );
                }
                catch ( IOException e )
                {
                    log.error( "Could not write to keyfile {}: {}", this.keystoreFilePath, e.getMessage( ), e );
                }
            }
        }
        return id;
    }

    /**
     * Simple internal DTO for keeping key data and its key id.
     */
    private static class KeyHolder {
        final Long id;
        final SecretKey secretKey;
        final KeyPair keyPair;

        KeyHolder(Long id, SecretKey key) {
            this.id = id;
            this.secretKey = key;
            this.keyPair = null;
        }
        KeyHolder(Long id, KeyPair key) {
            this.id = id;
            this.secretKey = null;
            this.keyPair = key;
        }

        public Long getId( )
        {
            return id;
        }

        public SecretKey getSecretKey( )
        {
            return secretKey;
        }

        public KeyPair getKeyPair( )
        {
            return keyPair;
        }

        public Key getSignerKey() {
            return keyPair != null ? this.keyPair.getPrivate( ) : this.secretKey;
        }
    }

    private KeyHolder getSignerKey() {
        final Long id = keyCounter.get( );
        if (this.symmetricAlgorithm) {
            return new KeyHolder( id, getSecretKey( id ) );
        } else {
            return new KeyHolder( id, getKeyPair( id ) );
        }
    }

    /**
     * Creates a token for the given user id. The token contains the following data:
     * <ul>
     *     <li>the userid as subject</li>
     *     <li>a issuer archiva.apache.org/redback</li>
     *     <li>a id header with the key id</li>
     * </ul>the user id as subject.
     *
     * @param userId the user identifier to set as subject
     * @return the token string
     */
    public Token generateToken( String userId )
    {
        final KeyHolder signerKey = getSignerKey( );
        Instant now = Instant.now( );
        Instant expiration = now.plus( lifetime );
        final String token = Jwts.builder( )
            .setSubject( userId )
            .setIssuer( ISSUER )
            .setIssuedAt( Date.from( now ) )
            .setExpiration( Date.from( expiration ) )
            .setHeaderParam( JwsHeader.KEY_ID, signerKey.getId( ).toString( ) )
            .signWith( signerKey.getSignerKey( ) ).compact( );
        TokenData metadata = new SimpleTokenData( userId, lifetime.toMillis( ), 0 );
        return new StringToken( token, metadata );
    }

    /**
     * Allows to renew a token based on the origin token. If the presented <code>origin</code>
     * is valid, a new token with refreshed expiration time will be returned.
     *
     * @param origin the origin token
     * @return the newly created token
     * @throws AuthenticationException if the given origin token is not valid
     */
    public Token renewToken(String origin) throws AuthenticationException {
        try
        {
            Jws<Claims> signature = this.parser.parseClaimsJws( origin );
            return generateToken( signature.getBody( ).getSubject( ) );
        } catch (JwtException e) {
            throw new AuthenticationException( "Could not renew the token " + e.getMessage( ) );
        }
    }

    /**
     * Parses the given token and returns the JWS metadata stored in the token.
     *
     * @param token the token string
     * @return the parsed data
     * @throws JwtException if the token data is not valid anymore
     */
    public Jws<Claims> parseToken( String token) throws JwtException {
        return parser.parseClaimsJws( token );
    }

    /**
     * Verifies the given JWT Token and returns the stored subject, if successful
     * If the verification failed a AuthenticationException is thrown.
     * @param token the JWT representation
     * @return the subject of the JWT
     * @throws AuthenticationException if the verification failed
     */
    public String verify( String token ) throws TokenAuthenticationException
    {
        try
        {
            Jws<Claims> signature = this.parser.parseClaimsJws( token );
            String subject = signature.getBody( ).getSubject( );
            if ( StringUtils.isEmpty( subject ) )
            {
                throw new TokenAuthenticationException( BearerError.INVALID_TOKEN, "contains no subject" );
            }
            return subject;
        }
        catch ( ExpiredJwtException e )
        {
            throw new TokenAuthenticationException( BearerError.INVALID_TOKEN, "token expired" );
        }
        catch ( SignatureException e ) {
            throw new TokenAuthenticationException( BearerError.INVALID_TOKEN, "token signature does not match" );
        }
        catch ( UnsupportedJwtException e) {
            throw new TokenAuthenticationException( BearerError.INVALID_TOKEN, "jwt is unsupported" );
        }
        catch ( MalformedJwtException e)
        {
            throw new TokenAuthenticationException( BearerError.INVALID_TOKEN, "malformed token content" );
        }
        catch (JwtKeyIdNotFoundException e) {
            throw new TokenAuthenticationException( BearerError.INVALID_TOKEN, "signer key does not exist" );
        }
        catch ( JwtException e) {
            log.debug( "Unknown JwtException {}, {}", e.getClass( ), e.getMessage( ) );
            throw new TokenAuthenticationException( BearerError.INVALID_TOKEN, "unknown error " + e.getMessage( ) );
        }

    }

    /**
     * Removes all signing keys and creates a new one. If you call this method, all JWT tokens generated before,
     * will be invalid.
     */
    public void revokeSigningKeys() {
        lock.writeLock( ).lock( );
        try {
            this.secretKey.clear();
            this.keyPair.clear();
            renewSigningKey( );
        } finally
        {
            lock.writeLock( ).unlock( );
        }
    }

    private SigningKeyResolver getResolver( )
    {
        return this.resolver;
    }

    /**
     * Returns <code>true</code>, if the signature algorithm ist a symmetric one, otherwise <code>false</code>
     * @return <code>true</code>, if symmetric algorithm, otherwise <code>false</code>
     */
    public boolean usesSymmetricAlgorithm( )
    {
        return symmetricAlgorithm;
    }

    /**
     * Returns the signature algorithm used for signing JWT tokens
     * @return the string representation of the signature algorithm
     */
    public String getSignatureAlgorithm( )
    {
        return signatureAlgorithm;
    }

    /**
     * Returns the keystore type that is setup for the authenticator
     * @return either <code>memory</code> or <code>plainfile</code>
     */
    public String getKeystoreType( )
    {
        return keystoreType;
    }

    /**
     * Returns the path to the keystore file or <code>null</code>, if the keystore type is <code>memory</code>
     * @return the path to the keystore file, or <code>null</code>
     */
    public Path getKeystoreFilePath( )
    {
        return keystoreFilePath;
    }

    /**
     * Returns the maximum number of signature keys to store in memory for verification
     * @return the maximum number of signature keys to keep in memory
     */
    public int getMaxInMemoryKeys( )
    {
        return maxInMemoryKeys;
    }

    /**
     * Returns the current size of the in memory key list
     * @return the number of memory stored signature keys
     */
    public int getCurrentKeyListSize() {
        if (symmetricAlgorithm) {
            return secretKey.size( );
        } else {
            return keyPair.size( );
        }
    }

    /**
     * Returns the current used key identifier.
     * @return the key identifier
     */
    public Long getCurrentKeyId() {
        return keyCounter.get( );
    }

    /**
     * Returns the default token lifetime of generated tokens.
     * @return the lifetime as duration
     */
    public Duration getTokenLifetime() {
        return this.lifetime;
    }

    /**
     * Sets the default token lifetime of generated tokens.
     * @param lifetime the lifetime as duration
     */
    public void setTokenLifetime(Duration lifetime) {
        this.lifetime = lifetime;
    }

    public UserConfiguration getUserConfiguration( )
    {
        return userConfiguration;
    }

    public void setUserConfiguration( UserConfiguration userConfiguration )
    {
        this.userConfiguration = userConfiguration;
    }
}
