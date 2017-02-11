package org.apache.archiva.redback.authentication;

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

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;


/**
 *
 * Class that manages tokens that are encrypted with a dynamic key. The tokens
 * are converted into BASE64 strings.
 *
 * Each token contains information about username,
 *
 * Created by Martin Stockhammer on 03.02.17.
 */
@Service("tokenManager#jce")
public class TokenManager {

    private static final SecureRandom rd = new SecureRandom();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private String algorithm = "AES/ECB/PKCS5Padding";
    private int keySize = -1;
    private Cipher deCipher;
    private Cipher enCipher;

    boolean paddingUsed = true;


    @PostConstruct
    public void initialize() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, EncryptionFailedException, InvalidAlgorithmParameterException {
        log.debug("Initializing key for token generator");
        try {
            enCipher = Cipher.getInstance(algorithm);
            deCipher = Cipher.getInstance(algorithm);
            String[] keyAlg = enCipher.getAlgorithm().split("/");
            if (keyAlg.length<1) {
                throw new EncryptionFailedException("Initialization of key failed. Not algorithm found.");
            }
            String encryptionAlgorithm = keyAlg[0];
            KeyGenerator keyGen = KeyGenerator.getInstance(encryptionAlgorithm);
            if (keySize>0) {
                keyGen.init(keySize);
            }
            if (keyAlg.length==3 && keyAlg[2].equals("NoPadding")) {
                paddingUsed=false;
            }
            SecretKey secretKey = keyGen.generateKey();
            enCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            // We have to provide the IV depending on the algorithm used
            // CBC needs an IV, ECB not.
            if (enCipher.getIV()==null) {
                deCipher.init(Cipher.DECRYPT_MODE, secretKey);
            } else {
                deCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(enCipher.getIV()));
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("Error occurred during key initialization. Requested algorithm not available. "+e.getMessage());
            throw e;
        } catch (NoSuchPaddingException e) {
            log.error("Error occurred during key initialization. Requested padding not available. "+e.getMessage());
            throw e;
        } catch (InvalidKeyException e) {
            log.error("The key is not valid.");
            throw e;
        } catch (InvalidAlgorithmParameterException e) {
            log.error("Invalid encryption parameters.");
            throw e;
        }
    }


    @SuppressWarnings("SameParameterValue")
    public String encryptToken(String user, long lifetime) throws EncryptionFailedException {
        return encryptToken(new SimpleTokenData(user, lifetime, createNonce()));
    }

    public String encryptToken(TokenData tokenData) throws EncryptionFailedException {
        try {
            return encode(encrypt(tokenData));
        } catch (IOException e) {
            log.error("Error during object conversion: "+e.getMessage());
            throw new EncryptionFailedException(e);
        } catch (BadPaddingException e) {
            log.error("Padding invalid");
            throw new EncryptionFailedException(e);
        } catch (IllegalBlockSizeException e) {
            log.error("Block size invalid");
            throw new EncryptionFailedException(e);
        }
    }

    public TokenData decryptToken(String token) throws InvalidTokenException {
        try {
            return decrypt(decode(token));
        } catch (IOException ex) {
            log.error("Error during data read. " + ex.getMessage());
            throw new InvalidTokenException(ex);
        } catch (ClassNotFoundException ex) {
            log.error("Token data invalid.");
            throw new InvalidTokenException(ex);
        } catch (BadPaddingException ex) {
            log.error("The encrypted token has the wrong padding.");
            throw new InvalidTokenException(ex);
        } catch (IllegalBlockSizeException ex) {
            log.error("The encrypted token has the wrong block size.");
            throw new InvalidTokenException(ex);
        }
    }

    private long createNonce() {
        return rd.nextLong();
    }

    protected byte[] encrypt(TokenData info) throws IOException, BadPaddingException, IllegalBlockSizeException {
        return doEncrypt(convertToByteArray(info));
    }

    protected byte[] doEncrypt(byte[] data) throws BadPaddingException, IllegalBlockSizeException {
        byte[] encData;
        if (!paddingUsed && (data.length % enCipher.getBlockSize())!=0) {
            int blocks = data.length / enCipher.getBlockSize();
            encData = Arrays.copyOf(data, enCipher.getBlockSize()*(blocks+1));
        } else {
            encData = data;
        }
        return enCipher.doFinal(encData);
    }

    protected TokenData decrypt(byte[] token) throws BadPaddingException, IllegalBlockSizeException, IOException, ClassNotFoundException {
        Object result = convertFromByteArray(doDecrypt(token));
        if (!(result instanceof TokenData)) {
            throw new InvalidClassException("No TokenData found in decrypted token");
        }
        return (TokenData)result;
    }

    protected byte[] doDecrypt(byte[] encryptedData) throws BadPaddingException, IllegalBlockSizeException {
        return deCipher.doFinal(encryptedData);
    }

    private String encode(byte[] token) {
        return Base64.encodeBase64String(token);
    }

    private byte[] decode(String token) {
        return Base64.decodeBase64(token);
    }


    private Object convertFromByteArray(byte[] byteObject) throws IOException,
            ClassNotFoundException {
        ByteArrayInputStream bais;
        ObjectInputStream in;
        bais = new ByteArrayInputStream(byteObject);
        in = new ObjectInputStream(bais);
        Object o = in.readObject();
        in.close();
        return o;

    }


    private byte[] convertToByteArray(Object complexObject) throws IOException {
        ByteArrayOutputStream baos;
        ObjectOutputStream out;
        baos = new ByteArrayOutputStream();
        out = new ObjectOutputStream(baos);
        out.writeObject(complexObject);
        out.close();
        return baos.toByteArray();
    }

    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Sets the encryption algorithm and resets the key size. You may change the key size after
     * calling this method.
     * Additionally run the initialize() method after setting algorithm and keysize.
     *
     *
     * @param algorithm The encryption algorithm to use.
     */
    public void setAlgorithm(String algorithm) {
        if (!this.algorithm.equals(algorithm)) {
            this.algorithm = algorithm;
            this.keySize=-1;
        }
    }

    public int getKeySize() {
        return keySize;
    }

    /**
     * Sets the key size for the encryption. This method must be called after
     * setting the algorithm. The keysize will be reset by calling <code>setAlgorithm()</code>
     *
     * The key size must be valid for the given algorithm.
     *
     * @param keySize The size of the encryption key
     */
    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }
}