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

import org.junit.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test the TokenManager implementation. Uses no spring dependencies.
 *
 *  Created by Martin Stockhammer on 11.02.17.
 */
public class TokenManagerTest {

    @Test
    public void initialize() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, EncryptionFailedException, InvalidAlgorithmParameterException {
        TokenManager tokenManager = new TokenManager();
        tokenManager.initialize();
    }

    @Test
    public void encryptToken() throws Exception {
        TokenManager tokenManager = new TokenManager();
        tokenManager.initialize();
        assertEquals(tokenManager.getAlgorithm(),"AES/ECB/PKCS5Padding");
        assertEquals(tokenManager.getKeySize(), -1);
        String token = tokenManager.encryptToken("testuser01",1000);
        assertNotNull(token);
        assertTrue("Token size too low",token.length()>300);

    }

    @Test
    public void decryptToken() throws Exception {
        TokenManager tokenManager = new TokenManager();
        tokenManager.initialize();
        String token = tokenManager.encryptToken("testuser00003",1000);
        assertNotNull(token);
        assertTrue("Token size too low",token.length()>300);
        TokenData tokenData = tokenManager.decryptToken(token);
        assertNotNull(tokenData);
        assertEquals("testuser00003", tokenData.getUser());
        assertTrue(tokenData.isValid());

    }

    @Test
    public void decryptExpiredToken() throws Exception {
        TokenManager tokenManager = new TokenManager();
        tokenManager.initialize();
        SimpleTokenData sToken = new SimpleTokenData("testuser00003", 0, 1345455);
        String token = tokenManager.encryptToken(sToken);
        assertNotNull(token);
        assertTrue("Token size too low",token.length()>300);
        TokenData tokenData = tokenManager.decryptToken(token);
        assertNotNull(tokenData);
        assertEquals("testuser00003", tokenData.getUser());
        assertFalse(tokenData.isValid());

    }

    @Test(expected = InvalidTokenException.class)
    public void decryptInvalidToken() throws Exception {
        TokenManager tokenManager = new TokenManager();
        tokenManager.initialize();
        SimpleTokenData sToken = new SimpleTokenData("testuser00003", 0, 1345455);
        String token = tokenManager.encryptToken(sToken);
        assertNotNull(token);
        assertTrue("Token size too low",token.length()>300);
        tokenManager.initialize();
        tokenManager.decryptToken(token);

    }

    @Test
    public void decryptTokenWithDifferentAlgorithm() throws Exception {
        TokenManager tokenManager = new TokenManager();
        tokenManager.setAlgorithm("DES/ECB/PKCS5Padding");
        tokenManager.initialize();
        String token = tokenManager.encryptToken("testuser00005",2000);
        assertNotNull(token);
        assertTrue("Token size too low",token.length()>300);
        TokenData tokenData = tokenManager.decryptToken(token);
        assertNotNull(tokenData);
        assertEquals("testuser00005", tokenData.getUser());
        assertTrue(tokenData.isValid());


        tokenManager.setAlgorithm("DES/ECB/NoPadding");
        tokenManager.initialize();
        token = tokenManager.encryptToken("testuser00006",2000);
        assertNotNull(token);
        assertTrue("Token size too low",token.length()>300);
        tokenData = tokenManager.decryptToken(token);
        assertNotNull(tokenData);
        assertEquals("testuser00006", tokenData.getUser());
        assertTrue(tokenData.isValid());

    }

    @Test
    public void nativeEncryption() throws EncryptionFailedException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidTokenException, BadPaddingException, IllegalBlockSizeException {
        TokenManager tokenManager = new TokenManager();
        tokenManager.setAlgorithm("DES/CBC/PKCS5Padding");
        tokenManager.setKeySize(56);
        tokenManager.initialize();
        byte[] data = { 1, 5, 12, 18, 124, 44, 88, -28, -44};
        byte[] token = tokenManager.doEncrypt(data);
        assertNotNull(token);
        byte[] tokenData = tokenManager.doDecrypt(token);
        assertNotNull(tokenData);
        assertArrayEquals(data, tokenData);


        tokenManager.setAlgorithm("AES/CBC/NoPadding");
        tokenManager.setKeySize(128);
        tokenManager.initialize();
        token = tokenManager.doEncrypt(data);
        assertNotNull(token);
        // Without padding the decrypted value is a multiple of the block size.
        tokenData = Arrays.copyOf(tokenManager.doDecrypt(token), data.length);
        assertNotNull(tokenData);
        assertArrayEquals(data, tokenData);

    }

}