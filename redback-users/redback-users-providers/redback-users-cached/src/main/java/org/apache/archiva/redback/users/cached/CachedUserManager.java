package org.apache.archiva.redback.users.cached;

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

import org.apache.archiva.components.cache.Cache;
import org.apache.archiva.redback.users.User;
import org.apache.archiva.redback.users.UserManagerException;
import org.apache.archiva.redback.users.UserManagerListener;
import org.apache.archiva.redback.users.UserNotFoundException;
import org.apache.archiva.redback.users.UserQuery;
import org.apache.archiva.redback.users.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.List;

/**
 * CachedUserManager
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 */
@Service("userManager#cached")
public class CachedUserManager
    implements UserManager, UserManagerListener
{

    private final Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    @Named(value = "userManager#default")
    private UserManager userImpl;

    @Inject
    @Named(value = "cache#users")
    private Cache<String, User> usersCache;

    @Override
    public boolean isReadOnly()
    {
        return userImpl.isReadOnly();
    }

    @Override
    public User createGuestUser()
        throws UserManagerException
    {
        return userImpl.createGuestUser();
    }

    @Override
    public User addUser( User user )
        throws UserManagerException
    {
        if ( user != null )
        {
            usersCache.remove( user.getUsername() );
        }
        return this.userImpl.addUser( user );
    }

    @Override
    public void addUserManagerListener( UserManagerListener listener )
    {
        this.userImpl.addUserManagerListener( listener );
    }

    @Override
    public void addUserUnchecked( User user )
        throws UserManagerException
    {
        if ( user != null )
        {
            usersCache.remove( user.getUsername() );
        }
        this.userImpl.addUserUnchecked( user );
    }

    @Override
    public User createUser( String username, String fullName, String emailAddress )
        throws UserManagerException
    {
        usersCache.remove( username );
        return this.userImpl.createUser( username, fullName, emailAddress );
    }

    @Override
    public void deleteUser( String username )
        throws UserNotFoundException, UserManagerException
    {
        usersCache.remove( username );
        this.userImpl.deleteUser( username );
    }

    @Override
    public void eraseDatabase()
    {
        try
        {
            this.userImpl.eraseDatabase();
        }
        finally
        {
            usersCache.clear();
        }
    }

    @Override
    public User findUser( String username )
        throws UserNotFoundException, UserManagerException
    {
        if ( GUEST_USERNAME.equals( username ) )
        {
            return getGuestUser();
        }

        User el = usersCache.get( username );
        if ( el != null )
        {
            return el;
        }
        else
        {
            User user = this.userImpl.findUser( username );
            usersCache.put( username, user );
            return user;
        }
    }

    @Override
    public User findUser( String username, boolean useCache )
        throws UserNotFoundException, UserManagerException
    {
        // force use of cache here :-)
        return findUser( username );
    }

    @Override
    public User getGuestUser()
        throws UserNotFoundException, UserManagerException
    {
        User el = usersCache.get( GUEST_USERNAME );
        if ( el != null )
        {
            return el;
        }
        else
        {
            User user = this.userImpl.getGuestUser();
            usersCache.put( GUEST_USERNAME, user );
            return user;
        }
    }

    @Override
    public UserQuery createUserQuery()
    {
        return userImpl.createUserQuery();
    }


    @Override
    public List<? extends User> findUsersByQuery( UserQuery query )
        throws UserManagerException
    {
        log.debug( "NOT CACHED - .findUsersByQuery(UserQuery)" );
        return this.userImpl.findUsersByQuery( query );
    }

    @Override
    public List<? extends User> findUsersByEmailKey( String emailKey, boolean orderAscending )
        throws UserManagerException
    {
        log.debug( "NOT CACHED - .findUsersByEmailKey(String, boolean)" );
        return this.userImpl.findUsersByEmailKey( emailKey, orderAscending );
    }

    @Override
    public List<? extends User> findUsersByFullNameKey( String fullNameKey, boolean orderAscending )
        throws UserManagerException
    {
        log.debug( "NOT CACHED - .findUsersByFullNameKey(String, boolean)" );
        return this.userImpl.findUsersByFullNameKey( fullNameKey, orderAscending );
    }

    @Override
    public List<? extends User> findUsersByUsernameKey( String usernameKey, boolean orderAscending )
        throws UserManagerException
    {
        log.debug( "NOT CACHED - .findUsersByUsernameKey(String, boolean)" );
        return this.userImpl.findUsersByUsernameKey( usernameKey, orderAscending );
    }

    @Override
    public String getId()
    {
        return "cached";
    }

    @Override
    public List<? extends User> getUsers()
        throws UserManagerException
    {
        log.debug( "NOT CACHED - .getUsers()" );
        return this.userImpl.getUsers();
    }

    @Override
    public List<? extends User> getUsers( boolean orderAscending )
        throws UserManagerException
    {
        log.debug( "NOT CACHED - .getUsers(boolean)" );
        return this.userImpl.getUsers( orderAscending );
    }

    @Override
    public void removeUserManagerListener( UserManagerListener listener )
    {
        this.userImpl.removeUserManagerListener( listener );
    }

    @Override
    public User updateUser( User user )
        throws UserNotFoundException, UserManagerException
    {
        return updateUser( user, false );
    }

    @Override
    public User updateUser( User user, boolean passwordChangeRequired )
        throws UserNotFoundException, UserManagerException
    {
        if ( user != null )
        {
            usersCache.remove( user.getUsername() );
        }
        return this.userImpl.updateUser( user, passwordChangeRequired );
    }

    @Override
    public boolean userExists( String userName )
        throws UserManagerException
    {
        if ( usersCache.hasKey( userName ) )
        {
            return true;
        }

        return this.userImpl.userExists( userName );
    }

    @Override
    public void userManagerInit( boolean freshDatabase )
    {
        if ( userImpl != null )
        {
            ( (UserManagerListener) this.userImpl ).userManagerInit( freshDatabase );
        }

        usersCache.clear();
    }

    @Override
    public void userManagerUserAdded( User user )
    {
        if ( userImpl != null )
        {
            ( (UserManagerListener) this.userImpl ).userManagerUserAdded( user );
        }

        if ( user != null )
        {
            usersCache.remove( user.getUsername() );
        }
    }

    @Override
    public void userManagerUserRemoved( User user )
    {
        if ( userImpl != null )
        {
            ( (UserManagerListener) this.userImpl ).userManagerUserRemoved( user );
        }

        if ( user != null )
        {
            usersCache.remove( user.getUsername() );
        }
    }

    @Override
    public void userManagerUserUpdated( User user )
    {
        if ( userImpl != null )
        {
            ( (UserManagerListener) this.userImpl ).userManagerUserUpdated( user );
        }

        if ( user != null )
        {
            usersCache.remove( user.getUsername() );
        }
    }

    public UserManager getUserImpl()
    {
        return userImpl;
    }

    public void setUserImpl( UserManager userImpl )
    {
        this.userImpl = userImpl;
    }

    public Cache<String, User> getUsersCache()
    {
        return usersCache;
    }

    public void setUsersCache( Cache<String, User> usersCache )
    {
        this.usersCache = usersCache;
    }

    @Override
    public void initialize()
    {
        // no op configurable impl do the job
    }

    @Override
    public boolean isFinalImplementation()
    {
        return false;
    }

    @Override
    public String getDescriptionKey()
    {
        return "archiva.redback.usermanager.cached";
    }

    /**
     * Clears the users cache
     */
    public void clearCache() {
        usersCache.clear( );
    }

    @PreDestroy
    public void shutdown() {
        this.clearCache( );
    }
}
