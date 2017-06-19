package org.apache.archiva.redback.rbac.jpa;

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

import org.apache.archiva.redback.rbac.*;
import org.apache.archiva.redback.rbac.jpa.model.*;
import org.apache.openjpa.persistence.Type;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by martin on 20.09.16.
 */
@Service("rbacManager#jpa")
public class JpaRbacManager extends AbstractRBACManager  {


    @PersistenceContext(unitName = "redback-jpa")
    EntityManager em;


    private AtomicBoolean initialized = new AtomicBoolean(false);


    public void setEntityManager(EntityManager em) {
        this.em = em;
    }



    @Override
    public Role createRole(String name) {
        JpaRole role = new JpaRole();
        role.setName(name);
        return role;
    }

    @Transactional
    @Override
    public Role saveRole(Role role) throws RbacObjectInvalidException, RbacManagerException {
        RBACObjectAssertions.assertValid( role );
        final EntityManager em = getEm();
        Role mergedRole = em.merge(role);
        fireRbacRoleSaved(mergedRole);
        for (Permission perm : mergedRole.getPermissions()) {
            fireRbacPermissionSaved(perm);
        }
        return mergedRole;
    }

    @Transactional
    @Override
    public Map<String, List<Permission>> getAssignedPermissionMap(String principal) throws RbacManagerException {
        return super.getAssignedPermissionMap(principal);
    }

    @Transactional
    @Override
    public Map<String, Role> getChildRoles(Role role) throws RbacManagerException {
        return super.getChildRoles(role);
    }

    @Transactional
    @Override
    public void addChildRole(Role role, Role childRole) throws RbacObjectInvalidException, RbacManagerException {
        super.addChildRole(role, childRole);
    }

    @Transactional
    @Override
    public void saveRoles(Collection<Role> roles) throws RbacObjectInvalidException, RbacManagerException {
        if ( roles == null )
        {
            // Nothing to do.
            return;
        }

        final EntityManager em = getEm();
        List<Role> merged = new ArrayList<Role>();
        for (Role role : roles ) {
            RBACObjectAssertions.assertValid(role);
            merged.add(em.merge(role));
        }
        for (Role role : merged) {
            fireRbacRoleSaved(role);
        }
    }


    @Override
    public Role getRole(String roleName) throws RbacObjectNotFoundException, RbacManagerException {
        final EntityManager em = getEm();
        TypedQuery<JpaRole> q = em.createQuery("SELECT r FROM JpaRole  r WHERE r.name = :rolename", JpaRole.class);
        q.setParameter("rolename",roleName);
        Role role;
        try {
            role = q.getSingleResult();
        } catch (NoResultException ex) {
            log.warn("Role {} not found", roleName);
            throw new RbacObjectNotFoundException("Role not found "+roleName);
        }
        return role;
    }

    @Override
    public List<Role> getAllRoles() throws RbacManagerException {
        final EntityManager em = getEm();
        Query q = em.createQuery("SELECT r FROM JpaRole r");
        return q.getResultList();
    }

    @Transactional
    @Override
    public void removeRole(Role role) throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException {
        RBACObjectAssertions.assertValid(role);
        if (!(role instanceof JpaRole)) {
            throw new RbacObjectInvalidException("Role object is not instance of JpaRole");
        }
        if ( role.isPermanent() )
        {
            throw new RbacPermanentException( "Unable to delete permanent role [" + role.getName() + "]" );
        }
        final EntityManager em = getEm();
        JpaRole myRole = em.find(JpaRole.class, role.getName());
        if (myRole == null) {
            throw new RbacObjectNotFoundException("Role not found "+role.getName());
        }
        myRole.setPermissions(new ArrayList<Permission>());
        em.remove(myRole);
        fireRbacRoleRemoved(myRole);
    }

    @Override
    public Permission createPermission(String name) throws RbacManagerException {
        JpaPermission permission = new JpaPermission();
        permission.setName(name);
        return permission;
    }

    @Override
    public Permission createPermission(String name, String operationName, String resourceIdentifier) throws RbacManagerException {
        JpaPermission permission = new JpaPermission();
        permission.setName(name);
        Operation op;
        try {
            op = getOperation(operationName);
        } catch (RbacObjectNotFoundException ex) {
            op = createOperation(operationName);
        }
        permission.setOperation(op);
        Resource res;
        try {
            res = getResource(resourceIdentifier);
        } catch (RbacObjectNotFoundException ex) {
            res = createResource(resourceIdentifier);
        }
        permission.setResource(res);
        return permission;
    }

    @Transactional
    @Override
    public Permission savePermission(Permission permission) throws RbacObjectInvalidException, RbacManagerException {
        RBACObjectAssertions.assertValid(permission);
        if (!(permission instanceof JpaPermission)) {
            throw new RbacObjectInvalidException("The permission object ist not instance of JpaPermission");
        }
        final EntityManager em = getEm();
        Permission savedPermission = em.merge(permission);
        fireRbacPermissionSaved(savedPermission);
        return savedPermission;
    }

    @Override
    public Permission getPermission(String permissionName) throws RbacObjectNotFoundException, RbacManagerException {
        final EntityManager em = getEm();
        TypedQuery<Permission> q = em.createQuery("SELECT p FROM JpaPermission p WHERE p.name=:name", Permission.class);
        q.setParameter("name",permissionName);
        Permission res = q.getSingleResult();
        if (res==null) {
            throw new RbacObjectNotFoundException("Permission "+permissionName+" not found");
        }
        return res;
    }

    @Override
    public List<Permission> getAllPermissions() throws RbacManagerException {
        final EntityManager em = getEm();
        TypedQuery<JpaPermission> q = em.createQuery("SELECT p FROM JpaPermission p",JpaPermission.class);
        return (List<Permission>)(List<?>)q.getResultList();
    }

    @Transactional
    @Override
    public void removePermission(Permission permission) throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException {
        RBACObjectAssertions.assertValid(permission);
        if (!(permission instanceof JpaPermission)) {
            throw new RbacObjectInvalidException("The permission object is not JpaPermission object");
        }
        if ( permission.isPermanent() )
        {
            throw new RbacPermanentException( "Unable to delete permanent permission [" + permission.getName() + "]" );
        }
        final EntityManager em = getEm();
        JpaPermission p = em.find(JpaPermission.class, permission.getName());
        if (p == null) {
            throw new RbacObjectNotFoundException("Permission " + permission.getName() + " not found");
        }
        em.remove(p);
        fireRbacPermissionRemoved(p);
    }

    @Override
    public Operation createOperation(String name) throws RbacManagerException {
        JpaOperation op = new JpaOperation();
        op.setName(name);
        return op;
    }

    @Transactional
    @Override
    public Operation saveOperation(Operation operation) throws RbacObjectInvalidException, RbacManagerException {
        RBACObjectAssertions.assertValid(operation);
        if (!(operation instanceof JpaOperation)) {
            throw new RbacObjectInvalidException("Operation is not JpaOperation object");
        }
        final EntityManager em = getEm();
        Operation savedOperation = em.merge(operation);
        return savedOperation;
    }

    @Override
    public Operation getOperation(String operationName) throws RbacObjectNotFoundException, RbacManagerException {
        final EntityManager em = getEm();
        Operation op = em.find(JpaOperation.class,operationName);
        if(op==null) {
            throw new RbacObjectNotFoundException("Operation "+operationName+" not found");
        }
        return op;
    }

    @Override
    public List<Operation> getAllOperations() throws RbacManagerException {
        final EntityManager em = getEm();
        Query q = em.createQuery("SELECT o FROM JpaOperation o");
        return q.getResultList();
    }

    @Transactional
    @Override
    public void removeOperation(Operation operation) throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException {
        RBACObjectAssertions.assertValid(operation);
        if (!(operation instanceof JpaOperation)) {
            throw new RbacObjectInvalidException("Operation is not JpaOperation object");
        }
        if ( operation.isPermanent() )
        {
            throw new RbacPermanentException( "Unable to delete permanent operation [" + operation.getName() + "]" );
        }
        final EntityManager em = getEm();
        JpaOperation op = em.find(JpaOperation.class, operation.getName());
        if (op==null) {
            throw new RbacObjectNotFoundException("Operation not found "+operation.getName());
        }
        em.remove(op);
    }

    @Override
    public Resource createResource(String identifier) throws RbacManagerException {
        JpaResource resource = new JpaResource();
        resource.setIdentifier(identifier);
        return resource;
    }

    @Transactional
    @Override
    public Resource saveResource(Resource resource) throws RbacObjectInvalidException, RbacManagerException {
        RBACObjectAssertions.assertValid(resource);
        if (!(resource instanceof JpaResource)) {
            throw new RbacObjectInvalidException("Resource is not JpaResource");
        }
        final EntityManager em = getEm();
        Resource savedResource = em.merge(resource);
        return savedResource;
    }

    // Overriding to add the transactional attribute here
    @Transactional
    @Override
    public Resource getGlobalResource()
            throws RbacManagerException
    {
        return super.getGlobalResource();
    }

    @Override
    public Resource getResource(String resourceIdentifier) throws RbacObjectNotFoundException, RbacManagerException {
        final EntityManager em = getEm();
        Resource r = em.find(JpaResource.class,resourceIdentifier);
        if (r==null) {
            throw new RbacObjectNotFoundException("Resource "+resourceIdentifier+" not found");
        }
        return r;
    }

    @Override
    public List<Resource> getAllResources() throws RbacManagerException {
        final EntityManager em = getEm();
        TypedQuery<JpaResource> q = em.createQuery("SELECT r FROM JpaResource r",JpaResource.class);
        return (List<Resource>)(List<?>)q.getResultList();
    }

    @Transactional
    @Override
    public void removeResource(Resource resource) throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException {
        RBACObjectAssertions.assertValid(resource);
        if (!(resource instanceof JpaResource)) {
            throw new RbacObjectInvalidException("Resource is not JpaResource");
        }
        if (resource.isPermanent()) {
            throw new RbacObjectInvalidException("Unable to delete permanent resource ["+resource.getIdentifier()+ "]");
        }
        final EntityManager em = getEm();
        JpaResource res = em.find(JpaResource.class, resource.getIdentifier());
        if (res==null) {
            throw new RbacObjectNotFoundException("Resource "+resource.getIdentifier()+" not found");
        }
        em.remove(res);
    }

    @Override
    public UserAssignment createUserAssignment(String principal) throws RbacManagerException {
        JpaUserAssignment ua = new JpaUserAssignment();
        ua.setPrincipal(principal);
        return ua;
    }

    @Transactional
    @Override
    public UserAssignment saveUserAssignment(UserAssignment userAssignment) throws RbacObjectInvalidException, RbacManagerException {
        RBACObjectAssertions.assertValid(userAssignment);
        if (!(userAssignment instanceof JpaUserAssignment)) {
            throw new RbacObjectInvalidException("Cannto save object that is not JpaUserAssignment");
        }
        final EntityManager em = getEm();
        UserAssignment savedAssignment = em.merge(userAssignment);
        fireRbacUserAssignmentSaved(savedAssignment);
        return savedAssignment;
    }

    @Override
    public UserAssignment getUserAssignment(String principal) throws RbacObjectNotFoundException, RbacManagerException {
        final EntityManager em = getEm();
        UserAssignment ua = em.find(JpaUserAssignment.class, principal);
        if (ua==null) {
            throw new RbacObjectNotFoundException("User assignment not found "+principal);
        }
        return ua;
    }

    @Override
    public List<UserAssignment> getAllUserAssignments() throws RbacManagerException {
        final EntityManager em = getEm();
        Query q = em.createQuery("SELECT ua FROM JpaUserAssignment ua");
        return q.getResultList();
    }

    @Override
    public List<UserAssignment> getUserAssignmentsForRoles(Collection<String> roleNames) throws RbacManagerException {
        try {
            final EntityManager em = getEm();
            Query q = em.createQuery("SELECT ua FROM JpaUserAssignment ua WHERE ua.roleNames IN :roles");
            q.setParameter("roles", roleNames);
            return q.getResultList();
        } catch (Exception ex) {
            log.error("Query failed: {}",ex.getMessage(),ex);
            if (log.isDebugEnabled()) {
                ex.printStackTrace();
            }
            throw new RbacManagerException(ex.getMessage(),ex);
        }
    }

    @Transactional
    @Override
    public void removeUserAssignment(UserAssignment userAssignment) throws RbacObjectNotFoundException, RbacObjectInvalidException, RbacManagerException {
        RBACObjectAssertions.assertValid(userAssignment);
        if (userAssignment.isPermanent()) {
            throw new RbacObjectInvalidException("Cannot remove permanent object "+userAssignment.getPrincipal());
        }
        final EntityManager em = getEm();
        JpaUserAssignment ua = em.find(JpaUserAssignment.class, userAssignment.getPrincipal());
        if (ua==null) {
            throw new RbacObjectNotFoundException("User assignment not found "+userAssignment.getPrincipal());
        }
        em.remove(ua);
        fireRbacUserAssignmentRemoved(userAssignment);
    }

    @Transactional
    @Override
    public void eraseDatabase() {
        final EntityManager em = getEm();
        // Deletion is a bit tricky, because the JPA bulk delete queries do not cascade
        // or keep foreign keys into account. 
        TypedQuery<JpaPermission> tqp = em.createQuery("SELECT r FROM JpaPermission r",JpaPermission.class);
        for(JpaPermission p : tqp.getResultList()) {
            p.setOperation(null);
            p.setResource(null);
        }
        TypedQuery<JpaRole> tqr = em.createQuery("SELECT r FROM JpaRole r",JpaRole.class);
        for (JpaRole r : tqr.getResultList()) {
            r.getPermissions().clear();
        }
        em.flush();
        TypedQuery<JpaOperation> tqo = em.createQuery("SELECT o FROM JpaOperation o",JpaOperation.class);
        for(JpaOperation o : tqo.getResultList()) {
            em.remove(o);
        }
        TypedQuery<JpaResource> tqre = em.createQuery("SELECT re FROM JpaResource re",JpaResource.class);
        for(JpaResource re : tqre.getResultList()) {
            em.remove(re);
        }
        for (JpaPermission p : tqp.getResultList()) {
            em.remove(p);
        }
        for (JpaRole r : tqr.getResultList()) {
            em.remove(r);
        }
        TypedQuery<JpaUserAssignment> tqu = em.createQuery("SELECT ua FROM JpaUserAssignment ua", JpaUserAssignment.class);
        for(JpaUserAssignment ua : tqu.getResultList()) {
            em.remove(ua);
        }
        em.flush();
        em.clear();

    }

    @Override
    public String getDescriptionKey() {
            return "archiva.redback.rbacmanager.jpa";
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    private EntityManager getEm() {
        if (initialized.compareAndSet(false, true)) {
            Query q = em.createQuery("SELECT COUNT(r.name) FROM JpaRole r");
            boolean dbInit = q.getFirstResult()==0;
            fireRbacInit(dbInit);
        }
        return em;
    }

    @Override
    public boolean isFinalImplementation() {
        return true;
    }
}
