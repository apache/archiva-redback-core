package org.apache.archiva.redback.keys.jpa;

/*
 * Copyright 2001-2016 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.archiva.redback.keys.AbstractKeyManager;
import org.apache.archiva.redback.keys.AuthenticationKey;
import org.apache.archiva.redback.keys.KeyManagerException;
import org.apache.archiva.redback.keys.KeyNotFoundException;
import org.apache.archiva.redback.keys.jpa.model.JpaAuthenticationKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.*;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Key Manager Implementation for JPA.
 *
 * Uses an injected Entity Manager.
 *
 * @author <a href="mailto:martin_s@apache.org">Martin Stockhammer</a>
 */
@Service( "keyManager#jpa" )
public class JpaKeyManager extends AbstractKeyManager {

    @PersistenceContext(unitName = "redback-jpa")
    EntityManager em;

    // JpaUserManager is a singleton and initialization should be thread safe
    private AtomicBoolean initialized = new AtomicBoolean(false);

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    private EntityManager getEm() {
        if (initialized.compareAndSet(false,true)) {
            Query q = em.createQuery("SELECT COUNT(u.key) FROM JpaAuthenticationKey u");
            boolean dbInit = q.getFirstResult()==0;
        }
        return em;
    }

    public String getId()
    {
        return "JPA Key Manager - " + this.getClass().getName();
    }


    @Override
    @Transactional
    public AuthenticationKey createKey( String principal, String purpose, int expirationMinutes )
            throws KeyManagerException
    {
        JpaAuthenticationKey authkey = new JpaAuthenticationKey();
        authkey.setKey( super.generateUUID() );
        authkey.setForPrincipal( principal );
        authkey.setPurpose( purpose );

        Calendar now = getNowGMT();
        authkey.setDateCreated( now.getTime() );

        if ( expirationMinutes >= 0 )
        {
            Calendar expiration = getNowGMT();
            expiration.add( Calendar.MINUTE, expirationMinutes );
            authkey.setDateExpires( expiration.getTime() );
        }

        return addKey( authkey );
    }

    @Transactional
    @Override
    public AuthenticationKey addKey(AuthenticationKey key) {
        final EntityManager em = getEm();
        AuthenticationKey mergedKey = em.merge((JpaAuthenticationKey)key);
        return mergedKey;
    }

    @Transactional
    @Override
    public void eraseDatabase()
    {
        final EntityManager em = getEm();
        Query q = em.createQuery("DELETE FROM JpaAuthenticationKey k");
        q.executeUpdate();
    }

    @Transactional
    @Override
    public AuthenticationKey findKey(final String key) throws KeyNotFoundException, KeyManagerException {
        final EntityManager em = getEm();
        if ( StringUtils.isEmpty( key ) )
        {
            throw new KeyNotFoundException( "Empty key not found." );
        }

        try
        {
            TypedQuery<JpaAuthenticationKey> q =
                    em.createQuery("SELECT k FROM JpaAuthenticationKey k WHERE k.key = :key",JpaAuthenticationKey.class);
            q.setParameter("key",key);
            JpaAuthenticationKey authkey = q.getSingleResult();
            if ( authkey == null )
            {
                throw new KeyNotFoundException( "Key [" + key + "] not found." );
            }
            assertNotExpired( authkey );

            return authkey;
        } catch (NoResultException ex) {
            throw new KeyNotFoundException("Key [" + key + "] not found.");
        } catch (KeyNotFoundException ex) {
            throw ex;
        } catch (Throwable ex) {
            log.error("Error while trying to retrieve JpaAuthenticationKey {}", key);
            throw new KeyManagerException("Error while retrieving key "+key+": "+ex.getMessage(), ex);
        }
    }

    @Transactional
    @Override
    protected void assertNotExpired(AuthenticationKey authkey) throws KeyManagerException {
        super.assertNotExpired(authkey);
    }

    @Transactional
    @Override
    public void deleteKey(AuthenticationKey key) throws KeyManagerException {
        final EntityManager em = getEm();
        em.remove((JpaAuthenticationKey)key);
    }

    @Transactional
    @Override
    public void deleteKey(String key) throws KeyManagerException {
        try {
            JpaAuthenticationKey foundKey = (JpaAuthenticationKey)findKey(key);
            em.remove(foundKey);
        } catch (KeyNotFoundException ex) {
            // Ignore
        } catch (Exception ex) {
            log.error("Error occured while trying to find key {}: {}", key, ex.getMessage());
            throw new KeyManagerException("Error while retrieving key "+key, ex);
        }
    }

    @Override
    public List<AuthenticationKey> getAllKeys() {
        final EntityManager em = getEm();
        TypedQuery<AuthenticationKey> q= em.createQuery("SELECT x from JpaAuthenticationKey x", AuthenticationKey.class);
        return q.getResultList();
    }




}
