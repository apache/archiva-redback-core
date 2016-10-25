package org.apache.archiva.redback.users.jpa;

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

import org.apache.archiva.redback.policy.UserSecurityPolicy;
import org.apache.archiva.redback.users.*;
import org.apache.archiva.redback.users.jpa.model.JpaUser;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by martin on 20.09.16.
 */
@Service("userManager#jpa")
public class JpaUserManager extends AbstractUserManager {


    @PersistenceContext(unitName = "redback-jpa")
    EntityManager em;

    @Inject
    private UserSecurityPolicy userSecurityPolicy;

    // JpaUserManager is a singleton and initialization should be thread safe
    private AtomicBoolean initialized = new AtomicBoolean(false);


    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getId() {
        return "jpa";
    }

    private EntityManager getEm() {
        if (initialized.compareAndSet(false,true)) {
            Query q = em.createQuery("SELECT COUNT(u.username) FROM JpaUser u");
            boolean dbInit = q.getFirstResult()==0;
            fireUserManagerInit(dbInit);
        }
        return em;
    }


    @Override
    public User createUser(String username, String fullName, String emailAddress) throws UserManagerException {
        JpaUser user = new JpaUser();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setEmail(emailAddress);
        return user;
    }

    @Override
    public UserQuery createUserQuery() {
        return new JpaUserQuery();
    }

    @Override
    public List<User> getUsers() throws UserManagerException {
        final EntityManager em = getEm();
        Query q= em.createQuery("SELECT x from JpaUser x");
        return q.getResultList();
    }

    @Override
    public List<User> getUsers(boolean orderAscending) throws UserManagerException {
        final EntityManager em = getEm();
        final String orderFlag = orderAscending ? "ASC" : "DESC";
        Query q = em.createQuery("SELECT u FROM JpaUser u ORDER BY u.username "+orderFlag);
        return q.getResultList();
    }

    @Transactional
    @Override
    public User addUser(User user) throws UserManagerException {
        EntityManager em = getEm();
        if ( !( user instanceof JpaUser ) )
        {
            throw new UserManagerException( "Unable to Add User. User object " + user.getClass().getName() +
                    " is not an instance of " + JpaUser.class.getName() );
        }

        if ( StringUtils.isEmpty( user.getUsername() ) )
        {
            throw new IllegalStateException(
                    Messages.getString( "user.manager.cannot.add.user.without.username" ) ); //$NON-NLS-1$
        }

        userSecurityPolicy.extensionChangePassword( user );

        fireUserManagerUserAdded( user );

        // TODO: find a better solution
        // workaround for avoiding the admin from providing another password on the next login after the
        // admin account has been created
        // extensionChangePassword by default sets the password change status to false
        if ( "admin".equals( user.getUsername() ) )
        {
            user.setPasswordChangeRequired( false );
        }
        else
        {
            user.setPasswordChangeRequired( true );
        }
        if (user.getLastPasswordChange()==null) {
            user.setLastPasswordChange(new Date());
        }
        em.persist((JpaUser)user);
        return user;
    }

    @Transactional
    @Override
    public User updateUser(User user) throws UserNotFoundException, UserManagerException {
        return updateUser(user, false);
    }

    @Override
    public User findUser(String username) throws UserNotFoundException, UserManagerException {
        if (username==null) {
            throw new UserNotFoundException("Username was <null>");
        }
        final EntityManager em = getEm();
        TypedQuery<JpaUser> q = em.createQuery("SELECT u FROM JpaUser u WHERE LOWER(u.username)=:uname", JpaUser.class);
        q.setParameter("uname",username.toLowerCase());
        User result;
        try {
            result = q.getSingleResult();
        } catch (NoResultException ex ) {
            throw new UserNotFoundException(ex);
        }
        return result;
    }

    @Override
    public User findUser(String username, boolean useCache) throws UserNotFoundException, UserManagerException {
        return findUser(username);
    }

    @Override
    public List<User> findUsersByUsernameKey(String usernameKey, boolean orderAscending) throws UserManagerException {
        return findUsers("username",usernameKey,"username",orderAscending);
    }

    @Override
    public List<User> findUsersByFullNameKey(String fullNameKey, boolean orderAscending) throws UserManagerException {
        return findUsers("fullName",fullNameKey,"username",orderAscending);
    }

    @Override
    public List<User> findUsersByEmailKey(String emailKey, boolean orderAscending) throws UserManagerException {
        return findUsers("email",emailKey,"username", orderAscending);
    }

    @Override
    public List<User> findUsersByQuery(final UserQuery queryParam) throws UserManagerException {
        final EntityManager em = getEm();
        final JpaUserQuery query = (JpaUserQuery)queryParam;
        String orderByAttribute = "";
        if (UserQuery.ORDER_BY_EMAIL.equals(query.getOrderBy())) {
            orderByAttribute="email";
        } else if (UserQuery.ORDER_BY_FULLNAME.equals(query.getOrderBy())) {
            orderByAttribute="fullName";
        } else if (UserQuery.ORDER_BY_USERNAME.equals(query.getOrderBy())) {
            orderByAttribute="username";
        } else {
            throw new IllegalArgumentException("Unknown order attribute "+query.getOrderBy());
        }
        StringBuilder sb = new StringBuilder("SELECT u FROM JpaUser u ");
        if (query.hasUsername()||query.hasFullName()||query.hasEmail()) {
            sb.append("WHERE ");
        }
        boolean checkBefore = false;
        if (query.hasUsername()) {
            sb.append("LOWER(u.username) LIKE :username ");
            checkBefore=true;
        }
        if (query.hasEmail()) {
            if (checkBefore) {
                sb.append("AND ");
            }
            checkBefore=true;
            sb.append("LOWER(u.email) LIKE :email ");
        }
        if (query.hasFullName()) {
            if (checkBefore) {
                sb.append("AND ");
            }
            sb.append("LOWER(u.fullName) LIKE :fullname ");
        }
        if (query.getOrderBy()!=null && !"".equals(query.getOrderBy())) {
            sb.append("ORDER BY u.").append(orderByAttribute).append(query.isAscending() ? " ASC" : " DESC");
        }
        TypedQuery<User> q = em.createQuery(sb.toString(), User.class);
        if (query.hasUsername()) {
            q.setParameter("username", "%"+query.getUsername().toLowerCase()+"%");
        }
        if (query.hasEmail()) {
            q.setParameter("email", "%"+query.getEmail().toLowerCase()+"%");
        }
        if (query.hasFullName()) {
            q.setParameter("fullname", "%"+query.getFullName().toLowerCase()+"%");
        }
        q.setFirstResult((int)query.getFirstResult()).setMaxResults((int)query.getMaxResults());
        return q.getResultList();
    }

    private List<User> findUsers(final String attribute, final String pattern,
                                 final String orderAttribute, final boolean orderAscending)  {
        final EntityManager em = getEm();
        StringBuilder sb = new StringBuilder("SELECT u FROM JpaUser u WHERE LOWER(u.");
        sb.append(attribute).append(") LIKE :patternvalue ORDER BY u.").append(orderAttribute);
        sb.append(orderAscending ? " ASC" : " DESC");
        TypedQuery<User> q = em.createQuery(sb.toString(),User.class);
        q.setParameter("patternvalue","%"+pattern.toLowerCase()+"%");
        return q.getResultList();
    }

    @Override
    public boolean userExists(String principal) throws UserManagerException  {
        EntityManager em = getEm();
        JpaUser user = em.find(JpaUser.class, principal);
        return user != null;
    }



    @Transactional
    @Override
    public void deleteUser(String username) throws UserNotFoundException, UserManagerException {
        final EntityManager em = getEm();
        User u = findUser(username);
        if (u.isPermanent()) {
            throw new PermanentUserException("User "+username+" cannot be deleted");
        }
        em.remove(u);
        fireUserManagerUserRemoved(u);
    }

    @Transactional
    @Override
    public void addUserUnchecked(User user) throws UserManagerException {
        log.info("addUserUnchecked "+user.getUsername());
        if ( !( user instanceof JpaUser ) )
        {
            throw new UserManagerException( "Unable to Add User. User object " + user.getClass().getName() +
                    " is not an instance of " + JpaUser.class.getName() );
        }

        if ( org.codehaus.plexus.util.StringUtils.isEmpty( user.getUsername() ) )
        {
            throw new IllegalStateException(
                    Messages.getString( "user.manager.cannot.add.user.without.username" ) ); //$NON-NLS-1$
        }

        TypedQuery<JpaUser> q = em.createQuery("SELECT u FROM JpaUser u", JpaUser.class);
        for (JpaUser u : q.getResultList()) {
            log.info("USER FOUND: "+u.getUsername());
        }
        log.info("NEW USER "+user.getUsername());
        em.persist((JpaUser)user);

    }

    @Transactional
    @Override
    public void eraseDatabase() {
        EntityManager em = getEm();
        TypedQuery<JpaUser> q = em.createQuery("SELECT u FROM JpaUser u", JpaUser.class);
        for (JpaUser u : q.getResultList()) {
            u.getPreviousEncodedPasswords().clear();
        }
        em.flush();
        Query qd = em.createQuery("DELETE FROM JpaUser u");
        qd.executeUpdate();
        em.clear();

    }

    @Transactional
    @Override
    public User updateUser(User user, boolean passwordChangeRequired) throws UserNotFoundException, UserManagerException {
        if ( !( user instanceof JpaUser ) )
        {
            throw new UserManagerException( "Unable to update user. User object " + user.getClass().getName() +
                    " is not an instance of " + JpaUser.class.getName() );
        }
        if ( StringUtils.isNotEmpty( user.getPassword() ) )
        {
            userSecurityPolicy.extensionChangePassword( user, passwordChangeRequired );
        }
        JpaUser jpaUser = (JpaUser) user;
        final EntityManager em = getEm();
        jpaUser = em.merge(jpaUser);
        fireUserManagerUserUpdated(jpaUser);
        return jpaUser;
    }

    @Override
    public String getDescriptionKey() {
        return "archiva.redback.usermanager.jpa";
    }


    @Override
    public boolean isFinalImplementation() {
        return true;
    }


    // Override to add transactional annotation
    @Transactional
    @Override
    public User createGuestUser() throws UserManagerException {
        return super.createGuestUser();
    }
}
