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
import org.apache.commons.lang.ArrayUtils;
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

    private final ThreadLocal<SecureRandom> rd = new ThreadLocal<SecureRandom>();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private String algorithm = "AES/CBC/PKCS5Padding";
    private int keySize = -1;
    private int ivSize = -1;
    private SecretKey secretKey;

    boolean paddingUsed = true;


    @PostConstruct
    public void initialize() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, EncryptionFailedException, InvalidAlgorithmParameterException {
        log.debug("Initializing key for token generator");
        try {
            rd.set(new SecureRandom());
            Cipher enCipher = Cipher.getInstance(algorithm);
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
            this.secretKey = keyGen.generateKey();
            enCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            // We have to provide the IV depending on the algorithm used
            // CBC needs an IV, ECB not.
            if (enCipher.getIV()==null) {
                ivSize=-1;
            } else {
                ivSize=enCipher.getIV().length;
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
        }
    }

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
        } catch (NoSuchPaddingException e) {
            log.error("Padding not available "+algorithm);
            throw new EncryptionFailedException(e);
        } catch (InvalidKeyException e) {
            log.error("Bad encryption key");
            throw new EncryptionFailedException(e);
        } catch (NoSuchAlgorithmException e) {
            log.error("Bad encryption algorithm "+algorithm);
            throw new EncryptionFailedException(e);
        } catch (InvalidAlgorithmParameterException e) {
            log.error("Invalid encryption parameters");
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
        } catch (NoSuchPaddingException e) {
            log.error("Padding not available "+algorithm);
            throw new InvalidTokenException(e);
        } catch (InvalidKeyException e) {
            log.error("Invalid decryption key");
            throw new InvalidTokenException(e);
        } catch (NoSuchAlgorithmException e) {
            log.error("Encryption algorithm not available "+algorithm);
            throw new InvalidTokenException(e);
        } catch (InvalidAlgorithmParameterException e) {
            log.error("Invalid encryption parameters");
            throw new InvalidTokenException(e);
        }
    }

    private long createNonce() {
        if (rd.get()==null) {
            rd.set(new SecureRandom());
        }
        return rd.get().nextLong();
    }

    protected byte[] encrypt(TokenData info) throws IOException, BadPaddingException, IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        return doEncrypt(convertToByteArray(info), info.getNonce());
    }

    private byte[] getIv(long nonce) {
        byte[] iv = new byte[ivSize];
        SecureRandom sr = getRandomGenerator();
        sr.setSeed(nonce);
        sr.nextBytes(iv);
        return iv;
    }

    protected byte[] doEncrypt(byte[] data, long nonce) throws BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        Cipher cipher = getEnCipher();
        byte[] encData;
        byte[] iv;
        if (ivSize>0) {
            iv = getIv(nonce);
            cipher.init(Cipher.ENCRYPT_MODE,this.secretKey,new IvParameterSpec(iv));
        } else {
            iv = new byte[0];
            cipher.init(Cipher.ENCRYPT_MODE,this.secretKey);
        }
        if (!paddingUsed && (data.length % cipher.getBlockSize())!=0) {
            int blocks = data.length / cipher.getBlockSize();
            encData = Arrays.copyOf(data, cipher.getBlockSize()*(blocks+1));
        } else {
            encData = data;
        }
        byte[] encrypted = cipher.doFinal(encData);
        // prepending the iv to the token
        return ArrayUtils.addAll(iv,encrypted);
    }

    protected TokenData decrypt(byte[] token) throws BadPaddingException, IllegalBlockSizeException, IOException, ClassNotFoundException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
        Object result = convertFromByteArray(doDecrypt(token));
        if (!(result instanceof TokenData)) {
            throw new InvalidClassException("No TokenData found in decrypted token");
        }
        return (TokenData)result;
    }

    protected byte[] doDecrypt(byte[] encryptedData) throws BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = getDeCipher();
        if (ivSize>0) {
            byte[] iv = Arrays.copyOfRange(encryptedData,0,ivSize);
            cipher.init(Cipher.DECRYPT_MODE,this.secretKey,new IvParameterSpec(iv));
            return cipher.doFinal(encryptedData,ivSize,encryptedData.length-ivSize);
        } else {
            cipher.init(Cipher.DECRYPT_MODE,this.secretKey);
            return cipher.doFinal(encryptedData);
        }
    }

    private SecureRandom getRandomGenerator() {
        if (rd.get()==null) {
            rd.set(new SecureRandom());
        }
        return rd.get();
    }

    private Cipher getEnCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance(algorithm);
    }

    private Cipher getDeCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        return Cipher.getInstance(algorithm);
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